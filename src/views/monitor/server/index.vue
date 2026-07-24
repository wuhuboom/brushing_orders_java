<template>
  <div class="app-container ant-pro-member-page">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="12">
        <a-card title="CPU" :bordered="false">
          <a-table
            size="middle"
            row-key="label"
            :columns="cpuColumns"
            :data-source="cpuRows"
            :pagination="false"
          />
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="12">
        <a-card title="内存" :bordered="false">
          <a-table
            size="middle"
            row-key="label"
            :columns="memoryColumns"
            :data-source="memoryRows"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'mem'">
                <span :class="{ 'text-danger': record.memDanger }">{{ record.mem }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'jvm'">
                <span :class="{ 'text-danger': record.jvmDanger }">{{ record.jvm }}</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="24">
        <a-card title="服务器信息" :bordered="false">
          <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item label="服务器名称">{{ server.sys?.computerName }}</a-descriptions-item>
            <a-descriptions-item label="操作系统">{{ server.sys?.osName }}</a-descriptions-item>
            <a-descriptions-item label="服务器IP">{{ server.sys?.computerIp }}</a-descriptions-item>
            <a-descriptions-item label="系统架构">{{ server.sys?.osArch }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <a-col :span="24">
        <a-card title="Java虚拟机信息" :bordered="false">
          <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item label="Java名称">{{ server.jvm?.name }}</a-descriptions-item>
            <a-descriptions-item label="Java版本">{{ server.jvm?.version }}</a-descriptions-item>
            <a-descriptions-item label="启动时间">{{ server.jvm?.startTime }}</a-descriptions-item>
            <a-descriptions-item label="运行时长">{{ server.jvm?.runTime }}</a-descriptions-item>
            <a-descriptions-item label="安装路径" :span="2">{{ server.jvm?.home }}</a-descriptions-item>
            <a-descriptions-item label="项目路径" :span="2">{{ server.sys?.userDir }}</a-descriptions-item>
            <a-descriptions-item label="运行参数" :span="2">{{ server.jvm?.inputArgs }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <a-col :span="24">
        <a-card title="磁盘状态" :bordered="false">
          <a-table
            size="middle"
            row-key="dirName"
            :columns="diskColumns"
            :data-source="server.sysFiles || []"
            :pagination="false"
            :scroll="{ x: 1000 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'usage'">
                <span :class="{ 'text-danger': record.usage > 80 }">{{ record.usage }}%</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { getServer } from "@/api/monitor/server";

const server = ref({
  cpu: {},
  mem: {},
  jvm: {},
  sys: {},
  sysFiles: [],
});
const { proxy } = getCurrentInstance();

const cpuColumns = [
  { title: "属性", dataIndex: "label", width: 180 },
  { title: "值", dataIndex: "value" },
];

const memoryColumns = [
  { title: "属性", dataIndex: "label", width: 180 },
  { title: "内存", dataIndex: "mem" },
  { title: "JVM", dataIndex: "jvm" },
];

const diskColumns = [
  { title: "盘符路径", dataIndex: "dirName", width: 180 },
  { title: "文件系统", dataIndex: "sysTypeName", width: 160 },
  { title: "盘符类型", dataIndex: "typeName", width: 160 },
  { title: "总大小", dataIndex: "total", width: 120 },
  { title: "可用大小", dataIndex: "free", width: 120 },
  { title: "已用大小", dataIndex: "used", width: 120 },
  { title: "已用百分比", dataIndex: "usage", width: 140 },
];

const cpuRows = computed(() => [
  { label: "核心数", value: server.value.cpu?.cpuNum },
  { label: "用户使用率", value: formatPercent(server.value.cpu?.used) },
  { label: "系统使用率", value: formatPercent(server.value.cpu?.sys) },
  { label: "当前空闲率", value: formatPercent(server.value.cpu?.free) },
]);

const memoryRows = computed(() => [
  {
    label: "总内存",
    mem: formatSize(server.value.mem?.total, "G"),
    jvm: formatSize(server.value.jvm?.total, "M"),
  },
  {
    label: "已用内存",
    mem: formatSize(server.value.mem?.used, "G"),
    jvm: formatSize(server.value.jvm?.used, "M"),
  },
  {
    label: "剩余内存",
    mem: formatSize(server.value.mem?.free, "G"),
    jvm: formatSize(server.value.jvm?.free, "M"),
  },
  {
    label: "使用率",
    mem: formatPercent(server.value.mem?.usage),
    jvm: formatPercent(server.value.jvm?.usage),
    memDanger: server.value.mem?.usage > 80,
    jvmDanger: server.value.jvm?.usage > 80,
  },
]);

function formatPercent(value) {
  return value === undefined ? "" : `${value}%`;
}

function formatSize(value, unit) {
  return value === undefined ? "" : `${value}${unit}`;
}

function getList() {
  proxy.$modal.loading("正在加载服务监控数据，请稍候！");
  getServer()
    .then((response) => {
      server.value = response.data || server.value;
    })
    .finally(() => {
      proxy.$modal.closeLoading();
    });
}

getList();
</script>
