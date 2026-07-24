<template>
  <a-drawer
    title="下级会员"
    v-model:open="visible"
    width="90%"
    :destroy-on-close="false"
    @close="handleClose"
  >
    <div class="drawer-table-wrap ant-pro-member-page">
      <ant-pro-table
        title="下级会员列表"
        :columns="subColumns"
        :data-source="subList"
        :loading="loading"
        row-key="id"
        :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
        :scroll="{ x: 2600 }"
        @page-change="handleAntPageChange"
        @refresh="getList"
      >
        <template #search>
          <a-form layout="horizontal" :model="queryParams">
            <a-row :gutter="24" align="middle">
              <a-col :span="7">
                <a-form-item label="用户名">
                  <a-input v-model:value="queryParams.username" placeholder="请输入用户名" allow-clear @pressEnter="handleQuery" />
                </a-form-item>
              </a-col>
              <a-col :span="7">
                <a-form-item label="范围">
                  <a-switch v-model:checked="showAll" checked-children="所有下级" un-checked-children="直属下级" @change="onScopeChange" />
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
          <template v-if="column.dataIndex === 'memberLevelName'">
            {{ record.memberLevel?.name || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'taskProgressText'">
            {{ record.taskProgress }} / {{ record.memberLevel?.orderCountPerDay ?? 0 }}
          </template>
          <template v-else-if="['isEnabled','isFrozen','isFake','depositBlockWithdrawal'].includes(column.dataIndex)">
            <dict-tag :options="user_yes_no" :value="record[column.dataIndex]" />
          </template>
          <template v-else-if="['accountStatus','transactionStatus','withdrawalStatus','assistWithdrawalStatus'].includes(column.dataIndex)">
            <dict-tag :options="sys_enabled" :value="record[column.dataIndex]" />
          </template>
          <template v-else-if="column.dataIndex === 'lastLoginTime'">
            {{ parseTime(record.lastLoginTime, "{y}-{m}-{d} {h}:{i}:{s}") }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-button type="link" size="small" @click="openFlow(record)">流水</a-button>
          </template>
        </template>
      </ant-pro-table>
    </div>
  </a-drawer>
</template>
<script setup>
import { ref, reactive, toRefs, watch, getCurrentInstance } from "vue";
import { selectChildrenById } from "@/api/member/orderuser";

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

const emit = defineEmits(["update:modelValue", "success", "open-flow"]);

const { proxy } = getCurrentInstance();
const { user_yes_no, sys_user_sex, sys_enabled } = proxy.useDict(
  "user_yes_no",
  "sys_user_sex",
  "sys_enabled"
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
const subList = ref([]);
const total = ref(0);

const subColumns = [
  { title: "ID", dataIndex: "id", align: "center", width: 80 },
  { title: "用户名", dataIndex: "username", align: "center", width: 140 },
  { title: "手机号", dataIndex: "phoneNumber", align: "center", width: 140 },
  { title: "VIP等级", dataIndex: "memberLevelName", align: "center", width: 120 },
  { title: "任务进度", dataIndex: "taskProgressText", align: "center", width: 120 },
  { title: "签到天数", dataIndex: "signDays", align: "center", width: 120 },
  { title: "今日提现次数", dataIndex: "todayWithdrawalCount", align: "center", width: 140 },
  { title: "最后登录地址", dataIndex: "lastLoginAddress", align: "center", width: 220 },
  { title: "直属下级数量", dataIndex: "directChildrenCount", align: "center", width: 140 },
  { title: "信誉分", dataIndex: "reputationScore", align: "center", width: 100 },
  { title: "累计签到次数", dataIndex: "totalSignDays", align: "center", width: 140 },
  { title: "余额", dataIndex: "balance", align: "center", width: 120 },
  { title: "冻结余额", dataIndex: "frozenBalance", align: "center", width: 120 },
  { title: "提现金额", dataIndex: "withdrawalAmount", align: "center", width: 140 },
  { title: "充值金额", dataIndex: "rechargeAmount", align: "center", width: 140 },
  { title: "今日重置次数", dataIndex: "todayResetCount", align: "center", width: 140 },
  { title: "累计重置次数", dataIndex: "totalResetCount", align: "center", width: 140 },
  { title: "今日佣金", dataIndex: "todayCommission", align: "center", width: 140 },
  { title: "是否启用", dataIndex: "isEnabled", align: "center", width: 110 },
  { title: "是否冻结", dataIndex: "isFrozen", align: "center", width: 110 },
  { title: "是否假人", dataIndex: "isFake", align: "center", width: 110 },
  { title: "账户状态", dataIndex: "accountStatus", align: "center", width: 120 },
  { title: "交易状态", dataIndex: "transactionStatus", align: "center", width: 120 },
  { title: "提现状态", dataIndex: "withdrawalStatus", align: "center", width: 120 },
  { title: "协助金提现状态", dataIndex: "assistWithdrawalStatus", align: "center", width: 160 },
  { title: "充值后禁止提现", dataIndex: "depositBlockWithdrawal", align: "center", width: 160 },
  { title: "上级邀请码", dataIndex: "parentInviteCode", align: "center", width: 140 },
  { title: "邀请码", dataIndex: "inviteCode", align: "center", width: 140 },
  { title: "最后登录时间", dataIndex: "lastLoginTime", align: "center", width: 180 },
  { title: "操作", dataIndex: "action", align: "center", width: 100, fixed: "right" },
];

const showAll = ref(false); // 寮€鍏筹細true -> all, false -> direct

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    username: null,
    scope: "direct",
  },
});

const { queryParams } = toRefs(data);

watch(
  () => props.userId,
  (id) => {
    if (id != null && visible.value) {
      queryParams.value.pageNum = 1;
      queryParams.value.scope = showAll.value ? "all" : "direct";
      getList();
    }
  }
);

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.userId != null) {
      queryParams.value.pageNum = 1;
      queryParams.value.scope = showAll.value ? "all" : "direct";
      getList();
    } else if (!val) {
      reset();
    }
  }
);

function handleClose() {
  visible.value = false;
}

function handleSelectionChange() {
  // no-op for parity
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function onScopeChange(val) {
  queryParams.value.scope = val ? "all" : "direct";
  handleQuery();
}

function resetQuery() {
  proxy.resetForm && proxy.resetForm("queryRef");
  queryParams.value.username = null;
  queryParams.value.scope = showAll.value ? "all" : "direct";
  handleQuery();
}

function reset() {
  subList.value = [];
  total.value = 0;
  queryParams.value.pageNum = 1;
  queryParams.value.pageSize = 10;
  queryParams.value.username = null;
  queryParams.value.scope = "direct";
  showAll.value = false;
}

function openFlow(row) {
  emit("open-flow", row.id);
}

function getList() {
  if (!props.userId) {
    subList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  const params = {
    ...queryParams.value,
    id: props.userId,
  };
  selectChildrenById(params)
    .then((res) => {
      // API may return rows or data.rows depending on backend wrapper
      subList.value = res.rows ?? res.data?.rows ?? [];
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


