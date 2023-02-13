package com.fit2cloud.constants;

import com.fit2cloud.common.constants.RoleConstants;
import com.fit2cloud.constants.PermissionConstants.GROUP;
import com.fit2cloud.constants.PermissionConstants.OPERATE;
import com.fit2cloud.dto.module.Menu;
import com.fit2cloud.dto.module.MenuPermission;
import com.fit2cloud.dto.module.Menus;
import com.fit2cloud.service.MenuService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class MenuConstants {

    public static List<Menu> MENUS;

    @Resource
    private MenuService menuService;

    @Value("${spring.application.name}")
    public void setModule(String module) {

        MENUS = MENUS_BUILDER.module(module).build().getMenus();

        //推送到redis
        menuService.init(module, MENUS);

    }

    private static final Menus.Builder MENUS_BUILDER = new Menus.Builder()
            .menu(new Menu.Builder()
                    .name("vm_cloud_server")
                    .title("云主机")
                    .path("/vm_cloud_server")
                    .icon("xuniyunzhuji")
                    .componentPath("/src/views/vm_cloud_server/index.vue")
                    .redirect("/vm_cloud_server/list")
                    .order(1)
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(GROUP.CLOUD_SERVER, OPERATE.READ)
                    )
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ORGADMIN)
                            .permission(GROUP.CLOUD_SERVER, OPERATE.READ)
                    )
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.USER)
                            .permission(GROUP.CLOUD_SERVER, OPERATE.READ)
                    )
                    .childOperationRoute(new Menu.Builder()
                            .name("server_list")
                            .title("列表")
                            .path("/list")
                            .componentPath("/src/views/vm_cloud_server/list.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.USER)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.READ)
                            )
                    )
                    .childOperationRoute(new Menu.Builder()
                            .name("server_detail")
                            .title("详情")
                            .path("/detail/:id")
                            .componentPath("/src/views/vm_cloud_server/detail.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.USER)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.READ)
                            )
                    )
                    .childOperationRoute(
                            new Menu.Builder()
                                    .name("add_disk")
                                    .title("添加磁盘")
                                    .path("/add_disk/:id")
                                    .componentPath("/src/views/vm_cloud_server/AddDisk.vue")
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.CREATE)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ORGADMIN)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.CREATE)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.USER)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.CREATE)
                                    )
                    )
                    .childOperationRoute(
                            new Menu.Builder()
                                    .name("change_config")
                                    .title("配置变更")
                                    .path("/change_config/:id")
                                    .componentPath("/src/views/vm_cloud_server/ChangeConfig.vue")
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.CLOUD_SERVER, OPERATE.RESIZE)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ORGADMIN)
                                            .permission(GROUP.CLOUD_SERVER, OPERATE.RESIZE)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.USER)
                                            .permission(GROUP.CLOUD_SERVER, OPERATE.RESIZE)
                                    )
                    )
                    .childOperationRoute(new Menu.Builder()
                            .name("server_catalog")
                            .title("选择云账号")
                            .path("/catalog")
                            .quickAccess(true)
                            .quickAccessName("创建云主机")
                            .componentPath("/src/views/vm_cloud_server/create/catalog.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.CREATE)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.CREATE)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.USER)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.CREATE)
                            )
                    )
                    .childOperationRoute(new Menu.Builder()
                            .name("server_create")
                            .title("创建")
                            .path("/create/:accountId")
                            .componentPath("/src/views/vm_cloud_server/create/index.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.CREATE)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.CREATE)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.USER)
                                    .permission(GROUP.CLOUD_SERVER, OPERATE.CREATE)
                            )
                    )
            )
            .menu(new Menu.Builder()
                    .name("vm_cloud_disk")
                    .title("磁盘")
                    .path("/vm_cloud_disk")
                    .icon("yuncunchu")
                    .componentPath("/src/views/vm_cloud_disk/index.vue")
                    .redirect("/vm_cloud_disk/list")
                    .order(2)
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(GROUP.CLOUD_DISK, OPERATE.READ)
                    )
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ORGADMIN)
                            .permission(GROUP.CLOUD_DISK, OPERATE.READ)
                    )
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.USER)
                            .permission(GROUP.CLOUD_DISK, OPERATE.READ)
                    )
                    .childOperationRoute(new Menu.Builder()
                            .name("disk_list")
                            .title("磁盘列表")
                            .path("/list")
                            .componentPath("/src/views/vm_cloud_disk/list.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_DISK, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.CLOUD_DISK, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.USER)
                                    .permission(GROUP.CLOUD_DISK, OPERATE.READ)
                            )
                    ).childOperationRoute(
                            new Menu.Builder()
                                    .name("disk_detail")
                                    .title("详情")
                                    .path("/detail/:id")
                                    .componentPath("/src/views/vm_cloud_disk/detail.vue")
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.READ)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.EDIT)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ORGADMIN)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.READ)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.EDIT)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.USER)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.READ)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.EDIT)
                                    )
                    ).childOperationRoute(
                            new Menu.Builder()
                                    .name("enlarge")
                                    .title("扩容")
                                    .path("/enlarge/:id")
                                    .componentPath("/src/views/vm_cloud_disk/enlarge.vue")
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.RESIZE)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ORGADMIN)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.RESIZE)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.USER)
                                            .permission(GROUP.CLOUD_DISK, OPERATE.RESIZE)
                                    )
                    )
            )
