<template>
  <div class="app-container ant-pro-member-page">
    <a-row :gutter="[16, 16]">
      <a-col :span="24">
        <a-card title="基本信息" :bordered="false">
          <a-descriptions bordered size="small" :column="4">
            <a-descriptions-item label="Redis版本">{{ cache.info?.redis_version }}</a-descriptions-item>
            <a-descriptions-item label="运行模式">{{ cache.info?.redis_mode == "standalone" ? "单机" : "集群" }}</a-descriptions-item>
            <a-descriptions-item label="端口">{{ cache.info?.tcp_port }}</a-descriptions-item>
            <a-descriptions-item label="客户端数">{{ cache.info?.connected_clients }}</a-descriptions-item>
            <a-descriptions-item label="运行时间(天)">{{ cache.info?.uptime_in_days }}</a-descriptions-item>
            <a-descriptions-item label="使用内存">{{ cache.info?.used_memory_human }}</a-descriptions-item>
            <a-descriptions-item label="使用CPU">{{ usedCpu }}</a-descriptions-item>
            <a-descriptions-item label="内存配置">{{ cache.info?.maxmemory_human }}</a-descriptions-item>
            <a-descriptions-item label="AOF是否开启">{{ cache.info?.aof_enabled == "0" ? "否" : "是" }}</a-descriptions-item>
            <a-descriptions-item label="RDB是否成功">{{ cache.info?.rdb_last_bgsave_status }}</a-descriptions-item>
            <a-descriptions-item label="Key数量">{{ cache.dbSize }}</a-descriptions-item>
            <a-descriptions-item label="网络入口/出口">{{ networkTraffic }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="12">
        <a-card title="命令统计" :bordered="false">
          <div ref="commandstats" class="chart-box" />
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="12">
        <a-card title="内存信息" :bordered="false">
          <div ref="usedmemory" class="chart-box" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup name="Cache">
import { getCache } from "@/api/monitor/cache";

const cache = ref({ info: {}, dbSize: 0, commandStats: [] });
const commandstats = ref(null);
const usedmemory = ref(null);
const { proxy } = getCurrentInstance();

const usedCpu = computed(() => {
  const value = cache.value.info?.used_cpu_user_children;
  return value === undefined ? "" : parseFloat(value).toFixed(2);
});

const networkTraffic = computed(() => {
  const info = cache.value.info;
  if (!info) return "";
  return `${info.instantaneous_input_kbps || 0}kps/${info.instantaneous_output_kbps || 0}kps`;
});

function getList() {
  proxy.$modal.loading("正在加载缓存监控数据，请稍候！");
  getCache().then(async (response) => {
    proxy.$modal.closeLoading();
    cache.value = response.data || { info: {}, dbSize: 0, commandStats: [] };
    const { default: echarts } = await import("@/utils/echarts-monitor");

    const commandstatsIntance = echarts.init(commandstats.value, "macarons");
    commandstatsIntance.setOption({
      tooltip: {
        trigger: "item",
        formatter: "{a} <br/>{b} : {c} ({d}%)",
      },
      series: [
        {
          name: "命令",
          type: "pie",
          roseType: "radius",
          radius: [15, 95],
          center: ["50%", "38%"],
          data: cache.value.commandStats,
          animationEasing: "cubicInOut",
          animationDuration: 1000,
        },
      ],
    });

    const usedmemoryInstance = echarts.init(usedmemory.value, "macarons");
    usedmemoryInstance.setOption({
      tooltip: {
        formatter: "{b} <br/>{a} : " + cache.value.info.used_memory_human,
      },
      series: [
        {
          name: "峰值",
          type: "gauge",
          min: 0,
          max: 1000,
          detail: {
            formatter: cache.value.info.used_memory_human,
          },
          data: [
            {
              value: parseFloat(cache.value.info.used_memory_human),
              name: "内存消耗",
            },
          ],
        },
      ],
    });
    window.addEventListener("resize", () => {
      commandstatsIntance.resize();
      usedmemoryInstance.resize();
    });
  });
}

getList();
</script>

<style scoped>
.chart-box {
  height: 420px;
}
</style>
