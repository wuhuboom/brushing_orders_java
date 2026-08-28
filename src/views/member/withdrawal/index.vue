<template>
  <div class="app-container ant-pro-member-page transaction-alignment-page">
    <ant-pro-table
      :key="tableResetKey"
      title="提现记录列表"
      :columns="withdrawalColumns"
      :data-source="withdrawalList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total, size: 'small', showSizeChanger: true }"
      :scroll="{ x: 2702, y: 'calc(100vh - 440px)' }"
      @page-change="handleAntPageChange"
      @change="handleTableChange"
      @refresh="handleRefresh"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 24]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.username"
                  placeholder="请输入用户名"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="手机号码">
                <a-input
                  v-model:value="queryParams.phoneNumber"
                  placeholder="请输入手机号"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="上级用户名">
                <a-input
                  v-model:value="queryParams.parentUsername"
                  placeholder="请输入上级用户名"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="用户钱包地址">
                <a-input
                  v-model:value="queryParams.accountAddress"
                  allow-clear
                  placeholder="请输入"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="金额">
                <a-space-compact block>
                  <a-input-number
                    v-model:value="queryParams.amountMin"
                    string-mode
                    placeholder="请输入"
                    :precision="2"
                    class="amount-range-input"
                  />
                  <a-input disabled value="~" class="amount-range-separator" />
                  <a-input-number
                    v-model:value="queryParams.amountMax"
                    string-mode
                    placeholder="请输入"
                    :precision="2"
                    class="amount-range-input"
                  />
                </a-space-compact>
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="出金类型">
                <a-select v-model:value="queryParams.withdrawalType" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in order_zhlx" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="状态">
                <a-select v-model:value="queryParams.status" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in apply_status" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="是否假人">
                <a-select v-model:value="queryParams.isFake" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in liveYesNoOptions" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="dateRange"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  show-time
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="交易类型">
                <a-select v-model:value="queryParams.transactionType" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in liveTransactionTypeOptions" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="是否隐藏">
                <a-select v-model:value="queryParams.isHidden" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in liveYesNoOptions" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
                <a-button type="link" @click="advancedSearchVisible = !advancedSearchVisible">
                  {{ advancedSearchVisible ? "收起" : "展开" }}
                </a-button>
              </a-space>
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
          <a-button type="primary" :disabled="multiple" v-hasPermi="['member:withdrawal:edit']">
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
          <a-button type="primary" :disabled="multiple" v-hasPermi="['member:withdrawal:edit']">
            <EyeInvisibleOutlined />隐藏
          </a-button>
        </a-popconfirm>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'withdrawalAccount'">
          <div v-if="record.withdrawalAccountInfo?.type === '1'">
            <div v-if="record.withdrawalAccountInfo.accountName" class="account-line">
              <span>账户名称: {{ record.withdrawalAccountInfo.accountName }}</span>
            </div>
            <div class="account-line">
              <span>钱包名称: {{ record.withdrawalAccountInfo.walletName || "-" }}</span>
              <a-tooltip v-if="record.withdrawalAccountInfo.walletName" title="复制">
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
              <a-tooltip v-if="record.withdrawalAccountInfo.walletAddress" title="复制">
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
            <div class="account-line"><span>银行名称: {{ record.withdrawalAccountInfo.bankName || "-" }}</span></div>
            <div v-if="record.withdrawalAccountInfo.depositType" class="account-line">
              <span>存款种类: {{ record.withdrawalAccountInfo.depositType }}</span>
            </div>
            <div v-if="record.withdrawalAccountInfo.branchCode" class="account-line">
              <span>支行代码: {{ record.withdrawalAccountInfo.branchCode }}</span>
            </div>
            <div v-if="record.withdrawalAccountInfo.branchName" class="account-line">
              <span>支行名称: {{ record.withdrawalAccountInfo.branchName }}</span>
            </div>
            <div v-if="record.withdrawalAccountInfo.accountHolder" class="account-line">
              <span>账户持有人: {{ record.withdrawalAccountInfo.accountHolder }}</span>
            </div>
            <div v-if="record.withdrawalAccountInfo.accountName" class="account-line">
              <span>账户名称: {{ record.withdrawalAccountInfo.accountName }}</span>
            </div>
            <div class="account-line">
              <span>银行账号: {{ record.withdrawalAccountInfo.bankAccount || "-" }}</span>
              <a-tooltip v-if="record.withdrawalAccountInfo.bankAccount" title="复制">
                <a-button
                  type="link"
                  size="small"
                  aria-label="复制"
                  @click.stop="copyAccountValue(record.withdrawalAccountInfo.bankAccount)"
                ><CopyOutlined /></a-button>
              </a-tooltip>
            </div>
          </div>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'withdrawalType'">
          {{ withdrawalTypeText(record) }}
        </template>
        <template v-else-if="column.key === 'balanceInfo'">
          <div>总余额: {{ plainAmount(totalUserBalance(record)) }}</div>
          <div :class="{ 'negative-amount': isNegativeAmount(record.userBalance) }">
            余额: {{ plainAmount(record.userBalance) }}
          </div>
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
          <a-badge
            :status="applyBadge(record.status).status"
            :text="applyBadge(record.status).text"
          />
        </template>
        <template v-else-if="column.dataIndex === 'transactionType'">
          <a-badge status="processing" :text="transactionTypeText(record.transactionType)" />
        </template>
        <template v-else-if="column.dataIndex === 'isHidden'">
          <a-badge
            :status="hiddenBadge(record.isHidden).status"
            :text="hiddenBadge(record.isHidden).text"
          />
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
                :disabled="String(record.status) !== '1'"
                v-hasPermi="['member:withdrawal:edit']"
              >通过</a-button>
            </a-popconfirm>
            <a-button
              type="link"
              danger
              :disabled="String(record.status) !== '1'"
              @click="openReviewDialog(record, 'reject')"
              v-hasPermi="['member:withdrawal:edit']"
            >拒绝</a-button>
            <a-button
              type="link"
              @click="openReviewDialog(record, 'remark')"
              v-hasPermi="['member:withdrawal:edit']"
            >备注</a-button>
            <a-button
              type="link"
              :disabled="!record.withdrawalAccountId"
              @click="handleSensitiveAccount(record)"
              v-hasPermi="['member:withdrawal:sensitive']"
            >提现地址</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      v-if="dialogMode === 'reject'"
      v-model:open="open"
      title="拒绝"
      width="450px"
      :mask-closable="false"
      :confirm-loading="submitting"
      ok-text="确 定"
      cancel-text="取 消"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form ref="withdrawalRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="form.remarks" placeholder="请输入备注" :rows="4" allow-clear />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-else
      v-model:open="open"
      title="备注"
      width="85%"
      destroy-on-close
      :mask-closable="false"
      :closable="!submitting"
      :keyboard="!submitting"
      @close="cancel"
    >
      <a-form ref="withdrawalRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="form.remarks" placeholder="请输入备注" :rows="4" allow-clear />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="drawer-footer">
          <a-space>
            <a-button :disabled="submitting" @click="cancel">取 消</a-button>
            <a-button type="primary" :loading="submitting" @click="submitForm">确 定</a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>

    <a-drawer
      v-model:open="sensitiveOpen"
      title="提现地址"
      width="85%"
      destroy-on-close
      :mask-closable="false"
      :closable="!sensitiveSubmitting"
      :keyboard="!sensitiveSubmitting"
      @close="cancelSensitiveAccount"
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
      <template #footer>
        <div class="drawer-footer">
          <a-space>
            <a-button :disabled="sensitiveSubmitting" @click="cancelSensitiveAccount">取 消</a-button>
            <a-button type="primary" :loading="sensitiveSubmitting" @click="submitSensitiveAccount">确 定</a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>
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
let listRequestId = 0;
const tableResetKey = ref(0);
const advancedSearchVisible = ref(false);
const dateRange = ref([]);
const title = ref("");
const dialogMode = ref("remark");
const withdrawalRef = ref();
const submitting = ref(false);
const sensitiveOpen = ref(false);
const sensitiveAccount = ref({});
const sensitiveRef = ref();
const sensitiveSubmitting = ref(false);

