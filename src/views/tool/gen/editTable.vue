<template>
  <div class="app-container ant-pro-member-page">
    <a-card title="生成配置" :bordered="false" :loading="loading">
      <a-alert
        type="info"
        show-icon
        message="当前页面为 Ant 轻量配置预览。代码生成、预览、同步、导入、创建功能已保留，复杂配置编辑后续单独迁移。"
        class="mb16"
      />
      <a-descriptions bordered size="small" :column="2">
        <a-descriptions-item label="表名称">{{ info.tableName }}</a-descriptions-item>
        <a-descriptions-item label="表描述">{{ info.tableComment }}</a-descriptions-item>
        <a-descriptions-item label="实体类">{{ info.className }}</a-descriptions-item>
        <a-descriptions-item label="包路径">{{ info.packageName }}</a-descriptions-item>
        <a-descriptions-item label="模块名">{{ info.moduleName }}</a-descriptions-item>
        <a-descriptions-item label="业务名">{{ info.businessName }}</a-descriptions-item>
        <a-descriptions-item label="功能名">{{ info.functionName }}</a-descriptions-item>
        <a-descriptions-item label="生成方式">{{ info.genType === "1" ? "自定义路径" : "zip压缩包" }}</a-descriptions-item>
      </a-descriptions>

      <a-table
        class="mt16"
        row-key="columnId"
        size="middle"
        :columns="columnsDef"
        :data-source="columns"
        :pagination="false"
        :scroll="{ x: 1200 }"
      />

      <div class="page-actions">
        <a-button @click="close">返回</a-button>
      </div>
    </a-card>
  </div>
</template>

<script setup name="GenEdit">
import { getGenTable } from "@/api/tool/gen";

const route = useRoute();
const { proxy } = getCurrentInstance();
const loading = ref(true);
const info = ref({});
const columns = ref([]);

const columnsDef = [
  { title: "字段列名", dataIndex: "columnName", width: 180 },
  { title: "字段描述", dataIndex: "columnComment", width: 180 },
  { title: "物理类型", dataIndex: "columnType", width: 140 },
  { title: "Java类型", dataIndex: "javaType", width: 140 },
  { title: "Java属性", dataIndex: "javaField", width: 160 },
  { title: "插入", dataIndex: "isInsert", width: 80 },
  { title: "编辑", dataIndex: "isEdit", width: 80 },
  { title: "列表", dataIndex: "isList", width: 80 },
  { title: "查询", dataIndex: "isQuery", width: 80 },
  { title: "查询方式", dataIndex: "queryType", width: 120 },
  { title: "显示类型", dataIndex: "htmlType", width: 120 },
];

function getInfo() {
  loading.value = true;
  getGenTable(route.params.tableId)
    .then((res) => {
      info.value = res.data?.info || {};
      columns.value = res.data?.rows || [];
    })
    .finally(() => {
      loading.value = false;
    });
}

function close() {
  proxy.$tab.closeOpenPage({ path: "/tool/gen", query: { t: Date.now(), pageNum: route.query.pageNum } });
}

getInfo();
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}

.mt16 {
  margin-top: 16px;
}

.page-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
