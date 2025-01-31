<template>
  <layout-container v-for="group in groups" :key="group.group">
    <template #header>
      <h4>{{ group.name }}</h4>
    </template>
    <template #header_content>
      <p class="description">
        {{ group.description }}
      </p>
    </template>
    <template #content>
      <template v-for="form in group.forms" :key="form.index">
        <template
          v-if="
            form.confirmSpecial && !form.label && form.confirmPosition === 'TOP'
          "
        >
          <!--   无label的特殊组件           -->
          <component
            ref="formItemRef"
            :is="form.inputType"
            :model-value="getDisplayValue(form)"
            :field="form.field"
            :all-data="allData"
            :form-item="form"
            :otherParams="{}"
            :confirm="true"
          />
        </template>
      </template>
      <el-descriptions :column="group.items">
        <el-descriptions-item label="云账号" v-if="group.group === 0">
          <!--          <el-image
            style="margin-top: 3px; width: 16px; height: 16px"
            :src="platformIcon[cloudAccount?.platform].icon"
            v-if="cloudAccount?.platform"
          />-->
          <component
            style="margin-top: 3px; width: 16px; height: 16px"
            :is="platformIcon[cloudAccount?.platform]?.component"
            v-bind="platformIcon[cloudAccount?.platform]?.icon"
            :color="platformIcon[cloudAccount?.platform]?.color"
            size="16px"
            v-if="cloudAccount?.platform"
          ></component>
          <span style="margin-left: 10px">{{ cloudAccount?.name }}</span>
        </el-descriptions-item>
        <template v-for="form in group.forms" :key="form.index">
          <template v-if="form.label && checkShow(form)">
            <el-descriptions-item
              :label="form.label"
              :span="form.confirmItemSpan"
            >
              <template v-if="!form.confirmSpecial">
                <div class="description-inline">
                  <span
                    v-html="getDisplayValue(form)"
                    :title="fieldValueMap[form.field]"
                  />
                </div>
                <span v-if="form.unit">{{ form.unit }}</span>
              </template>
              <template v-else>
                <!--   有label的特殊组件           -->
                <component
                  ref="formItemRef"
                  :is="form.inputType"
                  :model-value="getDisplayValue(form)"
                  :all-data="allData"
                  :field="form.field"
                  :form-item="form"
                  :otherParams="{}"
                  :confirm="true"
                />
              </template>
            </el-descriptions-item>
          </template>
        </template>
      </el-descriptions>
      <template v-for="form in group.forms" :key="form.index">
        <template
          v-if="
            form.confirmSpecial &&
            !form.label &&
            form.confirmPosition === 'BOTTOM'
          "
        >
          <!--   无label的特殊组件           -->
          <component
            ref="formItemRef"
            :is="form.inputType"
            :model-value="getDisplayValue(form)"
            :field="form.field"
            :all-data="allData"
            :form-item="form"
            :otherParams="{}"
            :confirm="true"
          />
        </template>
      </template>
    </template>
  </layout-container>
</template>

<script setup lang="ts">
import type {
  FormViewObject,
  ConfirmAnnotation,
  FormView,
} from "@commons/components/ce-form/type";
import { computed, ref } from "vue";
import _ from "lodash";
import type { CloudAccount } from "@commons/api/cloud_account/type";
import { platformIcon } from "@commons/utils/platform";

const props = defineProps<{
  cloudAccount?: CloudAccount;
  allData?: any;
  allFormViewData?: FormViewObject;
}>();

interface Form extends ConfirmAnnotation {
  forms?: Array<FormView>;
}

const fieldValueMap = ref({});

const groups = computed(() => {
  const tempList = _.sortBy(
    _.values(props.allFormViewData?.confirmAnnotationMap),
    (group) => group.group
  );
  const list: Array<Form> = [];
  tempList.forEach((group) => {
    list.push({
      ...group,
      forms: _.clone(
        _.filter(
          props.allFormViewData?.forms,
          (form) => form.confirmGroup === group.group
        )
      ),
    });
  });

  return list;
});

function initFieldValueMap() {
  groups.value.forEach((group) => {
    group.forms?.forEach((form) => {
      fieldValueMap.value[form.field] = getDisplayValue(form);
    });
  });
}
initFieldValueMap();

function checkShow(currentItem: any) {
  let isShow = currentItem.label;
  if (currentItem.relationShows && currentItem.relationShowValues) {
    isShow = currentItem.relationShows.every((i: string) =>
      currentItem.relationShowValues.includes(
        _.get(props.allData, i) === true ? "true" : _.get(props.allData, i)
      )
    );
  }
  if (isShow) {
    isShow = currentItem.footerLocation === 0;
  }
  return isShow;
}

/**
 * 处理列表中取值展示
 * @param form
 */
function getDisplayValue(form: FormView) {
  const value = _.get(props.allData, form.field);
  let result: any = _.clone(value);
  if (!form.confirmSpecial) {
    if (
      form.optionList &&
      form.optionList instanceof Array &&
      form.optionList.length > 0
    ) {
      if (value instanceof Array) {
        form.valueItem = form.optionList.filter((o) =>
          value.includes(_.get(o, form.valueField ? form.valueField : "value"))
        );
      } else {
        form.valueItem = _.find(
          form.optionList,
          (o) => value === _.get(o, form.valueField ? form.valueField : "value")
        );
      }

      if (form.formatTextField && form.textField) {
        let temp = _.replace(form.textField, /{/g, "{form.valueItem['");
        temp = _.replace(temp, /}/g, "']}");
        result = eval("`" + temp + "`");
      } else {
        if (value instanceof Array) {
          result = "";
          form.valueItem.forEach((item: any) => {
            result =
              result + "," + item[form.textField ? form.textField : "label"];
          });
          result = result.replace(",", "");
        } else {
          result = _.get(
            form.valueItem,
            form.textField ? form.textField : "label"
          );
        }
      }
    }
  }
  if (
    result == undefined ||
    (typeof result === "string" && _.trim(result).length === 0)
  ) {
    if (
      form.optionList &&
      typeof form.optionList === "string" &&
      form.optionList.length > 0
    ) {
      return form.optionList;
    } else {
      return `<span style="color: var(--el-text-color-secondary)">空</span>`;
    }
  } else if (typeof result === "boolean") {
    return result ? "是" : "否";
  } else {
    return result;
  }
}
</script>

<style lang="scss" scoped>
.description-inline {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  span {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
