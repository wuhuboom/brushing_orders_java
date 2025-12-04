<!-- task-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="taskFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 第一行：任务背景图 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="任务背景图" prop="taskBackground">
          <!-- 用户指定 <image-upload>，假设项目已注册/导入；v-model 绑定，limit=1 -->
          <image-upload v-model="localForm.taskBackground" :limit="1" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="连单图片" prop="continuousOrderImage">
          <image-upload v-model="localForm.continuousOrderImage" :limit="1" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="连单标题" prop="continuousOrderTitle">
          <el-input
            v-model="localForm.continuousOrderTitle"
            placeholder="请输入连单标题"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第四行：连单内容 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="连单内容" prop="continuousOrderContent">
          <el-input
            v-model="localForm.continuousOrderContent"
            type="textarea"
            :rows="5"
            placeholder="请输入连单内容"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第五行：任务通知天数，任务通知条数 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="任务通知天数" prop="taskNotificationDays">
          <el-input-number
            v-model="localForm.taskNotificationDays"
            :min="0"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入天数"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="任务通知条数" prop="taskNotificationCount">
          <el-input-number
            v-model="localForm.taskNotificationCount"
            :min="0"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入条数"
          />
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

const taskFormRef = ref();
const localForm = reactive({ ...props.form });

// 规则
const localRules = reactive({
  taskBackground: [
    { required: true, message: "请上传任务背景图", trigger: "change" },
  ],
  continuousOrderImage: [
    { required: true, message: "请上传连单图片", trigger: "change" },
  ],
  continuousOrderTitle: [
    { required: true, message: "请输入连单标题", trigger: "blur" },
  ],
  continuousOrderContent: [
    { required: true, message: "请输入连单内容", trigger: "blur" },
    { min: 5, message: "内容长度不能少于5字符", trigger: "blur" },
  ],
  taskNotificationDays: [
    { required: true, message: "请输入任务通知天数", trigger: "blur" },
  ],
  taskNotificationCount: [
    { required: true, message: "请输入任务通知条数", trigger: "blur" },
  ],
});

// 初始化：监听父 form 变化，parse content 到 localForm
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        localForm.taskBackground = parsed.taskBackground || "";
        localForm.continuousOrderImage = parsed.continuousOrderImage || "";
        localForm.continuousOrderTitle = parsed.continuousOrderTitle || "";
        localForm.continuousOrderContent = parsed.continuousOrderContent || "";
        localForm.taskNotificationDays = parsed.taskNotificationDays || 0;
        localForm.taskNotificationCount = parsed.taskNotificationCount || 0;
      } catch (e) {
        ElMessage.error("解析配置失败");
        // 重置默认值
        localForm.taskBackground = "";
        localForm.continuousOrderImage = "";
        localForm.continuousOrderTitle = "";
        localForm.continuousOrderContent = "";
        localForm.taskNotificationDays = 0;
        localForm.taskNotificationCount = 0;
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
        taskBackground: localForm.taskBackground,
        continuousOrderImage: localForm.continuousOrderImage,
        continuousOrderTitle: localForm.continuousOrderTitle,
        continuousOrderContent: localForm.continuousOrderContent,
        taskNotificationDays: localForm.taskNotificationDays,
        taskNotificationCount: localForm.taskNotificationCount,
      }),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  taskFormRef.value.validate((valid) => {
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
/* 样式调整 textarea */
:deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.5;
  min-height: 120px;
}
</style>
