<template>
  <div class="app-container home-dashboard">
    <div class="summary-grid">
      <a-card v-for="card in summaryCards" :key="card.key" class="summary-card" :bordered="false">
        <div class="summary-main">
          <component :is="card.icon" class="summary-icon" :style="{ color: card.color }" />
          <div>
            <div class="summary-title">{{ card.title }}</div>
            <div class="summary-value" :style="{ color: card.color }">{{ card.value }}</div>
          </div>
        </div>
        <div class="summary-secondary">
          <span v-for="item in card.secondary" :key="item.label">
            {{ item.label }}<em :style="{ color: card.color }">{{ item.value }}</em>
          </span>
        </div>
        <div class="summary-total">
          <strong>{{ card.total.label }}</strong>
          <span :style="{ color: card.color }">{{ card.total.value }}</span>
        </div>
      </a-card>
    </div>

    <div class="chart-grid chart-grid-top">
      <a-card class="chart-card" :bordered="true">
        <h2>会员注册数量</h2>
        <div ref="memberChartRef" class="chart-container"></div>
      </a-card>
      <a-card class="chart-card" :bordered="true">
        <h2>交易金额</h2>
        <div ref="amountChartRef" class="chart-container"></div>
      </a-card>
    </div>

    <div class="chart-grid">
      <a-card class="chart-card chart-card-full" :bordered="true">
        <h2>交易数量</h2>
        <div ref="countChartRef" class="chart-container"></div>
      </a-card>
    </div>
  </div>
</template>

<script setup name="Index">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { MoneyCollectOutlined, NumberOutlined } from "@ant-design/icons-vue";
import { getStats } from "@/api/index";
import useSettingsStore from "@/store/modules/settings";

const settingsStore = useSettingsStore();
const memberChartRef = ref();
const amountChartRef = ref();
const countChartRef = ref();
const statsData = reactive({
  todayRechargeAmount: 0,
  todayGiftAmount: 0,
  totalGiftAmount: 0,
  totalRechargeAmount: 0,
  todayWithdrawalAmount: 0,
  todayCommissionAmount: 0,
  totalCommissionAmount: 0,
  totalWithdrawalAmount: 0,
  todayRegisteredUsers: 0,
  todayTasksCompleted: 0,
  totalTasksCompleted: 0,
  totalRegisteredUsers: 0,
  todayBettingUsers: 0,
  totalOrders: 0,
  todayOrders: 0,
  totalBettingUsers: 0,
  weeklyRegisteredUsers: [],
  weeklyTransactionAmounts: [],
  weeklyTransactionCounts: [],
});

let echartsPromise;
let memberChart;
let amountChart;
let countChart;
let resizeObserver;

const formatValue = (value, amount = false) => {
  const number = Number(value ?? 0);
  if (!Number.isFinite(number)) return "0";
  return amount
    ? number.toFixed(2).replace(/\.00$/, "").replace(/(\.\d)0$/, "$1")
    : String(Math.trunc(number));
};

const summaryCards = computed(() => [
  {
    key: "recharge",
    title: "今日充值金额",
    value: formatValue(statsData.todayRechargeAmount, true),
    icon: MoneyCollectOutlined,
    color: "#2f54eb",
    secondary: [
      { label: "今日赠送金额", value: formatValue(statsData.todayGiftAmount, true) },
      { label: "总赠送金额", value: formatValue(statsData.totalGiftAmount, true) },
    ],
    total: { label: "总充值金额", value: formatValue(statsData.totalRechargeAmount, true) },
  },
  {
    key: "withdrawal",
    title: "今日提现金额",
    value: formatValue(statsData.todayWithdrawalAmount, true),
    icon: MoneyCollectOutlined,
    color: "#52c41a",
    secondary: [
      { label: "今日返佣金额", value: formatValue(statsData.todayCommissionAmount, true) },
      { label: "总返佣金额", value: formatValue(statsData.totalCommissionAmount, true) },
    ],
    total: { label: "总提现金额", value: formatValue(statsData.totalWithdrawalAmount, true) },
  },
  {
    key: "member",
    title: "今日会员注册数量",
    value: formatValue(statsData.todayRegisteredUsers),
    icon: NumberOutlined,
    color: "#722ed1",
    secondary: [
      { label: "今日任务完成次数", value: formatValue(statsData.todayTasksCompleted) },
      { label: "总任务完成次数", value: formatValue(statsData.totalTasksCompleted) },
    ],
    total: { label: "总会员注册数量", value: formatValue(statsData.totalRegisteredUsers) },
  },
  {
    key: "betting",
    title: "今日投注人数",
    value: formatValue(statsData.todayBettingUsers),
    icon: NumberOutlined,
    color: "#faad14",
    secondary: [
      { label: "总订单数量", value: formatValue(statsData.totalOrders) },
      { label: "今日订单数量", value: formatValue(statsData.todayOrders) },
    ],
    total: { label: "总投注人数", value: formatValue(statsData.totalBettingUsers) },
  },
]);

