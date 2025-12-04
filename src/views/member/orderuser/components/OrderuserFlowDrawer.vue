<template>
  <el-drawer
    title="查看交易流水"
    v-model="visible"
    size="90%"
    with-header
    :destroy-on-close="false"
    :append-to-body="true"
    @close="handleClose"
  >
    <div class="pa12">
      <el-form
        :model="queryParams"
        ref="queryRef"
        :inline="true"
        label-width="80px"
      >
        <el-form-item label="流水编号">
          <el-input
            v-model="queryParams.serialCode"
            placeholder="流水编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="交易类型">
          <el-select
            v-model="queryParams.transactionType"
            placeholder="交易类型"
            clearable
            style="width: 220px"
          >
            <el-option
              v-for="dict in transaction_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
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
        :data="flowList"
        @selection-change="handleSelectionChange"
        :border="true"
        class="mt12"
      >
        <el-table-column label="ID" align="center" prop="id" width="80" />
        <el-table-column label="流水编号" align="center" prop="serialCode" />
        <el-table-column label="交易类型" align="center" prop="transactionType">
          <template #default="scope">
            <dict-tag
              :options="transaction_type"
              :value="scope.row.transactionType"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="交易前余额"
          align="center"
          prop="balanceBefore"
          width="120"
        />
        <el-table-column
          label="交易金额"
          align="center"
          prop="transactionAmount"
          width="120"
        />
        <el-table-column
          label="交易后余额"
          align="center"
          prop="balanceAfter"
          width="120"
        />
        <el-table-column
          label="创建时间"
          align="center"
          prop="createdTime"
          width="160"
        >
          <template #default="scope">
            <span>{{
              parseTime(scope.row.createdTime, "{y}-{m}-{d} {h}:{i}:{s}")
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" />
      </el-table>

      <div class="mt12" v-if="total > 0">
        <pagination
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, watch, getCurrentInstance } from "vue";
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
const { transaction_type, user_yes_no } = proxy.useDict(
  "transaction_type",
  "user_yes_no"
);

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
    console.log(
      "OrderuserFlowDrawer: props.userId changed ->",
      id,
      "visible:",
      visible.value
    );
    if (id != null && visible.value) {
      queryParams.value.pageNum = 1;
      getList();
    }
  }
);

watch(
  () => props.modelValue,
  (val) => {
    console.log(
      "OrderuserFlowDrawer: props.modelValue changed ->",
      val,
      "props.userId:",
      props.userId
    );
    if (val && props.userId != null) {
      queryParams.pageNum = 1;
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

function handleSelectionChange() {
  // no-op for now; kept for parity
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm && proxy.resetForm("queryRef");
  queryParams.value.serialCode = null;
  queryParams.value.transactionType = null;
  handleQuery();
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
.pa12 {
  padding: 12px;
}
.mt12 {
  margin-top: 12px;
}
</style>
