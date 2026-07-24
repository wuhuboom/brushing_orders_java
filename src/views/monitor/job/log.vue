<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="调度日志列表"
      :columns="logColumns"
      :data-source="jobLogList"
      :loading="loading"
      row-key="jobLogId"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 1280 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form v-show="showSearch" layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="任务名称">
                <a-input
                  v-model:value="queryParams.jobName"
                  allow-clear
                  placeholder="请输入任务名称"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="任务组名">
                <a-select v-model:value="queryParams.jobGroup" allow-clear placeholder="请选择任务组名">
                  <a-select-option v-for="dict in sys_job_group" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="执行状态">
                <a-select v-model:value="queryParams.status" allow-clear placeholder="请选择执行状态">
                  <a-select-option v-for="dict in sys_common_status" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="12" :lg="6">
              <a-form-item label="执行时间">
                <a-range-picker
                  v-model:value="dateRange"
                  value-format="YYYY-MM-DD"
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col flex="auto" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">搜 索</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-space>
          <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['monitor:job:remove']">删 除</a-button>
          <a-button danger @click="handleClean" v-hasPermi="['monitor:job:remove']">清 空</a-button>
          <a-button @click="handleExport" v-hasPermi="['monitor:job:export']">导 出</a-button>
          <a-button @click="handleClose">关 闭</a-button>
        </a-space>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'jobGroup'">
          <dict-tag :options="sys_job_group" :value="record.jobGroup" />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <dict-tag :options="sys_common_status" :value="record.status" />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-button type="link" size="small" @click="handleView(record)" v-hasPermi="['monitor:job:query']">
            详细
          </a-button>
        </template>
      </template>
    </ant-pro-table>

    <a-modal title="调度日志详细" v-model:open="open" width="760px" :footer="null">
      <a-descriptions bordered size="small" :column="2">
        <a-descriptions-item label="日志序号">{{ form.jobLogId }}</a-descriptions-item>
        <a-descriptions-item label="任务名称">{{ form.jobName }}</a-descriptions-item>
        <a-descriptions-item label="任务分组">{{ form.jobGroup }}</a-descriptions-item>
        <a-descriptions-item label="执行时间">{{ form.createTime }}</a-descriptions-item>
        <a-descriptions-item label="调用方法" :span="2">{{ form.invokeTarget }}</a-descriptions-item>
        <a-descriptions-item label="日志信息" :span="2">{{ form.jobMessage }}</a-descriptions-item>
        <a-descriptions-item label="执行状态">{{ form.status == 0 ? "正常" : "失败" }}</a-descriptions-item>
        <a-descriptions-item v-if="form.status == 1" label="异常信息" :span="2">
          {{ form.exceptionInfo }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup name="JobLog">
import { getJob } from "@/api/monitor/job";
import { listJobLog, delJobLog, cleanJobLog } from "@/api/monitor/jobLog";

const { proxy } = getCurrentInstance();
const { sys_common_status, sys_job_group } = proxy.useDict("sys_common_status", "sys_job_group");

const jobLogList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const multiple = ref(true);
const total = ref(0);
const dateRange = ref([]);
const route = useRoute();

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    jobName: undefined,
    jobGroup: undefined,
    status: undefined,
  },
});

const { queryParams, form } = toRefs(data);

const logColumns = [
  { title: "日志编号", dataIndex: "jobLogId", width: 100, align: "center" },
  { title: "任务名称", dataIndex: "jobName", width: 180, ellipsis: true },
  { title: "任务组名", dataIndex: "jobGroup", width: 130, ellipsis: true },
  { title: "调用目标字符串", dataIndex: "invokeTarget", width: 260, ellipsis: true },
  { title: "日志信息", dataIndex: "jobMessage", width: 260, ellipsis: true },
  { title: "执行状态", dataIndex: "status", width: 120, align: "center" },
  { title: "执行时间", dataIndex: "createTime", width: 180, align: "center" },
  { title: "操作", dataIndex: "action", width: 100, fixed: "right", align: "center" },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_, rows) => handleSelectionChange(rows),
}));

function getList() {
  loading.value = true;
  listJobLog(proxy.addDateRange(queryParams.value, dateRange.value))
    .then((response) => {
      jobLogList.value = response.rows || [];
      total.value = response.total || 0;
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleClose() {
  proxy.$tab.closeOpenPage({ path: "/monitor/job" });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  dateRange.value = [];
  Object.assign(queryParams.value, {
    pageNum: 1,
    pageSize: queryParams.value.pageSize,
    jobName: undefined,
    jobGroup: undefined,
    status: undefined,
  });
  getList();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.jobLogId);
  multiple.value = !selection.length;
}

function handleView(row) {
  open.value = true;
  form.value = row;
}

function handleDelete() {
  proxy.$modal
    .confirm('是否确认删除调度日志编号为"' + ids.value + '"的数据项?')
    .then(() => delJobLog(ids.value))
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

function handleClean() {
  proxy.$modal
    .confirm("是否确认清空所有调度日志数据项?")
    .then(() => cleanJobLog())
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("清空成功");
    })
    .catch(() => {});
}

function handleExport() {
  proxy.download("monitor/jobLog/export", {
    ...queryParams.value,
  }, `job_log_${new Date().getTime()}.xlsx`);
}

(() => {
  const jobId = route.params && route.params.jobId;
  if (jobId !== undefined && jobId != 0) {
    getJob(jobId).then((response) => {
      queryParams.value.jobName = response.data.jobName;
      queryParams.value.jobGroup = response.data.jobGroup;
      getList();
    });
  } else {
    getList();
  }
})();
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