function loadEcharts() {
  if (!echartsPromise) echartsPromise = import("@/utils/echarts-home").then(({ default: echarts }) => echarts);
  return echartsPromise;
}

function dateKeys() {
  const result = [];
  const today = new Date();
  for (let offset = 6; offset >= 0; offset -= 1) {
    const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() - offset);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    result.push(`${year}-${month}-${day}`);
  }
  return result;
}

function normalizeRows(rows, fields) {
  const source = new Map((Array.isArray(rows) ? rows : []).map((row) => [row.date, row]));
  return dateKeys().map((date) => {
    const row = source.get(date) || {};
    return fields.reduce((result, field) => {
      result[field] = Number(row[field] ?? 0);
      return result;
    }, { date });
  });
}

function chartTheme() {
  return settingsStore.isDark
    ? { text: "#d0d0d0", muted: "#8c8c8c", line: "#303030", tooltip: "#1f1f1f" }
    : { text: "rgba(0,0,0,.88)", muted: "rgba(0,0,0,.45)", line: "#e8e8e8", tooltip: "#ffffff" };
}

function baseOption(dates) {
  const theme = chartTheme();
  return {
    animationDuration: 500,
    textStyle: { color: theme.text },
    tooltip: {
      trigger: "axis",
      backgroundColor: theme.tooltip,
      borderColor: theme.line,
      textStyle: { color: theme.text },
    },
    legend: { bottom: 0, textStyle: { color: theme.text } },
    grid: { left: 24, right: 48, top: 20, bottom: 48, containLabel: true },
    xAxis: {
      type: "category",
      data: dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: theme.line } },
      axisLabel: { color: theme.muted },
      axisTick: { show: false },
    },
    yAxis: {
      type: "value",
      axisLabel: { color: theme.muted },
      splitLine: { lineStyle: { color: theme.line } },
    },
  };
}

function lineSeries(name, data, color, areaOpacity = 0.08) {
  return {
    name,
    type: "line",
    smooth: true,
    showSymbol: true,
    symbolSize: 5,
    data,
    lineStyle: { width: 2, color },
    itemStyle: { color },
    areaStyle: { color, opacity: areaOpacity },
  };
}

