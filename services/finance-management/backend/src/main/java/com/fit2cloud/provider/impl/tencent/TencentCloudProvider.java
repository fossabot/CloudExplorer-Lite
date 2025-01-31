package com.fit2cloud.provider.impl.tencent;

import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.es.entity.CloudBill;
import com.fit2cloud.provider.AbstractCloudProvider;
import com.fit2cloud.provider.ICloudProvider;
import com.fit2cloud.provider.impl.tencent.api.TencentBillApi;
import com.fit2cloud.provider.impl.tencent.api.TencentBucketApi;
import com.fit2cloud.provider.impl.tencent.entity.credential.TencentBillCredential;
import com.fit2cloud.provider.impl.tencent.entity.request.ListBucketMonthRequest;
import com.fit2cloud.provider.impl.tencent.entity.request.SyncBillRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/10/18  5:52 PM}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TencentCloudProvider extends AbstractCloudProvider<TencentBillCredential> implements ICloudProvider {
    @Override
    public List<CloudBill> syncBill(String request) {
        SyncBillRequest syncBillRequest = JsonUtil.parseObject(request, SyncBillRequest.class);
        return Objects.nonNull(syncBillRequest.getBill()) && StringUtils.equals(syncBillRequest.getBill().getSyncMode(), "bucket") ? TencentBucketApi.listBill(syncBillRequest) : TencentBillApi.listBill(syncBillRequest);
    }

    @Override
    public List<String> listBucketFileMonth(String request) {
        ListBucketMonthRequest listBucketMonthRequest = JsonUtil.parseObject(request, ListBucketMonthRequest.class);
        return TencentBucketApi.listBucketFileMonth(listBucketMonthRequest);
    }
}
