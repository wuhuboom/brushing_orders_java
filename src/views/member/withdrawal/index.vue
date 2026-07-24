<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="提现列表"
      :columns="withdrawalColumns"
      :data-source="withdrawalList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 1900 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams">
          <a-row :gutter="24" align="middle">
            <a-col :span="7">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.username"
                  placeholder="请输入用户名"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :span="7">
              <a-form-item label="手机号">
                <a-input
                  v-model:value="queryParams.phoneNumber"
                  placeholder="请输入手机号"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :span="10" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'withdrawalAccount'">
          <div v-if="record.accountMask">{{ record.accountMask }}</div>
          <div v-else-if="record.withdrawalAccountInfo?.type === '1'">
            <div>钱包名称：{{ record.withdrawalAccountInfo.walletName }}</div>
            <div>钱包地址：{{ record.withdrawalAccountInfo.walletAddress }}</div>
          </div>
          <div v-else-if="record.withdrawalAccountInfo">
            <div>银行名称：{{ record.withdrawalAccountInfo.bankName }}</div>
            <div>银行账号：{{ record.withdrawalAccountInfo.bankAccount }}</div>
          </div>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'withdrawalType'">
          {{ record.withdrawalAccountInfo?.withdrawalType || "-" }}
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <dict-tag :options="apply_status" :value="record.status" />
        </template>
        <template v-else-if="column.dataIndex === 'transactionType'">
          <dict-tag :options="transaction_type" :value="record.transactionType" />
        </template>
        <template v-else-if="column.dataIndex === 'isHidden'">
          <dict-tag :options="user_yes_no" :value="record.isHidden" />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="4">
            <a-button
              type="link"
              size="small"
              @click="handleSensitiveAccount(record)"
              v-hasPermi="['member:withdrawal:sensitive']"
            >完整账户</a-button>
            <a-button
              type="link"
              size="small"
              :disabled="String(record.status) !== '1'"
              @click="handleApprove(record)"
              v-hasPermi="['member:withdrawal:edit']"
            >通过</a-button>
            <a-button
              type="link"
              danger
              size="small"
              :disabled="String(record.status) !== '1'"
              @click="handleReject(record)"
              v-hasPermi="['member:withdrawal:edit']"
            >拒绝</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal v-model:open="open" :title="title" width="500px" :mask-closable="false" @cancel="cancel">
      <a-form ref="withdrawalRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="form.remarks" placeholder="请输入备注" :rows="4" allow-clear />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="cancel">取 消</a-button>
          <a-button type="primary" @click="submitForm">确 定</a-button>
        </a-space>
      </template>
    </a-modal>

    <a-modal
      v-model:open="sensitiveOpen"
      title="完整付款账户"
      width="620px"
      :footer="null"
      :destroy-on-close="true"
    >
      <a-descriptions bordered :column="1" size="small">
        <a-descriptions-item label="出金类型">{{ sensitiveAccount.withdrawalType || "-" }}</a-descriptions-item>
        <template v-if="sensitiveAccount.type === '1'">
          <a-descriptions-item label="钱包名称">{{ sensitiveAccount.walletName || "-" }}</a-descriptions-item>
          <a-descriptions-item label="钱包地址">{{ sensitiveAccount.walletAddress || "-" }}</a-descriptions-item>
          <a-descriptions-item label="账户名称">{{ sensitiveAccount.accountName || "-" }}</a-descriptions-item>
        </template>
        <template v-else>
          <a-descriptions-item label="银行名称">{{ sensitiveAccount.bankName || "-" }}</a-descriptions-item>
          <a-descriptions-item label="开户行">{{ sensitiveAccount.branchName || "-" }}</a-descriptions-item>
          <a-descriptions-item label="银行账号">{{ sensitiveAccount.bankAccount || "-" }}</a-descriptions-item>
          <a-descriptions-item label="账户持有人">{{ sensitiveAccount.accountHolder || "-" }}</a-descriptions-item>
        </template>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup name="Withdrawal">
import {
  listWithdrawal,
  getWithdrawal,
  delWithdrawal,
  addWithdrawal,
  updateWithdrawal,
  getSensitiveWithdrawalAccount,
} from "@/api/member/withdrawal";

const { proxy } = getCurrentInstance();
const { transaction_type, apply_status, order_zhlx, user_yes_no } =
  proxy.useDict(
    "transaction_type",
    "apply_status",
    "order_zhlx",
    "user_yes_no"
  );

const withdrawalList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const withdrawalRef = ref();
const sensitiveOpen = ref(false);
const sensitiveAccount = ref({});

