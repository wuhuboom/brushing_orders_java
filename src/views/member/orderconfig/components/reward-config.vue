<!-- reward-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="rewardFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 第一行：状态 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="localForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="是否跨级领取" prop="allowCrossLevel">
          <el-radio-group v-model="localForm.allowCrossLevel">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二行：是否跨级领取，是否允许任务中领取 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="是否允许任务中领取" prop="allowInTask">
          <el-radio-group v-model="localForm.allowInTask">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="领取需要完成的任务组数" prop="requiredTaskGroups">
          <el-input-number
            v-model="localForm.requiredTaskGroups"
            :min="0"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入任务组数"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 等级奖励列表表格 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="等级奖励列表">
          <el-table
            :data="localForm.levels"
            border
            style="width: 100%; margin-bottom: 10px"
          >
            <el-table-column label="序号" width="200" align="center">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.order"
                  :min="1"
                  :precision="0"
                  controls-position="right"
                  @blur="sortByOrder"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="奖金" align="center" prop="bonus">
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
  </el-form>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);

const rewardFormRef = ref();

// 初始化表单数据
const localForm = reactive({
  status: 1, // 默认启用
  allowCrossLevel: 0, // 默认不允许跨级领取
  allowInTask: 0, // 默认不允许任务中领取
  requiredTaskGroups: 0, // 默认需要完成的任务组数
  levels: [], // 等级奖励列表
});

// 验证规则
const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  allowCrossLevel: [
    { required: true, message: "请选择是否跨级领取", trigger: "change" },
  ],
  allowInTask: [
    { required: true, message: "请选择是否允许任务中领取", trigger: "change" },
  ],
  requiredTaskGroups: [
    { required: true, message: "请输入需要完成的任务组数", trigger: "blur" },
    { type: "number", min: 0, message: "任务组数不能小于0", trigger: "blur" },
  ],
});

// 组件挂载时初始化数据
onMounted(() => {
  initFormData();
});

// 监听父组件form变化
watch(
  () => props.form,
  (newForm) => {
    if (newForm && newForm.content) {
      try {
        const parsed = JSON.parse(newForm.content);
        Object.assign(localForm, parsed);

        // 确保每个等级都有order字段
        ensureOrderField();
      } catch (e) {
        console.error("解析配置失败:", e);
        ElMessage.error("解析配置失败");
      }
    }
  },
  { immediate: true, deep: true }
);

// 初始化表单数据
function initFormData() {
  if (props.form && props.form.content) {
    try {
      const parsed = JSON.parse(props.form.content);
      Object.assign(localForm, parsed);

      // 确保每个等级都有order字段
      ensureOrderField();
    } catch (e) {
      console.error("解析配置失败:", e);
      ElMessage.error("解析配置失败");
    }
  }
}

// 确保每个等级都有order字段
function ensureOrderField() {
  if (localForm.levels && localForm.levels.length > 0) {
    localForm.levels.forEach((level, index) => {
      if (level.order === undefined) {
        level.order = index + 1;
      }
    });
  }
}

// 添加新行
function addRow() {
  // 计算新行的序号（当前最大序号+1）
  const maxOrder =
    localForm.levels.length > 0
      ? Math.max(...localForm.levels.map((level) => level.order || 0))
      : 0;

  localForm.levels.push({
    order: maxOrder + 1,
    bonus: 0,
  });

  updateParentForm();
  ElMessage.success("添加成功");
}

// 保存行
function saveRow(index) {
  // 这里可以添加行数据的验证逻辑
  ElMessage.success(`第${index + 1}行数据已保存`);
  updateParentForm();
}

// 删除行
function deleteRow(index) {
  localForm.levels.splice(index, 1);
  updateParentForm();
  ElMessage.success("删除成功");
}

// 根据序号排序
function sortByOrder() {
  localForm.levels.sort((a, b) => (a.order || 0) - (b.order || 0));
  updateParentForm();
}

// 更新父组件表单数据
function updateParentForm() {
  emit("update:form", {
    ...props.form,
    content: JSON.stringify(localForm),
  });
}

// 监听localForm变化，更新父组件
watch(
  localForm,
  () => {
    updateParentForm();
  },
  { deep: true }
);

// 提交表单
function handleSubmit() {
  rewardFormRef.value.validate((valid) => {
    if (valid) {
      emit("submit");
    } else {
      ElMessage.error("表单验证失败，请检查输入");
    }
  });
}

// 取消操作
function handleCancel() {
  emit("cancel");
}

// 暴露方法给父组件
defineExpose({
  handleSubmit,
  handleCancel,
});
</script>
