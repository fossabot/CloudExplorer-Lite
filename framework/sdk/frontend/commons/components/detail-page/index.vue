<script setup lang="ts">
import { ref } from "vue";

const props = withDefaults(
  defineProps<{
    content: string;
    label?: string;
    value?: string;
  }>(),
  { label: "label", value: "value" }
);

const tooltipRef = ref();
const visible = ref(false); // 控制 tooltip 显示或者隐藏
const currentItem = ref(); // 鼠标选中元素的值
const spanRef = ref(); // 鼠标选中的元素
const showTips = (index: number, e: Event) => {
  spanRef.value = e.currentTarget;
  const spanWidth = spanRef.value.offsetWidth;
  const spanTextWidth = spanRef.value.scrollWidth;
  if (spanTextWidth > spanWidth) {
    visible.value = true;
  }
  currentItem.value = props.content[index];
};
</script>
<template>
  <div class="container">
    <div v-for="(item, index) in content" :key="index" class="item">
      <div>
        <p class="label">
          {{ item[label] }}
        </p>
        <div class="value">
          <span
            v-if="!item.hideValue"
            class="truncate"
            @mouseover="showTips(index, $event)"
            @mouseout="visible = false"
          >
            {{
              item[value] === null || item[value] === "null" ? "-" : item[value]
            }}
          </span>
          <component v-bind:is="item.render" />
        </div>
      </div>
    </div>
    <el-tooltip
      v-if="currentItem && visible"
      ref="tooltipRef"
      :content="currentItem[value]"
      placement="top"
      effect="light"
      virtual-triggering
      :virtual-ref="spanRef"
    />
  </div>
</template>
<style lang="scss" scoped>
.container {
  display: flex;
  flex-wrap: wrap;
  width: 100%;

  .item {
    width: 25%;
    margin-bottom: 8px;

    .label {
      font-style: normal;
      bold: medium;
      font-weight: 400;
      font-size: 14px;
      line-height: 22px;
      color: #6c6c6c;
      margin: 6px 0px;
    }

    .value {
      display: flex;

      .truncate {
        display: inline-block;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 90%;
        white-space: nowrap;
      }
    }
  }
}
</style>
