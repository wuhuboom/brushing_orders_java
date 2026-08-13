<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="提现记录列表"
      :columns="withdrawalColumns"
      :data-source="withdrawalList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 2500 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.username"
                  placeholder="请输入用户名"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="手机号码">
                <a-input
                  v-model:value="queryParams.phoneNumber"
                  placeholder="请输入手机号"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="上级用户名">
                <a-input
                  v-model:value="queryParams.parentUsername"
                  placeholder="请输入上级用户名"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col flex="auto" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
                <a-button type="link" @click="advancedSearchVisible = !advancedSearchVisible">
                  {{ advancedSearchVisible ? "收起" : "展开" }}
                </a-button>
              </a-space>
            </a-col>
          </a-row>
          <a-row v-if="advancedSearchVisible" :gutter="[24, 16]" class="advanced-query-row">
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="用户钱包地址">
                <a-input
                  v-model:value="queryParams.accountAddress"
                  allow-clear
                  placeholder="请输入"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="金额">
                <a-space-compact block>
                  <a-input-number
                    v-model:value="queryParams.amountMin"
                    placeholder="请输入"
                    :precision="2"
                    class="amount-range-input"
                  />
                  <a-input disabled value="~" class="amount-range-separator" />
                  <a-input-number
                    v-model:value="queryParams.amountMax"
                    placeholder="请输入"
                    :precision="2"
                    class="amount-range-input"
                  />
                </a-space-compact>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="出金类型">
                <a-select v-model:value="queryParams.withdrawalType" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in order_zhlx" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="状态">
                <a-select v-model:value="queryParams.status" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in apply_status" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="是否假人">
                <a-select v-model:value="queryParams.isFake" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in user_yes_no" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="dateRange"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  show-time
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="交易类型">
                <a-select v-model:value="queryParams.transactionType" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in transaction_type" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="是否隐藏">
                <a-select v-model:value="queryParams.isHidden" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in user_yes_no" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-popconfirm
          title="显示"
          ok-text="确 定"
          cancel-text="取 消"
          :disabled="multiple"
          @confirm="handleHidden('1')"
        >
          <a-button :disabled="multiple" v-hasPermi="['member:withdrawal:edit']">
            <EyeOutlined />显示
          </a-button>
        </a-popconfirm>
        <a-popconfirm
          title="隐藏"
          ok-text="确 定"
          cancel-text="取 消"
          :disabled="multiple"
          @confirm="handleHidden('0')"
        >
          <a-button :disabled="multiple" v-hasPermi="['member:withdrawal:edit']">
            <EyeInvisibleOutlined />隐藏
          </a-button>
        </a-popconfirm>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'withdrawalAccount'">
          <div v-if="record.withdrawalAccountInfo?.type === '1'">
            <div class="account-line">
              <span>钱包名称: {{ record.withdrawalAccountInfo.walletName || "-" }}</span>
              <a-tooltip v-if="String(record.status) === '1' && record.withdrawalAccountInfo.walletName" title="复制">
                <a-button
                  type="link"
                  size="small"
                  aria-label="复制"
                  @click.stop="copyAccountValue(record.withdrawalAccountInfo.walletName)"
                ><CopyOutlined /></a-button>
              </a-tooltip>
            </div>
            <div class="account-line">
              <span>用户钱包地址: {{ record.withdrawalAccountInfo.walletAddress || "-" }}</span>
              <a-tooltip v-if="String(record.status) === '1' && record.withdrawalAccountInfo.walletAddress" title="复制">
                <a-button
                  type="link"
                  size="small"
                  aria-label="复制"
                  @click.stop="copyAccountValue(record.withdrawalAccountInfo.walletAddress)"
                ><CopyOutlined /></a-button>
              </a-tooltip>
            </div>
          </div>
          <div v-else-if="record.withdrawalAccountInfo">
            <div>银行名称: {{ record.withdrawalAccountInfo.bankName || "-" }}</div>
            <div>银行账号: {{ record.withdrawalAccountInfo.bankAccount || "-" }}</div>
          </div>
          <div v-else-if="record.accountMask">{{ record.accountMask }}</div>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'withdrawalType'">
          {{ withdrawalTypeText(record) }}
        </template>
        <template v-else-if="column.key === 'balanceInfo'">
          <div>总余额: {{ plainAmount(totalUserBalance(record)) }}</div>
          <div>余额: {{ plainAmount(record.userBalance) }}</div>
          <div>冻结余额: {{ plainAmount(record.userFrozenBalance) }}</div>
        </template>
        <template v-else-if="column.key === 'attachment'">
          <a-image
            v-if="record.withdrawalAccountInfo?.attachment"
            :src="record.withdrawalAccountInfo.attachment"
            :width="64"
          />
          <span v-else>-</span>
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
        <template v-else-if="column.dataIndex === 'updateTime'">
          {{ parseTime(record.updateTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'remarks' || column.dataIndex === 'updateBy'">
          {{ record[column.dataIndex] || "-" }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="4">
            <a-popconfirm
              title="通过选中的记录？"
              ok-text="确 定"
              cancel-text="取 消"
              :disabled="String(record.status) !== '1'"
              @confirm="handleApprove(record)"
            >
              <a-button
                type="link"
                size="small"
                :disabled="String(record.status) !== '1'"
                v-hasPermi="['member:withdrawal:edit']"
              >通过</a-button>
            </a-popconfirm>
            <a-button
              type="link"
              danger
              size="small"
              :disabled="String(record.status) !== '1'"
              @click="openReviewDialog(record, 'reject')"
              v-hasPermi="['member:withdrawal:edit']"
            >拒绝</a-button>
            <a-button
              type="link"
              size="small"
              @click="openReviewDialog(record, 'remark')"
              v-hasPermi="['member:withdrawal:edit']"
            >备注</a-button>
            <a-button
              type="link"
              size="small"
              :disabled="!record.withdrawalAccountId"
              @click="handleSensitiveAccount(record)"
              v-hasPermi="['member:withdrawal:sensitive']"
            >提现地址</a-button>
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
      title="提现地址"
      width="500px"
      :destroy-on-close="true"
      :mask-closable="false"
      :confirm-loading="sensitiveSubmitting"
      ok-text="确 定"
      cancel-text="取 消"
      @ok="submitSensitiveAccount"
      @cancel="cancelSensitiveAccount"
    >
      <a-form ref="sensitiveRef" :model="sensitiveAccount" layout="vertical">
        <template v-if="sensitiveAccount.type === '1'">
          <a-form-item label="账户名称">
            <a-input v-model:value="sensitiveAccount.accountName" allow-clear />
          </a-form-item>
          <a-form-item label="钱包名称">
            <a-input v-model:value="sensitiveAccount.walletName" allow-clear />
          </a-form-item>
          <a-form-item
            label="用户钱包地址"
            name="walletAddress"
            :rules="[{ required: true, message: '用户钱包地址不能为空', trigger: 'blur' }]"
          >
            <a-input v-model:value="sensitiveAccount.walletAddress" placeholder="用户钱包地址" allow-clear />
          </a-form-item>
        </template>
        <template v-else>
          <a-form-item label="银行名称">
            <a-input v-model:value="sensitiveAccount.bankName" allow-clear />
          </a-form-item>
          <a-form-item label="开户行">
            <a-input v-model:value="sensitiveAccount.branchName" allow-clear />
          </a-form-item>
          <a-form-item label="账户持有人">
            <a-input v-model:value="sensitiveAccount.accountHolder" allow-clear />
          </a-form-item>
          <a-form-item
            label="银行账号"
            name="bankAccount"
            :rules="[{ required: true, message: '银行账号不能为空', trigger: 'blur' }]"
          >
            <a-input v-model:value="sensitiveAccount.bankAccount" allow-clear />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="Withdrawal">
import {
  CopyOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
} from "@ant-design/icons-vue";
import {
  listWithdrawal,
  delWithdrawal,
  updateWithdrawal,
  reviewWithdrawal,
  getSensitiveWithdrawalAccount,
  updateSensitiveWithdrawalAccount,
} from "@/api/member/withdrawal";
import useUserStore from "@/store/modules/user";

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
const advancedSearchVisible = ref(false);
const dateRange = ref([]);
const title = ref("");
const dialogMode = ref("remark");
const withdrawalRef = ref();
const sensitiveOpen = ref(false);
const sensitiveAccount = ref({});
const sensitiveRef = ref();
const sensitiveSubmitting = ref(false);
const userStore = useUserStore();

const withdrawalColumns = [
  { title: "用户名", dataIndex: "username", align: "center", width: 150 },
  { title: "手机号码", dataIndex: "phoneNumber", align: "center", width: 150 },
  { title: "上级用户名", dataIndex: "parentUsername", align: "center", width: 150 },
  { title: "提现账户", key: "withdrawalAccount", dataIndex: "withdrawalAccountInfo", width: 280 },
  { title: "余额信息", key: "balanceInfo", width: 190 },
  { title: "金额", dataIndex: "amount", align: "center", width: 120 },
  { title: "转换后金额", dataIndex: "netAmount", key: "convertedAmount", align: "center", width: 140, hidden: true },
  { title: "附件", key: "attachment", width: 110 },
  { title: "出金类型", key: "withdrawalType", align: "center", width: 140 },
  { title: "状态", dataIndex: "status", align: "center", width: 120 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "备注", dataIndex: "remarks", align: "center", width: 150 },
  { title: "交易类型", dataIndex: "transactionType", align: "center", width: 130 },
  { title: "订单号", dataIndex: "orderNumber", align: "center", width: 190 },
  { title: "是否隐藏", dataIndex: "isHidden", align: "center", width: 120 },
  { title: "手续费", dataIndex: "fee", align: "center", width: 120 },
  { title: "最后修改人", dataIndex: "updateBy", align: "center", width: 140 },
  { title: "最后修改时间", dataIndex: "updateTime", align: "center", width: 180 },
  { title: "操作", key: "operation", align: "center", fixed: "right", width: 240 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_, selectedRows) => handleSelectionChange(selectedRows),
}));

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    username: null,
    phoneNumber: null,
    parentUsername: null,
    accountAddress: null,
    amountMin: null,
    amountMax: null,
    isFake: null,
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

function canViewSensitiveAccounts() {
  const permissions = userStore.permissions || [];
  return permissions.includes("*:*:*") || permissions.includes("member:withdrawal:sensitive");
}

async function enrichSensitiveAccounts(rows) {
  if (!canViewSensitiveAccounts()) return rows;
  return Promise.all(rows.map(async (row) => {
    if (!row.withdrawalAccountId) return row;
    try {
      const response = await getSensitiveWithdrawalAccount(row.id);
      return {
        ...row,
        withdrawalAccountInfo: {
          ...(row.withdrawalAccountInfo || {}),
          ...(response.data || {}),
        },
      };
    } catch {
      return row;
    }
  }));
}

/** 查询提现列表 */
async function getList() {
  loading.value = true;
  const {
    amountMin,
    amountMax,
    ...base
  } = queryParams.value;
  const request = { ...base };
  const params = {};
  if (amountMin !== null && amountMin !== undefined) params.amountMin = amountMin;
  if (amountMax !== null && amountMax !== undefined) params.amountMax = amountMax;
  if (dateRange.value?.length === 2) {
    params.beginTime = dateRange.value[0];
    params.endTime = dateRange.value[1];
  }
  if (Object.keys(params).length) request.params = params;
  try {
    const response = await listWithdrawal(request);
    withdrawalList.value = await enrichSensitiveAccounts(response.rows || []);
    total.value = response.total;
  } finally {
    loading.value = false;
  }
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
  queryParams.value.parentUsername = null;
  queryParams.value.accountAddress = null;
  queryParams.value.amountMin = null;
  queryParams.value.amountMax = null;
  queryParams.value.withdrawalType = null;
  queryParams.value.status = null;
  queryParams.value.isFake = null;
  queryParams.value.transactionType = null;
  queryParams.value.isHidden = null;
  dateRange.value = [];
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
/** 淇敼鎸夐挳鎿嶄綔 */
/** 通过操作 */
function handleApprove(row) {
  return reviewWithdrawal({ id: row.id, status: "2" }).then(() => {
    proxy.$modal.msgSuccess("操作成功");
    getList();
  });
}

/** 拒绝操作 */
function openReviewDialog(row, mode) {
  reset();
  form.value = { ...row };
  dialogMode.value = mode;
  title.value = mode === "reject" ? "拒绝" : "备注";
  open.value = true;
}

function handleSensitiveAccount(row) {
  getSensitiveWithdrawalAccount(row.id).then((response) => {
    sensitiveAccount.value = { ...(response.data || {}) };
    sensitiveOpen.value = true;
    nextTick(() => sensitiveRef.value?.clearValidate?.());
  });
}

/** 提交按钮 */
function submitForm() {
  withdrawalRef.value?.validate().then(() => {
    const request = dialogMode.value === "reject"
      ? reviewWithdrawal({ id: form.value.id, status: "3", remarks: form.value.remarks })
      : updateWithdrawal({ id: form.value.id, remarks: form.value.remarks });
    request.then(() => {
        proxy.$modal.msgSuccess("操作成功");
        open.value = false;
        getList();
      });
  }).catch(() => {});
}

async function submitSensitiveAccount() {
  try {
    await sensitiveRef.value?.validate();
  } catch {
    return;
  }
  sensitiveSubmitting.value = true;
  try {
    await updateSensitiveWithdrawalAccount(
      sensitiveAccount.value.withdrawalId,
      sensitiveAccount.value
    );
    proxy.$modal.msgSuccess("操作成功");
    sensitiveOpen.value = false;
    await getList();
  } finally {
    sensitiveSubmitting.value = false;
  }
}

function cancelSensitiveAccount() {
  sensitiveOpen.value = false;
  sensitiveAccount.value = {};
}

function totalUserBalance(record) {
  return Number(record.userBalance || 0) + Number(record.userFrozenBalance || 0);
}

function plainAmount(value) {
  return value === null || value === undefined || value === "" ? 0 : Number(value);
}

async function copyAccountValue(value) {
  try {
    await navigator.clipboard.writeText(value);
    proxy.$modal.msgSuccess("复制成功");
  } catch {
    proxy.$modal.msgError("复制失败，请手动复制");
  }
}

function withdrawalTypeText(record) {
  const account = record.withdrawalAccountInfo;
  if (!account) return "-";
  const typeOption = (order_zhlx.value || []).find(
    (item) => item && String(item.value) === String(account.type)
  );
  const type = typeOption?.label || (account.type ?? "");
  const channel = account.withdrawalType || "";
  return [type, channel].filter(Boolean).join("-") || "-";
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

<style scoped>
.full-width {
  width: 100%;
}

.account-line {
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}

.account-line :deep(.ant-btn) {
  height: auto;
  padding: 0 4px;
}

.amount-range-input {
  width: calc(50% - 18px);
}

.amount-range-separator {
  width: 36px;
  padding-inline: 8px;
  text-align: center;
}
</style>
