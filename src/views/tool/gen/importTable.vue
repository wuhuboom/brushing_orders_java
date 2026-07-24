<template>
  <a-modal
    title="导入表"
    v-model:open="visible"
    width="820px"
    ok-text="确 定"
    cancel-text="取 消"
    :confirm-loading="submitting"
    @ok="handleImportTable"
    @cancel="visible = false"
  >
    <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
      <a-row :gutter="[16, 12]" align="middle">
        <a-col :span="8">
          <a-form-item label="表名称">
            <a-input v-model:value="queryParams.tableName" allow-clear placeholder="请输入表名称" @pressEnter="handleQuery" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="表描述">
            <a-input v-model:value="queryParams.tableComment" allow-clear placeholder="请输入表描述" @pressEnter="handleQuery" />
          </a-form-item>
        </a-col>
        <a-col :span="8" class="ant-pro-query-actions">
          <a-space>
            <a-button @click="resetQuery">重 置</a-button>
            <a-button type="primary" @click="handleQuery">搜 索</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-form>

    <a-table
      row-key="tableName"
      size="middle"
      :columns="columns"
      :data-source="dbTableList"
      :row-selection="rowSelection"
      :pagination="false"
      :scroll="{ y: 260 }"
      :custom-row="customRow"
    />

    <div v-if="total > 0" class="modal-pagination">
      <a-pagination
        v-model:current="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        show-size-changer
        @change="getList"
        @showSizeChange="getList"
      />
    </div>
  </a-modal>
</template>

<script setup>
import { listDbTable, importTable } from "@/api/tool/gen";

const total = ref(0);
const visible = ref(false);
const tables = ref([]);
const dbTableList = ref([]);
const selectedRowKeys = ref([]);
const submitting = ref(false);
const { proxy } = getCurrentInstance();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tableName: undefined,
  tableComment: undefined,
});

const columns = [
  { title: "表名称", dataIndex: "tableName", ellipsis: true },
  { title: "表描述", dataIndex: "tableComment", ellipsis: true },
  { title: "创建时间", dataIndex: "createTime", width: 180 },
  { title: "更新时间", dataIndex: "updateTime", width: 180 },
];

const emit = defineEmits(["ok"]);

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys) => {
    selectedRowKeys.value = keys;
    tables.value = keys;
  },
}));

function show() {
  getList();
  visible.value = true;
}

function customRow(row) {
  return {
    onClick: () => {
      const key = row.tableName;
      selectedRowKeys.value = selectedRowKeys.value.includes(key)
        ? selectedRowKeys.value.filter((item) => item !== key)
        : [...selectedRowKeys.value, key];
      tables.value = selectedRowKeys.value;
    },
  };
}

function getList() {
  listDbTable(queryParams).then((res) => {
    dbTableList.value = res.rows || [];
    total.value = res.total || 0;
  });
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.tableName = undefined;
  queryParams.tableComment = undefined;
  handleQuery();
}

async function handleImportTable() {
  const tableNames = tables.value.join(",");
  if (tableNames == "") {
    proxy.$modal.msgError("请选择要导入的表");
    return;
  }
  submitting.value = true;
  try {
    const res = await importTable({ tables: tableNames });
    proxy.$modal.msgSuccess(res.msg);
    if (res.code === 200) {
      visible.value = false;
      emit("ok");
    }
  } finally {
    submitting.value = false;
  }
}

defineExpose({
  show,
});
</script>

<style scoped>
.modal-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
