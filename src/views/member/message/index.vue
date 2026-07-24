<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="列表" :columns="messageColumns" :data-source="messageList" :loading="loading" row-key="id" :row-selection="rowSelection" :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }" @page-change="handleAntPageChange" @refresh="getList">
      <template #search><a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle"><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="标题"><a-input v-model:value="queryParams.title" allow-clear placeholder="请输入标题" @pressEnter="handleQuery" /></a-form-item></a-col><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="是否启用"><a-select v-model:value="queryParams.isEnabled" allow-clear placeholder="请选择是否启用"><a-select-option v-for="dict in user_yes_no" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option></a-select></a-form-item></a-col><a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="handleQuery">查询</a-button></a-space></a-col></a-row></a-form></template>
      <template #toolbar><a-button type="primary" @click="handleAdd" v-hasPermi="['member:message:add']">创建</a-button><a-button :disabled="single" @click="handleUpdate" v-hasPermi="['member:message:edit']">修改</a-button><a-button :disabled="single" @click="handleInternationalization" v-hasPermi="['member:message:edit']">国际化</a-button><a-button :disabled="single" @click="handleCopy" v-hasPermi="['member:message:add']">复制</a-button><a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:message:remove']">删除</a-button><a-button @click="handleExport" v-hasPermi="['member:message:export']">导出</a-button></template>
      <template #bodyCell="{ column, record }"><template v-if="column.key === 'members'">{{ record.pushUsersDisplay }}</template><template v-else-if="column.key === 'enabled'">{{ dictText(user_yes_no, record.isEnabled) }}</template><template v-else-if="column.key === 'operation'"><a-space><a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:message:edit']">修改</a-button><a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:message:add']">复制</a-button><a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['member:message:remove']">删除</a-button></a-space></template></template>
    </ant-pro-table>
    <!-- 添加或修改站内信对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" ok-text="确 定" cancel-text="取 消" :confirm-loading="submitting" @ok="submitForm" @cancel="cancel">
      <a-form ref="messageRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="标题" name="title"><a-input v-model:value="form.title" placeholder="请输入标题" /></a-form-item>
        <a-form-item label="会员列表" name="memberList"><a-select v-model:value="form.memberList" placeholder="请选择用户" mode="multiple" show-search allow-clear class="full-width"><a-select-option v-for="u in usersList" :key="u.id" :value="u.id">{{ u.username }}</a-select-option></a-select></a-form-item>
        <a-form-item label="是否启用" name="isEnabled"><a-radio-group v-model:value="form.isEnabled"><a-radio v-for="dict in user_yes_no" :key="dict.value" :value="parseInt(dict.value)">{{ dict.label }}</a-radio></a-radio-group></a-form-item>
        <a-form-item label="内容" name="content"><editor v-model="form.content" :min-height="192" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="Message">
import {
  listMessage,
  getMessage,
  delMessage,
  addMessage,
  updateMessage,
} from "@/api/member/message";
import { allUser } from "@/api/member/orderuser";

const { proxy } = getCurrentInstance();
const { user_yes_no } = proxy.useDict("user_yes_no");

const messageList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const usersList = ref([]);
const submitting = ref(false);
const messageRef = ref();

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: null,
    memberList: null,
    isEnabled: null,
    content: null,
  },
  rules: {
    title: [{ required: true, message: "标题不能为空", trigger: "blur" }],
    memberList: [
      { required: true, message: "会员列表不能为空", trigger: "blur" },
    ],
    isEnabled: [
      { required: true, message: "是否启用不能为空", trigger: "change" },
    ],
    content: [{ required: true, message: "内容不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

const messageColumns = [
  { title: "ID", dataIndex: "id", width: 90 },
  { title: "标题", dataIndex: "title", width: 220 },
  { title: "会员列表", key: "members", dataIndex: "memberList", width: 240 },
  { title: "是否启用", key: "enabled", dataIndex: "isEnabled", width: 120 },
  { title: "操作", key: "operation", width: 130, fixed: "right" },
];
const rowSelection = computed(() => ({ selectedRowKeys: ids.value, onChange: (_keys, rows) => handleSelectionChange(rows) }));
function dictText(options, value) { return options.value?.find((item) => String(item.value) === String(value))?.label ?? value ?? "-"; }
function handleAntPageChange({ page, pageSize }) { queryParams.value.pageNum = page; queryParams.value.pageSize = pageSize; getList(); }

function loadUsers() {
  allUser()
    .then((response) => {
      usersList.value = response.rows || response.data || response;
    })
    .catch(() => {
      usersList.value = [];
    });
}

/** 查询站内信列表 */
function getList() {
  loading.value = true;
  listMessage(queryParams.value)
    .then((response) => {
      messageList.value = (response.rows || []).map((row) => {
        const ids = String(row.memberList || "")
          .split(",")
          .map((id) => parseInt(id.trim()))
          .filter((id) => id && !isNaN(id));
        const usernames = ids
          .map((id) => usersList.value.find((u) => u.id === id)?.username || id)
          .join(", ");
        row.pushUsersDisplay = usernames || "-";
        return row;
      });
      total.value = response.total;
      loading.value = false;
    })
    .catch(() => {});
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    title: null,
    memberList: null,
    isEnabled: 0,
    createTime: null,
    content: null,
  };
  nextTick(() => messageRef.value?.clearValidate?.());
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();

  open.value = true;
  title.value = "添加站内信";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getMessage(_id).then((response) => {
    form.value = response.data;
    const userIds = form.value.memberList
      .split(",")
      .map((id) => parseInt(id.trim()))
      .filter((id) => id && !isNaN(id));
    form.value.memberList = userIds;
    open.value = true;
    title.value = "修改站内信";
  });
}

function handleInternationalization() {
  const row = messageList.value.find((item) => item.id === ids.value[0]);
  if (row) {
    handleUpdate(row);
  }
}

function handleCopy(row) {
  const _id = row?.id || ids.value[0];
  getMessage(_id).then((response) => {
    const copied = { ...response.data };
    delete copied.id;
    delete copied.createTime;
    if (Array.isArray(copied.memberList)) {
      copied.memberList = copied.memberList.join(",");
    }
    addMessage(copied).then(() => {
      proxy.$modal.msgSuccess("复制成功");
      getList();
    });
  });
}

/** 提交按钮 */
async function submitForm() {
  try {
    await messageRef.value?.validate();
  } catch {
    return;
  }

  const payload = {
    ...form.value,
    memberList: Array.isArray(form.value.memberList)
      ? form.value.memberList.join(",")
      : form.value.memberList,
  };

  submitting.value = true;
  try {
    if (form.value.id != null) {
      await updateMessage(payload);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addMessage(payload);
      proxy.$modal.msgSuccess("新增成功");
    }
    open.value = false;
    getList();
  } finally {
    submitting.value = false;
  }
}

/** 删除按钮操作 */
function handleDelete(row = {}) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除站内信编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delMessage(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/message/export",
    {
      ...queryParams.value,
    },
    `message_${new Date().getTime()}.xlsx`
  );
}

getList();
loadUsers();
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
