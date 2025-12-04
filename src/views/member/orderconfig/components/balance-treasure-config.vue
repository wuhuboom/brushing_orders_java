<!-- balance-treasure-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="balanceFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 第一行：状态 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="localForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二行：累计存储时长 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="累计存储时长" prop="cumulativeStorageDuration">
          <el-input-number
            v-model="localForm.cumulativeStorageDuration"
            :min="0"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入时长（天）"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 级别表格 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="级别">
          <el-table
            :data="localForm.levels"
            border
            style="width: 100%; margin-bottom: 10px"
          >
            <el-table-column label="金额" align="center" prop="amount">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.amount"
                  :min="0"
                  :precision="2"
                  @blur="saveRow(scope.$index)"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="日利率 (%)" align="center" prop="dailyRate">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.dailyRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  @blur="saveRow(scope.$index)"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center">
              <template #default="scope">
                <el-button
                  type="primary"
                  size="small"
                  @click="saveRow(scope.$index)"
                  >保存</el-button
                >
                <el-button
                  type="danger"
                  size="small"
                  @click="deleteRow(scope.$index)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
          <el-button type="primary" @click="addRow">添加一行数据</el-button>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 规则（富文本） -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="规则" prop="rules">
          <editor v-model="localForm.rules" :min-height="200" />
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

const balanceFormRef = ref();
const localForm = reactive({
  status: 1, // 默认启用
  cumulativeStorageDuration: 0,
  levels: [], // 表格数据数组
  rules: "",
  ...props.form,
});

// 规则
const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  cumulativeStorageDuration: [
    { required: true, message: "请输入累计存储时长", trigger: "blur" },
  ],
  rules: [
    { required: true, message: "请输入规则内容", trigger: "blur" },
    { min: 10, message: "内容不少于10字符", trigger: "blur" },
  ],
});

// 添加一行
function addRow() {
  localForm.levels.push({ amount: "", dailyRate: "" });
}

// 保存行
function saveRow(index) {
  const row = localForm.levels[index];
  if (row.amount === "" || row.dailyRate === "") {
    ElMessage.warning("请填写完整金额和日利率");
    return;
  }
  ElMessage.success("保存成功");
}

// 删除行
function deleteRow(index) {
  localForm.levels.splice(index, 1);
  ElMessage.success("删除成功");
}

// 初始化：监听父 form 变化，parse content 到 localForm
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        localForm.status = parsed.status || 1;
        localForm.cumulativeStorageDuration =
          parsed.cumulativeStorageDuration || 0;
        localForm.levels = parsed.levels || [];
        localForm.rules = parsed.rules || "";
      } catch (e) {
        ElMessage.error("解析配置失败");
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
        cumulativeStorageDuration: localForm.cumulativeStorageDuration,
        levels: localForm.levels,
        rules: localForm.rules,
      }),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  balanceFormRef.value.validate((valid) => {
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
