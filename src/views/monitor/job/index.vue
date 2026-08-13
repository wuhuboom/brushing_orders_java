<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="定时任务列表"
      :columns="jobColumns"
      :data-source="jobList"
      :loading="loading"
      row-key="jobId"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 1320 }"
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
              <a-form-item label="任务状态">
                <a-select v-model:value="queryParams.status" allow-clear placeholder="请选择任务状态">
                  <a-select-option v-for="dict in sys_job_status" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
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
          <a-button type="primary" @click="handleAdd" v-hasPermi="['monitor:job:add']">新 增</a-button>
          <a-button :disabled="single" @click="handleUpdate" v-hasPermi="['monitor:job:edit']">修 改</a-button>
          <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['monitor:job:remove']">删 除</a-button>
          <a-button @click="handleExport" v-hasPermi="['monitor:job:export']">导 出</a-button>
          <a-button @click="handleJobLog" v-hasPermi="['monitor:job:query']">日 志</a-button>
        </a-space>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'jobGroup'">
          <dict-tag :options="sys_job_group" :value="record.jobGroup" />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch
            :checked="record.status === '0'"
            checked-children="启用"
            un-checked-children="停用"
            @change="(checked) => handleStatusChange(record, checked)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['monitor:job:edit']">修改</a-button>
            <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['monitor:job:remove']">删除</a-button>
            <a-button type="link" size="small" @click="handleRun(record)" v-hasPermi="['monitor:job:changeStatus']">执行一次</a-button>
            <a-button type="link" size="small" @click="handleView(record)" v-hasPermi="['monitor:job:query']">详细</a-button>
            <a-button type="link" size="small" @click="handleJobLog(record)" v-hasPermi="['monitor:job:query']">日志</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      :title="title"
      v-model:open="open"
      width="820px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="submitting"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form ref="jobRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="任务名称" name="jobName">
              <a-input v-model:value="form.jobName" placeholder="请输入任务名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="任务分组" name="jobGroup">
              <a-select v-model:value="form.jobGroup" placeholder="请选择">
                <a-select-option v-for="dict in sys_job_group" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item name="invokeTarget">
              <template #label>
                <a-space :size="4">
                  <span>调用方法</span>
                  <a-tooltip>
                    <template #title>
                      <div>Bean调用格式：beanName.method('参数')</div>
                      <div>Class调用格式：com.order.module.Task.method('参数')</div>
                      <div>参数说明：支持字符串、布尔、长整型、浮点型、整型</div>
                    </template>
                    <QuestionCircleOutlined />
                  </a-tooltip>
                </a-space>
              </template>
              <a-input v-model:value="form.invokeTarget" placeholder="请输入调用目标字符串" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="cron表达式" name="cronExpression">
              <a-space-compact class="full-width">
                <a-input v-model:value="form.cronExpression" placeholder="请输入cron执行表达式" />
                <a-button @click="handleShowCron">生成表达式</a-button>
              </a-space-compact>
            </a-form-item>
          </a-col>
          <a-col v-if="form.jobId !== undefined" :span="24">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio v-for="dict in sys_job_status" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="执行策略" name="misfirePolicy">
              <a-radio-group v-model:value="form.misfirePolicy" option-type="button" button-style="solid">
                <a-radio-button value="1">立即执行</a-radio-button>
                <a-radio-button value="2">执行一次</a-radio-button>
                <a-radio-button value="3">放弃执行</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否并发" name="concurrent">
              <a-radio-group v-model:value="form.concurrent" option-type="button" button-style="solid">
                <a-radio-button value="0">允许</a-radio-button>
                <a-radio-button value="1">禁止</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      title="Cron表达式生成器"
      v-model:open="openCron"
      width="900px"
      :footer="null"
      destroy-on-close
    >
      <crontab
        ref="crontabRef"
        :expression="expression"
        @hide="openCron = false"
        @fill="crontabFill"
      />
    </a-modal>

    <a-modal title="任务详细" v-model:open="openView" width="760px" :footer="null">
      <a-descriptions bordered size="small" :column="2">
        <a-descriptions-item label="任务编号">{{ form.jobId }}</a-descriptions-item>
        <a-descriptions-item label="任务名称">{{ form.jobName }}</a-descriptions-item>
        <a-descriptions-item label="任务分组">{{ jobGroupFormat(form) }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ form.createTime }}</a-descriptions-item>
        <a-descriptions-item label="cron表达式">{{ form.cronExpression }}</a-descriptions-item>
        <a-descriptions-item label="下次执行时间">{{ parseTime(form.nextValidTime) }}</a-descriptions-item>
        <a-descriptions-item label="调用目标方法" :span="2">{{ form.invokeTarget }}</a-descriptions-item>
        <a-descriptions-item label="任务状态">{{ form.status == 0 ? "正常" : "暂停" }}</a-descriptions-item>
        <a-descriptions-item label="是否并发">{{ form.concurrent == 0 ? "允许" : "禁止" }}</a-descriptions-item>
        <a-descriptions-item label="执行策略">{{ misfirePolicyText(form.misfirePolicy) }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup name="Job">
import { QuestionCircleOutlined } from "@ant-design/icons-vue";
import Crontab from "@/components/Crontab";
import { listJob, getJob, delJob, addJob, updateJob, runJob, changeJobStatus } from "@/api/monitor/job";

const router = useRouter();
const { proxy } = getCurrentInstance();
const { sys_job_group, sys_job_status } = proxy.useDict("sys_job_group", "sys_job_status");

const jobList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const openView = ref(false);
const openCron = ref(false);
const expression = ref("");
const submitting = ref(false);
const jobRef = ref();

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    jobName: undefined,
    jobGroup: undefined,
    status: undefined,
  },
  rules: {
    jobName: [{ required: true, message: "任务名称不能为空", trigger: "blur" }],
    jobGroup: [{ required: true, message: "任务分组不能为空", trigger: "change" }],
    invokeTarget: [{ required: true, message: "调用目标字符串不能为空", trigger: "blur" }],
    cronExpression: [{ required: true, message: "cron执行表达式不能为空", trigger: "change" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

const jobColumns = [
  { title: "任务编号", dataIndex: "jobId", width: 100, align: "center" },
  { title: "任务名称", dataIndex: "jobName", width: 180, ellipsis: true },
  { title: "任务组名", dataIndex: "jobGroup", width: 130, align: "center" },
  { title: "调用目标字符串", dataIndex: "invokeTarget", width: 260, ellipsis: true },
  { title: "cron执行表达式", dataIndex: "cronExpression", width: 180, ellipsis: true },
  { title: "状态", dataIndex: "status", width: 120, align: "center" },
  { title: "操作", dataIndex: "action", width: 300, fixed: "right", align: "center" },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_, rows) => handleSelectionChange(rows),
}));