async function renderCharts() {
  await nextTick();
  const echarts = await loadEcharts();
  if (!memberChartRef.value || !amountChartRef.value || !countChartRef.value) return;
  memberChart ||= echarts.init(memberChartRef.value);
  amountChart ||= echarts.init(amountChartRef.value);
  countChart ||= echarts.init(countChartRef.value);

  const members = normalizeRows(statsData.weeklyRegisteredUsers, ["count"]);
  const amounts = normalizeRows(statsData.weeklyTransactionAmounts, [
    "recharge_amount", "withdrawal_amount", "commission_amount", "sub_commission_amount", "gift_amount", "signin_amount",
  ]);
  const counts = normalizeRows(statsData.weeklyTransactionCounts, [
    "recharge_count", "withdrawal_count", "commission_count", "sub_commission_count", "gift_count", "signin_count",
    "order_count", "betting_user_count", "task_completion_count", "reset_count",
  ]);
  const dates = members.map((item) => item.date);

  memberChart.setOption({
    ...baseOption(dates),
    xAxis: { ...baseOption(dates).xAxis, boundaryGap: true },
    legend: { ...baseOption(dates).legend, data: ["会员注册数量"] },
    series: [{ name: "会员注册数量", type: "bar", data: members.map((item) => item.count), barMaxWidth: 82, itemStyle: { color: "#5b8ff9" } }],
  }, true);

  const amountColors = ["#5b8ff9", "#5ad8a6", "#5d7092", "#f6bd16", "#6f5ef9", "#6dc8ec"];
  amountChart.setOption({
    ...baseOption(dates),
    legend: { ...baseOption(dates).legend, data: ["充值金额", "提现金额", "返佣金额", "上级返佣金额", "赠送金额", "签到金额"] },
    series: [
      lineSeries("充值金额", amounts.map((item) => item.recharge_amount), amountColors[0]),
      lineSeries("提现金额", amounts.map((item) => item.withdrawal_amount), amountColors[1]),
      lineSeries("返佣金额", amounts.map((item) => item.commission_amount), amountColors[2]),
      lineSeries("上级返佣金额", amounts.map((item) => item.sub_commission_amount), amountColors[3]),
      lineSeries("赠送金额", amounts.map((item) => item.gift_amount), amountColors[4]),
      lineSeries("签到金额", amounts.map((item) => item.signin_amount), amountColors[5]),
    ],
  }, true);

  const countDefinitions = [
    ["充值次数", "recharge_count", "#5b8ff9"],
    ["提现次数", "withdrawal_count", "#5ad8a6"],
    ["返佣次数", "commission_count", "#5d7092"],
    ["上级返佣次数", "sub_commission_count", "#f6bd16"],
    ["赠送次数", "gift_count", "#6f5ef9"],
    ["签到次数", "signin_count", "#6dc8ec"],
    ["订单数量", "order_count", "#9270ca"],
    ["投注人数", "betting_user_count", "#ff9d4d"],
    ["任务完成数量", "task_completion_count", "#269a99"],
    ["重置次数", "reset_count", "#ff99c3"],
  ];
  countChart.setOption({
    ...baseOption(dates),
    legend: { ...baseOption(dates).legend, data: countDefinitions.map(([name]) => name) },
    series: countDefinitions.map(([name, field, color]) => lineSeries(name, counts.map((item) => item[field]), color, 0.05)),
  }, true);
}

function resizeCharts() {
  memberChart?.resize();
  amountChart?.resize();
  countChart?.resize();
}

async function loadDashboard() {
  try {
    const response = await getStats();
    Object.assign(statsData, response.data || {});
  } catch (_) {
    // Request interceptor already reports the error; keep the dashboard usable with zero values.
  } finally {
    renderCharts();
  }
}

watch(() => settingsStore.isDark, () => renderCharts());

onMounted(() => {
  loadDashboard();
  resizeObserver = new ResizeObserver(resizeCharts);
  [memberChartRef.value, amountChartRef.value, countChartRef.value].filter(Boolean).forEach((element) => resizeObserver.observe(element));
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  memberChart?.dispose();
  amountChart?.dispose();
  countChart?.dispose();
});
</script>

<style scoped lang="scss">
.home-dashboard {
  margin-top: 32px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
}

.summary-card,
.chart-card {
  overflow: hidden;
  border-radius: 8px;
  background: var(--card-bg);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.summary-card {
  height: 182px;
}

.summary-card :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 24px 8px;
}

.summary-main {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.summary-icon {
  margin-top: 4px;
  font-size: 22px;
}

.summary-title {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.summary-value {
  margin-top: 8px;
  font-size: 27px;
  line-height: 32px;
}

.summary-secondary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  min-height: 42px;
  margin-top: 22px;
  color: var(--text-primary);
  font-size: 14px;
}

.summary-secondary em {
  margin-left: 6px;
  font-style: normal;
}

.summary-total {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 14px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
  margin-top: 24px;
}

.chart-card {
  height: 408px;
}

.chart-card-full {
  grid-column: 1 / -1;
}

.chart-card :deep(.ant-card-body) {
  height: 100%;
  padding: 24px;
}

.chart-card h2 {
  margin: 0 0 18px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

@media (max-width: 1199px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .chart-grid { grid-template-columns: minmax(0, 1fr); }
}

@media (max-width: 767px) {
  .home-dashboard { margin-top: 16px; }
  .summary-grid { grid-template-columns: minmax(0, 1fr); }
  .summary-secondary { margin-top: 14px; }
}
</style>
