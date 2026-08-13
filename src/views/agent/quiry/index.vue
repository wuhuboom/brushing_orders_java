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

    <el-table :data="list" v-loading="loading" border stripe show-summary :summary-method="getSummaries">
      <el-table-column prop="level" :label="t('agent.level')" />
      <el-table-column prop="totalMembers" :label="t('agent.totalMembers')" />
      <el-table-column prop="newRegisterCount" :label="t('agent.newRegisterCount')"  />
      <el-table-column prop="orderUserCount" :label="t('agent.orderUserCount')" />
      <el-table-column prop="orderAmount" :label="t('agent.orderAmount')" >
        <template #default="{ row }">{{ formatAmount(row.orderAmount) }}</template>
      </el-table-column>
      <el-table-column prop="totalBalance" :label="t('agent.totalBalance')" >
        <template #default="{ row }">{{ formatAmount(row.totalBalance) }}</template>
      </el-table-column>
      <el-table-column prop="firstDepositCount" :label="t('agent.firstDepositCount')"  />
      <el-table-column prop="rechargeAmount" :label="t('agent.rechargeAmount')">
        <template #default="{ row }">{{ formatAmount(row.rechargeAmount) }}</template>
      </el-table-column>
      <el-table-column prop="withdrawAmount" :label="t('agent.withdrawAmount')" >
        <template #default="{ row }">{{ formatAmount(row.withdrawAmount) }}</template>
      </el-table-column>
      <el-table-column prop="withdrawUserCount" :label="t('agent.withdrawUserCount')"  />
      <el-table-column prop="depositWithdrawDiff" :label="t('agent.depositWithdrawDiff')" >
        <template #default="{ row }">{{ formatAmount(row.depositWithdrawDiff) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { useI18n } from 'vue-i18n';
import { hierarchyStats } from "@/api/member/member";
import dayjs from "dayjs";
import { watch } from "vue";

export default {
  name: "AgentQuiry",
  setup() {
    const { t } = useI18n();
    const filters = ref({ username: "", beginTime: "", endTime: "" });
    const preset = ref("today");
    const dateRange = ref([]);
    const list = ref([]);
    const loading = ref(false);

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
        // 切到自定义时，不主动清空已有时间，保留当前范围并把它展示到 dateRange
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
        // no preset selected -> 清空但保持 filters 不改
        dateRange.value = [];
      }
    }

    function formatAmount(v) {
      const n = v == null ? 0 : Number(v);
      return new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n);
    }

    function getSummaries(param) {
      const { columns, data } = param;
      const sums = [];
      columns.forEach((column, index) => {
        if (index === 0) {
          sums[index] = t('agent.sum');
          return;
        }
        const property = column.property;
        if (!property) {
          sums[index] = '';
          return;
        }
        // For numeric columns, sum values
        const vals = data.map(item => item[property]);
        const allNumeric = vals.every(v => v == null || !isNaN(Number(v)));
        if (allNumeric) {
          const s = vals.reduce((acc, v) => acc + (v == null ? 0 : Number(v)), 0);
          // format numbers with 2 decimals for amounts
          sums[index] = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(s);
        } else {
          sums[index] = '';
        }
      });
      return sums;
    }

    // 当用户选择自定义日期区间时，把日期写回 filters
    watch(dateRange, (val) => {
      if (Array.isArray(val) && val.length === 2 && val[0] && val[1]) {
        const s = dayjs(val[0]).startOf("day").format("YYYY-MM-DD HH:mm:ss");
        const e = dayjs(val[1]).endOf("day").format("YYYY-MM-DD HH:mm:ss");
        filters.value.beginTime = s;
        filters.value.endTime = e;
      }
    });

    async function fetchStats() {
      if (!filters.value.username || !String(filters.value.username).trim()) {
        ElMessage.warning(t('agent.enterUsernameWarning'));
        return;
      }
      if (!filters.value.beginTime || !filters.value.endTime) {
        ElMessage.warning(t('agent.selectDateRangeWarning'));
        return;
      }
      loading.value = true;
      try {
        const params = {
          username: filters.value.username,
          beginTime: filters.value.beginTime,
          endTime: filters.value.endTime,
        };
        const res = await hierarchyStats(params);
        // 后端返回 success 包裹的结果，保守取 data
        list.value = res && res.data ? res.data : res;
      } catch (e) {
        console.error(e);
      } finally {
        loading.value = false;
      }
    }

    function reset() {
      filters.value.username = "";
      // 恢复默认为今天
      preset.value = "today";
      setPresetRange("today");
      list.value = [];
    }

    // 初始化：默认时间为今天，但不自动请求数据
    setPresetRange(preset.value);

    return { filters, preset, dateRange, list, loading, onPresetChange, fetchStats, reset, formatAmount, getSummaries, t };
  },
};
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
