<template>
  <a-drawer
    v-model:open="visible"
    title="用户订单明细"
    width="90%"
    :destroy-on-close="false"
  >
    <div class="drawer-table-wrap ant-pro-member-page">
      <ant-pro-table
        title="订单明细"
        :columns="orderinfoColumns"
        :data-source="orderinfoList"
        :loading="loading"
        row-key="id"
        :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
        :scroll="{ x: 1900 }"
        @page-change="handleAntPageChange"
        @refresh="getList"
      >
        <template #search>
          <a-form layout="horizontal" :model="queryParams">
            <a-row :gutter="24" align="middle">
              <a-col :span="7">
                <a-form-item label="订单编号">
                  <a-input
                    v-model:value="queryParams.orderNumber"
                    placeholder="请输入订单编号"
                    allow-clear
                    @pressEnter="handleQuery"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="7">
                <a-form-item label="类型">
                  <a-select
                    v-model:value="queryParams.type"
                    placeholder="请选择类型"
                    allow-clear
                  >
                    <a-select-option
                      v-for="opt in order_type"
                      :key="opt.value"
                      :value="opt.value"
                    >{{ opt.label }}</a-select-option>
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
          <template v-if="column.dataIndex === 'type'">
            <dict-tag :options="order_type" :value="record.type" />
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <dict-tag :options="order_status" :value="record.status" />
          </template>
          <template v-else-if="column.dataIndex === 'expiryTime'">
            {{ parseTime(record.expiryTime, "{y}-{m}-{d}") }}
          </template>
          <template v-else-if="column.dataIndex === 'productImage'">
            <image-preview :src="record.productImage" :width="50" :height="50" />
          </template>
        </template>
      </ant-pro-table>
    </div>
  </a-drawer>
</template>
<script setup>
import {
  ref,
  reactive,
  toRefs,
  watch,
  getCurrentInstance,
  computed,
} from "vue";
import { listOrderinfo } from "@/api/member/orderinfo";

const props = defineProps({
  modelValue: { type: Boolean, default: undefined },
  userId: { type: [String, Number], default: null },
});
const emit = defineEmits(["update:modelValue", "success"]);

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});

const { proxy } = getCurrentInstance();
const { order_status, order_type } = proxy.useDict(
  "order_status",
  "order_type"
);

const orderinfoList = ref([]);
const loading = ref(false);
const total = ref(0);
const showSearch = ref(true);

const orderinfoColumns = [
  { title: "明细编号", dataIndex: "orderNumber", align: "center", width: 180 },
  { title: "用户名", dataIndex: "username", align: "center", width: 140 },
  { title: "类型", dataIndex: "type", align: "center", width: 100 },
  { title: "单数", dataIndex: "orderCount", align: "center", width: 100 },
  { title: "金额", dataIndex: "amount", align: "center", width: 120 },
  { title: "返佣百分比", dataIndex: "rebatePercentage", align: "center", width: 130 },
  { title: "返佣", dataIndex: "rebate", align: "center", width: 100 },
  { title: "上级返佣百分比", dataIndex: "upperRebatePercentage", align: "center", width: 160 },
  { title: "上级返佣", dataIndex: "upperRebate", align: "center", width: 130 },
  { title: "状态", dataIndex: "status", align: "center", width: 120 },
  { title: "过期时间", dataIndex: "expiryTime", align: "center", width: 160 },
  { title: "商品图片", dataIndex: "productImage", align: "center", width: 120 },
  { title: "商品标题", dataIndex: "productTitle", align: "center", width: 260 },
  { title: "额外佣金", dataIndex: "extraCommissionId", align: "center", width: 120 },
  { title: "备注", dataIndex: "remarks", align: "center", width: 180 },
];

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNumber: null,
    userId: props.userId ?? null,
    type: null,
  },
});

const { queryParams } = toRefs(data);

function getList() {
  if (!props.userId) {
    orderinfoList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  const params = {
    ...queryParams.value,
    userId: props.userId,
  };
  listOrderinfo(params)
    .then((res) => {
      const data = res.rows || res.data || res;
      orderinfoList.value = data.rows || data || [];
      total.value = data.total ?? (Array.isArray(data) ? data.length : 0);
    })
    .catch((err) => {
      console.error(err);
    })
    .finally(() => {
      loading.value = false;
    });
}

// 鍙栨秷/閲嶇疆 helpers
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.value.orderNumber = null;
  queryParams.value.type = null;
  handleQuery();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

function handleSelectionChange() {}

watch(
  () => props.userId,
  (id) => {
    queryParams.value.userId = id ?? queryParams.value.userId;
    if (visible.value) {
      getList();
    }
  }
);

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      queryParams.value.userId = props.userId ?? queryParams.value.userId;
      getList();
    }
  }
);
</script>

<style scoped>
.drawer-table-wrap { padding: 12px; }
</style>


