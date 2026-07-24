<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="在线用户列表"
      :columns="onlineColumns"
      :data-source="pagedOnlineList"
      :loading="loading"
      row-key="tokenId"
      :pagination="{ current: pageNum, pageSize, total }"
      :scroll="{ x: 1280 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="登录地址">
                <a-input
                  v-model:value="queryParams.ipaddr"
                  allow-clear
                  placeholder="请输入登录地址"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="用户名称">
                <a-input
                  v-model:value="queryParams.userName"
                  allow-clear
                  placeholder="请输入用户名称"
                  @pressEnter="handleQuery"
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

      <template #bodyCell="{ column, record, index }">
        <template v-if="column.dataIndex === 'index'">
          {{ (pageNum - 1) * pageSize + index + 1 }}
        </template>
        <template v-else-if="column.dataIndex === 'loginTime'">
          {{ parseTime(record.loginTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-button type="link" danger size="small" @click="handleForceLogout(record)" v-hasPermi="['monitor:online:forceLogout']">
            强退
          </a-button>
        </template>
      </template>
    </ant-pro-table>
  </div>
</template>

<script setup name="Online">
import { forceLogout, list as initData } from "@/api/monitor/online";

const { proxy } = getCurrentInstance();

const onlineList = ref([]);
const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const queryParams = ref({
  ipaddr: undefined,
  userName: undefined,
});

const onlineColumns = [
  { title: "序号", dataIndex: "index", width: 80, align: "center" },
  { title: "会话编号", dataIndex: "tokenId", width: 260, ellipsis: true },
  { title: "登录名称", dataIndex: "userName", width: 140, ellipsis: true },
  { title: "所属部门", dataIndex: "deptName", width: 150, ellipsis: true },
  { title: "主机", dataIndex: "ipaddr", width: 150, ellipsis: true },
  { title: "登录地点", dataIndex: "loginLocation", width: 160, ellipsis: true },
  { title: "操作系统", dataIndex: "os", width: 150, ellipsis: true },
  { title: "浏览器", dataIndex: "browser", width: 150, ellipsis: true },
  { title: "登录时间", dataIndex: "loginTime", width: 180, align: "center" },
  { title: "操作", dataIndex: "action", width: 100, fixed: "right", align: "center" },
];

const pagedOnlineList = computed(() =>
  onlineList.value.slice((pageNum.value - 1) * pageSize.value, pageNum.value * pageSize.value)
);

function getList() {
  loading.value = true;
  initData(queryParams.value)
    .then((response) => {
      onlineList.value = response.rows || [];
      total.value = response.total || onlineList.value.length;
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleQuery() {
  pageNum.value = 1;
  getList();
}

function resetQuery() {
  queryParams.value.ipaddr = undefined;
  queryParams.value.userName = undefined;
  handleQuery();
}

function handleAntPageChange({ page, pageSize: nextPageSize }) {
  pageNum.value = page;
  pageSize.value = nextPageSize;
}

function handleForceLogout(row) {
  proxy.$modal
    .confirm('是否确认强退名称为"' + row.userName + '"的用户?')
    .then(() => forceLogout(row.tokenId))
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

getList();
</script>
