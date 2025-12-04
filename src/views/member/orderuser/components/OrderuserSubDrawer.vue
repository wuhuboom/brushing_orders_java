<template>
  <el-drawer
    title="下级会员"
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
        label-width="100px"
      >
        <el-form-item label="用户名">
          <el-input
            v-model="queryParams.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="范围">
          <el-switch
            v-model="showAll"
            active-text="查询所有下级"
            inactive-text="查询直属下级"
            @change="onScopeChange"
          />
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
        :data="subList"
        @selection-change="handleSelectionChange"
        :border="true"
        class="mt12"
      >
        <el-table-column label="ID" align="center" prop="id" width="80" />
        <el-table-column
          label="用户名"
          align="center"
          prop="username"
          width="160"
        />
        <el-table-column
          label="手机号"
          align="center"
          prop="phoneNumber"
          width="140"
        />
        <el-table-column
          label="VIP 等级"
          align="center"
          prop="memberLevel.name"
          width="120"
        />
        <el-table-column label="任务进度" align="center" width="120">
          <template #default="scope">
            <div>
              {{ scope.row.taskProgress }} /
              {{ scope.row.memberLevel?.orderCountPerDay ?? 0 }}
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="签到天数"
          align="center"
          prop="signDays"
          width="120"
        />

        <el-table-column
          label="今日提现次数"
          align="center"
          prop="todayWithdrawalCount"
          width="140"
        />

        <el-table-column
          label="最后登录地址"
          align="center"
          prop="lastLoginAddress"
          width="220"
        />
        <el-table-column
          label="直属下级数量"
          align="center"
          prop="directChildrenCount"
          width="140"
        />
        <el-table-column
          label="信誉分"
          align="center"
          prop="reputationScore"
          width="100"
        />
        <el-table-column
          label="累计签到次数"
          align="center"
          prop="totalSignDays"
          width="140"
        />
        <el-table-column
          label="余额"
          align="center"
          prop="balance"
          width="120"
        />
        <el-table-column
          label="冻结余额"
          align="center"
          prop="frozenBalance"
          width="120"
        />
        <el-table-column
          label="提现金额"
          align="center"
          prop="withdrawalAmount"
          width="140"
        />
        <el-table-column
          label="充值金额"
          align="center"
          prop="rechargeAmount"
          width="140"
        />
        <el-table-column
          label="今日重置次数"
          align="center"
          prop="todayResetCount"
          width="140"
        />
        <el-table-column
          label="累计重置次数"
          align="center"
          prop="totalResetCount"
          width="140"
        />
        <el-table-column
          label="今日佣金"
          align="center"
          prop="todayCommission"
          width="140"
        />

        <el-table-column
          label="是否启用"
          align="center"
          prop="isEnabled"
          width="100"
        >
          <template #default="scope">
            <dict-tag :options="user_yes_no" :value="scope.row.isEnabled" />
          </template>
        </el-table-column>
        <el-table-column
          label="是否冻结"
          align="center"
          prop="isFrozen"
          width="100"
        >
          <template #default="scope">
            <dict-tag :options="user_yes_no" :value="scope.row.isFrozen" />
          </template>
        </el-table-column>
        <el-table-column
          label="是否假人"
          align="center"
          prop="isFake"
          width="100"
        >
          <template #default="scope">
            <dict-tag :options="user_yes_no" :value="scope.row.isFake" />
          </template>
        </el-table-column>

        <el-table-column
          label="账户状态"
          align="center"
          prop="accountStatus"
          width="120"
        >
          <template #default="scope">
            <dict-tag :options="sys_enabled" :value="scope.row.accountStatus" />
          </template>
        </el-table-column>
        <el-table-column
          label="交易状态"
          align="center"
          prop="transactionStatus"
          width="120"
        >
          <template #default="scope">
            <dict-tag
              :options="sys_enabled"
              :value="scope.row.transactionStatus"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="提现状态"
          align="center"
          prop="withdrawalStatus"
          width="120"
        >
          <template #default="scope">
            <dict-tag
              :options="sys_enabled"
              :value="scope.row.withdrawalStatus"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="协助金提现状态"
          align="center"
          prop="assistWithdrawalStatus"
          width="160"
        >
          <template #default="scope">
            <dict-tag
              :options="sys_enabled"
              :value="scope.row.assistWithdrawalStatus"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="充值后禁止提现"
          align="center"
          prop="depositBlockWithdrawal"
          width="160"
        >
          <template #default="scope">
            <dict-tag
              :options="user_yes_no"
              :value="scope.row.depositBlockWithdrawal"
            />
          </template>
        </el-table-column>

        <el-table-column
          label="上级邀请码"
          align="center"
          prop="parentInviteCode"
          width="140"
        />
        <el-table-column
          label="邀请码"
          align="center"
          prop="inviteCode"
          width="140"
        />
        <el-table-column label="性别" align="center" prop="gender" width="100">
          <template #default="scope">
            <dict-tag :options="sys_user_sex" :value="scope.row.gender" />
          </template>
        </el-table-column>
        <el-table-column
          label="最后登录时间"
          align="center"
          prop="lastLoginTime"
          width="180"
        >
          <template #default="scope">
            <span>{{
              parseTime(scope.row.lastLoginTime, "{y}-{m}-{d} {h}:{i}:{s}")
            }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="创建时间"
          align="center"
          prop="createTime"
          width="180"
        >
          <template #default="scope">
            <span>{{
              parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}:{s}")
            }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="备注"
          align="center"
          prop="remarks"
          width="200"
        />

        <el-table-column label="操作" align="center" fixed="right" width="140">
          <template #default="scope">
            <el-button type="primary" @click="openFlow(scope.row)"
              >查看交易流水</el-button
            >
          </template>
        </el-table-column>
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

const showAll = ref(false); // 开关：true -> all, false -> direct

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
      queryParams.pageNum = 1;
      queryParams.scope = showAll.value ? "all" : "direct";
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
