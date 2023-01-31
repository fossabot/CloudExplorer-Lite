package com.fit2cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fit2cloud.autoconfigure.ThreadPoolConfig;
import com.fit2cloud.base.entity.CloudAccount;
import com.fit2cloud.base.entity.JobRecord;
import com.fit2cloud.base.entity.VmCloudDisk;
import com.fit2cloud.base.entity.VmCloudServer;
import com.fit2cloud.base.mapper.BaseJobRecordResourceMappingMapper;
import com.fit2cloud.base.mapper.BaseVmCloudServerMapper;
import com.fit2cloud.base.service.IBaseCloudAccountService;
import com.fit2cloud.base.service.IBaseRecycleBinService;
import com.fit2cloud.base.service.IBaseVmCloudDiskService;
import com.fit2cloud.common.constants.JobStatusConstants;
import com.fit2cloud.common.constants.JobTypeConstants;
import com.fit2cloud.common.constants.ResourceTypeConstants;
import com.fit2cloud.common.exception.Fit2cloudException;
import com.fit2cloud.common.form.util.FormUtil;
import com.fit2cloud.common.form.vo.FormObject;
import com.fit2cloud.common.log.constants.OperatedTypeEnum;
import com.fit2cloud.common.log.constants.ResourceTypeEnum;
import com.fit2cloud.common.log.utils.LogUtil;
import com.fit2cloud.common.provider.util.CommonUtil;
import com.fit2cloud.common.utils.*;
import com.fit2cloud.constants.ErrorCodeConstants;
import com.fit2cloud.controller.request.CreateJobRecordRequest;
import com.fit2cloud.controller.request.ExecProviderMethodRequest;
import com.fit2cloud.controller.request.GrantRequest;
import com.fit2cloud.controller.request.ResourceState;
import com.fit2cloud.controller.request.vm.BatchOperateVmRequest;
import com.fit2cloud.controller.request.vm.ChangeServerConfigRequest;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.controller.request.vm.PageVmCloudServerRequest;
import com.fit2cloud.dao.mapper.VmCloudServerMapper;
import com.fit2cloud.dto.InitJobRecordDTO;
import com.fit2cloud.dto.UserDto;
import com.fit2cloud.dto.VmCloudServerDTO;
import com.fit2cloud.provider.ICloudProvider;
import com.fit2cloud.provider.ICreateServerRequest;
import com.fit2cloud.provider.constants.CreateServerRequestConstants;
import com.fit2cloud.provider.constants.F2CInstanceStatus;
import com.fit2cloud.provider.constants.ProviderConstants;
import com.fit2cloud.provider.entity.F2CVirtualMachine;
import com.fit2cloud.provider.entity.result.CheckCreateServerResult;
import com.fit2cloud.response.JobRecordResourceResponse;
import com.fit2cloud.service.*;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author fit2cloud
 * @since
 */
@Service
public class VmCloudServerServiceImpl extends ServiceImpl<BaseVmCloudServerMapper, VmCloudServer> implements IVmCloudServerService {

    @Resource
    private OrganizationCommonService organizationCommonService;
    @Resource
    private WorkspaceCommonService workspaceCommonService;
    @Resource
    private VmCloudServerMapper vmCloudServerMapper;
    @Resource
    private IBaseCloudAccountService cloudAccountService;
    @Resource
    private IBaseVmCloudDiskService vmCloudDiskService;
    @Resource
    private ThreadPoolConfig threadPoolConfig;
    @Resource
    private JobRecordCommonService jobRecordCommonService;
    @Resource
    private IResourceOperateService resourceOperateService;

    @Resource
    private IBaseRecycleBinService recycleService;

    @Resource
    private BaseJobRecordResourceMappingMapper baseJobRecordResourceMappingMapper;

    /**
     * 云主机批量操作
     */
    private Map<OperatedTypeEnum, Consumer<String>> batchOperationMap;

    @Resource
    private IPermissionService permissionService;

    @PostConstruct
    private void init() {
        batchOperationMap = new HashMap<>();
        batchOperationMap.put(OperatedTypeEnum.POWER_ON, this::powerOn);
        batchOperationMap.put(OperatedTypeEnum.REBOOT, this::rebootInstance);
        batchOperationMap.put(OperatedTypeEnum.POWER_OFF, this::powerOff);
        batchOperationMap.put(OperatedTypeEnum.SHUTDOWN, this::shutdownInstance);
        batchOperationMap.put(OperatedTypeEnum.DELETE, this::deleteInstance);
        batchOperationMap.put(OperatedTypeEnum.RECYCLE_SERVER, this::recycleInstance);
    }

