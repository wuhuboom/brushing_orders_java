<!-- telegram-bot-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="telegramFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 名称 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="名称" prop="name">
          <el-input
            v-model="localForm.name"
            placeholder="请输入 Telegram 机器人名称"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 令牌 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="令牌" prop="token">
          <el-input
            v-model="localForm.token"
            placeholder="请输入 Telegram 机器人令牌"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- Tab 页面 -->
    <el-tabs v-model="activeTab" type="card" style="margin-bottom: 20px">
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.key"
        :label="tab.label"
        :name="tab.key"
      >
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item
              :label="`${tab.label}是否启用`"
              :prop="`${tab.key}.enabled`"
            >
              <el-radio-group v-model="localForm.tabs[tab.key].enabled">
                <el-radio label="0">启用</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item
              :label="`${tab.label}频道/群组ID`"
              :prop="`${tab.key}.channelId`"
            >
              <el-input
                v-model="localForm.tabs[tab.key].channelId"
                :placeholder="`请输入${tab.label}频道/群组ID`"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="`${tab.label}内容`" :prop="`${tab.key}.content`">
          <editor v-model="localForm.tabs[tab.key].content" :min-height="200" />
        </el-form-item>
      </el-tab-pane>
    </el-tabs>
  </el-form>
</template>
<script setup>
import { ref, reactive, watch, computed } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);

const telegramFormRef = ref();
const activeTab = ref("register"); // 默认第一个 tab

// Tab 配置
const tabs = [
  { key: "register", label: "注册" },
  { key: "login", label: "登录" },
  { key: "withdrawal", label: "提现" },
  { key: "completeTask", label: "完成任务" },
  { key: "signIn", label: "签到" },
  { key: "balanceTreasureOut", label: "余额宝转出" },
  { key: "balanceTreasureIn", label: "余额宝转入" },
];

// 动态规则
const localRules = computed(() => {
  const rules = {
    name: [{ required: true, message: "请输入名称", trigger: "blur" }],
    token: [{ required: true, message: "请输入令牌", trigger: "blur" }],
  };
  tabs.forEach((tab) => {
    rules[`tabs.${tab.key}.enabled`] = [
      {
        required: true,
        message: `请选择${tab.label}是否启用`,
        trigger: "change",
      },
    ];
    rules[`tabs.${tab.key}.channelId`] = [
      {
        required: true,
        message: `请输入${tab.label}频道/群组ID`,
        trigger: "blur",
      },
    ];
    rules[`tabs.${tab.key}.content`] = [
      { required: true, message: `请输入${tab.label}内容`, trigger: "blur" },
      { min: 10, message: "内容不少于10字符", trigger: "blur" },
    ];
  });
  return rules;
});

// localForm 初始化
const localForm = reactive({
  name: "",
  token: "",
  tabs: {},
  ...props.form,
});
tabs.forEach((tab) => {
  localForm.tabs[tab.key] = reactive({
    enabled: "1", // 默认启用
    channelId: "",
    content: "",
  });
});

// 初始化：监听父 form 变化
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        localForm.name = parsed.name || "";
        localForm.token = parsed.token || "";
        tabs.forEach((tab) => {
          if (parsed.tabs && parsed.tabs[tab.key]) {
            Object.assign(localForm.tabs[tab.key], parsed.tabs[tab.key]);
          }
        });
      } catch (e) {
        ElMessage.error("解析配置失败");
      }
    }
  },
  { immediate: true }
);

// 更新父 form
watch(
  localForm,
  () => {
    const tabsData = {};
    tabs.forEach((tab) => {
      tabsData[tab.key] = { ...localForm.tabs[tab.key] };
    });
    emit("update:form", {
      ...props.form,
      content: JSON.stringify({
        name: localForm.name,
        token: localForm.token,
        tabs: tabsData,
      }),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  telegramFormRef.value.validate((valid) => {
    if (valid) {
      emit("submit");
    }
  });
}

// 取消
function handleCancel() {
  emit("cancel");
}
</script>

<style scoped>
/* 样式调整 */
:deep(.editor-container) {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
