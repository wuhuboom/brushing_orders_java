<template>
  <div class="app-container">
    <a-form v-show="showSearch" ref="queryRef" :model="queryParams" layout="inline" class="ant-pro-search-form">
      <a-form-item label="用户名称" name="userName">
        <a-input v-model:value="queryParams.userName" placeholder="请输入用户名称" allow-clear @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="手机号码" name="phonenumber">
        <a-input v-model:value="queryParams.phonenumber" placeholder="请输入手机号码" allow-clear @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item class="ant-pro-search-actions">
        <a-button @click="resetQuery">重置</a-button>
        <a-button type="primary" @click="handleQuery">查询</a-button>
      </a-form-item>
    </a-form>

    <ant-pro-table
      row-key="userId"
      title="授权用户列表"
      :columns="userColumns"
      :data-source="userList"
      :loading="loading"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #toolbar>
        <a-button type="primary" @click="openSelectUser" v-hasPermi="['system:role:add']">添加用户</a-button>
        <a-button danger :disabled="multiple" @click="cancelAuthUserAll" v-hasPermi="['system:role:remove']">批量取消授权</a-button>
        <a-button @click="handleClose">关闭</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <dict-tag :options="sys_normal_disable" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-button type="link" size="small" danger @click="cancelAuthUser(record)" v-hasPermi="['system:role:remove']">取消授权</a-button>
        </template>
      </template>
    </ant-pro-table>

    <select-user ref="selectRef" :roleId="queryParams.roleId" @ok="handleQuery" />
  </div>
</template>

<script setup name="AuthUser">
import selectUser from "./selectUser"
import { allocatedUserList, authUserCancel, authUserCancelAll } from "@/api/system/role"

const route = useRoute()
const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

const userList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const multiple = ref(true)
const total = ref(0)
const userIds = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleId: route.params.roleId,
  userName: undefined,
  phonenumber: undefined,
})

const userColumns = [
  { title: "用户名称", dataIndex: "userName", width: 160 },
  { title: "用户昵称", dataIndex: "nickName", width: 160 },
  { title: "邮箱", dataIndex: "email", width: 220 },
  { title: "手机", dataIndex: "phonenumber", width: 140 },
  { title: "状态", dataIndex: "status", key: "status", width: 110 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "operation", width: 140, fixed: "right" }
]

const rowSelection = computed(() => ({
  selectedRowKeys: userIds.value,
  onChange: (_selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows)
}))

function getList() {
  loading.value = true
  allocatedUserList(queryParams).then(response => {
    userList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleClose() {
  const obj = { path: "/system/permissions/role" }
  proxy.$tab.closeOpenPage(obj)
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection) {
  userIds.value = selection.map(item => item.userId)
  multiple.value = !selection.length
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.pageNum = page
  queryParams.pageSize = pageSize
  getList()
}

function openSelectUser() {
  proxy.$refs["selectRef"].show()
}

function cancelAuthUser(row) {
  proxy.$modal.confirm(`确认要取消该用户 "${row.userName}" 角色吗？`).then(function () {
    return authUserCancel({ userId: row.userId, roleId: queryParams.roleId })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("取消授权成功")
  }).catch(() => {})
}

function cancelAuthUserAll() {
  const roleId = queryParams.roleId
  const uIds = userIds.value.join(",")
  proxy.$modal.confirm("是否取消选中用户授权数据项？").then(function () {
    return authUserCancelAll({ roleId: roleId, userIds: uIds })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("取消授权成功")
  }).catch(() => {})
}

getList()
</script>
