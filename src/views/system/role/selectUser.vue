<template>
  <a-modal v-model:open="visible" title="选择用户" width="860px" :body-style="{ maxHeight: '72vh', overflowY: 'auto' }" @ok="handleSelectUser">
    <a-form ref="queryRef" :model="queryParams" layout="inline" class="ant-pro-search-form compact">
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
      title="待选用户"
      :columns="userColumns"
      :data-source="userList"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :custom-row="customRow"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <dict-tag :options="sys_normal_disable" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
      </template>
    </ant-pro-table>

    <template #footer>
      <div class="modal-footer-actions">
        <a-space>
          <a-button type="primary" @click="handleSelectUser">确定</a-button>
          <a-button @click="visible = false">取消</a-button>
        </a-space>
      </div>
    </template>
  </a-modal>
</template>

<script setup name="SelectUser">
import { authUserSelectAll, unallocatedUserList } from "@/api/system/role"

const props = defineProps({
  roleId: {
    type: [Number, String]
  }
})

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

const userList = ref([])
const visible = ref(false)
const total = ref(0)
const userIds = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleId: undefined,
  userName: undefined,
  phonenumber: undefined
})

const userColumns = [
  { title: "用户名称", dataIndex: "userName", width: 150 },
  { title: "用户昵称", dataIndex: "nickName", width: 150 },
  { title: "邮箱", dataIndex: "email", width: 200 },
  { title: "手机", dataIndex: "phonenumber", width: 130 },
  { title: "状态", dataIndex: "status", key: "status", width: 100 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: userIds.value,
  onChange: selectedRowKeys => {
    userIds.value = selectedRowKeys
  }
}))

function show() {
  queryParams.roleId = props.roleId
  userIds.value = []
  getList()
  visible.value = true
}

function customRow(record) {
  return {
    onClick: () => clickRow(record)
  }
}

function clickRow(row) {
  const index = userIds.value.indexOf(row.userId)
  if (index >= 0) {
    userIds.value = userIds.value.filter(id => id !== row.userId)
  } else {
    userIds.value = [...userIds.value, row.userId]
  }
}

function handleSelectionChange(selection) {
  userIds.value = selection.map(item => item.userId)
}

function getList() {
  unallocatedUserList(queryParams).then(res => {
    userList.value = res.rows
    total.value = res.total
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.pageNum = page
  queryParams.pageSize = pageSize
  getList()
}

const emit = defineEmits(["ok"])
function handleSelectUser() {
  const roleId = queryParams.roleId
  const uIds = userIds.value.join(",")
  if (uIds == "") {
    proxy.$modal.msgError("请选择要分配的用户")
    return
  }
  authUserSelectAll({ roleId: roleId, userIds: uIds }).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    visible.value = false
    emit("ok")
  })
}

defineExpose({
  show,
})
</script>
