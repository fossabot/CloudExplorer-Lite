package com.fit2cloud.request.cloud_account;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @Author:张少虎
 * @Date: 2022/9/30  3:22 PM
 * @Version 1.0
 * @注释:
 */
@Data
public class SyncRequest {
    /**
     * 云账号id
     */
    @ApiModelProperty(value = "云账号id", notes = "云账号id")
    private String cloudAccountId;
    /**
     * 任务
     */
    @ApiModelProperty(value = "同步任务", notes = "同步任务")
    private List<Job> syncJob;

    /**
     * 同步参数
     */
    private Map<String, Object> params;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Job {
        /**
         * 模块名称
         */
        @ApiModelProperty(value = "模块名称", notes = "模块名称")
        private String module;
        /**
         * 任务名称
         */
        @ApiModelProperty(value = "任务名称", notes = "任务名称")
        private String jobName;

        @ApiModelProperty(value = "任务组", notes = "任务组")
        private String jobGroup;
    }
}
