<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="交易流水列表"
      :columns="flowColumns"
      :data-source="flowList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 1680 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="流水编号">
                <a-input
                  v-model:value="queryParams.serialCode"
                  allow-clear
                  placeholder="请输入流水编号"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.username"
                  allow-clear
                  placeholder="请输入用户名"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="交易类型">
                <a-select
                  v-model:value="queryParams.transactionType"
                  allow-clear
                  placeholder="请选择交易类型"
                >
                  <a-select-option
                    v-for="dict in transaction_type"
                    :key="dict.value"
                    :value="dict.value"
                  >{{ dict.label }}</a-select-option>
                </a-select>
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
              <a-form-item label="金额">
                <a-space-compact block>
                  <a-input-number
                    v-model:value="queryParams.amountMin"
                    placeholder="最小金额"
                    :precision="2"
                    class="amount-range-input"
                  />
                  <a-input-number
                    v-model:value="queryParams.amountMax"
                    placeholder="最大金额"
                    :precision="2"
                    class="amount-range-input"
                  />
                </a-space-compact>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="是否隐藏">
                <a-select v-model:value="queryParams.isHidden" allow-clear placeholder="请选择">
                  <a-select-option
                    v-for="dict in user_yes_no"
                    :key="dict.value"
                    :value="dict.value"
                  >{{ dict.label }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="交易编号">
                <a-input
                  v-model:value="queryParams.transactionCode"
                  allow-clear
                  placeholder="请输入交易编号"
                  @pressEnter="handleQuery"
                />
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
          <a-button :disabled="multiple" v-hasPermi="['member:flow:edit']">
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
          <a-button :disabled="multiple" v-hasPermi="['member:flow:edit']">
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
              @click="copyTransactionCode(record.transactionCode)"
            >复制</a-button>
          </a-space>
        </template>
        <template v-else-if="column.dict">
          {{ dictText(column.dict === "transaction" ? transaction_type : user_yes_no, record[column.dataIndex]) }}
        </template>
        <template v-else-if="column.key === 'createdTime'">
          {{ parseTime(record.createdTime) }}
        </template>
      </template>
    </ant-pro-table>
  </div>
</template>

<script setup name="Flow">
import { EyeInvisibleOutlined, EyeOutlined } from "@ant-design/icons-vue";
import { listFlow, updateFlow } from "@/api/member/flow";

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
const advancedSearchVisible = ref(false);
const dateRange = ref([]);

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
});

const flowColumns = [
  { title: "流水编号", dataIndex: "serialCode", key: "serialCode", width: 190, fixed: "left" },
  { title: "用户名", dataIndex: "username", key: "username", width: 140 },
  { title: "交易类型", dataIndex: "transactionType", key: "transactionType", dict: "transaction", width: 140 },
  { title: "交易前余额", dataIndex: "balanceBefore", key: "balanceBefore", width: 140 },
  { title: "金额", dataIndex: "transactionAmount", key: "transactionAmount", width: 140 },
  { title: "交易后余额", dataIndex: "balanceAfter", key: "balanceAfter", width: 140 },
  { title: "是否隐藏", dataIndex: "isHidden", key: "isHidden", dict: "yesNo", width: 120 },
  { title: "交易编号", dataIndex: "transactionCode", key: "transactionCode", width: 270 },
  { title: "创建时间", dataIndex: "createdTime", key: "createdTime", width: 180 },
  { title: "备注", dataIndex: "remark", key: "remark", width: 220 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_keys, rows) => handleSelectionChange(rows),
}));

function dictText(options, value) {
  return proxy.selectDictLabel(options, value) || value || "-";
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
  loading.value = true;
  listFlow(requestParams())
    .then((response) => {
      flowList.value = response.rows;
      total.value = response.total;
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.pageNum = page;
  queryParams.pageSize = pageSize;
  getList();
}

function handleQuery() {
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
  });
  dateRange.value = [];
  getList();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  multiple.value = !selection.length;
}

function handleHidden(isHidden) {
  Promise.all(ids.value.map((id) => updateFlow({ id, isHidden }))).then(() => {
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
.full-width {
  width: 100%;
}

.amount-range-input {
  width: 50%;
}
</style>
