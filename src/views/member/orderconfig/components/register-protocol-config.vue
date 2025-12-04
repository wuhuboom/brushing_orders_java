<!-- register-protocol-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="protocolFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <el-row :gutter="24">
      <el-col :span="24">
        <el-form-item label="序号" prop="sort">
          <el-input v-model.number="localForm.sort" placeholder="请输入序号" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="24">
      <el-col :span="24">
        <el-form-item label="注册协议" prop="protocolContent">
          <editor v-model="localForm.protocolContent" :min-height="400" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);

const protocolFormRef = ref();
const localForm = reactive({ ...props.form });

// 规则
const localRules = reactive({
  protocolContent: [
    { required: true, message: "请输入注册协议内容", trigger: "blur" },
    { min: 10, message: "内容长度不能少于10字符", trigger: "blur" },
  ],
});

// 初始化：监听父 form 变化，parse content 到 localForm
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        localForm.protocolContent = parsed.protocolContent || "";
      } catch (e) {
        ElMessage.error("解析配置失败");
        localForm.protocolContent = "";
      }
    }
  },
  { immediate: true }
);

// 更新父 form：localForm 变化时，stringify 到 content
watch(
  localForm,
  () => {
    emit("update:form", {
      ...props.form,
      content: JSON.stringify({
        protocolContent: localForm.protocolContent,
      }),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  protocolFormRef.value.validate((valid) => {
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
/* 样式调整 textarea 以支持简单 HTML 预览 */
:deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.5;
  min-height: 300px;
}
</style>
