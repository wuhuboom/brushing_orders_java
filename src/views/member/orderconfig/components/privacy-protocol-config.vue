<!-- privacy-protocol-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="privacyFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 唯一字段：隐私协议（富文本） -->
    <el-form-item label="隐私协议" prop="privacyContent">
      <!-- 用户指定 <editor>，假设项目已注册/导入；v-model 绑定，min-height=400 -->
      <editor v-model="localForm.privacyContent" :min-height="400" />
    </el-form-item>
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

const privacyFormRef = ref();
const localForm = reactive({ ...props.form });

// 规则
const localRules = reactive({
  privacyContent: [
    { required: true, message: "请输入隐私协议内容", trigger: "blur" },
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
        localForm.privacyContent = parsed.privacyContent || "";
      } catch (e) {
        ElMessage.error("解析配置失败");
        localForm.privacyContent = "";
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
        privacyContent: localForm.privacyContent,
      }),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  privacyFormRef.value.validate((valid) => {
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
