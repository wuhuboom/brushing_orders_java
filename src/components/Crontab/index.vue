<template>
  <div class="cron-panel">
    <a-space wrap class="cron-presets">
      <a-button v-for="preset in presets" :key="preset.value" @click="applyPreset(preset.value)">
        {{ preset.label }}
      </a-button>
    </a-space>

    <a-form layout="vertical" class="cron-form">
      <a-row :gutter="[16, 12]">
        <a-col
          v-for="field in visibleFields"
          :key="field.key"
          :xs="24"
          :sm="12"
          :md="8"
        >
          <a-form-item :label="field.label">
            <a-input v-model:value="cronFields[field.key]" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <a-descriptions bordered size="small" :column="1" class="cron-result">
      <a-descriptions-item label="Cron 表达式">{{ cronValue }}</a-descriptions-item>
    </a-descriptions>

    <div class="cron-actions">
      <a-space>
        <a-button type="primary" @click="submitFill">确定</a-button>
        <a-button @click="clearCron">重置</a-button>
        <a-button @click="hidePopup">取消</a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup>
const emit = defineEmits(["hide", "fill"]);
const props = defineProps({
  hideComponent: {
    type: Array,
    default: () => [],
  },
  expression: {
    type: String,
    default: "",
  },
});

const fields = [
  { key: "second", label: "秒" },
  { key: "min", label: "分钟" },
  { key: "hour", label: "小时" },
  { key: "day", label: "日" },
  { key: "month", label: "月" },
  { key: "week", label: "周" },
  { key: "year", label: "年" },
];

const presets = [
  { label: "每分钟", value: "0 * * * * ?" },
  { label: "每小时", value: "0 0 * * * ?" },
  { label: "每天零点", value: "0 0 0 * * ?" },
  { label: "每周一", value: "0 0 0 ? * MON" },
  { label: "每月1日", value: "0 0 0 1 * ?" },
];

const cronFields = reactive({
  second: "*",
  min: "*",
  hour: "*",
  day: "*",
  month: "*",
  week: "?",
  year: "",
});

const visibleFields = computed(() =>
  fields.filter((field) => !props.hideComponent?.includes(field.key))
);

const cronValue = computed(() => {
  const base = [
    cronFields.second || "*",
    cronFields.min || "*",
    cronFields.hour || "*",
    cronFields.day || "*",
    cronFields.month || "*",
    cronFields.week || "?",
  ];
  if (cronFields.year) {
    base.push(cronFields.year);
  }
  return base.join(" ");
});

watch(
  () => props.expression,
  (value) => {
    parseExpression(value);
  },
  { immediate: true }
);

function parseExpression(value) {
  if (!value) {
    clearCron();
    return;
  }
  const arr = String(value).trim().split(/\s+/);
  if (arr.length < 6) {
    return;
  }
  Object.assign(cronFields, {
    second: arr[0],
    min: arr[1],
    hour: arr[2],
    day: arr[3],
    month: arr[4],
    week: arr[5],
    year: arr[6] || "",
  });
}

function applyPreset(value) {
  parseExpression(value);
}

function hidePopup() {
  emit("hide");
}

function submitFill() {
  emit("fill", cronValue.value);
  hidePopup();
}

function clearCron() {
  Object.assign(cronFields, {
    second: "*",
    min: "*",
    hour: "*",
    day: "*",
    month: "*",
    week: "?",
    year: "",
  });
}
</script>

<style scoped>
.cron-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cron-presets {
  margin-bottom: 4px;
}

.cron-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.cron-result {
  word-break: break-all;
}

.cron-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
