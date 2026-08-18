<template>
  <div class="app-container ant-pro-member-page transaction-alignment-page">
    <ant-pro-table
      :key="tableResetKey"
      title="交易流水列表"
      :columns="flowColumns"
      :data-source="flowList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total, size: 'small', showSizeChanger: true }"
      :scroll="{ x: 1702, y: 'calc(100vh - 440px)' }"
      @page-change="handleAntPageChange"
      @change="handleTableChange"
      @refresh="handleRefresh"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 24]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="流水编号">
                <a-input
                  v-model:value="queryParams.serialCode"
                  allow-clear
                  placeholder="请输入流水编号"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.username"
                  allow-clear
                  placeholder="请输入用户名"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="交易类型">
                <a-select
                  v-model:value="queryParams.transactionType"
                  allow-clear
                  placeholder="请选择交易类型"
                >
                  <a-select-option
                    v-for="dict in liveTransactionTypeOptions"
                    :key="dict.value"
                    :value="dict.value"
                  >{{ dict.label }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="金额">
                <a-space-compact block>
                  <a-input-number
                    v-model:value="queryParams.amountMin"
                    string-mode
                    placeholder="最小金额"
                    :precision="2"
                    class="amount-range-input"
                  />
                  <a-input disabled value="~" class="amount-range-separator" />
                  <a-input-number
                    v-model:value="queryParams.amountMax"
                    string-mode
                    placeholder="最大金额"
                    :precision="2"
                    class="amount-range-input"
                  />
                </a-space-compact>
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="是否隐藏">
                <a-select v-model:value="queryParams.isHidden" allow-clear placeholder="请选择">
                  <a-select-option
                    v-for="dict in liveYesNoOptions"
                    :key="dict.value"
                    :value="dict.value"
                  >{{ dict.label }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="advancedSearchVisible" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="交易编号">
                <a-input
                  v-model:value="queryParams.transactionCode"
                  allow-clear
                  placeholder="请输入交易编号"
                  @pressEnter="handleQuery"
                />
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
          <a-button type="primary" :disabled="multiple" v-hasPermi="['member:flow:edit']">
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
          <a-button type="primary" :disabled="multiple" v-hasPermi="['member:flow:edit']">
            <EyeInvisibleOutlined />隐藏
          </a-button>
        </a-popconfirm>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'transactionCode'">
          <a-space :size="4">
            <span>{{ record.transactionCode || "-" }}</span>
            <a-button
              v-if="record.transactionCode"
              type="link"
              size="small"
              aria-label="复制交易编号"
              @click="copyTransactionCode(record.transactionCode)"
            ><CopyOutlined /></a-button>
          </a-space>
        </template>
        <template v-else-if="column.key === 'transactionType'">
          <a-badge status="processing" :text="transactionTypeText(record.transactionType)" />
        </template>
        <template v-else-if="column.key === 'isHidden'">
          <a-badge
            :status="hiddenBadge(record.isHidden).status"
            :text="hiddenBadge(record.isHidden).text"
          />
        </template>
        <template v-else-if="column.key === 'createdTime'">
          {{ parseTime(record.createdTime) }}
        </template>
      </template>
    </ant-pro-table>
  </div>
</template>

<script setup name="Flow">
import { CopyOutlined, EyeInvisibleOutlined, EyeOutlined } from "@ant-design/icons-vue";
import { listFlow, updateFlow } from "@/api/member/flow";
import { flowTransactionTypeFallbackLabel } from "./transactionTypeLabels.js";

const { proxy } = getCurrentInstance();
const { user_yes_no, transaction_type } = proxy.useDict(
  "user_yes_no",
  "transaction_type"
);

const flowList = ref([]);
const loading = ref(true);
const ids = ref([]);
const multiple = ref(true);
const total = ref(0);
let listRequestId = 0;
const tableResetKey = ref(0);
const advancedSearchVisible = ref(false);
const dateRange = ref([]);

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
  { value: "jj", label: "奖金" },
  { value: "rwjl", label: "任务奖励" },
].map((expected) => {
  const option = (transaction_type.value || []).find(
    (item) => String(item?.value) === expected.value
  );
  return option ? { ...option, label: expected.label } : expected;
}));

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  serialCode: undefined,
  username: undefined,
  transactionType: undefined,
  amountMin: undefined,
  amountMax: undefined,
  isHidden: undefined,
  transactionCode: undefined,
  orderByColumn: "gtf.serial_code",
  isAsc: "desc",
});

