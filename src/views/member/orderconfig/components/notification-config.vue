<template>
  <a-form
    ref="notificationFormRef"
    :model="localForm"
    layout="vertical"
    class="notification-form"
  >
    <a-tabs v-model:activeKey="activeTab" class="notification-tabs">
      <a-tab-pane
        v-for="notification in notificationKinds"
        :key="notification.key"
        :tab="notification.label"
      >
        <a-row :gutter="[24, 0]">
          <a-col :span="24">
            <a-form-item
              label="是否启用"
              :name="[notification.key, 'enabled']"
              :rules="[{ required: true, message: '请选择是否启用' }]"
            >
              <a-radio-group
                v-model:value="localForm[notification.key].enabled"
                :disabled="readonly"
              >
                <a-radio :value="0">禁用</a-radio>
                <a-radio :value="1">启用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="是否格式化金额"
              :name="[notification.key, 'formatAmount']"
              :rules="[{ required: true, message: '请选择是否格式化金额' }]"
            >
              <a-radio-group
                v-model:value="localForm[notification.key].formatAmount"
                :disabled="readonly"
              >
                <a-radio :value="0">否</a-radio>
                <a-radio :value="1">是</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="标题"
              :name="[notification.key, 'title']"
              :rules="requiredRules(notification.key, '请输入标题')"
            >
              <a-input
                v-model:value="localForm[notification.key].title"
                :disabled="readonly"
                placeholder="请输入标题"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="内容"
              :name="[notification.key, 'content']"
              :rules="requiredRules(notification.key, '请输入内容')"
            >
              <editor
                v-model="localForm[notification.key].content"
                :min-height="260"
                :read-only="readonly"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-tab-pane>
    </a-tabs>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import {
  createNotificationTemplate,
  notificationKinds,
} from "../notificationKinds";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
  readonly: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);
const notificationFormRef = ref();
const activeTab = ref(notificationKinds[0].key);
const localForm = reactive(
  Object.fromEntries(
    notificationKinds.map(({ key }) => [key, createNotificationTemplate()])
  )
);
let hydrating = false;

watch(
  () => props.form.content,
  (content) => {
    hydrating = true;
    try {
      const parsed = content ? JSON.parse(content) : {};
      notificationKinds.forEach(({ key }) => {
        Object.assign(localForm[key], createNotificationTemplate(parsed[key]));
      });
    } catch {
      message.error("通知配置格式错误");
    } finally {
      queueMicrotask(() => {
        hydrating = false;
      });
    }
  },
  { immediate: true }
);

watch(
  localForm,
  () => {
    if (hydrating) {
      return;
    }
    const content = Object.fromEntries(
      notificationKinds.map(({ key }) => [key, { ...localForm[key] }])
    );
    emit("update:form", {
      ...props.form,
      content: JSON.stringify(content),
    });
  },
  { deep: true }
);

function requiredRules(key, messageText) {
  return localForm[key].enabled === 1
    ? [{ required: true, message: messageText, trigger: "blur" }]
    : [];
}

async function handleSubmit() {
  try {
    await notificationFormRef.value?.validate?.();
    emit("submit");
    return true;
  } catch (error) {
    const firstError = error?.errorFields?.[0];
    const invalidTab = firstError?.name?.[0];
    if (invalidTab && notificationKinds.some(({ key }) => key === invalidTab)) {
      activeTab.value = invalidTab;
    }
    message.error(firstError?.errors?.[0] || "请完善通知配置后再提交");
    return false;
  }
}

function handleCancel() {
  emit("cancel");
}

defineExpose({ handleSubmit, handleCancel });
</script>

<style scoped>
.notification-tabs {
  margin-top: 4px;
}

.notification-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}

.notification-tabs :deep(.ant-tabs-nav-wrap) {
  padding-bottom: 2px;
}

.notification-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.notification-form :deep(.editor-container) {
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}
</style>
