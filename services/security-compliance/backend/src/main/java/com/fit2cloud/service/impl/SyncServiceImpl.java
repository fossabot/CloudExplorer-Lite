package com.fit2cloud.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.mapping.NestedProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fit2cloud.base.entity.CloudAccount;
import com.fit2cloud.base.entity.JobRecord;
import com.fit2cloud.base.entity.JobRecordResourceMapping;
import com.fit2cloud.base.service.IBaseCloudAccountService;
import com.fit2cloud.base.service.IBaseJobRecordResourceMappingService;
import com.fit2cloud.common.constants.JobStatusConstants;
import com.fit2cloud.common.constants.JobTypeConstants;
import com.fit2cloud.common.job_record.JobLink;
import com.fit2cloud.common.job_record.JobLinkTypeConstants;
import com.fit2cloud.common.job_record.JobRecordParam;
import com.fit2cloud.common.platform.credential.Credential;
import com.fit2cloud.common.provider.exception.SkipPageException;
import com.fit2cloud.common.provider.util.CommonUtil;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.constants.ResourceTypeConstants;
import com.fit2cloud.constants.SyncDimensionConstants;
import com.fit2cloud.dao.entity.ComplianceRule;
import com.fit2cloud.dao.entity.ComplianceRuleGroup;
import com.fit2cloud.dao.entity.ComplianceScanResourceResult;
import com.fit2cloud.dao.entity.ComplianceScanResult;
import com.fit2cloud.es.entity.ResourceInstance;
import com.fit2cloud.provider.ICloudProvider;
import com.fit2cloud.quartz.CloudAccountSyncJob;
import com.fit2cloud.service.*;
import io.reactivex.rxjava3.functions.Action;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/12/6  14:29}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Service
public class SyncServiceImpl extends BaseSyncService implements ISyncService {
    @Resource
    private IBaseCloudAccountService cloudAccountService;
    @Resource
    private ElasticsearchClient elasticsearchClient;
    @Resource
    private IComplianceScanService complianceScanService;
    @Resource
    private IComplianceRuleService complianceRuleService;
    @Resource
    private IBaseJobRecordResourceMappingService jobRecordResourceMappingService;
    @Resource
    private IComplianceRuleGroupService complianceRuleGroupService;
    @Resource
    private Redisson redisson;
    @Resource
    private IComplianceScanResultService complianceScanResultService;
    @Resource
    private IComplianceScanResourceResultService complianceScanResourceResultService;

