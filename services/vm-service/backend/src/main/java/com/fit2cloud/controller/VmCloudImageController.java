package com.fit2cloud.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fit2cloud.base.entity.VmCloudImage;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.controller.request.images.PageVmCloudImageRequest;
import com.fit2cloud.dto.VmCloudImageDTO;
import com.fit2cloud.service.IVmCloudImageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author jianneng
 * @date 2022/9/27 14:31
 **/
@RestController
@RequestMapping("/api/image")
@Validated
@Api("镜像相关接口")
public class VmCloudImageController {
    @Resource
    private IVmCloudImageService imageService;

    @ApiOperation(value = "分页查询镜像", notes = "分页查询镜像")
    @GetMapping("/page")
    @PreAuthorize("hasAnyCePermission('CLOUD_IMAGE:READ')")
    public ResultHolder<IPage<VmCloudImageDTO>> list(@Validated PageVmCloudImageRequest pageVmCloudImageRequest) {
        return ResultHolder.success(imageService.pageVmCloudImage(pageVmCloudImageRequest));
    }

    @ApiOperation(value = "查询镜像数量", notes = "查询镜像数量")
    @GetMapping("/count")
    @PreAuthorize("hasAnyCePermission('CLOUD_IMAGE:READ')")
    public ResultHolder<Long> count() {
        return ResultHolder.success(imageService.count(
                new LambdaQueryWrapper<VmCloudImage>()
                        .ne(VmCloudImage::getStatus, "DELETED"))
        );
    }
}