    private void setCurrentInfos(PageVmCloudServerRequest request) {
        // 普通用户
        if (CurrentUserUtils.isUser() && StringUtils.isNotBlank(CurrentUserUtils.getWorkspaceId())) {
            request.setSourceIds(Collections.singletonList(CurrentUserUtils.getWorkspaceId()));
        }
        // 组织管理员
        if (CurrentUserUtils.isOrgAdmin()) {
            List<String> orgWorkspaceList = new ArrayList<>();
            orgWorkspaceList.add(CurrentUserUtils.getOrganizationId());
            orgWorkspaceList.addAll(organizationCommonService.getOrgIdsByParentId(CurrentUserUtils.getOrganizationId()));
            orgWorkspaceList.addAll(workspaceCommonService.getWorkspaceIdsByOrgIds(orgWorkspaceList));
            request.setSourceIds(orgWorkspaceList);
        }
    }


    @Override
    public IPage<VmCloudServerDTO> pageVmCloudServer(PageVmCloudServerRequest request) {
        setCurrentInfos(request);

        Page<VmCloudServerDTO> page = PageUtil.of(request, VmCloudServerDTO.class, new OrderItem(ColumnNameUtil.getColumnName(VmCloudServerDTO::getCreateTime, true), false), true);
        // 构建查询参数
        QueryWrapper<VmCloudServerDTO> wrapper = addQuery(request);
        return vmCloudServerMapper.pageVmCloudServer(page, wrapper);
    }

    @Override
    public List<VmCloudServer> listVmCloudServer(PageVmCloudServerRequest request) {
        setCurrentInfos(request);
        QueryWrapper<VmCloudServer> wrapper = addQuery(request);
        return this.list(wrapper);
    }

    @Override
    public long countVmCloudServer() {
        PageVmCloudServerRequest request = new PageVmCloudServerRequest();
        setCurrentInfos(request);
        QueryWrapper<VmCloudServer> wrapper = addQuery(request);
        return this.count(wrapper);
    }

    private <T extends VmCloudServer> QueryWrapper<T> addQuery(PageVmCloudServerRequest request) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();

        wrapper.like(StringUtils.isNotBlank(request.getWorkspaceId()), ColumnNameUtil.getColumnName(VmCloudServer::getSourceId, true), request.getWorkspaceId());
        wrapper.like(StringUtils.isNotBlank(request.getInstanceName()), ColumnNameUtil.getColumnName(VmCloudServer::getInstanceName, true), request.getInstanceName());
        wrapper.like(StringUtils.isNotBlank(request.getAccountName()), ColumnNameUtil.getColumnName(CloudAccount::getName, true), request.getAccountName());
        wrapper.like(StringUtils.isNotBlank(request.getIpArray()), ColumnNameUtil.getColumnName(VmCloudServer::getIpArray, true), request.getIpArray());
        wrapper.in(CollectionUtils.isNotEmpty(request.getAccountIds()), ColumnNameUtil.getColumnName(VmCloudServer::getAccountId, true), request.getAccountIds());
        wrapper.in(CollectionUtils.isNotEmpty(request.getInstanceStatus()), ColumnNameUtil.getColumnName(VmCloudServer::getInstanceStatus, true), request.getInstanceStatus());
        wrapper.in(CollectionUtils.isNotEmpty(request.getSourceIds()), ColumnNameUtil.getColumnName(VmCloudServer::getSourceId, true), request.getSourceIds());