    @SneakyThrows
    @Override
    public void syncInstance(String cloudAccountId, ResourceTypeConstants instanceType) {
        // 加锁
        RLock lock = redissonClient.getLock(cloudAccountId + instanceType.name());
        // 如果指定时间拿不到锁就不执行同步
        if (!lock.tryLock()) {
            return;
        }
        try {

            CloudAccount cloudAccount = cloudAccountService.getById(cloudAccountId);
            // 如果云账号没删除 没查询到
            if (Objects.isNull(cloudAccount)) {
                // 删除资源数据
                deleteResourceDataByCloudAccountId(cloudAccountId);
                // 删除定时任务
                cloudAccountService.deleteJobByCloudAccountId(cloudAccountId);
                return;
            }

            ArrayList<ResourceInstance> resourceInstancesAll = new ArrayList<>();
            Class<? extends ICloudProvider> iCloudProviderClazz = ICloudProvider.of(cloudAccount.getPlatform());
            List<DefaultKeyValue<ResourceTypeConstants, SyncDimensionConstants>> map = CommonUtil.exec(iCloudProviderClazz, ICloudProvider::getResourceSyncDimensionConstants);
            // 如果不存在同步粒度则为不支持的资源类型
            Optional<DefaultKeyValue<ResourceTypeConstants, SyncDimensionConstants>> first = map.stream().filter(item -> item.getKey().equals(instanceType)).findFirst();
            if (first.isEmpty()) {
                return;
            }
            LocalDateTime syncTime = getSyncTime();
            JobRecord jobRecord = initJobRecord("扫描" + instanceType.getMessage(), syncTime, cloudAccountId, instanceType);
            // todo 校验云账号
            if (!proxyJob(jobRecord, new JobLink("校验云账号", JobLinkTypeConstants.VERIFICATION_CLOUD_ACCOUNT), () -> verification(cloudAccount), null)) {
                // todo 更新缓存
                proxyJob(jobRecord, new JobLink("扫描历史数据", JobLinkTypeConstants.SYSTEM_SAVE_DATA), () -> scan(instanceType, cloudAccountId), null);
                // todo 更新扫描时间
                proxyJob(jobRecord, new JobLink("更新扫描时间", JobLinkTypeConstants.SYSTEM_SAVE_DATA), () ->
                                complianceRuleService.update(new LambdaUpdateWrapper<ComplianceRule>().eq(ComplianceRule::getResourceType, instanceType.name()).set(ComplianceRule::getUpdateTime, syncTime))
                        , null);
                jobRecord.setStatus(JobStatusConstants.FAILED);
                updateJobRecord(jobRecord, JobRecordParam.success(new JobLink("任务执行结束", JobLinkTypeConstants.JOB_END), null));
                return;
            }
            SyncDimensionConstants syncDimensionConstants = first.get().getValue();
            List<Map<String, Object>> dimension = syncDimensionConstants.getDimensionExecParams().apply(cloudAccount, null);
            for (Map<String, Object> execParams : dimension) {
                try {
                    List<ResourceInstance> resourceInstances = CommonUtil.exec(iCloudProviderClazz, JsonUtil.toJSONString(execParams), instanceType.getExec());
                    resourceInstances.forEach(resourceInstance -> resourceInstance.setCloudAccountId(cloudAccountId));
                    resourceInstancesAll.addAll(resourceInstances);
                    updateJobRecord(jobRecord, resourceInstances, syncDimensionConstants, execParams);
                } catch (Exception e) {
                    if (e instanceof SkipPageException) {
                        // todo 跳过当前区域
                        updateJobRecord(jobRecord, new ArrayList<>(), syncDimensionConstants, execParams);
                    } else {
                        // todo 记录当前区域同步错误
                        updateJobRecord(jobRecord, getErrorDimensionJobRecordParam(jobRecord, e, syncDimensionConstants, execParams));
                    }
                }
            }
            // todo 插入数据
            proxyJob(jobRecord, new JobLink("插入原始数据", JobLinkTypeConstants.SYSTEM_SAVE_DATA), () -> saveOrUpdateData(cloudAccount, instanceType, resourceInstancesAll), null);
            // todo 更新缓存
            proxyJob(jobRecord, new JobLink("扫描合规资源", JobLinkTypeConstants.SYSTEM_SAVE_DATA), () -> scan(instanceType, cloudAccountId), null);
            // todo 更新扫描时间
            proxyJob(jobRecord, new JobLink("更新扫描时间", JobLinkTypeConstants.SYSTEM_SAVE_DATA), () ->
                            complianceRuleService.update(new LambdaUpdateWrapper<ComplianceRule>().eq(ComplianceRule::getResourceType, instanceType.name()).set(ComplianceRule::getUpdateTime, syncTime))
                    , null);
            // todo 更新任务状态
            JobStatusConstants jobRecordStatus = getJobRecordStatus(jobRecord);
            jobRecord.setStatus(jobRecordStatus);
            updateJobRecord(jobRecord, JobRecordParam.success(new JobLink("任务执行结束", JobLinkTypeConstants.JOB_END), null));
        } finally {
            // 清除不存在的云账号数据
            deleteNotFountCloudAccountData();
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    /**
     * 校验云账号
     *
     * @param cloudAccount 云账号对象
     */
    private void verification(CloudAccount cloudAccount) {
        Credential credential = Credential.of(cloudAccount.getPlatform(), cloudAccount.getCredential());
        // 如果云账号无效 修改状态 并且跳过执行
        try {
            credential.verification();
        } catch (Exception e) {
            cloudAccount.setState(false);
            cloudAccountService.updateById(cloudAccount);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 清理不存在的云账号数据
     */
    @Override
    public void deleteNotFountCloudAccountData() {
        // 所有的云账号
        List<CloudAccount> cloudAccounts = cloudAccountService.list();
        Query query = new BoolQuery.Builder().mustNot(new Query.Builder().terms(new TermsQuery.Builder()
                .terms(new TermsQueryField.Builder().value(cloudAccounts.stream().map(CloudAccount::getId)
                        .map(FieldValue::of).toList()).build()).field("cloudAccountId").build()).build()).build()._toQuery();
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest.Builder().query(query).refresh(Boolean.TRUE)
                .index(ResourceInstance.class.getAnnotation(Document.class).indexName()).build();
        try {
            // 删除es数据
            elasticsearchClient.deleteByQuery(deleteByQueryRequest);
            // 删除扫描数据
            complianceScanResultService.remove(new LambdaQueryWrapper<ComplianceScanResult>()
                    .notIn(ComplianceScanResult::getCloudAccountId, (cloudAccounts.stream().map(CloudAccount::getId).toList())));
            // 删除扫描资源数据
            complianceScanResourceResultService.remove(new LambdaQueryWrapper<ComplianceScanResourceResult>()
                    .notIn(ComplianceScanResourceResult::getCloudAccountId, (cloudAccounts.stream().map(CloudAccount::getId).toList())));
        } catch (Exception ignored) {
        }

    }

    /**
     * 扫描原始资源
     *
     * @param instanceType 实例类型
     */
    private void scan(ResourceTypeConstants instanceType, String cloudAccountId) {
        complianceScanService.scanComplianceOrSave(instanceType, cloudAccountId);
        complianceScanService.scanComplianceResourceOrSave(instanceType, cloudAccountId);
    }

    /**
     * 插入或者更新数据
     *
     * @param cloudAccount         云账号id
     * @param instanceType         资源实例类型
     * @param resourceInstancesAll 资源数据
     * @throws IOException 插入可能抛出的异常
     */
    private synchronized void saveOrUpdateData(CloudAccount cloudAccount, ResourceTypeConstants instanceType, ArrayList<ResourceInstance> resourceInstancesAll) throws IOException {
        if (CollectionUtils.isEmpty(resourceInstancesAll)) {
            return;
        }
        // todo 删除实例历史数据
        Query query = new Query.Builder().bool(new BoolQuery.Builder()
                        .must(new Query.Builder().term(new TermQuery.Builder().field("cloudAccountId").value(cloudAccount.getId()).build()).build(),
                                new Query.Builder().term(new TermQuery.Builder().field("resourceType").value(instanceType.name()).build()).build()).build())
                .build();
        DeleteByQueryRequest build = new DeleteByQueryRequest.Builder().index(ResourceInstance.class.getAnnotation(Document.class).indexName()).query(query).refresh(true).build();
        // todo 处理嵌套数组问题
        List<String> filterArrayKeys = resourceInstancesAll.stream().map(ResourceInstance::getFilterArray).filter(Objects::nonNull).flatMap(f -> f.keySet().stream()).distinct().toList();
        for (String filterArrayKey : filterArrayKeys) {
            elasticsearchClient.indices().putMapping(b -> b.properties(Map.of("filterArray." + filterArrayKey, Property.of(p -> p.nested(NestedProperty.of(n -> n)))))
                    .index(ResourceInstance.class.getAnnotation(Document.class).indexName()));
        }
        // todo 删除数据
        elasticsearchClient.deleteByQuery(build);
        BulkRequest bulkRequest = new BulkRequest.Builder().index(ResourceInstance.class.getAnnotation(Document.class).indexName())
                .operations(resourceInstancesAll.stream().map(source -> new BulkOperation.Builder()
                        .index(new IndexOperation.Builder<>().document(source).build()).build()).toList()).refresh(Refresh.True).build();
        // todo 插入数据
        elasticsearchClient.bulk(bulkRequest);
    }

    @SneakyThrows
    private void deleteResourceDataByCloudAccountId(String cloudAccountId) {
        Query query = new TermQuery.Builder().field("cloudAccountId").value(cloudAccountId).build()._toQuery();
        DeleteByQueryRequest build = new DeleteByQueryRequest.Builder().index(ResourceInstance.class.getAnnotation(Document.class).indexName()).query(query).refresh(true).build();
        elasticsearchClient.deleteByQuery(build);
        complianceScanResultService.remove(new LambdaQueryWrapper<ComplianceScanResult>()
                .eq(ComplianceScanResult::getCloudAccountId, cloudAccountId));
        complianceScanResourceResultService.remove(
                new LambdaQueryWrapper<ComplianceScanResourceResult>()
                        .eq(ComplianceScanResourceResult::getCloudAccountId, cloudAccountId)
        );
    }

    /**
     * 代理执行,记录任务节点
     *
     * @param record    任务记录
     * @param jobLink   任务环节
     * @param runnable  任务执行器
     * @param jobParams 当前环节额外参数
     */
    public boolean proxyJob(JobRecord record, JobLink jobLink, Action runnable, Map<String, Object> jobParams) {
        try {
            runnable.run();
            updateJobRecord(record, JobRecordParam.success(jobLink, jobParams));
            return true;
        } catch (Throwable e) {
            updateJobRecord(record, JobRecordParam.error(jobLink, e.getMessage()));
            return false;
        }
    }

    @Override
    public void syncInstanceByInstanceType(String cloudAccountId, List<String> instanceType) {
        for (String type : instanceType) {
            syncInstance(cloudAccountId, ResourceTypeConstants.valueOf(type));
        }
    }

    @Override
    public void syncInstance(String cloudAccountId, List<String> ruleGroupId) {
        syncInstance(cloudAccountId, ruleGroupId, () -> cloudAccountId + ":RULE_GROUP");
    }


    public void syncInstance(String cloudAccountId, List<String> ruleGroupId, Supplier<String> getLockKey) {
        RLock lock = redisson.getLock(getLockKey.get());
        if (lock.isLocked()) {
            return;
        }
        CloudAccountSyncJob.SyncScanJob.run(() -> {
            if (lock.tryLock()) {
                try {
                    CloudAccount cloudAccount = cloudAccountService.getById(cloudAccountId);
                    // 获取同步资源
                    List<String> instanceTypes = complianceRuleService.list(new LambdaQueryWrapper<ComplianceRule>()
                                    .in(ComplianceRule::getRuleGroupId, ruleGroupId)
                                    .eq(Objects.nonNull(cloudAccount), ComplianceRule::getPlatform, Objects.nonNull(cloudAccount) ? cloudAccount.getPlatform() : null))
                            .stream()
                            .map(ComplianceRule::getResourceType)
                            .distinct()
                            .toList();

                    // 一个云账号最多使用三个线程
                    List<List<String>> lists = split(instanceTypes, 3);
                    List<CompletableFuture<Void>> completableFutures = lists.stream()
                            .filter(CollectionUtils::isNotEmpty)
                            .map(group -> CloudAccountSyncJob.SyncScanJob.run(() -> syncInstanceByInstanceType(cloudAccountId, group)))
                            .toList();
                    completableFutures.forEach(CompletableFuture::join);
                } finally {
                    if (lock.isLocked()) {
                        lock.unlock();
                    }
                }
            }
        });

    }

    @Override
    public void syncInstance(String cloudAccountId) {
        List<ComplianceRuleGroup> list = complianceRuleGroupService.list();
        // 指定云账号同步的 与 指定云账号和规则组同步 不适用一个lock
        syncInstance(cloudAccountId, list.stream().map(ComplianceRuleGroup::getId).toList(), () -> cloudAccountId);
    }

    /**
     * 指定多少个数据拆分为一个数组
     *
     * @param array 原始数据
     * @param num   指定数据
     * @param <T>   数据泛型
     * @return 拆分后的二维数组
     */
    private <T> List<List<T>> splitArr(List<T> array, int num) {
        int count = array.size() % num == 0 ? array.size() / num : array.size() / num + 1;
        List<List<T>> arrayList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = i * num;
            List<T> list = new ArrayList<>();
            int j = 0;
            while (j < num && index < array.size()) {
                list.add(array.get(index++));
                j++;
            }
            arrayList.add(list);
        }
        return arrayList;
    }

    /**
     * 拆分为指定长度的二维数组
     *
     * @param sourceDataList 元数据
     * @param splitNum       指定的长度
     * @param <T>            数据泛型
     * @return 拆分后的二维数组
     */
    private <T> List<List<T>> split(List<T> sourceDataList, int splitNum) {
        List<List<T>> splitRes = new ArrayList<>();
        for (int i = 0; i < splitNum; i++) {
            splitRes.add(new ArrayList<>());
        }
        for (int index = 0; index < sourceDataList.size(); index++) {
            splitRes.get(index % splitNum).add(sourceDataList.get(index));
        }
        return splitRes;
    }

    /**
     * 初始化任务记录
     *
     * @param jobDescription 任务描述
     * @param syncTime       同步时间
     * @param cloudAccountId 云账户id
     * @return 任务记录对象
     */
    private JobRecord initJobRecord(String jobDescription, LocalDateTime syncTime, String cloudAccountId, ResourceTypeConstants instanceType) {
        JobRecord jobRecord = new JobRecord();
        jobRecord.setDescription(jobDescription);
        jobRecord.setStatus(JobStatusConstants.SYNCING);
        jobRecord.setParams(getStartJobParams());
        jobRecord.setType(JobTypeConstants.SECURITY_COMPLIANCE_CLOUD_ACCOUNT_SYNC_JOB);
        jobRecord.setCreateTime(syncTime);
        // 插入任务数据
        baseJobRecordService.save(jobRecord);
        // 插入关联关系
        JobRecordResourceMapping jobRecordResourceMapping = new JobRecordResourceMapping();
        jobRecordResourceMapping.setResourceId(cloudAccountId);
        jobRecordResourceMapping.setJobType(JobTypeConstants.SECURITY_COMPLIANCE_CLOUD_ACCOUNT_SYNC_JOB);
        jobRecordResourceMapping.setJobRecordId(jobRecord.getId());
        jobRecordResourceMapping.setResourceType(instanceType.name());
        jobRecordResourceMappingService.save(jobRecordResourceMapping);
        return jobRecord;
    }

    /**
     * 获取开始任务参数
     *
     * @return 任务开始参数
     */
    public HashMap<String, Object> getStartJobParams() {
        HashMap<String, Object> jobParams = new HashMap<>();
        List<JobRecordParam<Map<String, Object>>> jobLink = new ArrayList<>();
        jobLink.add(JobRecordParam.success(new JobLink("任务开始执行", JobLinkTypeConstants.JOB_START), null));
        jobParams.put(JobTypeConstants.SECURITY_COMPLIANCE_CLOUD_ACCOUNT_SYNC_JOB.name(), jobLink);
        return jobParams;
    }

    /**
     * 修改任务记录
     *
     * @param record                 任务记录
     * @param resourceInstances      资源列表
     * @param syncDimensionConstants JobRecord
     * @param execParams             执行参数
     */
    private void updateJobRecord(JobRecord record, List<ResourceInstance> resourceInstances, SyncDimensionConstants syncDimensionConstants, Map<String, Object> execParams) {
        JobRecordParam<Map<String, Object>> successDimensionJobRecordParam = getSuccessDimensionJobRecordParam(record, resourceInstances, syncDimensionConstants, execParams);
        updateJobRecord(record, successDimensionJobRecordParam);
    }

    /**
     * 获取成功的粒度任务参数
     *
     * @param resourceInstances      资源实例
     * @param syncDimensionConstants 同步粒度对象
     * @param execParams             执行参数
     * @return 当前粒度参数
     */
    private JobRecordParam<Map<String, Object>> getSuccessDimensionJobRecordParam(JobRecord record, List<ResourceInstance> resourceInstances, SyncDimensionConstants syncDimensionConstants, Map<String, Object> execParams) {
        Map<String, Object> params = syncDimensionConstants.getJobParams().apply(execParams);
        JobLink jobLink = syncDimensionConstants.getJobLink().apply(record, execParams);
        params.put("size", resourceInstances.size());
        return JobRecordParam.success(jobLink, params);
    }

    /**
     * 获取失败的参数
     *
     * @param e 异常信息
     * @return 失败参数
     */
    private JobRecordParam<Map<String, Object>> getErrorDimensionJobRecordParam(JobRecord record, Exception e, SyncDimensionConstants syncDimensionConstants, Map<String, Object> execParams) {
        Map<String, Object> params = syncDimensionConstants.getJobParams().apply(execParams);
        JobLink jobLink = syncDimensionConstants.getJobLink().apply(record, execParams);
        return JobRecordParam.error(jobLink, params, e.getMessage());
    }

    /**
     * 更新任务记录
     *
     * @param jobRecord               任务记录
     * @param jobDimensionRecordParam 任务记录参数
     */
    private void updateJobRecord(JobRecord jobRecord, JobRecordParam<Map<String, Object>> jobDimensionRecordParam) {
        Map<String, Object> jobParams = jobRecord.getParams();
        if (Objects.isNull(jobParams.get(JobTypeConstants.SECURITY_COMPLIANCE_CLOUD_ACCOUNT_SYNC_JOB.name()))) {
            List<JobRecordParam<Map<String, Object>>> resourceJobParams = new ArrayList<>();
            resourceJobParams.add(jobDimensionRecordParam);
            jobParams.put(JobTypeConstants.SECURITY_COMPLIANCE_CLOUD_ACCOUNT_SYNC_JOB.name(), resourceJobParams);
        } else {
            List<JobRecordParam<Map<String, Object>>> resourceJobParams = (List<JobRecordParam<Map<String, Object>>>) jobParams.get(JobTypeConstants.SECURITY_COMPLIANCE_CLOUD_ACCOUNT_SYNC_JOB.name());
            resourceJobParams.add(jobDimensionRecordParam);
        }
        baseJobRecordService.updateById(jobRecord);
    }

    /**
     * 获取任务状态
     *
     * @param jobRecord 任务记录
     * @return 任务状态
     */
    private JobStatusConstants getJobRecordStatus(JobRecord jobRecord) {
        Map<String, Object> jobParams = jobRecord.getParams();
        List<JobRecordParam<Map<String, Object>>> resourceJobParams = (List<JobRecordParam<Map<String, Object>>>) jobParams.get(JobTypeConstants.SECURITY_COMPLIANCE_CLOUD_ACCOUNT_SYNC_JOB.name());
        boolean error = resourceJobParams.stream().anyMatch(link -> link.getCode().equals(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        return error ? JobStatusConstants.FAILED : JobStatusConstants.SUCCESS;
    }


}
