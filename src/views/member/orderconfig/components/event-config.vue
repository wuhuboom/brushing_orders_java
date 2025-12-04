<!-- event-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="eventFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 唯一字段：事件（富文本） -->
    <el-form-item label="事件" prop="eventContent">
      <!-- 用户指定 <editor>，假设项目已注册/导入；v-model 绑定，min-height=400 -->
      <editor v-model="localForm.eventContent" :min-height="400" />
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

const eventFormRef = ref();
const localForm = reactive({ ...props.form });

// 规则
const localRules = reactive({
  eventContent: [
    { required: true, message: "请输入事件内容", trigger: "blur" },
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
        localForm.eventContent = parsed.eventContent || "";
      } catch (e) {
        ElMessage.error("解析配置失败");
        localForm.eventContent = "";
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
        eventContent: localForm.eventContent,
      }),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  eventFormRef.value.validate((valid) => {
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
