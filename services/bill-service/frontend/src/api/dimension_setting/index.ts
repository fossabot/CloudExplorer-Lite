import { get, post, del, put } from "@commons/request";
import type { Ref } from "vue";
import type Result from "@commons/request/Result";
import type { SimpleMap } from "@commons/api/base/type";
import type { BillView } from "@/api/bill_view/type";
import type {
  BillDimensionSetting,
  BillAuthorizeRule,
} from "@/api/dimension_setting/type";
/**
 * 获取可授权的字段
 * @returns
 */
const listAuthorizeKeys: (
  loading?: Ref<boolean>
) => Promise<Result<Array<SimpleMap<string>>>> = (loading) => {
  return get("/api/dimension_setting/group_keys", {}, loading);
};

/**
 *获取指定字段的值
 * @param authorizeKey 授权的key
 * @returns  对应key的所有值
 */
const listAuthorizeValues: (
  authorizeKey: string,
  loading?: Ref<boolean>
) => Promise<Result<Array<SimpleMap<string>>>> = (authorizeKey, loading) => {
  return get(`/api/dimension_setting/group_values`, { authorizeKey }, loading);
};

/**
 *获取分账设置
 * @param authorizeId 授权id
 * @param type        授权类型
 * @param loading     加载器
 * @returns
 */
const getBillDimensionSetting: (
  authorizeId: string,
  type: "WORKSPACE" | "ORGANIZATION",
  loading?: Ref<boolean>
) => Promise<Result<BillDimensionSetting>> = (authorizeId, type, loading) => {
  return get(`/api/dimension_setting/${authorizeId}/${type}`, {}, loading);
};

const saveOrUpdate: (
  authorizeId: string,
  type: "WORKSPACE" | "ORGANIZATION",
  billAuthorizeRule: BillAuthorizeRule,
  loading?: Ref<boolean>
) => Promise<Result<BillDimensionSetting>> = (
  authorizeId,
  type,
  billAuthorizeRule,
  loading
) => {
  return post(
    `/api/dimension_setting/${authorizeId}/${type}`,
    {},
    billAuthorizeRule,
    loading
  );
};

export default {
  listAuthorizeKeys,
  listAuthorizeValues,
  getBillDimensionSetting,
  saveOrUpdate,
};