function getList() {
  loading.value = true;
  listJob(queryParams.value)
    .then((response) => {
      jobList.value = response.rows || [];
      total.value = response.total || 0;
    })
    .finally(() => {
      loading.value = false;
    });
}

function jobGroupFormat(row) {
  return proxy.selectDictLabel(sys_job_group.value, row.jobGroup);
}

function misfirePolicyText(value) {
  const map = {
    0: "默认策略",
    1: "立即执行",
    2: "执行一次",
    3: "放弃执行",
  };
  return map[value] || "-";
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    jobId: undefined,
    jobName: undefined,
    jobGroup: undefined,
    invokeTarget: undefined,
    cronExpression: undefined,
    misfirePolicy: "1",
    concurrent: "1",
    status: "0",
  };
  nextTick(() => jobRef.value?.clearValidate?.());
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
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
  ids.value = selection.map((item) => item.jobId);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

function handleStatusChange(row, checked) {
  const oldStatus = row.status;
  const nextStatus = checked ? "0" : "1";
  row.status = nextStatus;
  const text = nextStatus === "0" ? "启用" : "停用";
  proxy.$modal
    .confirm('确认要"' + text + '""' + row.jobName + '"任务吗?')
    .then(() => changeJobStatus(row.jobId, nextStatus))
    .then(() => {
      proxy.$modal.msgSuccess(text + "成功");
    })
    .catch(() => {
      row.status = oldStatus;
    });
}

function handleRun(row) {
  proxy.$modal
    .confirm('确认要立即执行一次"' + row.jobName + '"任务吗?')
    .then(() => runJob(row.jobId, row.jobGroup))
    .then(() => {
      proxy.$modal.msgSuccess("执行成功");
    })
    .catch(() => {});
}

function handleView(row) {
  getJob(row.jobId).then((response) => {
    form.value = response.data;
    openView.value = true;
  });
}

function handleShowCron() {
  expression.value = form.value.cronExpression;
  openCron.value = true;
}

function crontabFill(value) {
  form.value.cronExpression = value;
}

function handleJobLog(row = {}) {
  const jobId = row.jobId || 0;
  router.push("/monitor/job-log/index/" + jobId);
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加任务";
}

function handleUpdate(row = {}) {
  reset();
  const jobId = row.jobId || ids.value;
  getJob(jobId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改任务";
  });
}

async function submitForm() {
  try {
    await jobRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (form.value.jobId !== undefined) {
      await updateJob(form.value);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addJob(form.value);
      proxy.$modal.msgSuccess("新增成功");
    }
    open.value = false;
    getList();
  } finally {
    submitting.value = false;
  }
}

function handleDelete(row = {}) {
  const jobIds = row.jobId || ids.value;
  proxy.$modal
    .confirm('是否确认删除定时任务编号为"' + jobIds + '"的数据项?')
    .then(() => delJob(jobIds))
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

function handleExport() {
  proxy.download("monitor/job/export", {
    ...queryParams.value,
  }, `job_${new Date().getTime()}.xlsx`);
}

getList();
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
