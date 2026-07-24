<template>
  <a-drawer
    title="查看交易流水"
    v-model:open="visible"
    width="90%"
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
        :scroll="{ x: 1100 }"
        @page-change="handleAntPageChange"
        @refresh="getList"
      >
        <template #search>
          <a-form layout="horizontal" :model="queryParams">
            <a-row :gutter="24" align="middle">
              <a-col :span="7">
                <a-form-item label="流水编号">
                  <a-input
                    v-model:value="queryParams.serialCode"
                    placeholder="请输入流水编号"
                    allow-clear
                    @pressEnter="handleQuery"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="7">
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
          <template v-if="column.dataIndex === 'transactionType'">
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

const flowColumns = [
  { title: "ID", dataIndex: "id", align: "center", width: 80 },
  { title: "流水编号", dataIndex: "serialCode", align: "center", width: 180 },
  { title: "交易类型", dataIndex: "transactionType", align: "center", width: 140 },
  { title: "交易前余额", dataIndex: "balanceBefore", align: "center", width: 130 },
  { title: "交易金额", dataIndex: "transactionAmount", align: "center", width: 130 },
  { title: "交易后余额", dataIndex: "balanceAfter", align: "center", width: 130 },
  { title: "创建时间", dataIndex: "createdTime", align: "center", width: 180 },
  { title: "备注", dataIndex: "remark", align: "center", width: 180 },
];

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    serialCode: null,
    transactionType: null,
  },
});

const { queryParams } = toRefs(data);

watch(
  () => props.userId,
  (id) => {
    if (id != null && visible.value) {
      queryParams.value.pageNum = 1;
      getList();
    }
  }
);

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.userId != null) {
      queryParams.value.pageNum = 1;
      getList();
    } else if (!val) {
      // reset when closed
      reset();
    }
  }
);

function handleClose() {
  visible.value = false;
}

function handleSelectionChange() {}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.value.serialCode = null;
  queryParams.value.transactionType = null;
  handleQuery();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

function reset() {
  flowList.value = [];
  total.value = 0;
  queryParams.value.pageNum = 1;
  queryParams.value.pageSize = 10;
  queryParams.value.serialCode = null;
  queryParams.value.transactionType = null;
}

function getList() {
  if (!props.userId) {
    flowList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  const params = {
    ...queryParams.value,
    userId: props.userId,
  };
  listFlow(params)
    .then((res) => {
      flowList.value = res.rows ?? res.data?.rows ?? [];
      total.value = res.total ?? res.data?.total ?? 0;
    })
    .catch((err) => {
      console.error(err);
    })
    .finally(() => {
      loading.value = false;
    });
}
</script>

<style scoped>
.drawer-table-wrap { padding: 12px; }
</style>


