<!-- work-bonus-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="workBonusFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 第一行：状态 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="localForm.status">
            <el-radio label="1">启用</el-radio>
            <el-radio label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二行：统计类型 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="统计类型" prop="statType">
          <el-radio-group
            v-model="localForm.statType"
            placeholder="请选择统计类型"
            style="width: 100%"
          >
            <el-radio label="日" value="day" />
            <el-radio label="周" value="week" />
            <el-radio label="月" value="month" />
            <el-radio label="年" value="year" />
            <el-radio label="所有" value="all" />
          </el-radio-group>
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
            <el-table-column align="center" label="订单金额" prop="amount">
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
            <el-table-column align="center" label="奖金" prop="bonus">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.bonus"
                  :min="0"
                  :precision="2"
                  @blur="saveRow(scope.$index)"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column align="center" label="操作">
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

const workBonusFormRef = ref();
const localForm = reactive({
  status: "0", // 默认启用
  statType: "day", // 默认日
  levels: [], // 表格数据
  ...props.form,
});

// 规则
const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  statType: [{ required: true, message: "请选择统计类型", trigger: "change" }],
});

// 添加一行
function addRow() {
  localForm.levels.push({ amount: "", bonus: "" });
}

// 保存行
function saveRow(index) {
  const row = localForm.levels[index];
  if (row.amount === "" || row.bonus === "") {
    ElMessage.warning("请填写完整订单金额和奖金");
    return;
  }
  ElMessage.success("保存成功");
}

// 删除行
function deleteRow(index) {
  localForm.levels.splice(index, 1);
  ElMessage.success("删除成功");
}

// 初始化：监听父 form 变化
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        localForm.status = parsed.status || 1;
        localForm.statType = parsed.statType || "day";
        localForm.levels = parsed.levels || [];
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
    emit("update:form", {
      ...props.form,
      content: JSON.stringify({
        status: localForm.status,
        statType: localForm.statType,
        levels: localForm.levels,
      }),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  workBonusFormRef.value.validate((valid) => {
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
:deep(.el-table .cell) {
  padding: 8px 0;
}
</style>