const liveYesNoOptions = computed(() => [
  { value: "1", label: "否" },
  { value: "0", label: "是" },
].map((expected) => {
  const option = (user_yes_no.value || []).find(
    (item) => String(item?.value) === expected.value
  );
  return option ? { ...option, label: expected.label } : expected;
}));

const liveTransactionTypeOptions = computed(() => [
  { value: "zs", label: "赠送" },
  { value: "kk", label: "扣款" },
  { value: "cz", label: "充值" },
  { value: "txz", label: "提现中" },
  { value: "txjd", label: "提现解冻" },
  { value: "tx", label: "提现" },
  { value: "rw", label: "任务" },
  { value: "bjfh", label: "本金返回" },
  { value: "fy", label: "返佣" },
  { value: "xjfy", label: "下级返佣" },
].map((expected) => {
  const option = (transaction_type.value || []).find(
    (item) => String(item?.value) === expected.value
  );
  return option ? { ...option, label: expected.label } : expected;
}));

const withdrawalColumns = [
  { title: "用户名", dataIndex: "username", key: "username", width: 140 },
  { title: "手机号码", dataIndex: "phoneNumber", key: "phoneNumber", width: 140 },
  { title: "上级用户名", dataIndex: "parentUsername", key: "parentUsername", width: 140 },
  { title: "提现账户", key: "withdrawalAccount", dataIndex: "withdrawalAccountInfo", width: 300 },
  { title: "余额信息", key: "balanceInfo", width: 180 },
  { title: "金额", dataIndex: "amount", key: "amount", width: 120, sorter: true },
  { title: "附件", key: "attachment", width: 100 },
  { title: "出金类型", key: "withdrawalType", width: 120, sorter: true },
  { title: "状态", dataIndex: "status", key: "status", width: 100, sorter: true },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 170, sorter: true },
  { title: "备注", dataIndex: "remarks", key: "remarks", width: 200 },
  { title: "交易类型", dataIndex: "transactionType", key: "transactionType", width: 100, sorter: true },
  { title: "订单号", dataIndex: "orderNumber", key: "orderNumber", width: 200, sorter: true, defaultSortOrder: "descend" },
  { title: "是否隐藏", dataIndex: "isHidden", key: "isHidden", width: 100, sorter: true },
  { title: "手续费", dataIndex: "fee", key: "fee", width: 80 },
  { title: "最后修改人", dataIndex: "updateBy", key: "updateBy", width: 120 },
  { title: "最后修改时间", dataIndex: "updateTime", key: "updateTime", width: 170 },
  { title: "操作", key: "operation", fixed: "right", width: 190 },
];

