<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="充值记录"
      :columns="rechargeColumns"
      :data-source="rechargeList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 1600 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.userName"
                  placeholder="请输入用户名"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="手机号">
                <a-input
                  v-model:value="queryParams.phoneNumber"
                  placeholder="请输入手机号"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="订单号">
                <a-input
                  v-model:value="queryParams.orderNumber"
                  placeholder="请输入订单号"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col flex="auto" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-button :disabled="multiple" @click="handleHidden('0')" v-hasPermi="['member:recharge:edit']">显示</a-button>
        <a-button :disabled="multiple" @click="handleHidden('1')" v-hasPermi="['member:recharge:edit']">隐藏</a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <dict-tag :options="apply_status" :value="record.status" />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'transactionType'">
          <dict-tag :options="transaction_type" :value="record.transactionType" />
        </template>
        <template v-else-if="column.dataIndex === 'isHidden'">
          <dict-tag :options="user_yes_no" :value="record.isHidden" />
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="4">
            <a-button
              type="link"
              size="small"
              :disabled="String(record.status) !== '1'"
              @click="handleApprove(record)"
              v-hasPermi="['member:recharge:edit']"
            >通过</a-button>
            <a-button
              type="link"
              danger
              size="small"
              :disabled="String(record.status) !== '1'"
              @click="handleReject(record)"
              v-hasPermi="['member:recharge:edit']"
            >拒绝</a-button>
            <a-button
              type="link"
              size="small"
              @click="handleUpdate(record)"
              v-hasPermi="['member:recharge:edit']"
            >备注</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      :title="title"
      v-model:open="open"
      width="500px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="submitting"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form
        ref="rechargeRef"
        :model="form"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="form.remark"
            placeholder="请输入内容"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="Recharge">
import {
  listRecharge,
  getRecharge,
  delRecharge,
  addRecharge,
  updateRecharge,
} from "@/api/member/recharge";

const { proxy } = getCurrentInstance();
const { transaction_type, user_yes_no, apply_status } = proxy.useDict(
  "transaction_type",
  "user_yes_no",
  "apply_status"
);

const rechargeList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const submitting = ref(false);
const rechargeRef = ref();

const rechargeColumns = [
  { title: "用户名", dataIndex: "username", align: "center", width: 140 },
  { title: "手机号", dataIndex: "phoneNumber", align: "center", width: 140 },
  { title: "上级用户名", dataIndex: "parentUsername", align: "center", width: 150 },
  { title: "金额", dataIndex: "amount", align: "center", width: 120 },
  { title: "充值类型", dataIndex: "withdrawalType", align: "center", width: 130 },
  { title: "赠送金额", dataIndex: "giftAmount", align: "center", width: 130 },
  { title: "到账金额", dataIndex: "receivedAmount", align: "center", width: 130 },
  { title: "状态", key: "status", dataIndex: "status", align: "center", width: 110 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "备注", dataIndex: "remark", align: "center", width: 150 },
  { title: "交易类型", dataIndex: "transactionType", align: "center", width: 130 },
  { title: "订单号", dataIndex: "orderNumber", align: "center", width: 180 },
  { title: "是否隐藏", dataIndex: "isHidden", align: "center", width: 120 },
  { title: "操作", key: "operation", align: "center", fixed: "right", width: 100 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_, selectedRows) => handleSelectionChange(selectedRows),
}));

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userName: null,
    phoneNumber: null,
    userId: null,
    amount: null,
    withdrawalType: null,
    giftAmount: null,
    receivedAmount: null,
    status: null,
    createdTime: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
  },
  rules: {
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
    withdrawalType: [{ required: true, message: "充值类型不能为空", trigger: "change" }],
    receivedAmount: [{ required: true, message: "到账金额不能为空", trigger: "blur" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }],
    transactionType: [{ required: true, message: "交易类型不能为空", trigger: "change" }],
    orderNumber: [{ required: true, message: "订单号不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询充值记录列表 */
function getList() {
  loading.value = true;
  listRecharge(queryParams.value).then((response) => {
    rechargeList.value = response.rows;
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
    userId: null,
    amount: null,
    withdrawalType: null,
    giftAmount: null,
    receivedAmount: null,
    status: null,
    createdTime: null,
    remark: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
  };
  nextTick(() => rechargeRef.value?.clearValidate?.());
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.userName = null;
  queryParams.value.phoneNumber = null;
  queryParams.value.orderNumber = null;
  handleQuery();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
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
  title.value = "修改备注";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getRecharge(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改备注";
  });
}

function handleApprove(row) {
  proxy.$modal
    .confirm("确认通过该充值申请？")
    .then(() => updateRecharge({ id: row.id, status: "0" }))
    .then(() => {
      proxy.$modal.msgSuccess("操作成功");
      getList();
    })
    .catch(() => {});
}

function handleReject(row) {
  proxy.$modal
    .confirm("确认拒绝该充值申请？")
    .then(() => updateRecharge({ id: row.id, status: "2" }))
    .then(() => {
      proxy.$modal.msgSuccess("操作成功");
      getList();
    })
    .catch(() => {});
}

/** 提交按钮 */
async function submitForm() {
  try {
    await rechargeRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (form.value.id != null) {
      await updateRecharge(form.value);
    } else {
      await addRecharge(form.value);
    }
    proxy.$modal.msgSuccess("操作成功");
    open.value = false;
    getList();
  } finally {
    submitting.value = false;
  }
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(`是否确认删除充值记录编号为 "${_ids}" 的数据项？`)
    .then(function () {
      return delRecharge(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("操作成功");
    })
    .catch(() => {});
}

function handleHidden(isHidden) {
  Promise.all(ids.value.map((id) => updateRecharge({ id, isHidden }))).then(() => {
    proxy.$modal.msgSuccess("操作成功");
    getList();
  });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/recharge/export",
    {
      ...queryParams.value,
    },
    `recharge_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
