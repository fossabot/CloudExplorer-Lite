package com.fit2cloud.quartz;

import com.fit2cloud.common.constants.PlatformConstants;
import com.fit2cloud.common.platform.credential.Credential;
import com.fit2cloud.common.scheduler.handler.AsyncJob;
import com.fit2cloud.dao.entity.CloudAccount;
import com.fit2cloud.service.ICloudAccountService;
import jdk.jfr.Name;
import lombok.SneakyThrows;
import org.quartz.Job;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @Author:张少虎
 * @Date: 2022/9/18  1:00 AM
 * @Version 1.0
 * @注释: 校验云账号定时任务
 */
@Component
@Name("校验云账号定时任务")
public class VerificationCloudAccountJob extends AsyncJob implements Job {
    @Resource
    private ICloudAccountService cloudAccountService;

    @Override
    @SneakyThrows
    protected void run(Map<String, Object> map) {
        List<CloudAccount> list = cloudAccountService.list();
        for (CloudAccount cloudAccount : list) {
            PlatformConstants platformConstants = PlatformConstants.valueOf(cloudAccount.getPlatform());
            Credential verification = platformConstants.getCredentialClass().getConstructor().newInstance().deCode(cloudAccount.getCredential());
            try {
                verification.verification();
            } catch (Exception e) {
                cloudAccount.setState(false);
                cloudAccountService.updateById(cloudAccount);
            }
        }
    }
}
