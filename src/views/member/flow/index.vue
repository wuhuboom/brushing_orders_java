<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="流水编号" prop="serialCode">
        <el-input
          v-model="queryParams.serialCode"
          placeholder="请输入流水编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="交易类型" prop="transactionType">
        <el-select
          v-model="queryParams.transactionType"
          placeholder="请选择交易类型"
          style="width: 240px"
          clearable
        >
          <el-option
            v-for="dict in transaction_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery"
          >搜索</el-button
        >
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="flowList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="流水编号" align="center" prop="serialCode" />
      <el-table-column label="用户名" align="center" prop="username" />
      <el-table-column label="交易类型" align="center" prop="transactionType">
        <template #default="scope">
          <dict-tag
            :options="transaction_type"
            :value="scope.row.transactionType"
          />
        </template>
      </el-table-column>
      <el-table-column label="交易前余额" align="center" prop="balanceBefore" />
      <el-table-column
        label="交易金额"
        align="center"
        prop="transactionAmount"
      />
      <el-table-column label="交易后余额" align="center" prop="balanceAfter" />
      <el-table-column label="是否隐藏" align="center" prop="isHidden">
        <template #default="scope">
          <dict-tag :options="user_yes_no" :value="scope.row.isHidden" />
        </template>
      </el-table-column>
      <el-table-column label="交易编号" align="center" prop="transactionCode" />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createdTime"
        width="180"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.createdTime, "{y}-{m}-{d}") }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="Flow">
import {
  listFlow,
  getFlow,
  delFlow,
  addFlow,
  updateFlow,
} from "@/api/member/flow";

const { proxy } = getCurrentInstance();
const { user_yes_no, transaction_type } = proxy.useDict(
  "user_yes_no",
  "transaction_type"
);

const flowList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    serialCode: null,
    username: null,
  },
  rules: {},
});

const { queryParams, form, rules } = toRefs(data);

/** 查询交易流水列表 */
function getList() {
  loading.value = true;
  listFlow(queryParams.value).then((response) => {
    flowList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    serialCode: null,
    userId: null,
    transactionType: null,
    balanceBefore: null,
    transactionAmount: null,
    balanceAfter: null,
    isHidden: null,
    transactionCode: null,
    createdTime: null,
    remark: null,
  };
  proxy.resetForm("flowRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加交易流水";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getFlow(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改交易流水";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["flowRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateFlow(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addFlow(form.value).then((response) => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除交易流水编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delFlow(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/flow/export",
    {
      ...queryParams.value,
    },
    `flow_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
