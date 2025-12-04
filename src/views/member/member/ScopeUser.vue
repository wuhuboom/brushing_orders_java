<template>
  <div class="app-container">
    <!-- 查询区域：只保留范围开关 -->
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('member.submember.username')">
        <el-input
          v-model="queryParams.username"
          :placeholder="$t('member.submember.enterUsername')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>

      <el-form-item :label="$t('member.submember.phone')">
        <el-input
          v-model="queryParams.phone"
          :placeholder="$t('member.submember.enterPhone')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>

      <el-form-item :label="$t('member.submember.scope')">
        <el-switch
          v-model="queryParams.scope"
          :active-text="$t('member.submember.directSub')"
          :inactive-text="$t('member.submember.allSub')"
          active-value="direct"
          inactive-value="all"
          @change="handleQuery"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">{{
          $t("member.submember.search")
        }}</el-button>
        <el-button icon="Refresh" @click="resetQuery">{{
          $t("member.submember.reset")
        }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 列表 -->
    <el-table
      v-loading="loading"
      :data="memberList"
      :border="true"
      style="width: 100%"
    >
      <el-table-column
        :label="$t('member.submember.id')"
        align="center"
        prop="id"
        width="80"
      />
      <el-table-column
        :label="$t('member.submember.username')"
        align="center"
        prop="username"
        width="120"
      />
      <el-table-column
        :label="$t('member.submember.phone')"
        align="center"
        prop="phone"
        width="160"
      />
      <el-table-column
        :label="$t('member.submember.vipLevel')"
        align="center"
        prop="userLevel.nameZh"
        width="160"
      />
      <el-table-column
        :label="$t('member.submember.parentId')"
        align="center"
        prop="parentId"
        width="100"
      />
      <el-table-column
        :label="$t('member.submember.parentUsername')"
        align="center"
        prop="parentUsername"
        width="140"
      />
      <el-table-column
        :label="$t('member.submember.balance')"
        align="center"
        prop="balance"
        width="120"
      />
      <el-table-column
        :label="$t('member.submember.frozenBalance')"
        align="center"
        prop="frozenBalance"
        width="120"
      />
      <el-table-column
        :label="$t('member.submember.totalBalance')"
        align="center"
        prop="totalBalance"
        width="120"
      />
      <el-table-column
        :label="$t('member.submember.creditScore')"
        align="center"
        prop="creditScore"
        width="100"
      />
      <el-table-column
        :label="$t('member.submember.inviteCode')"
        align="center"
        prop="inviteCode"
        width="120"
      />

      <el-table-column
        :label="$t('member.submember.accountStatus')"
        align="center"
        prop="accountStatus"
        width="120"
      >
        <template #default="{ row }">
          <dict-tag :options="sys_normal_disable" :value="row.accountStatus" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('member.submember.tradeStatus')"
        align="center"
        prop="tradeStatus"
        width="120"
      >
        <template #default="{ row }">
          <dict-tag :options="sys_normal_disable" :value="row.tradeStatus" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('member.submember.withdrawStatus')"
        align="center"
        prop="withdrawStatus"
        width="120"
      >
        <template #default="{ row }">
          <dict-tag :options="sys_normal_disable" :value="row.withdrawStatus" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('member.submember.loginTime')"
        align="center"
        prop="lastLoginTime"
        width="160"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.lastLoginTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('member.submember.registerTime')"
        align="center"
        prop="createTime"
        width="160"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        fixed="right"
        :label="$t('member.submember.remark')"
        align="center"
        prop="remark"
        width="160"
      />

      <el-table-column
        fixed="right"
        :label="$t('member.submember.action')"
        align="center"
        width="160"
      >
        <template #default="scope">
          <div class="operation-buttons">
            <el-button
              icon="Money"
              type="primary"
              @click="handleAccount(scope.row)"
            >
              {{ $t("member.submember.accountChange") }}
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 用户账单抽屉 -->
    <el-drawer
      :title="$t('member.submember.userAccount')"
      v-model="accountOpen"
      direction="rtl"
      size="92%"
      append-to-body
    >
      <Account :userId="selectedUserId" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, toRefs, watch, getCurrentInstance } from "vue";
import { useI18n } from "vue-i18n";
import { scopeList } from "@/api/member/member";
import Account from "@/views/member/member/Account.vue";

const { t } = useI18n();

const props = defineProps({
  userId: { type: [Number, String], required: true },
});

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  username: null,
  phone: null,
  userId: null, // ✅ 将由 props.userId 同步
  scope: "direct", // ✅ 默认直属下级
});

const { proxy } = getCurrentInstance();
const { sys_yes_no, sys_normal_disable } = proxy.useDict(
  "sys_yes_no",
  "sys_normal_disable"
);

const loading = ref(true);
const showSearch = ref(true);

// 范围开关：direct=直属，all=所有（默认直属）
const scope = ref("direct");

// 列表与分页
const memberList = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

// 抽屉
const accountOpen = ref(false);
const selectedUserId = ref(null);

// 获取列表（改为 scopeList(userId, scope)）
function getList() {
  console.log(props.userId);
  if (!props.userId) return;
  loading.value = true;
  console.log(queryParams.value);
  scopeList(queryParams.value)
    .then((response) => {
      memberList.value = response.rows || response.data || [];
      total.value = response.total || memberList.value.length || 0;
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}
function resetQuery() {
  queryParams.pageNum = 1;
  queryParams.pageSize = 10;
  queryParams.username = null;
  queryParams.phone = null;
  queryParams.scope = "direct"; // 每次重置回直属
  getList();
}
// 打开账变信息
function handleAccount(row) {
  selectedUserId.value = row.id;
  accountOpen.value = true;
}

// scope 切换自动刷新
watch(scope, () => handleQuery());

// 首次加载
getList();

watch(
  () => props.userId,
  (val) => {
    queryParams.value.userId = val;
    // 新用户进来时，按需重置范围为直属
    queryParams.value.scope = "direct";
    handleQuery();
  },
  { immediate: true } // 首次挂载也执行一次
);
</script>

<style scoped>
.operation-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>
