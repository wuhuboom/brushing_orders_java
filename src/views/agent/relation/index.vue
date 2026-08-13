<template>
  <div class="app-container">
    <el-form :inline="true" :model="filters" class="mb8" label-width="0px">
      <el-form-item>
        <el-input v-model="filters.username" :placeholder="t('agent.username')" clearable style="width:200px" @keyup.enter="fetchStats" />
      </el-form-item>
      <el-form-item>
        <el-radio-group v-model="preset" @change="onPresetChange">
          <el-radio-button label="today">{{ t('agent.preset.today') }}</el-radio-button>
          <el-radio-button label="yesterday">{{ t('agent.preset.yesterday') }}</el-radio-button>
          <el-radio-button label="thisWeek">{{ t('agent.preset.thisWeek') }}</el-radio-button>
          <el-radio-button label="thisMonth">{{ t('agent.preset.thisMonth') }}</el-radio-button>
          <el-radio-button label="lastMonth">{{ t('agent.preset.lastMonth') }}</el-radio-button>
          <el-radio-button label="custom">{{ t('agent.preset.custom') }}</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="preset==='custom'">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="-" :start-placeholder="t('agent.startDate')" :end-placeholder="t('agent.endDate')" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="fetchStats">{{ t('agent.search') }}</el-button>
        <el-button @click="reset">{{ t('agent.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="userId" :label="t('agent.userId')" width="120" />
      <el-table-column prop="username" :label="t('agent.username')" />
      <el-table-column prop="totalSubCount" :label="t('agent.totalSubCount')" />
      <el-table-column prop="directInviteCount" :label="t('agent.directInviteCount')" />
      <el-table-column prop="totalBalance" :label="t('agent.totalBalance')" >
        <template #default="{ row }">{{ formatAmount(row.totalBalance) }}</template>
      </el-table-column>
      <el-table-column prop="rechargeAmount" :label="t('agent.rechargeAmount')">
        <template #default="{ row }">{{ formatAmount(row.rechargeAmount) }}</template>
      </el-table-column>
      <el-table-column prop="totalCommission" :label="t('agent.totalCommission')">
        <template #default="{ row }">{{ formatAmount(row.totalCommission) }}</template>
      </el-table-column>
      <el-table-column prop="withdrawAmount" :label="t('agent.withdrawAmount')">
        <template #default="{ row }">{{ formatAmount(row.withdrawAmount) }}</template>
      </el-table-column>
      <el-table-column prop="depositWithdrawDiff" :label="t('agent.depositWithdrawDiff')">
        <template #default="{ row }">{{ formatAmount(row.depositWithdrawDiff) }}</template>
      </el-table-column>
      <el-table-column fixed="right" :label="t('agent.operation')" width="160">
        <template #default="{ row }">
          <el-button type="primary" size="small" :disabled="!row.totalSubCount" @click="row.totalSubCount && openScopeUser(row)">{{ t('agent.viewSubUsers') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
    <!-- 查看下级用户抽屉 -->
    <el-drawer :title="t('agent.subUsersTitle')" v-model="scopeOpen" direction="rtl" size="80%" append-to-body>
      <SubStats v-if="selectedUserId" :userId="selectedUserId" />
    </el-drawer>
  </div>
</template>

<script>
import { ref, reactive, toRefs, watch, getCurrentInstance } from "vue";
import { ElMessage } from "element-plus";
import dayjs from "dayjs";
import { topLevelStats } from "@/api/member/member";
import SubStats from "./SubStats.vue";
import { useI18n } from 'vue-i18n';

export default {
  name: "AgentRelation",
  components: {
    SubStats,
  },
  setup() {
    const { t } = useI18n();
    const filters = ref({ username: "", beginTime: "", endTime: "" });
    const preset = ref("today");
    const dateRange = ref([]);
    const list = ref([]);
    const loading = ref(false);
    const total = ref(0);
    const scopeOpen = ref(false);
    const selectedUserId = ref(null);

    const data = reactive({
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
    });

    const { queryParams } = toRefs(data);

    function setPresetRange(p) {
      const now = dayjs();
      let start, end;
      switch (p) {
        case "today":
          start = now.startOf("day");
          end = now.endOf("day");
          break;
        case "yesterday":
          start = now.subtract(1, "day").startOf("day");
          end = now.subtract(1, "day").endOf("day");
          break;
        case "thisWeek":
          start = now.startOf("week");
          end = now.endOf("day");
          break;
        case "thisMonth":
          start = now.startOf("month");
          end = now.endOf("day");
          break;
        case "lastMonth":
          start = now.subtract(1, "month").startOf("month");
          end = now.subtract(1, "month").endOf("month");
          break;
        default:
          start = now.startOf("day");
          end = now.endOf("day");
      }
      filters.value.beginTime = start.format("YYYY-MM-DD HH:mm:ss");
      filters.value.endTime = end.format("YYYY-MM-DD HH:mm:ss");
      dateRange.value = [start.format("YYYY-MM-DD"), end.format("YYYY-MM-DD")];
    }

    function onPresetChange(val) {
      if (val && val !== "custom") {
        setPresetRange(val);
      } else if (val === "custom") {
        if (filters.value.beginTime && filters.value.endTime) {
          try {
            const s = dayjs(filters.value.beginTime).format("YYYY-MM-DD");
            const e = dayjs(filters.value.endTime).format("YYYY-MM-DD");
            dateRange.value = [s, e];
          } catch (e) {
            dateRange.value = [];
          }
        } else {
          dateRange.value = [];
        }
      } else {
        dateRange.value = [];
      }
    }

    function formatAmount(v) {
      const n = v == null ? 0 : Number(v);
      return new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n);
    }

    watch(dateRange, (val) => {
      if (Array.isArray(val) && val.length === 2 && val[0] && val[1]) {
        const s = dayjs(val[0]).startOf("day").format("YYYY-MM-DD HH:mm:ss");
        const e = dayjs(val[1]).endOf("day").format("YYYY-MM-DD HH:mm:ss");
        filters.value.beginTime = s;
        filters.value.endTime = e;
      }
    });

    async function getList() {
      loading.value = true;
      try {
        const params = {
          username: filters.value.username,
          beginTime: filters.value.beginTime,
          endTime: filters.value.endTime,
          pageNum: queryParams.value.pageNum,
          pageSize: queryParams.value.pageSize,
        };
        const res = await topLevelStats(params);
        const dataRes = res && res.data ? res.data : res;
        list.value = dataRes.rows || dataRes;
        total.value = dataRes.total || (Array.isArray(dataRes) ? dataRes.length : 0);
      } catch (e) {
        console.error(e);
      } finally {
        loading.value = false;
      }
    }

    function fetchStats() {
      // username is optional; allow fetching without username to retrieve paged top-level stats
      queryParams.value.pageNum = 1;
      getList();
    }

    function reset() {
      filters.value.username = "";
      preset.value = "today";
      setPresetRange("today");
      list.value = [];
      total.value = 0;
      queryParams.value.pageNum = 1;
    }

    // init
    setPresetRange(preset.value);
    // 默认初始化时自动加载第一页数据
    getList();

    function openScopeUser(row) {
      // Try common id field names returned by different APIs
      const keys = ["userId", "id", "user_id", "uid", "memberId", "member_id", "userID"];
      let id = null;
      if (row) {
        for (const k of keys) {
          if (Object.prototype.hasOwnProperty.call(row, k) && row[k] != null) {
            id = row[k];
            break;
          }
        }
        // try nested user object
        if (!id && row.user && (row.user.id || row.user.userId)) {
          id = row.user.id || row.user.userId;
        }
      }
      if (!id) {
        ElMessage.warning(t('agent.cannotDetermineUserId'));
        return;
      }
      selectedUserId.value = id;
      scopeOpen.value = true;
    }

    return { filters, preset, dateRange, list, loading, onPresetChange, fetchStats, reset, formatAmount, total, queryParams, getList, scopeOpen, selectedUserId, openScopeUser, t };
  },
};
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