// 目前不展示镜像列表，暂时屏蔽
//            .menu(new Menu.Builder()
//                    .name("vm_cloud_image")
//                    .title("镜像")
//                    .path("/vm_cloud_image")
//                    .icon("jingxiang")
//                    .componentPath("/src/views/vm_cloud_image/index.vue")
//                    .redirect("/vm_cloud_image/list")
//                    .order(3)
//                    .requiredPermission(new MenuPermission.Builder()
//                            .role(RoleConstants.ROLE.ADMIN)
//                            .permission(GROUP.CLOUD_IMAGE, OPERATE.READ)
//                    )
//                    .requiredPermission(new MenuPermission.Builder()
//                            .role(RoleConstants.ROLE.ORGADMIN)
//                            .permission(GROUP.CLOUD_IMAGE, OPERATE.READ)
//                    )
//                    .requiredPermission(new MenuPermission.Builder()
//                            .role(RoleConstants.ROLE.USER)
//                            .permission(GROUP.CLOUD_IMAGE, OPERATE.READ)
//                    )
//                    .childOperationRoute(new Menu.Builder()
//                            .name("image_list")
//                            .title("镜像列表")
//                            .path("/list")
//                            .componentPath("/src/views/vm_cloud_image/list.vue")
//                            .requiredPermission(new MenuPermission.Builder()
//                                    .role(RoleConstants.ROLE.ADMIN)
//                                    .permission(GROUP.CLOUD_IMAGE, OPERATE.READ)
//
//                            )
//                            .requiredPermission(new MenuPermission.Builder()
//                                    .role(RoleConstants.ROLE.ORGADMIN)
//                                    .permission(GROUP.CLOUD_IMAGE, OPERATE.READ)
//
//                            )
//                            .requiredPermission(new MenuPermission.Builder()
//                                    .role(RoleConstants.ROLE.USER)
//                                    .permission(GROUP.CLOUD_IMAGE, OPERATE.READ)
//
//                            )
//                    )
//                    .childOperationRoute(
//                            new Menu.Builder()
//                                    .name("detail")
//                                    .title("详情")
//                                    .path("/detail")
//                                    .componentPath("/src/views/vm_cloud_image/detail.vue")
//                                    .requiredPermission(new MenuPermission.Builder()
//                                            .role(RoleConstants.ROLE.ADMIN)
//                                            .permission(GROUP.CLOUD_IMAGE, OPERATE.READ)
//                                    )
//                                    .requiredPermission(new MenuPermission.Builder()
//                                            .role(RoleConstants.ROLE.ORGADMIN)
//                                            .permission(GROUP.CLOUD_IMAGE, OPERATE.READ)
//                                    )
//                                    .requiredPermission(new MenuPermission.Builder()
//                                            .role(RoleConstants.ROLE.USER)
//                                            .permission(GROUP.CLOUD_IMAGE, OPERATE.READ)
//                                    )
//                    )
//
//
//            )
            .menu(new Menu.Builder()
                    .name("jobs")
                    .title("任务")
                    .path("/jobs")
                    .icon("xitongshezhi")
                    .order(4)
                    .componentPath("/src/views/jobs/index.vue")
                    .redirect("/jobs/list")
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(GROUP.JOBS, OPERATE.READ)
                    )
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ORGADMIN)
                            .permission(GROUP.JOBS, OPERATE.READ)
                    )
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.USER)
                            .permission(GROUP.JOBS, OPERATE.READ)
                    )
                    .childOperationRoute(new Menu.Builder()
                            .name("job_list")
                            .title("列表")
                            .path("/list")
                            .componentPath("/src/views/jobs/list.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.JOBS, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.JOBS, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.USER)
                                    .permission(GROUP.JOBS, OPERATE.READ)
                            )
                    )
                    .childOperationRoute(new Menu.Builder()
                            .name("job_detail")
                            .title("详情")
                            .path("/detail/:id")
                            .componentPath("/src/views/jobs/detail.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.JOBS, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.JOBS, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.USER)
                                    .permission(GROUP.JOBS, OPERATE.READ)
                            )
                    )
            )
            .menu(new Menu.Builder()
                    .name("recycle_bin")
                    .title("回收站")
                    .path("/recycle_bin")
                    .icon("chanpinfabu")
                    .order(5)
                    .componentPath("/src/views/recycle_bin/index.vue")
                    .redirect("/recycle_bin/list")
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(GROUP.RECYCLE_BIN, OPERATE.READ)
                    )
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ORGADMIN)
                            .permission(GROUP.RECYCLE_BIN, OPERATE.READ)
                    )
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.USER)
                            .permission(GROUP.RECYCLE_BIN, OPERATE.READ)
                    )
                    .childOperationRoute(new Menu.Builder()
                            .name("recycle_bin_list")
                            .title("列表")
                            .path("/list")
                            .componentPath("/src/views/recycle_bin/list.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.RECYCLE_BIN, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.RECYCLE_BIN, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.USER)
                                    .permission(GROUP.RECYCLE_BIN, OPERATE.READ)
                            )
                    )
            )
            //...
            ;


}
