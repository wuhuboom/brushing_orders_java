<template>
  <div class="app-container">
    <h4 class="form-header h4">基本信息</h4>
    <a-form :model="form" layout="vertical" class="auth-user-form">
      <a-row :gutter="24">
        <a-col :span="8" :offset="2">
          <a-form-item label="用户昵称" name="nickName">
            <a-input v-model:value="form.nickName" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="8" :offset="2">
          <a-form-item label="登录账号" name="userName">
            <a-input v-model:value="form.userName" disabled />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <ant-pro-table
      row-key="roleId"
      title="角色信息"
      :columns="roleColumns"
      :data-source="pagedRoles"
      :loading="loading"
      :row-selection="rowSelection"
      :pagination="{ current: pageNum, pageSize, total }"
      :custom-row="customRow"
      @page-change="handleAntPageChange"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'index'">
          {{ (pageNum - 1) * pageSize + index + 1 }}
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
      </template>
    </ant-pro-table>

    <div class="auth-role-actions">
      <a-space>
        <a-button type="primary" @click="submitForm">提交</a-button>
        <a-button @click="close">返回</a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup name="AuthRole">
import { getAuthRole, updateAuthRole } from "@/api/system/user"

const route = useRoute()
const { proxy } = getCurrentInstance()

const loading = ref(true)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const roleIds = ref([])
const roles = ref([])
const form = ref({
  nickName: undefined,
  userName: undefined,
  userId: undefined
})

const roleColumns = [
  { title: "序号", key: "index", width: 80 },
  { title: "角色编号", dataIndex: "roleId", width: 120 },
  { title: "角色名称", dataIndex: "roleName", width: 160 },
  { title: "权限字符", dataIndex: "roleKey", width: 180 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 }
]

const pagedRoles = computed(() => roles.value.slice((pageNum.value - 1) * pageSize.value, pageNum.value * pageSize.value))
const rowSelection = computed(() => ({
  selectedRowKeys: roleIds.value,
  preserveSelectedRowKeys: true,
  getCheckboxProps: record => ({ disabled: !checkSelectable(record) }),
  onChange: selectedRowKeys => {
    roleIds.value = selectedRowKeys
  }
}))

function customRow(record) {
  return {
    onClick: () => clickRow(record)
  }
}

function clickRow(row) {
  if (!checkSelectable(row)) return
  const index = roleIds.value.indexOf(row.roleId)
  if (index >= 0) {
    roleIds.value = roleIds.value.filter(id => id !== row.roleId)
  } else {
    roleIds.value = [...roleIds.value, row.roleId]
  }
}

function handleSelectionChange(selection) {
  roleIds.value = selection.map(item => item.roleId)
}

function getRowKey(row) {
  return row.roleId
}

function checkSelectable(row) {
  return row.status === "0"
}

function handleAntPageChange({ page, pageSize: size }) {
  pageNum.value = page
  pageSize.value = size
}

function close() {
  const obj = { path: "/system/users/user" }
  proxy.$tab.closeOpenPage(obj)
}

function submitForm() {
  const userId = form.value.userId
  const rIds = roleIds.value.join(",")
  updateAuthRole({ userId: userId, roleIds: rIds }).then(() => {
    proxy.$modal.msgSuccess("授权成功")
    close()
  })
}

(() => {
  const userId = route.params && route.params.userId
  if (userId) {
    loading.value = true
    getAuthRole(userId).then(response => {
      form.value = response.user
      roles.value = response.roles
      total.value = roles.value.length
      roleIds.value = roles.value.filter(row => row.flag).map(row => row.roleId)
      loading.value = false
    })
  }
})()
</script>

<style scoped>
.auth-user-form {
  margin-bottom: 16px;
}

.auth-role-actions {
  margin-top: 28px;
  text-align: center;
}
</style>
