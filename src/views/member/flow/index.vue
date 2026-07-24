<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="交易流水列表" :columns="flowColumns" :data-source="flowList" :loading="loading"
      row-key="id" :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      @page-change="handleAntPageChange" @refresh="getList">
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="6"><a-form-item label="流水编号"><a-input v-model:value="queryParams.serialCode" allow-clear placeholder="请输入流水编号" @pressEnter="handleQuery" /></a-form-item></a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6"><a-form-item label="用户名"><a-input v-model:value="queryParams.username" allow-clear placeholder="请输入用户名" @pressEnter="handleQuery" /></a-form-item></a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6"><a-form-item label="交易类型"><a-select v-model:value="queryParams.transactionType" allow-clear placeholder="请选择交易类型"><a-select-option v-for="dict in transaction_type" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option></a-select></a-form-item></a-col>
            <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重 置</a-button><a-button type="primary" @click="handleQuery">查 询</a-button></a-space></a-col>
          </a-row>
        </a-form>
      </template>
      <template #toolbar>
        <a-button :disabled="multiple" @click="handleHidden('0')" v-hasPermi="['member:flow:edit']">显示</a-button>
        <a-button :disabled="multiple" @click="handleHidden('1')" v-hasPermi="['member:flow:edit']">隐藏</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dict">{{ dictText(column.dict === 'transaction' ? transaction_type : user_yes_no, record[column.dataIndex]) }}</template>
        <template v-else-if="column.key === 'createdTime'">{{ parseTime(record.createdTime) }}</template>
      </template>
    </ant-pro-table>
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
    transactionType: null,
  },
  rules: {},
});

const { queryParams, form, rules } = toRefs(data);

const flowColumns = [
  { title: "ID", dataIndex: "id", key: "id", width: 80 },
  { title: "流水编号", dataIndex: "serialCode", key: "serialCode", width: 180 },
  { title: "用户名", dataIndex: "username", key: "username", width: 140 },
  { title: "交易类型", dataIndex: "transactionType", key: "transactionType", dict: "transaction", width: 140 },
  { title: "交易前余额", dataIndex: "balanceBefore", key: "balanceBefore", width: 140 },
  { title: "交易金额", dataIndex: "transactionAmount", key: "transactionAmount", width: 140 },
  { title: "交易后余额", dataIndex: "balanceAfter", key: "balanceAfter", width: 140 },
  { title: "是否隐藏", dataIndex: "isHidden", key: "isHidden", dict: "yesNo", width: 120 },
  { title: "交易编号", dataIndex: "transactionCode", key: "transactionCode", width: 180 },
  { title: "创建时间", dataIndex: "createdTime", key: "createdTime", width: 180 },
  { title: "备注", dataIndex: "remark", key: "remark" },
];
const rowSelection = computed(() => ({ selectedRowKeys: ids.value, onChange: (_keys, rows) => handleSelectionChange(rows) }));
function dictText(options, value) { return proxy.selectDictLabel(options, value) || value || "-"; }
function handleAntPageChange({ page, pageSize }) { queryParams.value.pageNum = page; queryParams.value.pageSize = pageSize; getList(); }

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
  queryParams.value.serialCode = null;
  queryParams.value.username = null;
  queryParams.value.transactionType = null;
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

function handleHidden(isHidden) {
  Promise.all(ids.value.map((id) => updateFlow({ id, isHidden }))).then(() => {
    proxy.$modal.msgSuccess("操作成功");
    getList();
  });
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
