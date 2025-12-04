<template>
  <el-drawer v-model="visible" title="用户订单明细" size="90%" append-to-body>
    <div class="app-container">
      <el-form
        :model="queryParams"
        ref="queryRef"
        :inline="true"
        v-show="showSearch"
        label-width="68px"
      >
        <el-form-item label="订单编号" prop="orderNumber">
          <el-input
            v-model="queryParams.orderNumber"
            placeholder="请输入订单编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select
            v-model="queryParams.type"
            placeholder="请选择类型"
            style="width: 160px"
            clearable
          >
            <el-option
              v-for="opt in order_type"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery"
            >搜索</el-button
          >
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="orderinfoList"
        @selection-change="handleSelectionChange"
        :border="true"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="明细编号" align="center" prop="orderNumber" />
        <el-table-column label="单数" align="center" prop="orderCount" />
        <el-table-column label="金额" align="center" prop="amount" />
        <el-table-column label="返佣" align="center" prop="rebate" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="order_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column
          label="过期时间"
          align="center"
          prop="expiryTime"
          width="160"
        >
          <template #default="scope">
            <span>{{ parseTime(scope.row.expiryTime, "{y}-{m}-{d}") }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remarks" />
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
  </el-drawer>
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

// 取消/重置 helpers
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm && proxy.resetForm("queryRef");
  handleQuery();
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
.app-container {
  padding: 12px;
}
</style>