        // 默认不展示已删除状态的机器
        if (CollectionUtils.isEmpty(request.getInstanceStatus())) {
            wrapper.ne(ColumnNameUtil.getColumnName(VmCloudServer::getInstanceStatus, true), F2CInstanceStatus.Deleted.name());
        }
        return wrapper;
    }

    @Override
    public boolean powerOff(String vmId) {
        operate(vmId, OperatedTypeEnum.POWER_OFF.getDescription(), ICloudProvider::powerOff,
                F2CInstanceStatus.Stopping.name(), F2CInstanceStatus.Stopped.name(), this::modifyResource,
                jobRecordCommonService::initJobRecord, jobRecordCommonService::modifyJobRecord, JobTypeConstants.CLOUD_SERVER_STOP_JOB);
        return true;
    }

    @Override
    public boolean powerOn(String vmId) {
        operate(vmId, OperatedTypeEnum.POWER_ON.getDescription(), ICloudProvider::powerOn,
                F2CInstanceStatus.Starting.name(), F2CInstanceStatus.Running.name(), this::modifyResource,
                jobRecordCommonService::initJobRecord, jobRecordCommonService::modifyJobRecord, JobTypeConstants.CLOUD_SERVER_START_JOB);
        return true;
    }


    public boolean shutdownInstance(String vmId) {
        operate(vmId, OperatedTypeEnum.SHUTDOWN.getDescription(), ICloudProvider::shutdownInstance,
                F2CInstanceStatus.Stopping.name(), F2CInstanceStatus.Stopped.name(), this::modifyResource,
                jobRecordCommonService::initJobRecord, jobRecordCommonService::modifyJobRecord, JobTypeConstants.CLOUD_SERVER_STOP_JOB);
        return true;
    }

    @Override
    public boolean rebootInstance(String vmId) {
        operate(vmId, OperatedTypeEnum.REBOOT.getDescription(), ICloudProvider::rebootInstance,
                F2CInstanceStatus.Rebooting.name(), F2CInstanceStatus.Running.name(), this::modifyResource,
                jobRecordCommonService::initJobRecord, jobRecordCommonService::modifyJobRecord, JobTypeConstants.CLOUD_SERVER_RESTART_JOB);
        return true;
    }

    @Override
    public boolean deleteInstance(String vmId) {
        operate(vmId, OperatedTypeEnum.DELETE.getDescription(), ICloudProvider::deleteInstance,
                F2CInstanceStatus.Deleting.name(), F2CInstanceStatus.Deleted.name(), this::modifyResource,
                jobRecordCommonService::initJobRecord, jobRecordCommonService::modifyJobRecord, JobTypeConstants.CLOUD_SERVER_DELETE_JOB);
        return true;
    }

    @Override
    public boolean recycleInstance(String vmId) {
        QueryWrapper<VmCloudServer> wrapper = new QueryWrapper<VmCloudServer>()
                .eq(ColumnNameUtil.getColumnName(VmCloudServer::getId, true), vmId)
                .ne(ColumnNameUtil.getColumnName(VmCloudServer::getInstanceStatus, true), F2CInstanceStatus.Deleted.name());
        VmCloudServer vmCloudServer = baseMapper.selectOne(wrapper);
        Optional.ofNullable(vmCloudServer).orElseThrow(() -> new Fit2cloudException(ErrorCodeConstants.VM_NOT_EXIST.getCode(), ErrorCodeConstants.VM_NOT_EXIST.getMessage()));

        String beforeStatus = F2CInstanceStatus.Stopping.name();
        if (vmCloudServer.getInstanceStatus().equalsIgnoreCase(F2CInstanceStatus.Stopped.name())) {
            beforeStatus = F2CInstanceStatus.Stopped.name();
        }

        operate(vmId, OperatedTypeEnum.RECYCLE_SERVER.getDescription(), ICloudProvider::shutdownInstance,
                beforeStatus, F2CInstanceStatus.Stopped.name(), this::modifyResource,
                jobRecordCommonService::initJobRecord, jobRecordCommonService::modifyJobRecord, JobTypeConstants.CLOUD_SERVER_RECYCLE_JOB);
        return true;
    }

    public boolean recoverInstance(String recycleBinId) {
        return recycleService.updateRecycleRecordOnRecover(recycleBinId);
    }

    @Override
    public boolean batchOperate(BatchOperateVmRequest request) {
        if (request.getInstanceIds().size() == 0) {
            throw new Fit2cloudException(ErrorCodeConstants.SELECT_AT_LEAST_ONE_VM.getCode(), ErrorCodeConstants.SELECT_AT_LEAST_ONE_VM.getMessage());
        }
        OperatedTypeEnum operatedType = OperatedTypeEnum.valueOf(request.getOperate());
        if (batchOperationMap.get(operatedType) == null) {
            throw new Fit2cloudException(ErrorCodeConstants.NOT_SUPPORTED_TEMPORARILY.getCode(), ErrorCodeConstants.NOT_SUPPORTED_TEMPORARILY.getMessage() + " - " + request.getOperate());
        }
        request.getInstanceIds().forEach(instanceId -> {
            try {
                batchOperationMap.get(operatedType).accept(instanceId);
            } catch (Throwable e) {
                throw new Fit2cloudException(ErrorCodeConstants.VM_BATCH_OPERATE_FAIL.getCode(), e.getMessage());
            }
        });
        return true;
    }

    @Override
    public List<JobRecordResourceResponse> findCloudServerOperateStatus(List<String> vmIds) {
        return baseJobRecordResourceMappingMapper.findLastResourceJobRecord(vmIds, Arrays.asList(
                JobTypeConstants.CLOUD_SERVER_OPERATE_JOB.getCode(),
                JobTypeConstants.CLOUD_SERVER_CREATE_JOB.getCode(),
                JobTypeConstants.CLOUD_SERVER_DELETE_JOB.getCode(),
                JobTypeConstants.CLOUD_SERVER_START_JOB.getCode(),
                JobTypeConstants.CLOUD_SERVER_STOP_JOB.getCode(),
                JobTypeConstants.CLOUD_SERVER_RESTART_JOB.getCode()
        ));
    }

    @Override
    public VmCloudServerDTO getById(String vmId) {
        QueryWrapper<VmCloudServerDTO> wrapper = new QueryWrapper<>();
        wrapper.eq("vm_cloud_server.id", vmId);
        VmCloudServerDTO vo = vmCloudServerMapper.getById(wrapper);
        return vo;
    }

    @Override
    public List<VmCloudServerDTO> getByIds(List<String> vmIds) {
        QueryWrapper<VmCloudServerDTO> wrapper = new QueryWrapper<>();
        wrapper.in("vm_cloud_server.id", vmIds);
        return vmCloudServerMapper.getByIds(wrapper);
    }


    /**
     * 云主机操作
     *
     * @param vmId            云主机ID
     * @param jobDescription  任务描述
     * @param execMethod      操作方法
     * @param modifyResource  修改云主机
     * @param iniJobMethod    初始化任务
     * @param modifyJobRecord 修改任务
     * @param beforeStatus    初始化资源状态
     * @param afterStatus     最终资源状态
     */
    private void operate(String vmId, String jobDescription, BiFunction<ICloudProvider, String, Boolean> execMethod, String beforeStatus, String afterStatus, Consumer<VmCloudServer> modifyResource, Function<InitJobRecordDTO, JobRecord> iniJobMethod, Consumer<JobRecord> modifyJobRecord) {
        operate(vmId, jobDescription, execMethod, beforeStatus, afterStatus, modifyResource, iniJobMethod, modifyJobRecord, JobTypeConstants.CLOUD_SERVER_OPERATE_JOB);
    }

    private void operate(String vmId, String jobDescription, BiFunction<ICloudProvider, String, Boolean> execMethod, String beforeStatus, String afterStatus, Consumer<VmCloudServer> modifyResource, Function<InitJobRecordDTO, JobRecord> iniJobMethod, Consumer<JobRecord> modifyJobRecord, JobTypeConstants operateType) {
        threadPoolConfig.workThreadPool().execute(() -> {
            try {
                LocalDateTime createTime = DateUtil.getSyncTime();
                QueryWrapper<VmCloudServer> wrapper = new QueryWrapper<VmCloudServer>()
                        .eq(ColumnNameUtil.getColumnName(VmCloudServer::getId, true), vmId)
                        .ne(ColumnNameUtil.getColumnName(VmCloudServer::getInstanceStatus, true), F2CInstanceStatus.Deleted.name());
                VmCloudServer vmCloudServer = baseMapper.selectOne(wrapper);
                if (vmCloudServer == null) {
                    throw new Fit2cloudException(ErrorCodeConstants.VM_NOT_EXIST.getCode(), ErrorCodeConstants.VM_NOT_EXIST.getMessage());
                }
                String instanceStatus = vmCloudServer.getInstanceStatus();
                //初始化任务
                JobRecord jobRecord = iniJobMethod.apply(
                        InitJobRecordDTO.builder()
                                .jobDescription(jobDescription)
                                .jobStatus(JobStatusConstants.EXECUTION_ING)
                                .jobType(operateType)
                                .resourceId(vmCloudServer.getId())
                                .resourceType(ResourceTypeEnum.CLOUD_SERVER)
                                .createTime(createTime)
                                .build());
                vmCloudServer.setInstanceStatus(beforeStatus);
                modifyResource.accept(vmCloudServer);
                CloudAccount cloudAccount = cloudAccountService.getById(vmCloudServer.getAccountId());
                Class<? extends ICloudProvider> cloudProvider = ProviderConstants.valueOf(cloudAccount.getPlatform()).getCloudProvider();
                HashMap<String, Object> params = CommonUtil.getParams(cloudAccount.getCredential(), vmCloudServer.getRegion());
                params.put("uuid", vmCloudServer.getInstanceUuid());

                try {
                    boolean result = CommonUtil.exec(cloudProvider, JsonUtil.toJSONString(params), execMethod);
                    if (result) {
                        vmCloudServer.setInstanceStatus(afterStatus);
                        jobRecord.setStatus(JobStatusConstants.SUCCESS);
                        switch (operateType) {
                            case CLOUD_SERVER_STOP_JOB, CLOUD_SERVER_START_JOB -> vmCloudServer.setLastShutdownTime(DateUtil.getSyncTime());
                            case CLOUD_SERVER_RECYCLE_JOB ->
                                    recycleService.insertRecycleRecord(vmId, ResourceTypeConstants.VM);
                            case CLOUD_SERVER_DELETE_JOB ->
                                    recycleService.updateRecycleRecordOnDelete(vmId, ResourceTypeConstants.VM);
                            default -> {
                            }
                        }
                    } else {
                        vmCloudServer.setInstanceStatus(instanceStatus);
                        jobRecord.setStatus(JobStatusConstants.FAILED);
                    }
                } catch (Exception e) {
                    vmCloudServer.setInstanceStatus(instanceStatus);
                    jobRecord.setStatus(JobStatusConstants.FAILED);
                    jobRecord.setResult(e.getMessage());
                    LogUtil.error("Cloud server exec operate fail - {}", e.getMessage());
                    e.printStackTrace();
                }
                modifyResource.accept(vmCloudServer);
                jobRecord.setFinishTime(DateUtil.getSyncTime());
                modifyJobRecord.accept(jobRecord);
            } catch (Throwable e) {
                LogUtil.error("Cloud server operate fail - {}", e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    private void modifyResource(VmCloudServer vmCloudServer) {
        this.updateById(vmCloudServer);
    }


    @Override
    public boolean createServer(CreateServerRequest request) {

        CloudAccount cloudAccount = cloudAccountService.getById(request.getAccountId());

        Class<? extends ICloudProvider> cloudProvider = ProviderConstants.valueOf(cloudAccount.getPlatform()).getCloudProvider();

        Class<? extends ICreateServerRequest> createRequest = CreateServerRequestConstants.valueOf(cloudAccount.getPlatform()).getCreateRequest();

        ICreateServerRequest requestObj = JsonUtil.parseObject(request.getCreateRequest(), createRequest);

        //设置账号信息
        requestObj.setCredential(cloudAccount.getCredential());

        CheckCreateServerResult checkCreateServerResult = CommonUtil.exec(cloudProvider, JsonUtil.toJSONString(requestObj), ICloudProvider::validateServerCreateRequest);
        if (!checkCreateServerResult.isPass()) {
            throw new RuntimeException(checkCreateServerResult.getErrorInfo());
        }

        int count = requestObj.getCount();

        for (int i = 0; i < count; i++) {

            //设置index
            requestObj.setIndex(i);

            //提前设置uuid
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            requestObj.setId(uuid);

            //先插入数据库占位
            F2CVirtualMachine tempData = CommonUtil.exec(cloudProvider, JsonUtil.toJSONString(requestObj), ICloudProvider::getSimpleServerByCreateRequest);

            VmCloudServer vmCloudServer = new VmCloudServer();
            BeanUtils.copyProperties(tempData, vmCloudServer);
            vmCloudServer.setInstanceName(tempData.getName());
            vmCloudServer.setAccountId(request.getAccountId());
            vmCloudServer.setUpdateTime(DateUtil.getSyncTime());
            vmCloudServer.setIpArray(JsonUtil.toJSONString(tempData.getIpArray()));
            vmCloudServer.setInstanceStatus(F2CInstanceStatus.WaitCreating.name());

            this.save(vmCloudServer);

            //执行创建
            //F2CVirtualMachine result = CommonUtil.exec(cloudProvider, JsonUtil.toJSONString(requestObj), ICloudProvider::createVirtualMachine);

            createServerJob(vmCloudServer.getId(), JsonUtil.toJSONString(requestObj), request, OperatedTypeEnum.CREATE_SERVER.getDescription(), this::modifyResource, jobRecordCommonService::initJobRecord, jobRecordCommonService::modifyJobRecord);

        }

        return true;
    }

    private void createServerJob(String serverId, String createRequest, CreateServerRequest request, String jobDescription, Consumer<VmCloudServer> modifyResource, Function<InitJobRecordDTO, JobRecord> iniJobMethod, Consumer<JobRecord> modifyJobRecord) {
        threadPoolConfig.workThreadPool().execute(() -> {
            try {
                LocalDateTime createTime = DateUtil.getSyncTime();

                VmCloudServer vmCloudServer = this.getById(serverId);

                CreateServerRequest requestToSave = new CreateServerRequest();
                BeanUtils.copyProperties(request, requestToSave);
                requestToSave.setCreateRequest(createRequest);
                Map<String, Object> jobParams = new HashMap<>();
                jobParams.put("order", requestToSave);

                UserDto user = CurrentUserUtils.getUser();
                jobParams.put("username", user.getUsername());
                jobParams.put("userDisplayName", user.getName());

                //初始化任务
                JobRecord jobRecord = iniJobMethod.apply(
                        InitJobRecordDTO.builder()
                                .params(jobParams)
                                .jobDescription(jobDescription)
                                .jobStatus(JobStatusConstants.EXECUTION_ING)
                                .jobType(JobTypeConstants.CLOUD_SERVER_CREATE_JOB)
                                .resourceId(vmCloudServer.getId())
                                .resourceType(ResourceTypeEnum.CLOUD_SERVER)
                                .createTime(createTime)
                                .build());
                vmCloudServer.setInstanceStatus(F2CInstanceStatus.Creating.name());
                modifyResource.accept(vmCloudServer);

                CloudAccount cloudAccount = cloudAccountService.getById(vmCloudServer.getAccountId());
                Class<? extends ICloudProvider> cloudProvider = ProviderConstants.valueOf(cloudAccount.getPlatform()).getCloudProvider();
                HashMap<String, Object> params = CommonUtil.getParams(cloudAccount.getCredential(), vmCloudServer.getRegion());
                params.put("id", vmCloudServer.getId());
                try {
                    F2CVirtualMachine result = CommonUtil.exec(cloudProvider, createRequest, ICloudProvider::createVirtualMachine);

                    vmCloudServer = SyncProviderServiceImpl.toVmCloudServer(result, vmCloudServer.getAccountId(), DateUtil.getSyncTime());
                    jobRecord.setStatus(JobStatusConstants.SUCCESS);


                } catch (Exception e) {
                    vmCloudServer.setInstanceStatus(F2CInstanceStatus.Failed.name());
                    jobRecord.setStatus(JobStatusConstants.FAILED);
                    jobRecord.setResult(e.getMessage());
                    LogUtil.error("Create Cloud server fail - {}", e.getMessage());
                    e.printStackTrace();
                }
                modifyResource.accept(vmCloudServer);
                jobRecord.setFinishTime(DateUtil.getSyncTime());
                modifyJobRecord.accept(jobRecord);
            } catch (Throwable e) {
                LogUtil.error("Create Cloud server fail - {}", e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public boolean changeConfig(ChangeServerConfigRequest request) {
        try {
            VmCloudServer vmCloudServer = baseMapper.selectById(request.getId());

            // 配置变更前状态
            VmCloudServer before = new VmCloudServer();
            BeanUtils.copyProperties(vmCloudServer, before);

            // 配置变更中状态
            vmCloudServer.setInstanceStatus(F2CInstanceStatus.ConfigChanging.name());
            VmCloudServer processing = new VmCloudServer();
            BeanUtils.copyProperties(vmCloudServer, processing);

            // 配置变更后状态
            vmCloudServer.setInstanceStatus(F2CInstanceStatus.Running.name());
            vmCloudServer.setInstanceType(request.getNewInstanceType());
            VmCloudServer after = new VmCloudServer();
            BeanUtils.copyProperties(vmCloudServer, after);

            // 构建执行插件方法的参数
            CloudAccount cloudAccount = cloudAccountService.getById(vmCloudServer.getAccountId());
            String platform = cloudAccount.getPlatform();
            HashMap<String, Object> params = CommonUtil.getParams(cloudAccount.getCredential(), vmCloudServer.getRegion());
            params.put("instanceUuid", vmCloudServer.getInstanceUuid());
            params.put("newInstanceType", request.getNewInstanceType());
            params.put("instanceChargeType", vmCloudServer.getInstanceChargeType());
            params.put("cpu", request.getCpu());
            params.put("memory", request.getMemory());

            // 执行
            ResourceState<VmCloudServer, F2CVirtualMachine> resourceState = ResourceState.<VmCloudServer, F2CVirtualMachine>builder()
                    .beforeResource(before)
                    .processingResource(processing)
                    .afterResource(after)
                    .updateResourceMethod(this::updateCloudServer)
                    .updateResourceMethodNeedTransfer(this::updateCloudServer)
                    .build();
            ExecProviderMethodRequest execProviderMethod = ExecProviderMethodRequest.builder().
                    execMethod(ICloudProvider::changeVmConfig)
                    .methodParams(params)
                    .platform(platform)
                    .resultNeedTransfer(true)
                    .build();
            CreateJobRecordRequest createJobRecordRequest = CreateJobRecordRequest.builder().
                    resourceOperateType(OperatedTypeEnum.CHANGE_SERVER_CONFIG).
                    resourceId(vmCloudServer.getId()).
                    resourceType(ResourceTypeEnum.CLOUD_SERVER).
                    jobType(JobTypeConstants.CLOUD_SERVER_CONFIG_CHANGE_JOB)
                    .build();
            resourceOperateService.operateWithJobRecord(createJobRecordRequest, execProviderMethod, resourceState);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to change cloud server config: " + e.getMessage());
        }
    }

    /**
     * 更新云主机
     *
     * @param vmCloudServer
     * @param
     */
    private void updateCloudServer(VmCloudServer vmCloudServer) {
        baseMapper.updateById(vmCloudServer);
    }

    /**
     * 更新云主机
     *
     * @param vmCloudServer
     * @param f2CVirtualMachine
     */
    private void updateCloudServer(VmCloudServer vmCloudServer, F2CVirtualMachine f2CVirtualMachine) {
        VmCloudServer vmCloudServerUpdate = SyncProviderServiceImpl.toVmCloudServer(f2CVirtualMachine, vmCloudServer.getAccountId(), DateUtil.getSyncTime());
        BeanUtils.copyProperties(vmCloudServerUpdate, vmCloudServer, new String[]{"id", "ipArray"});
        baseMapper.updateById(vmCloudServer);
    }

    public FormObject getConfigUpdateForm(String platform) {
        Class<? extends ICloudProvider> cloudProvider = ProviderConstants.valueOf(platform).getCloudProvider();
        try {
            return cloudProvider.getConstructor().newInstance().getConfigUpdateForm();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get config update form!" + e.getMessage(), e);
        }
    }

    public String calculateConfigUpdatePrice(String platform, Map<String, Object> params) {
        Class<? extends ICloudProvider> cloudProvider = ProviderConstants.valueOf(platform).getCloudProvider();
        try {
            return (String) FormUtil.exec(cloudProvider.getName(), false, "calculateConfigUpdatePrice", params);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get config update price!" + e.getMessage(), e);
        }
    }

    public boolean grant(GrantRequest grantServerRequest) {
        String sourceId = grantServerRequest.getGrant() ? grantServerRequest.getSourceId() : "";

        UpdateWrapper<VmCloudServer> updateWrapper = new UpdateWrapper();
        updateWrapper.lambda().in(VmCloudServer::getId, grantServerRequest.getIds())
                .set(VmCloudServer::getSourceId, sourceId);
        update(updateWrapper);

        // 更新云主机关联的云磁盘
        List<String> instanceUuids = listByIds(Arrays.asList(grantServerRequest.getIds())).stream().map(vmCloudServer -> vmCloudServer.getInstanceUuid()).collect(Collectors.toList());
        UpdateWrapper<VmCloudDisk> updateDiskWrapper = new UpdateWrapper();
        updateDiskWrapper.lambda().in(VmCloudDisk::getInstanceUuid, instanceUuids)
                .set(VmCloudDisk::getSourceId, sourceId);
        vmCloudDiskService.update(updateDiskWrapper);

        return true;
    }
}
