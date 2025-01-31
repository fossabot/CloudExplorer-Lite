<template>
  <template v-if="!confirm">
    <el-form
      ref="ruleFormRef"
      label-suffix=":"
      label-position="left"
      :model="_data"
      size="small"
      style="width: 100%"
    >
      <div style="margin-bottom: 10px">
        <el-input
          v-model="searchName"
          placeholder="输入关键字搜索"
          style="width: 20%"
          @keyup="handleQueryClick"
        >
          <template #append>
            <el-button style="color: var(--el-color-primary)" icon="Search" />
          </template>
        </el-input>
      </div>
      <el-form-item style="width: 100%">
        <el-radio-group v-model="selectRowId" style="width: 100%">
          <el-table
            ref="multipleTableRef"
            :data="props.formItem?.ext?.networkConfig?.searchTableData"
            highlight-current-row
            style="width: 100%; height: 150px"
            @current-change="handleCurrentChange"
            v-loading="_loading"
            border
          >
            <el-table-column width="55">
              <template #default="scope">
                <el-radio :label="scope.row.uuid">
                  <template #default>{{}}</template>
                </el-radio>
              </template>
            </el-table-column>
            <el-table-column property="name" label="子网名称" />
            <el-table-column property="vpcName" label="所属VPC" />
            <el-table-column property="cidr" label="IPv4网段" />
          </el-table>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </template>
  <template v-else>
    {{ modelValue?.name }}
  </template>
</template>
<script setup lang="ts">
import type { FormView } from "@commons/components/ce-form/type";
import formApi from "@commons/api/form_resource_api";
import { computed, onMounted, ref, watch } from "vue";
import _ from "lodash";
import type { ElTable, FormInstance } from "element-plus";

const props = defineProps<{
  modelValue?: Network;
  allData?: any;
  allFormViewData?: Array<FormView>;
  field: string;
  otherParams: any;
  formItem: FormView;
  confirm?: boolean;
}>();

interface Network {
  uuid: string;
  name: string;
  vpcId: string;
  vpcName: string;
}

const selectRowId = ref<any>("");

const searchName = ref("");

const emit = defineEmits(["update:modelValue", "change"]);

const _data = computed({
  get() {
    return props.modelValue;
  },
  set(value) {
    emit("update:modelValue", value);
  },
});

// 校验实例对象
const ruleFormRef = ref<FormInstance>();
/**
 * 列表选中
 */
const currentRow = computed<Network | undefined>({
  get() {
    return _.find(
      props.formItem?.ext?.networkConfig?.searchTableData,
      (o: Network) => o.uuid === _data.value?.uuid
    );
  },
  set(value) {
    _data.value = value;
    emit("change");
  },
});

function handleCurrentChange(val: Network | undefined) {
  currentRow.value = val;
  selectRowId.value = val?.uuid;
  emit("change");
}

const _loading = ref<boolean>(false);

const singleTableRef = ref<InstanceType<typeof ElTable>>();

function getTempRequest() {
  return _.assignWith(
    {},
    {},
    props.otherParams,
    props.allData,
    (objValue, srcValue) => {
      return _.isUndefined(objValue) ? srcValue : objValue;
    }
  );
}

function getList() {
  const _temp = getTempRequest();
  const clazz = "com.fit2cloud.provider.impl.huawei.HuaweiCloudProvider";
  const method = "listSubnet";
  formApi
    .getResourceMethod(false, clazz, method, _temp, _loading)
    .then((ok) => {
      _.set(props.formItem, "ext.networkConfig", ok.data);
      _.set(props.formItem, "ext.networkConfig.searchTableData", ok.data);
      if (currentRow.value === undefined) {
        currentRow.value = ok.data[0];
        selectRowId.value = ok.data[0].uuid;
      } else {
        //设置界面默认选中
        singleTableRef.value?.setCurrentRow(currentRow.value);
        selectRowId.value = currentRow.value.uuid;
      }
    });
}

onMounted(() => {
  if (!props.confirm) {
    getList();
  }
});
watch(
  () => props.allData,
  (n, o) => {
    if (n.usePublicIp || !n.usePublicIp) {
      const amountFormView = _.find(props.allFormViewData, (formViewData) => {
        return formViewData.field === "totalAmountText";
      });
      getAmount(amountFormView);
    }
  }
);

function getAmount(amountFowmView: any) {
  const _temp = getTempRequest();
  formApi
    .getResourceMethod(
      false,
      amountFowmView.clazz,
      amountFowmView.method,
      _temp
    )
    .then((ok) => {
      if (ok.data) {
        amountFowmView.optionList = ok.data;
      }
    });
}

watch(
  () => props.allData.regionId,
  (n, o) => {
    getList();
  }
);
const handleQueryClick = () => {
  if (!props.formItem?.ext?.networkConfig) {
    return;
  }
  const networkConfig = props?.formItem?.ext?.networkConfig;
  let arr: string | any[] = [];
  if (networkConfig) {
    arr = [...networkConfig];
  }
  if (searchName.value.trim() && arr.length > 0) {
    arr = _.filter(props.formItem?.ext?.networkConfig, function (v) {
      const columnNames = Object.keys(arr[0]);
      let isShow = false;
      for (let i = 0; i < columnNames.length; i++) {
        if (
          columnNames[i] === "name" ||
          columnNames[i] === "cidr" ||
          columnNames[i] === "vpcName"
        ) {
          if (v[columnNames[i]]?.toString().indexOf(searchName.value) > -1) {
            isShow = true;
          }
        }
      }
      return isShow;
    });
  }
  _.set(props.formItem, "ext.networkConfig.searchTableData", arr);
};
</script>
<style lang="scss" scoped>
.usage-bar-top-text {
  display: flex;
  flex-direction: row-reverse;
  justify-content: space-between;
  font-size: smaller;
  font-weight: bold;
}

.usage-bar-bottom-text {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  font-size: smaller;
}
</style>