const rowSelection = computed(() => ({
  columnWidth: 32,
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
    orderByColumn: "ow.order_number",
    isAsc: "desc",
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

function dictText(options, value) {
  const values = Array.isArray(options) ? options : options?.value;
  return proxy.selectDictLabel((values || []).filter(Boolean), value) || value || "-";
}

const applyBadgeMap = Object.freeze({
  "1": Object.freeze({ status: "processing", text: "待审核" }),
  "2": Object.freeze({ status: "success", text: "已通过" }),
  "3": Object.freeze({ status: "error", text: "已拒绝" }),
});

const hiddenBadgeMap = Object.freeze({
  "0": Object.freeze({ status: "error", text: "是" }),
  "1": Object.freeze({ status: "error", text: "否" }),
});

const sortColumnMap = Object.freeze({
  amount: "ow.amount",
  withdrawalType: "gwa.withdrawal_type_id",
  status: "ow.status",
  createTime: "ow.create_time",
  transactionType: "ow.transaction_type",
  orderNumber: "ow.order_number",
  isHidden: "ow.is_hidden",
});

function applyBadge(value) {
  return applyBadgeMap[String(value)] || { status: "default", text: dictText(apply_status, value) };
}

function hiddenBadge(value) {
  return hiddenBadgeMap[String(value)] || { status: "default", text: dictText(user_yes_no, value) };
}

function transactionTypeText(value) {
  const option = liveTransactionTypeOptions.value.find(
    (item) => String(item.value) === String(value)
  );
  return option?.label || dictText(transaction_type, value);
}

function clearSelection() {
  ids.value = [];
  single.value = true;
  multiple.value = true;
}

/** 查询提现列表 */
async function getList() {
  const requestId = ++listRequestId;
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
    if (requestId !== listRequestId) return;
    withdrawalList.value = response.rows || [];
    total.value = response.total || 0;
  } finally {
    if (requestId === listRequestId) loading.value = false;
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
  clearSelection();
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
  queryParams.value.orderByColumn = "ow.order_number";
  queryParams.value.isAsc = "desc";
  dateRange.value = [];
  tableResetKey.value += 1;
  handleQuery();
}

function handleAntPageChange({ page, pageSize }) {
  clearSelection();
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

function handleRefresh() {
  clearSelection();
  getList();
}

function handleTableChange(_pagination, _filters, sorter) {
  const columnKey = sorter?.columnKey;
  queryParams.value.orderByColumn = sorter?.order ? sortColumnMap[columnKey] || null : null;
  queryParams.value.isAsc = sorter?.order === "ascend"
    ? "asc"
    : sorter?.order === "descend"
      ? "desc"
      : null;
  clearSelection();
  queryParams.value.pageNum = 1;
  getList();
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
async function submitForm() {
  try {
    await withdrawalRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (dialogMode.value === "reject") {
      await reviewWithdrawal({ id: form.value.id, status: "3", remarks: form.value.remarks });
    } else {
      await updateWithdrawal({ id: form.value.id, remarks: form.value.remarks });
    }
    proxy.$modal.msgSuccess("操作成功");
    open.value = false;
    getList();
  } finally {
    submitting.value = false;
  }
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
  return addDecimalStrings(record.userBalance, record.userFrozenBalance);
}

function plainAmount(value) {
  if (value === null || value === undefined || value === "") return "0";
  return String(value);
}

function decimalParts(value) {
  const match = /^([+-]?)(\d+)(?:\.(\d+))?$/.exec(plainAmount(value).trim());
  if (!match) return { unscaled: 0n, scale: 0 };
  const fraction = match[3] || "";
  const digits = `${match[2]}${fraction}`.replace(/^0+(?=\d)/, "") || "0";
  const sign = match[1] === "-" ? -1n : 1n;
  return { unscaled: sign * BigInt(digits), scale: fraction.length };
}

function addDecimalStrings(left, right) {
  const leftParts = decimalParts(left);
  const rightParts = decimalParts(right);
  const scale = Math.max(leftParts.scale, rightParts.scale);
  const leftValue = leftParts.unscaled * (10n ** BigInt(scale - leftParts.scale));
  const rightValue = rightParts.unscaled * (10n ** BigInt(scale - rightParts.scale));
  const sum = leftValue + rightValue;
  const sign = sum < 0n ? "-" : "";
  const digits = (sum < 0n ? -sum : sum).toString().padStart(scale + 1, "0");
  if (scale === 0) return `${sign}${digits}`;
  return `${sign}${digits.slice(0, -scale)}.${digits.slice(-scale)}`;
}

function isNegativeAmount(value) {
  const text = plainAmount(value).trim();
  return text.startsWith("-") && !/^-0(?:\.0+)?$/.test(text);
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
  if (!account) return dictText(order_zhlx, record.withdrawalType);
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
  const selectedIds = [...ids.value];
  Promise.all(selectedIds.map((id) => updateWithdrawal({ id, isHidden }))).then(() => {
    clearSelection();
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
:global(body:has(.transaction-alignment-page)::-webkit-scrollbar) {
  width: 15px;
}

:global(body:has(.transaction-alignment-page) .copyright) {
  display: none;
}

:global(body:has(.transaction-alignment-page) .app-main) {
  padding-bottom: 0 !important;
}

.transaction-alignment-page {
  padding-top: 28px;
  margin-bottom: 0;
}

.transaction-alignment-page :deep(.ant-pro-query-form .ant-form-item-label) {
  flex: 0 0 80px;
  max-width: 80px;
}

.transaction-alignment-page :deep(.ant-pro-query-form .ant-picker) {
  height: 32px;
  padding-block: 4px;
}

.full-width {
  width: 100%;
}

.account-line {
  display: flex;
  align-items: flex-start;
  gap: 2px;
  white-space: normal;
}

.account-line > span {
  flex: 1 1 auto;
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-all;
}

.account-line :deep(.ant-btn) {
  flex: 0 0 auto;
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

.negative-amount {
  color: #ff4d4f;
}

.transaction-alignment-page :deep(.ant-pro-table .ant-table-thead > tr > th) {
  box-sizing: border-box;
  padding: 12px 8px;
  font-size: 15px;
  font-weight: 600;
  line-height: 23.57px;
}

.transaction-alignment-page :deep(.ant-pro-table .ant-table-column-sorters) {
  height: 23.57px;
}

.transaction-alignment-page :deep(.ant-pro-table .ant-table-tbody > tr > td) {
  padding: 12px 8px;
  font-size: 15px;
  line-height: 23.57px;
}

.transaction-alignment-page :deep(.ant-badge-status-text) {
  font-size: 15px;
  line-height: 23.57px;
}

.transaction-alignment-page :deep(.ant-pro-pagination) {
  padding-top: 16px;
}

.drawer-footer {
  text-align: right;
}
</style>