const flowColumns = [
  { title: "流水编号", dataIndex: "serialCode", key: "serialCode", width: 200, fixed: "left", sorter: true, defaultSortOrder: "descend" },
  { title: "用户名", dataIndex: "username", key: "username", width: 140 },
  { title: "交易类型", dataIndex: "transactionType", key: "transactionType", width: 140, sorter: true },
  { title: "交易前余额", dataIndex: "balanceBefore", key: "balanceBefore", width: 160 },
  { title: "金额", dataIndex: "transactionAmount", key: "transactionAmount", width: 160, sorter: true },
  { title: "交易后余额", dataIndex: "balanceAfter", key: "balanceAfter", width: 160 },
  { title: "是否隐藏", dataIndex: "isHidden", key: "isHidden", width: 100, sorter: true },
  { title: "交易编号", dataIndex: "transactionCode", key: "transactionCode", width: 240 },
  { title: "创建时间", dataIndex: "createdTime", key: "createdTime", width: 170, sorter: true },
  { title: "备注", dataIndex: "remark", key: "remark", width: 200, ellipsis: true },
];

const rowSelection = computed(() => ({
  columnWidth: 32,
  selectedRowKeys: ids.value,
  onChange: (_keys, rows) => handleSelectionChange(rows),
}));

function dictText(options, value) {
  const values = Array.isArray(options) ? options : options?.value;
  return proxy.selectDictLabel((values || []).filter(Boolean), value) || value || "-";
}

const hiddenBadgeMap = Object.freeze({
  "0": Object.freeze({ status: "error", text: "是" }),
  "1": Object.freeze({ status: "error", text: "否" }),
});

const sortColumnMap = Object.freeze({
  serialCode: "gtf.serial_code",
  transactionType: "gtf.transaction_type",
  transactionAmount: "gtf.transaction_amount",
  isHidden: "gtf.is_hidden",
  createdTime: "gtf.created_time",
});

function hiddenBadge(value) {
  return hiddenBadgeMap[String(value)] || { status: "default", text: dictText(user_yes_no, value) };
}

function transactionTypeText(value) {
  const option = liveTransactionTypeOptions.value.find(
    (item) => String(item.value) === String(value)
  );
  return option?.label
    || flowTransactionTypeFallbackLabel(value)
    || dictText(transaction_type, value);
}

function clearSelection() {
  ids.value = [];
  multiple.value = true;
}

function requestParams() {
  const {
    amountMin,
    amountMax,
    ...base
  } = queryParams;
  const params = {};
  if (amountMin !== undefined && amountMin !== null) params.amountMin = amountMin;
  if (amountMax !== undefined && amountMax !== null) params.amountMax = amountMax;
  if (dateRange.value?.length === 2) {
    params.beginTime = dateRange.value[0];
    params.endTime = dateRange.value[1];
  }
  return Object.keys(params).length ? { ...base, params } : { ...base };
}

function getList() {
  const requestId = ++listRequestId;
  loading.value = true;
  return listFlow(requestParams())
    .then((response) => {
      if (requestId !== listRequestId) return;
      flowList.value = response.rows || [];
      total.value = response.total || 0;
    })
    .finally(() => {
      if (requestId === listRequestId) loading.value = false;
    });
}

function handleAntPageChange({ page, pageSize }) {
  clearSelection();
  queryParams.pageNum = page;
  queryParams.pageSize = pageSize;
  getList();
}

function handleQuery() {
  clearSelection();
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  Object.assign(queryParams, {
    pageNum: 1,
    serialCode: undefined,
    username: undefined,
    transactionType: undefined,
    amountMin: undefined,
    amountMax: undefined,
    isHidden: undefined,
    transactionCode: undefined,
    orderByColumn: "gtf.serial_code",
    isAsc: "desc",
  });
  dateRange.value = [];
  clearSelection();
  tableResetKey.value += 1;
  getList();
}

function handleRefresh() {
  clearSelection();
  getList();
}

function handleTableChange(_pagination, _filters, sorter) {
  const columnKey = sorter?.columnKey;
  queryParams.orderByColumn = sorter?.order ? sortColumnMap[columnKey] || null : null;
  queryParams.isAsc = sorter?.order === "ascend"
    ? "asc"
    : sorter?.order === "descend"
      ? "desc"
      : null;
  clearSelection();
  queryParams.pageNum = 1;
  getList();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  multiple.value = !selection.length;
}

function handleHidden(isHidden) {
  const selectedIds = [...ids.value];
  Promise.all(selectedIds.map((id) => updateFlow({ id, isHidden }))).then(() => {
    clearSelection();
    proxy.$modal.msgSuccess("操作成功");
    getList();
  });
}

async function copyTransactionCode(value) {
  try {
    await navigator.clipboard.writeText(value);
    proxy.$modal.msgSuccess("交易编号已复制");
  } catch {
    proxy.$modal.msgError("复制失败，请手动复制");
  }
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

.amount-range-input {
  width: calc(50% - 18px);
}

.amount-range-separator {
  width: 36px;
  padding-inline: 8px;
  text-align: center;
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
</style>
