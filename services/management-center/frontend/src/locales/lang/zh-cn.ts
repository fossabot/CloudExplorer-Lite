const message = {
  user: {
    type: "用户类型",
    name: "姓名",
    email: "邮箱",
    phone: "手机号码",
    wechatAccount: "企业微信账号",
    role: "角色",
    password: "密码",
    local: "本地创建",
    extra: "第三方",
    source: "来源",
    status: "状态",
    manage: "用户管理",
    delete_role: "删除角色",
    add_role: "添加角色",
    set_role: "设置角色",
    add_org: "添加组织",
    add_workspace: "添加工作空间",
    delete_confirm: "确认删除用户",
    notify_setting: "通知设置",
    notify_tips:
      "邮箱、手机号设置后将与用户基本信息关联。手机号将做为钉钉平台推送标识。企业微信账号参考",
    validate: {
      phone_format: "手机号码格式错误",
      email_format: "邮箱格式错误",
      selected: "请选择用户",
    },
  },
  workspace: {
    user_count: "用户数",
  },
  // 云账号相关国际化
  cloud_account: {
    name: "云账号名称",
    name_placeholder: "请输入云账号名称",
    base_setting: "基本设置",
    sync_setting: "同步设置",
    cloud_account_size: "云账号必须选择一条",
    verification: "校验",
    sync_message: "同步",
    edit_job_message: "编辑定时任务",
    platform: "云平台",
    native_state_valid_message: "云账号有效",
    native_state_invalid_message: "云账号无效",
    native_state: "云账号状态",
    native_state_valid: "有效",
    native_state_invalid: "无效",
    native_sync_status: "云账号同步状态",
    native_sync: {
      init: "初始化",
      success: "成功",
      failed: "失败",
      syncing: "同步中",
      unknown: "未知",
    },
    last_sync_time: "最近同步时间",
    please_select_platform_message: "请选择云平台",
    account_information_message: "账号信息",
    field_is_not_null: "字段不能为空",
    name_is_not_empty: "云账号名称不能为空",
    platform_is_not_empty: "云平台不能为空",
    sync: {
      once: "同步一次",
      region: "区域",
      range: "同步范围",
      timing: "定时同步",
      interval: "间隔",
      interval_time_unit: {
        millisecond: "毫秒",
        second: "秒",
        minute: "分钟",
        hour: "小时",
        day: "天",
      },
    },
  },
  // 组织相关国际化
  org_manage: {
    affiliated_organization: "所属组织",
    organization_name_is_not_empty: "组织名称不能为空",
    organization_description_is_not_empty: "组织描述不能为空",
  },
};
export default {
  ...message,
};