const withdrawalColumns = [
  { title: "ID", dataIndex: "id", align: "center", width: 80 },
  { title: "用户名", dataIndex: "username", align: "center", width: 150 },
  { title: "手机号", dataIndex: "phoneNumber", align: "center", width: 150 },
  { title: "上级用户名", dataIndex: "parentUsername", align: "center", width: 150 },
  { title: "提现账户", key: "withdrawalAccount", dataIndex: "withdrawalAccountInfo", width: 280 },
  { title: "金额", dataIndex: "amount", align: "center", width: 120 },
  { title: "出金类型", key: "withdrawalType", align: "center", width: 140 },
  { title: "状态", dataIndex: "status", align: "center", width: 120 },
  { title: "备注", dataIndex: "remarks", align: "center", width: 150 },
  { title: "交易类型", dataIndex: "transactionType", align: "center", width: 130 },
  { title: "订单号", dataIndex: "orderNumber", align: "center", width: 190 },
  { title: "是否隐藏", dataIndex: "isHidden", align: "center", width: 120 },
  { title: "手续费", dataIndex: "fee", align: "center", width: 120 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "操作", key: "operation", align: "center", fixed: "right", width: 260 },
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
    username: null,
    phoneNumber: null,
    userId: null,
    amount: null,
    withdrawalType: null,
    status: null,
    remarks: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
    fee: null,
    withdrawalAccountId: null,
  },
  rules: {
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
    withdrawalType: [{ required: true, message: "出金类型不能为空", trigger: "change" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }],
    transactionType: [{ required: true, message: "交易类型不能为空", trigger: "change" }],
    orderNumber: [{ required: true, message: "订单号不能为空", trigger: "blur" }],
    isHidden: [{ required: true, message: "是否隐藏不能为空", trigger: "blur" }],
    fee: [{ required: true, message: "手续费不能为空", trigger: "blur" }],
    withdrawalAccountId: [{ required: true, message: "提现账号ID不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 鏌ヨ鎻愮幇鍒楄〃 */
function getList() {
  loading.value = true;
  listWithdrawal(queryParams.value).then((response) => {
    withdrawalList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 鍙栨秷鎸夐挳
function cancel() {
  open.value = false;
  reset();
}

// 琛ㄥ崟閲嶇疆
function reset() {
  form.value = {
    id: null,
    userId: null,
    amount: null,
    withdrawalType: null,
    status: null,
    createTime: null,
    remarks: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
    fee: null,
    withdrawalAccountId: null,
  };
  withdrawalRef.value?.clearValidate?.();
}

/** 鎼滅储鎸夐挳鎿嶄綔 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 閲嶇疆鎸夐挳鎿嶄綔 */
function resetQuery() {
  queryParams.value.username = null;
  queryParams.value.phoneNumber = null;
  handleQuery();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

// 澶氶€夋閫変腑鏁版嵁
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 鏂板鎸夐挳鎿嶄綔 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "修改提现";
}

/** 淇敼鎸夐挳鎿嶄綔 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getWithdrawal(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改提现";
  });
}

/** 通过操作 */
function handleApprove(row) {
  proxy.$modal
    .confirm("确认通过该提现申请？")
    .then(() => {
      const updateData = { id: row.id, status: "0" };
      updateWithdrawal(updateData).then(() => {
        proxy.$modal.msgSuccess("操作成功");
        getList();
      });
    })
    .catch(() => {});
}

/** 拒绝操作 */
function handleReject(row) {
  proxy.$modal
    .confirm("确认拒绝该提现申请？")
    .then(() => {
      const updateData = { id: row.id, status: "2" };
      updateWithdrawal(updateData).then(() => {
        proxy.$modal.msgSuccess("操作成功");
        getList();
      });
    })
    .catch(() => {});
}

function handleSensitiveAccount(row) {
  getSensitiveWithdrawalAccount(row.id).then((response) => {
    sensitiveAccount.value = response.data || {};
    sensitiveOpen.value = true;
  });
}

/** 提交按钮 */
function submitForm() {
  withdrawalRef.value?.validate().then(() => {
    if (form.value.id != null) {
      updateWithdrawal(form.value).then(() => {
        proxy.$modal.msgSuccess("操作成功");
        open.value = false;
        getList();
      });
    } else {
      addWithdrawal(form.value).then(() => {
        proxy.$modal.msgSuccess("操作成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(`是否确认删除提现编号为 "${_ids}" 的数据项？`)
    .then(function () {
      return delWithdrawal(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("操作成功");
    })
    .catch(() => {});
}

function handleHidden(isHidden) {
  Promise.all(ids.value.map((id) => updateWithdrawal({ id, isHidden }))).then(() => {
    proxy.$modal.msgSuccess("操作成功");
    getList();
  });
}

/** 瀵煎嚭鎸夐挳鎿嶄綔 */
function handleExport() {
  proxy.download(
    "member/withdrawal/export",
    {
      ...queryParams.value,
    },
    `withdrawal_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
