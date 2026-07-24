<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="代码生成列表"
      :columns="genColumns"
      :data-source="tableList"
      :loading="loading"
      row-key="tableId"
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
              <a-form-item label="表名称">
                <a-input v-model:value="queryParams.tableName" allow-clear placeholder="请输入表名称" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="表描述">
                <a-input v-model:value="queryParams.tableComment" allow-clear placeholder="请输入表描述" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="10" :lg="7">
              <a-form-item label="创建时间">
                <a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" class="full-width" />
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
          <a-button type="primary" :disabled="multiple" @click="handleGenTable" v-hasPermi="['tool:gen:code']">生 成</a-button>
          <a-button @click="openCreateTable" v-hasRole="['admin']">创 建</a-button>
          <a-button @click="openImportTable" v-hasPermi="['tool:gen:import']">导 入</a-button>
          <a-button :disabled="single" @click="handleEditTable" v-hasPermi="['tool:gen:edit']">修 改</a-button>
          <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['tool:gen:remove']">删 除</a-button>
        </a-space>
      </template>

      <template #bodyCell="{ column, record, index }">
        <template v-if="column.dataIndex === 'index'">
          {{ (queryParams.pageNum - 1) * queryParams.pageSize + index + 1 }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handlePreview(record)" v-hasPermi="['tool:gen:preview']">预览</a-button>
            <a-button type="link" size="small" @click="handleEditTable(record)" v-hasPermi="['tool:gen:edit']">编辑</a-button>
            <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['tool:gen:remove']">删除</a-button>
            <a-button type="link" size="small" @click="handleSynchDb(record)" v-hasPermi="['tool:gen:edit']">同步</a-button>
            <a-button type="link" size="small" @click="handleGenTable(record)" v-hasPermi="['tool:gen:code']">生成代码</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal :title="preview.title" v-model:open="preview.open" width="80%" :footer="null">
      <a-tabs v-model:activeKey="preview.activeName">
        <a-tab-pane v-for="item in previewTabs" :key="item.name" :tab="item.name">
          <div class="preview-toolbar">
            <a-button size="small" @click="copyCode(item.value)">复制</a-button>
          </div>
          <pre class="code-preview">{{ item.value }}</pre>
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <import-table ref="importRef" @ok="handleQuery" />
    <create-table ref="createRef" @ok="handleQuery" />
  </div>
</template>

<script setup name="Gen">
import { listTable, previewTable, delTable, genCode, synchDb } from "@/api/tool/gen";
import importTable from "./importTable";
import createTable from "./createTable";

const route = useRoute();
const { proxy } = getCurrentInstance();

const tableList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const tableNames = ref([]);
const dateRange = ref([]);
const uniqueId = ref("");
const importRef = ref();
const createRef = ref();

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    tableName: undefined,
    tableComment: undefined,
    orderByColumn: "createTime",
    isAsc: "descending",
  },
  preview: {
    open: false,
    title: "代码预览",
    data: {},
    activeName: "domain.java",
  },
});

const { queryParams, preview } = toRefs(data);

const genColumns = [
  { title: "序号", dataIndex: "index", width: 80, align: "center" },
  { title: "表名称", dataIndex: "tableName", width: 180, ellipsis: true },
  { title: "表描述", dataIndex: "tableComment", width: 220, ellipsis: true },
  { title: "实体", dataIndex: "className", width: 180, ellipsis: true },
  { title: "创建时间", dataIndex: "createTime", width: 180, align: "center" },
  { title: "更新时间", dataIndex: "updateTime", width: 180, align: "center" },
  { title: "操作", dataIndex: "action", width: 330, fixed: "right", align: "center" },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_, rows) => handleSelectionChange(rows),
}));

const previewTabs = computed(() =>
  Object.entries(preview.value.data || {}).map(([key, value]) => ({
    name: key.substring(key.lastIndexOf("/") + 1, key.indexOf(".vm")),
    value,
  }))
);

onActivated(() => {
  const time = route.query.t;
  if (time != null && time != uniqueId.value) {
    uniqueId.value = time;
    queryParams.value.pageNum = Number(route.query.pageNum);
    dateRange.value = [];
    getList();
  }
});

function getList() {
  loading.value = true;
  listTable(proxy.addDateRange(queryParams.value, dateRange.value))
    .then((response) => {
      tableList.value = response.rows || [];
      total.value = response.total || 0;
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

function handleGenTable(row = {}) {
  const tbNames = row.tableName || tableNames.value;
  if (tbNames == "") {
    proxy.$modal.msgError("请选择要生成的数据");
    return;
  }
  if (row.genType === "1") {
    genCode(row.tableName).then(() => {
      proxy.$modal.msgSuccess("成功生成到自定义路径：" + row.genPath);
    });
  } else {
    proxy.$download.zip("/tool/gen/batchGenCode?tables=" + tbNames, "ruoyi.zip");
  }
}

function handleSynchDb(row) {
  const tableName = row.tableName;
  proxy.$modal
    .confirm('确认要强制同步"' + tableName + '"表结构吗？')
    .then(() => synchDb(tableName))
    .then(() => {
      proxy.$modal.msgSuccess("同步成功");
    })
    .catch(() => {});
}

function openImportTable() {
  importRef.value?.show();
}

function openCreateTable() {
  createRef.value?.show();
}

function resetQuery() {
  dateRange.value = [];
  Object.assign(queryParams.value, {
    pageNum: 1,
    tableName: undefined,
    tableComment: undefined,
  });
  getList();
}

function handlePreview(row) {
  previewTable(row.tableId).then((response) => {
    preview.value.data = response.data || {};
    preview.value.open = true;
    preview.value.activeName = "domain.java";
  });
}

function copyCode(value) {
  navigator.clipboard?.writeText(value);
  proxy.$modal.msgSuccess("复制成功");
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.tableId);
  tableNames.value = selection.map((item) => item.tableName);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

function handleEditTable(row = {}) {
  const tableId = row.tableId || ids.value[0];
  const tableName = row.tableName || tableNames.value[0];
  const params = { pageNum: queryParams.value.pageNum };
  proxy.$tab.openPage("查看[" + tableName + "]生成配置", "/tool/gen-edit/index/" + tableId, params);
}

function handleDelete(row = {}) {
  const tableIds = row.tableId || ids.value;
  proxy.$modal
    .confirm('是否确认删除表编号为"' + tableIds + '"的数据项？')
    .then(() => delTable(tableIds))
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

getList();
</script>

<style scoped>
.full-width {
  width: 100%;
}

.preview-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.code-preview {
  max-height: 60vh;
  margin: 0;
  padding: 16px;
  overflow: auto;
  background: #f6f8fa;
  border-radius: 6px;
  font-size: 12px;
}
</style>
