<!-- sign-in-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="signInFormRef"
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
    <!-- 第一行：状态，自动签到，连续签到验证 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="localForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="自动签到" prop="autoSign">
          <el-radio-group v-model="localForm.autoSign">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二行：最低完成任务次数，赠送金额 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="连续签到验证" prop="continuousVerify">
          <el-radio-group v-model="localForm.continuousVerify">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="最低完成任务次数" prop="minTaskCount">
          <el-input-number
            v-model="localForm.minTaskCount"
            :min="0"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入最低任务次数"
          />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="赠送金额" prop="giftAmount">
          <el-input
            v-model="localForm.giftAmount"
            type="textarea"
            :rows="3"
            placeholder="请输入赠送金额（支持多行描述）"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 可扩展更多行，如签到奖励规则等 -->
  </el-form>
</template>

<script setup>
import { ref, reactive, watch, nextTick } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);

const signInFormRef = ref();
const localForm = reactive({ ...props.form });

// 初始化后立即删除通用字段，确保 localForm 干净
["id", "type", "name", "createTime", "updateTime", "content"].forEach((key) => {
  delete localForm[key];
});

// 所有规则都在子组件中定义
const localRules = reactive({
  sort: [{ required: true, message: "序号不能为空", trigger: "blur" }],
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  autoSign: [{ required: true, message: "请选择自动签到", trigger: "change" }],
  continuousVerify: [
    { required: true, message: "请选择连续签到验证", trigger: "change" },
  ],
  minTaskCount: [
    { required: true, message: "请输入最低完成任务次数", trigger: "blur" },
  ],
  giftAmount: [{ required: true, message: "请输入赠送金额", trigger: "blur" }],
});

// 初始化：监听父 form 变化，parse content 到 localForm（仅 sign-in 字段）
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        // 只合并 sign-in 特定字段，排除通用字段
        const signInFields = {
          status: parsed.status,
          autoSign: parsed.autoSign,
          continuousVerify: parsed.continuousVerify,
          minTaskCount: parsed.minTaskCount,
          giftAmount: parsed.giftAmount,
          // sort 从父 form 取，不从 content
          sort: localForm.sort,
        };
        Object.assign(localForm, signInFields);
      } catch (e) {
        ElMessage.error("解析配置失败");
      }
    }
  },
  { immediate: true }
);

// 更新父 form：localForm 变化时，过滤不想要的字段后 stringify 到 content，并同步 sort
watch(
  localForm,
  () => {
    // 创建只含 sign-in 字段的 toSave
    const toSave = {
      status: localForm.status,
      autoSign: localForm.autoSign,
      continuousVerify: localForm.continuousVerify,
      minTaskCount: localForm.minTaskCount,
      giftAmount: localForm.giftAmount,
      // sort 不放入 content，只同步到父 form
    };

    emit("update:form", {
      ...props.form,
      sort: localForm.sort, // 单独同步 sort
      content: JSON.stringify(toSave),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  signInFormRef.value.validate((valid) => {
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
/* 如需样式，可添加 */
</style>
