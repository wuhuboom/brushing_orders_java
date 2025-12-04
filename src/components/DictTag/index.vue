<template>
  <div>
    <template v-for="(item, index) in options">
      <template v-if="values.includes(item.value)">
        <span
          v-if="
            (item.elTagType == 'default' || item.elTagType == '') &&
            (item.elTagClass == '' || item.elTagClass == null)
          "
          :key="item.value"
          :index="index"
          :class="item.elTagClass"
          >{{ resolveLabel(item) + " " }}</span
        >
        <el-tag
          v-else
          :disable-transitions="true"
          :key="item.value + ''"
          :index="index"
          :type="item.elTagType"
          :class="item.elTagClass"
          >{{ resolveLabel(item) + " " }}</el-tag
        >
      </template>
    </template>
    <template v-if="unmatch && showValue">
      {{ unmatchArray | handleArray }}
    </template>
  </div>
</template>

<script setup>
import { useI18n } from "vue-i18n";
import typeData from "@/locales/typeData.json";

// 记录未匹配的项
const unmatchArray = ref([]);

const props = defineProps({
  // 数据
  options: {
    type: Array,
    default: null,
  },
  // 当前的值
  value: [Number, String, Array],
  // 当未找到匹配的数据时，显示value
  showValue: {
    type: Boolean,
    default: true,
  },
  separator: {
    type: String,
    default: ",",
  },
});

const { locale } = useI18n();

const localeKey = computed(() => (locale.value === "en_US" ? "en" : "zh"));

const dictTypeName = computed(() => {
  const options = props.options;
  if (
    Array.isArray(options) &&
    Object.prototype.hasOwnProperty.call(options, "__dictType")
  ) {
    return options.__dictType;
  }
  return undefined;
});

function resolveLabel(item) {
  const lang = localeKey.value;
  const valueKey =
    item && item.value !== undefined && item.value !== null
      ? String(item.value)
      : "";
  const originalLabel =
    (item &&
      Object.prototype.hasOwnProperty.call(item, "label") &&
      item.label) ||
    (item &&
      Object.prototype.hasOwnProperty.call(item, "dictLabel") &&
      item.dictLabel) ||
    valueKey;
  const currentType = dictTypeName.value;
  if (
    currentType &&
    Object.prototype.hasOwnProperty.call(typeData, currentType)
  ) {
    const translations = typeData[currentType];
    if (translations) {
      if (Object.prototype.hasOwnProperty.call(translations, valueKey)) {
        const translationEntry = translations[valueKey];
        if (
          translationEntry &&
          Object.prototype.hasOwnProperty.call(translationEntry, lang)
        ) {
          return translationEntry[lang];
        }
      }
      const match = Object.values(translations).find(
        (entry) =>
          entry &&
          Object.prototype.hasOwnProperty.call(entry, "zh") &&
          entry.zh === originalLabel
      );
      if (
        match &&
        Object.prototype.hasOwnProperty.call(match, lang) &&
        match[lang]
      ) {
        return match[lang];
      }
    }
  }
  return originalLabel;
}

const values = computed(() => {
  if (
    props.value === null ||
    typeof props.value === "undefined" ||
    props.value === ""
  )
    return [];
  return Array.isArray(props.value)
    ? props.value.map((item) => "" + item)
    : String(props.value).split(props.separator);
});

const unmatch = computed(() => {
  unmatchArray.value = [];
  // 没有value不显示
  if (
    props.value === null ||
    typeof props.value === "undefined" ||
    props.value === "" ||
    !Array.isArray(props.options) ||
    props.options.length === 0
  )
    return false;
  // 传入值为数组
  let unmatch = false; // 添加一个标志来判断是否有未匹配项
  values.value.forEach((item) => {
    if (!props.options.some((v) => v.value === item)) {
      unmatchArray.value.push(item);
      unmatch = true; // 如果有未匹配项，将标志设置为true
    }
  });
  return unmatch; // 返回标志的值
});

function handleArray(array) {
  if (array.length === 0) return "";
  return array.reduce((pre, cur) => {
    return pre + " " + cur;
  });
}
</script>

<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
