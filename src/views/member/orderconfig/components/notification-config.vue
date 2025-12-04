<!-- notification-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="notificationFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 通知设置标签页（动态渲染） -->
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
              <el-radio-group v-model="localForm[tab.key].enabled">
                <el-radio :label="0">启用</el-radio>
                <el-radio :label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item
              :label="`${tab.label}标题`"
              :prop="`${tab.key}.title`"
            >
              <el-input
                v-model="localForm[tab.key].title"
                placeholder="请输入标题"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="`${tab.label}内容`" :prop="`${tab.key}.content`">
          <editor v-model="localForm[tab.key].content" :min-height="200" />
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

const notificationFormRef = ref();
const activeTab = ref("gift"); // 默认第一个 tab

// 标签页配置（简洁定义所有23个）
const tabs = [
  { key: "gift", label: "赠送" },
  { key: "deduction", label: "扣款" },
  { key: "recharge", label: "充值" },
  { key: "withdrawing", label: "提现中" },
  { key: "withdrawalUnfreeze", label: "提现解冻" },
  { key: "withdrawal", label: "提现" },
  { key: "task", label: "任务" },
  { key: "principalReturn", label: "本金返还" },
  { key: "rebate", label: "返佣" },
  { key: "subRebate", label: "下级返佣" },
  { key: "signIn", label: "签到" },
  { key: "fee", label: "手续费" },
  { key: "deposit", label: "存款" },
  { key: "bonus", label: "奖金" },
  { key: "baseSalary", label: "底薪" },
  { key: "aid", label: "援助金" },
  { key: "registerBonus", label: "注册赠送" },
  { key: "productShare", label: "商品分润" },
  { key: "taskReward", label: "任务奖励" },
  { key: "balanceOut", label: "余额宝转出" },
  { key: "balanceIn", label: "余额宝转入" },
  { key: "workBonus", label: "工作奖金" },
  { key: "upgradeBonus", label: "升级奖金" },
  { key: "other", label: "其他" },
];

// 动态规则（基于 tabs）
const localRules = computed(() => {
  const rules = {};
  tabs.forEach((tab) => {
    rules[`${tab.key}.enabled`] = [
      {
        required: true,
        message: `请选择${tab.label}是否启用`,
        trigger: "change",
      },
    ];
    rules[`${tab.key}.title`] = [
      { required: true, message: `请输入${tab.label}标题`, trigger: "blur" },
    ];
    rules[`${tab.key}.content`] = [
      { required: true, message: `请输入${tab.label}内容`, trigger: "blur" },
      { min: 10, message: "内容长度不能少于10字符", trigger: "blur" },
    ];
  });
  return rules;
});

// localForm 初始化（动态基于 tabs）
const localForm = reactive({});
tabs.forEach((tab) => {
  localForm[tab.key] = reactive({
    enabled: "", // 默认启用
    title: "",
    content: "",
  });
});

// 初始化：监听父 form 变化，parse content 到 localForm
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        tabs.forEach((tab) => {
          if (parsed[tab.key]) {
            Object.assign(localForm[tab.key], parsed[tab.key]);
          }
        });
      } catch (e) {
        ElMessage.error("解析配置失败");
        // 重置默认值
        tabs.forEach((tab) => {
          localForm[tab.key].enabled = 1;
          localForm[tab.key].title = "";
          localForm[tab.key].content = "";
        });
      }
    }
  },
  { immediate: true }
);

// 更新父 form：localForm 变化时，stringify 到 content
watch(
  localForm,
  () => {
    const data = {};
    tabs.forEach((tab) => {
      data[tab.key] = { ...localForm[tab.key] };
    });
    emit("update:form", {
      ...props.form,
      content: JSON.stringify(data),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  notificationFormRef.value.validate((valid) => {
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
/* 样式调整 editor 容器 */
:deep(.editor-container) {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
