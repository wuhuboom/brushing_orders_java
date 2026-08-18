<template>
  <a-drawer
    title="查看交易流水"
    v-model:open="visible"
    width="85%"
    :destroy-on-close="false"
    @close="handleClose"
  >
    <div class="drawer-table-wrap ant-pro-member-page">
      <ant-pro-table
        title="交易流水"
        :columns="flowColumns"
        :data-source="flowList"
        :loading="loading"
        row-key="id"
        :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
        :scroll="{ x: 1310 }"
        @page-change="handleAntPageChange"
        @refresh="getList"
      >
        <template #search>
          <a-form layout="horizontal" :model="queryParams">
            <a-row :gutter="24" align="middle">
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item label="流水编号">
                  <a-input
                    v-model:value="queryParams.serialCode"
                    placeholder="请输入流水编号"
                    allow-clear
                    @pressEnter="handleQuery"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item label="交易类型">
                  <a-select
                    v-model:value="queryParams.transactionType"
                    placeholder="请选择交易类型"
                    allow-clear
                  >
                    <a-select-option
                      v-for="dict in transaction_type"
                      :key="dict.value"
                      :value="dict.value"
                    >{{ dict.label }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="8">
                <a-form-item label="金额">
                  <a-space-compact block>
                    <a-input-number
                      v-model:value="queryParams.amountMin"
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
              <a-col :xs="24" :sm="12" :lg="4" class="ant-pro-query-actions">
                <a-space>
                  <a-button @click="resetQuery">重 置</a-button>
                  <a-button type="primary" @click="handleQuery">查 询</a-button>
                </a-space>
              </a-col>
            </a-row>
          </a-form>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'transactionCode'">
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
          <template v-else-if="column.dataIndex === 'transactionType'">
            <dict-tag :options="transaction_type" :value="record.transactionType" />
          </template>
          <template v-else-if="column.dataIndex === 'createdTime'">
            {{ parseTime(record.createdTime, "{y}-{m}-{d} {h}:{i}:{s}") }}
          </template>
        </template>
      </ant-pro-table>
    </div>
  </a-drawer>
</template>
<script setup>
import { ref, reactive, toRefs, watch, getCurrentInstance } from "vue";
import { listFlow } from "@/api/member/flow";
import { buildFlowListParams, createFlowQueryParams } from "./orderuserFlow.js";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  userId: {
    type: [String, Number],
    default: null,
  },
});

const emit = defineEmits(["update:modelValue", "success"]);

const { proxy } = getCurrentInstance();
const { transaction_type } = proxy.useDict("transaction_type");

const visible = ref(props.modelValue);
watch(
  () => props.modelValue,
  (v) => {
    visible.value = v;
  }
);

watch(visible, (v) => {
  emit("update:modelValue", v);
});

const loading = ref(false);
const flowList = ref([]);
const total = ref(0);
let listRequestToken = 0;

const flowColumns = [
  { title: "流水编号", dataIndex: "serialCode", align: "center", width: 180 },
  { title: "交易类型", dataIndex: "transactionType", align: "center", width: 140 },
  { title: "交易前余额", dataIndex: "balanceBefore", align: "center", width: 130 },
  { title: "金额", dataIndex: "transactionAmount", align: "center", width: 130 },
  { title: "交易后余额", dataIndex: "balanceAfter", align: "center", width: 130 },
  { title: "交易编号", dataIndex: "transactionCode", align: "center", width: 240 },
  { title: "创建时间", dataIndex: "createdTime", align: "center", width: 180 },
  { title: "备注", dataIndex: "remark", align: "center", width: 180 },
];

const data = reactive({
  queryParams: createFlowQueryParams(),
});

const { queryParams } = toRefs(data);

watch(
  () => props.userId,
  (id, previousId) => {
    if (!visible.value || String(id) === String(previousId)) return;
    reset();
    if (id != null) getList();
  }
);

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.userId != null) {
      reset();
      getList();
    } else if (!val) {
      reset();
    }
  },
  { immediate: true }
);

function handleClose() {
  visible.value = false;
  reset();
}

function handleSelectionChange() {}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.value.serialCode = null;
  queryParams.value.transactionType = null;
  queryParams.value.amountMin = null;
  queryParams.value.amountMax = null;
  handleQuery();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

function reset() {
  listRequestToken += 1;
  loading.value = false;
  flowList.value = [];
  total.value = 0;
  Object.assign(queryParams.value, createFlowQueryParams());
}

function getList() {
  const userId = props.userId;
  if (!visible.value || userId === null || userId === undefined || userId === "") {
    listRequestToken += 1;
    flowList.value = [];
    total.value = 0;
    loading.value = false;
    return;
  }
  const requestToken = ++listRequestToken;
  loading.value = true;
  const params = buildFlowListParams(queryParams.value, userId);
  listFlow(params)
    .then((res) => {
      if (
        requestToken !== listRequestToken
        || !visible.value
        || String(props.userId) !== String(userId)
      ) return;
      flowList.value = res.rows ?? res.data?.rows ?? [];
      total.value = res.total ?? res.data?.total ?? 0;
    })
    .catch((err) => {
      console.error(err);
    })
    .finally(() => {
      if (requestToken === listRequestToken) loading.value = false;
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
</script>

<style scoped>
.drawer-table-wrap { padding: 12px; }

.amount-range-input {
  width: calc((100% - 40px) / 2);
}

.amount-range-separator {
  width: 40px;
  padding-inline: 0;
  text-align: center;
  pointer-events: none;
}
</style>


