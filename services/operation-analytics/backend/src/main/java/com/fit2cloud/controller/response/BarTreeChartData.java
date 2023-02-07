package com.fit2cloud.controller.response;

import com.fit2cloud.base.entity.VmCloudServer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jianneng
 * @date 2023/1/9 11:46
 **/
@Getter
@Setter
public class BarTreeChartData {

    private String id;
    private String name;
    private Long value;
    private String pId;
    private String groupName;
    private List<BarTreeChartData> children;

}
