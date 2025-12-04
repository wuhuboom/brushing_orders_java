<!-- points-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="pointsFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 第一行：状态 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="localForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二行：赠送积分（文本域） -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="赠送积分" prop="giftPoints">
          <el-input
            v-model="localForm.giftPoints"
            type="textarea"
            :rows="5"
            placeholder="请输入赠送积分描述（支持多行文本）"
            style="width: 100%"
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

const pointsFormRef = ref();
const localForm = reactive({ ...props.form });

// 规则
const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  giftPoints: [
    { required: true, message: "请输入赠送积分描述", trigger: "blur" },
    { min: 5, message: "内容长度不能少于5字符", trigger: "blur" },
  ],
});

// 初始化：监听父 form 变化，parse content 到 localForm
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        localForm.status = parsed.status || 1;
        localForm.giftPoints = parsed.giftPoints || "";
      } catch (e) {
        ElMessage.error("解析配置失败");
        localForm.status = 1;
        localForm.giftPoints = "";
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
        status: localForm.status,
        giftPoints: localForm.giftPoints,
      }),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  pointsFormRef.value.validate((valid) => {
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
