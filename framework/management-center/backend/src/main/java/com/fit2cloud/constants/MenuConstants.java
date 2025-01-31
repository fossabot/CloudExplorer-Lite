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
            // 云账号相关菜单
            .menu(new Menu.Builder()
                    .name("cloud_account")
                    .title("云账号")
                    .path("/cloud_account")
                    .componentPath("/src/views/CloudAccount/index.vue")
                    .icon("icon_cloud_outlined")
                    .order(1)
                    .redirect("/cloud_account/list")
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(GROUP.CLOUD_ACCOUNT, OPERATE.READ))
                    .childOperationRoute(new Menu.Builder()
                            .name("cloud_account_list")
                            .path("/list")
                            .title("列表")
                            .saveRecent(true)
                            .componentPath("/src/views/CloudAccount/list.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_ACCOUNT, OPERATE.READ)
                            )
                    )
                    .childOperationRoute(new Menu.Builder()
                            .name("cloud_account_create")
                            .path("/create")
                            .title("创建")
                            .quickAccess(true)
                            .saveRecent(true)
                            .quickAccessName("添加云账号")
                            .quickAccessIcon("icon_cloud_outlined")
                            .componentPath("/src/views/CloudAccount/create.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_ACCOUNT, OPERATE.CREATE)
                            ))
                    .childOperationRoute(new Menu.Builder()
                            .name("cloud_account_update")
                            .path("/update/:id")
                            .title("修改")
                            .componentPath("/src/views/CloudAccount/edit.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_ACCOUNT, OPERATE.EDIT)
                            ))
                    .childOperationRoute(new Menu.Builder()
                            .name("cloud_account_sync_job")
                            .path("/sync_job/:id")
                            .title("数据同步设置")
                            .componentPath("/src/views/CloudAccount/syncJob.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_ACCOUNT, OPERATE.SYNC_SETTING)
                            ))
                    .childOperationRoute(new Menu.Builder()
                            .name("cloud_account_detail")
                            .path("/detail/:id")
                            .title("详情")
                            .componentPath("/src/views/CloudAccount/detail.vue")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.CLOUD_ACCOUNT, OPERATE.READ)
                            ))
            )
            .menu(new Menu.Builder()
                    .name("user_tenant")
                    .title("用户与租户")
                    .path("/user_tenant")
                    .icon("icon_member_outlined")
                    .order(2)
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(GROUP.USER, OPERATE.READ)
                            .permission(GROUP.ROLE, OPERATE.READ)
                            .permission(GROUP.ORGANIZATION, OPERATE.READ)
                            .permission(GROUP.WORKSPACE, OPERATE.READ)
                    )

                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ORGADMIN)
                            .permission(GROUP.USER, OPERATE.READ)
                            .permission(GROUP.ROLE, OPERATE.READ)
                            .permission(GROUP.ORGANIZATION, OPERATE.READ)
                            .permission(GROUP.WORKSPACE, OPERATE.READ)
                    )
                    .childMenu(new Menu.Builder()
                            .name("user")
                            .title("用户")
                            .path("/user")
                            .componentPath("/src/views/UserManage/index.vue")
                            .redirect("/user_tenant/user/list")
                            .order(1)
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.USER, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.USER, OPERATE.READ)
                            )
                            .childOperationRoute(new Menu.Builder()
                                    .name("user_list")
                                    .title("列表")
                                    .path("/list")
                                    .saveRecent(true)
                                    .componentPath("/src/views/UserManage/list.vue")
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.USER, OPERATE.READ)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ORGADMIN)
                                            .permission(GROUP.USER, OPERATE.READ)
                                    )
                            )
                            .childOperationRoute(new Menu.Builder()
                                    .name("user_detail")
                                    .title("详情")
                                    .path("/detail/:id")
                                    .componentPath("/src/views/UserManage/detail.vue")
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.USER, OPERATE.READ)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ORGADMIN)
                                            .permission(GROUP.USER, OPERATE.READ)
                                    )
                            )
                    )
                    .childMenu(new Menu.Builder()
                            .name("role")
                            .title("角色")
                            .path("/role")
                            .componentPath("/src/views/RoleManage/index.vue")
                            .redirect("/user_tenant/role/list")
                            .order(3)
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(GROUP.ROLE, OPERATE.READ)
                            )
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ORGADMIN)
                                    .permission(GROUP.ROLE, OPERATE.READ)
                            )
                            .childOperationRoute(new Menu.Builder()
                                    .name("role_list")
                                    .title("列表")
                                    .path("/list")
                                    .saveRecent(true)
                                    .componentPath("/src/views/RoleManage/manage.vue")
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.ROLE, OPERATE.READ)

                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ORGADMIN)
                                            .permission(GROUP.ROLE, OPERATE.READ)
                                    )
                            )

                    )
                    // 组织相关菜单
                    .childMenu(new Menu.Builder()
                                    .name("org")
                                    .title("组织架构")
                                    .path("/org")
                                    .componentPath("/src/views/OrgManage/index.vue")
                                    .redirect("/user_tenant/org/list")
                                    .order(4)
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.ORGANIZATION, OPERATE.READ)
                                    )
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ORGADMIN)
                                            .permission(GROUP.ORGANIZATION, OPERATE.READ)
                                    )
                                    .childOperationRoute(new Menu.Builder()
                                            .name("org_list")
                                            .title("列表")
                                            .path("/list")
                                            .saveRecent(true)
                                            .componentPath("/src/views/OrgManage/manage.vue")
                                            .requiredPermission(new MenuPermission.Builder()
                                                    .role(RoleConstants.ROLE.ADMIN)
                                                    .permission(GROUP.ORGANIZATION, OPERATE.READ)

                                            )
                                            .requiredPermission(new MenuPermission.Builder()
                                                    .role(RoleConstants.ROLE.ORGADMIN)
                                                    .permission(GROUP.ORGANIZATION, OPERATE.READ)
                                            )
                                    )

                            //...

                    )

            )
            //日志管理
            .menu(new Menu.Builder()
                    .name("log_manage")
                    .title("日志管理")
                    .path("/log_manage")
                    .icon("icon_logs_outlined")
                    .order(3)
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(GROUP.OPERATED_LOG, OPERATE.READ)
                            .permission(GROUP.SYS_LOG, OPERATE.READ)
                    )
                    .childMenu(new Menu.Builder()
                                    .name("operated_log")
                                    .title("操作日志")
                                    .path("/operated_log")
                                    .componentPath("/src/views/OperatedLog/index.vue")
                                    .redirect("/log_manage/operated_log/list")
                                    .order(1)
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.OPERATED_LOG, OPERATE.READ)
                                    )
                                    .childOperationRoute(new Menu.Builder()
                                            .name("operated_log_list")
                                            .title("列表")
                                            .path("/list")
                                            .saveRecent(true)
                                            .componentPath("/src/views/OperatedLog/list.vue")
                                            .requiredPermission(new MenuPermission.Builder()
                                                    .role(RoleConstants.ROLE.ADMIN)
                                                    .permission(GROUP.OPERATED_LOG, OPERATE.READ)

                                            )
                                    )
                            //...views/OperatedLog/list.vue
                    )
                    .childMenu(new Menu.Builder()
                                    .name("sys_log")
                                    .title("系统日志")
                                    .path("/sys_log")
                                    .componentPath("/src/views/SystemLog/index.vue")
                                    .redirect("/log_manage/sys_log/list")
                                    .order(2)
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.SYS_LOG, OPERATE.READ)
                                    )
                                    .childOperationRoute(new Menu.Builder()
                                            .name("log_list")
                                            .title("列表")
                                            .path("/list")
                                            .saveRecent(true)
                                            .componentPath("/src/views/SystemLog/list.vue")
                                            .requiredPermission(new MenuPermission.Builder()
                                                    .role(RoleConstants.ROLE.ADMIN)
                                                    .permission(GROUP.SYS_LOG, OPERATE.READ)

                                            )
                                    )
                            //...
                    )
            )
            .menu(new Menu.Builder()
                    .name("module_manage")
                    .title("模块管理")
                    .path("/module_manage")
                    .componentPath("/src/views/ModuleManage/ModuleManagePage.vue")
                    .icon("icon_plugin_outlined")
                    .saveRecent(true)
                    .order(4)
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(GROUP.MODULE_MANAGE, OPERATE.READ))
            )
            .menu(new Menu.Builder()
                    .name("system_setting")
                    .title("系统设置")
                    .path("/system_setting")
                    .icon("icon-setting")
                    .order(5)
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(GROUP.PARAMS_SETTING, OPERATE.READ)
                            .permission(GROUP.ABOUT, OPERATE.READ)
                    )
                    .childMenu(new Menu.Builder()
                                    .name("params_setting")
                                    .title("参数设置")
                                    .path("/params_setting")
                                    .componentPath("/src/views/SystemSetting/index.vue")
                                    .redirect("/system_setting/params_setting/params_detail")
                                    .order(1)
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.PARAMS_SETTING, OPERATE.READ)
                                    )
                                    .childOperationRoute(new Menu.Builder()
                                            .name("params_detail")
                                            .title("")
                                            .path("/params_detail")
                                            .saveRecent(true)
                                            .componentPath("/src/views/SystemSetting/ParamsSetting.vue")
                                            .requiredPermission(new MenuPermission.Builder()
                                                    .role(RoleConstants.ROLE.ADMIN)
                                                    .permission(GROUP.PARAMS_SETTING, OPERATE.READ)
                                            )
                                    )
                            //...
                    )
                    .childMenu(new Menu.Builder()
                                    .name("about")
                                    .title("关于")
                                    .path("/about")
                                    .componentPath("/src/views/About/index.vue")
                                    .saveRecent(true)
                                    .order(2)
                                    .requiredPermission(new MenuPermission.Builder()
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .permission(GROUP.ABOUT, OPERATE.READ)
                                    )

                            //...
                    )

            )

            //...
            ;


}
