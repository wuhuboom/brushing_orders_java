<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="用户列表" row-key="userId" :columns="columns" :data-source="rows" :loading="loading" :row-selection="rowSelection" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total }" :scroll="{ x: 2300 }" @page-change="pageChange" @refresh="load">
      <template #search>
        <a-form layout="horizontal" size="large" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle">
          <a-col :span="7"><a-form-item label="用户名"><a-input v-model:value="query.userName" allow-clear placeholder="请输入" @pressEnter="search" /></a-form-item></a-col>
          <a-col :span="7"><a-form-item label="手机号码"><a-input v-model:value="query.phonenumber" allow-clear placeholder="请输入" @pressEnter="search" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="是否在线"><a-select v-model:value="query.online" allow-clear placeholder="请选择"><a-select-option value="1">是</a-select-option><a-select-option value="0">否</a-select-option></a-select></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="组织"><a-tree-select v-model:value="query.deptId" :tree-data="deptOptions" :field-names="{ value: 'id', label: 'label', children: 'children' }" allow-clear tree-default-expand-all placeholder="请选择组织" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="名称"><a-input v-model:value="query.nickName" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="性别"><a-select v-model:value="query.sex" allow-clear placeholder="请选择"><a-select-option value="0">男</a-select-option><a-select-option value="1">女</a-select-option><a-select-option value="2">未知</a-select-option></a-select></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="最后登录IP"><a-input v-model:value="query.loginIp" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="是否启用"><a-select v-model:value="query.status" allow-clear placeholder="请选择"><a-select-option value="0">启用</a-select-option><a-select-option value="1">禁用</a-select-option></a-select></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="是否冻结"><a-select v-model:value="query.isLocked" allow-clear placeholder="请选择"><a-select-option value="0">是</a-select-option><a-select-option value="1">否</a-select-option></a-select></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="邮箱"><a-input v-model:value="query.email" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="是否内置"><a-select v-model:value="query.isBuiltin" allow-clear placeholder="请选择"><a-select-option value="Y">是</a-select-option><a-select-option value="N">否</a-select-option></a-select></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" /></a-form-item></a-col>
          <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="search">查询</a-button><a-button type="link" @click="queryExpanded = !queryExpanded">{{ queryExpanded ? '收起' : '展开' }}</a-button></a-space></a-col>
        </a-row></a-form>
      </template>
      <template #toolbar>
        <a-button :disabled="!selected.length" v-hasPermi="['system:user:edit']" @click="unlock"><UnlockOutlined />登录解冻</a-button>
        <a-button danger :disabled="!selected.length" v-hasPermi="['system:user:remove']" @click="remove"><DeleteOutlined />删除</a-button>
        <a-button type="primary" v-hasPermi="['system:user:add']" @click="create"><PlusOutlined />创建</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'username'"><span class="online-dot" :class="{ online: isOnline(record) }" />{{ record.userName }}</template>
        <template v-else-if="column.key === 'dept'">{{ record.dept?.deptName || '-' }}</template>
        <template v-else-if="column.key === 'avatar'"><image-preview v-if="record.avatar" :src="record.avatar" :width="48" :height="48" /><span v-else>-</span></template>
        <template v-else-if="column.key === 'sex'">{{ sexLabel(record.sex) }}</template>
        <template v-else-if="column.key === 'login'"><div class="login-info"><span>最后登录IP: {{ record.loginIp || '-' }}</span><span>最后登录地址: {{ record.loginLocation || '-' }}</span><span>最后登录时间: {{ parseTime(record.loginDate) || '-' }}</span></div></template>
        <template v-else-if="column.key === 'enabled'">{{ record.status === '0' ? '启用' : '禁用' }}</template>
        <template v-else-if="column.key === 'locked'">{{ record.isLocked === '0' ? '是' : '否' }}</template>
        <template v-else-if="column.key === 'builtin'">{{ isBuiltin(record.isBuiltin) ? '是' : '否' }}</template>
        <template v-else-if="column.key === 'time'">{{ parseTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'operation'"><a-space><a-button type="link" v-hasPermi="['system:user:edit']" @click="edit(record)">修改</a-button><a-button type="link" v-hasPermi="['system:user:add']" @click="copy(record)">复制</a-button><a-button type="link" v-hasPermi="['system:user:edit']" @click="openGoogle(record)">谷歌验证器</a-button></a-space></template>
      </template>
    </ant-pro-table>

    <a-drawer v-model:open="open" :title="title" width="1000px" destroy-on-close :body-style="{ paddingBottom: '72px' }" @close="close">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large">
        <a-row :gutter="24">
          <a-col :span="8"><a-form-item label="用户名" name="userName"><a-input v-model:value="form.userName" placeholder="用户名" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="手机号码" name="phonenumber"><a-input v-model:value="form.phonenumber" placeholder="手机号码" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="组织" name="deptId"><a-tree-select v-model:value="form.deptId" :tree-data="deptOptions" :field-names="{ value: 'id', label: 'label', children: 'children' }" tree-default-expand-all placeholder="组织" /></a-form-item></a-col>
          <a-col v-if="!form.userId" :span="8"><a-form-item label="登录密码" name="password"><a-input-password v-model:value="form.password" placeholder="登录密码" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="名称" name="nickName"><a-input v-model:value="form.nickName" placeholder="名称" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="性别" name="sex"><a-radio-group v-model:value="form.sex"><a-radio value="2" disabled>未知</a-radio><a-radio value="0">男</a-radio><a-radio value="1">女</a-radio></a-radio-group></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="是否启用" name="status"><a-radio-group v-model:value="form.status"><a-radio value="1">禁用</a-radio><a-radio value="0">启用</a-radio></a-radio-group></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="是否冻结" name="isLocked"><a-radio-group v-model:value="form.isLocked"><a-radio value="1">否</a-radio><a-radio value="0">是</a-radio></a-radio-group></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="邮箱"><a-input v-model:value="form.email" placeholder="邮箱" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="是否内置"><a-radio-group v-model:value="form.isBuiltin" disabled><a-radio value="N">否</a-radio><a-radio value="Y">是</a-radio></a-radio-group></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="头像"><image-upload v-model="form.avatar" :limit="1" :is-show-tip="false" :data="{ referenceType: 'USER_AVATAR', referenceTargetId: form.userId }" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" placeholder="备注" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="角色列表"><a-checkbox-group v-model:value="form.roleIds" class="legacy-checkbox-list"><a-checkbox v-for="item in roleOptions" :key="item.roleId" :value="item.roleId" :disabled="item.status === '1'">{{ item.roleName }}[{{ item.roleKey }}]</a-checkbox></a-checkbox-group></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="职位列表"><a-checkbox-group v-model:value="form.postIds" class="legacy-checkbox-list"><a-checkbox v-for="item in postOptions" :key="item.postId" :value="item.postId" :disabled="item.status === '1'">{{ item.postName }}<template v-if="item.postCode">[{{ item.postCode }}]</template></a-checkbox></a-checkbox-group></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="分组列表"><a-checkbox-group v-model:value="form.groupIds" class="legacy-checkbox-list"><a-checkbox v-for="item in groupOptions" :key="item.value" :value="item.value">{{ item.label }}</a-checkbox></a-checkbox-group></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer><div class="legacy-drawer-footer"><a-space><a-button size="large" @click="close">取消</a-button><a-button type="primary" size="large" @click="submit">确定</a-button></a-space></div></template>
    </a-drawer>

    <a-modal v-model:open="googleOpen" title="谷歌验证器" width="480px" @cancel="googleOpen = false">
      <a-form :label-col="{ style: { width: '140px' } }"><a-form-item label="启用谷歌验证器"><a-select v-model:value="googleForm.googleEnabled" style="width:100%"><a-select-option value="0">是</a-select-option><a-select-option value="1">否</a-select-option></a-select></a-form-item></a-form>
      <template #footer><a-space><a-button @click="googleOpen = false">取消</a-button><a-button type="primary" @click="saveGoogle">确定</a-button></a-space></template>
    </a-modal>
  </div>
</template>

<script setup name="User">
import { addUser, delUser, deptTreeSelect, getUser, listUser, toggleGoogleAuth, unlockUsers, updateUser } from '@/api/system/user'
import { listGroups } from '@/api/system/alignment'
import { DeleteOutlined, PlusOutlined, UnlockOutlined } from '@ant-design/icons-vue'

const { proxy } = getCurrentInstance()
const rows = ref([])
const selected = ref([])
const dateRange = ref([])
const loading = ref(false)
const total = ref(0)
const open = ref(false)
const title = ref('')
const queryExpanded = ref(false)
const formRef = ref()
const deptOptions = ref([])
const postOptions = ref([])
const roleOptions = ref([])
const groupOptions = ref([])
const initPassword = ref('')
const googleOpen = ref(false)
const googleForm = reactive({ userId: undefined, googleEnabled: '1' })
const query = reactive({ pageNum: 1, pageSize: 20, userName: undefined, phonenumber: undefined, online: undefined, deptId: undefined, nickName: undefined, sex: undefined, loginIp: undefined, status: undefined, isLocked: undefined, email: undefined, isBuiltin: undefined })
const form = reactive({ userId: undefined, userName: '', phonenumber: '', deptId: undefined, password: '', nickName: '', sex: '0', status: '0', isLocked: '1', email: '', isBuiltin: 'N', avatar: '', remark: '', roleIds: [], postIds: [], groupIds: [] })
const rules = {
  userName: [{ required: true, message: '用户名不能为空' }],
  phonenumber: [{ required: true, message: '手机号码不能为空' }],
  deptId: [{ required: true, message: '组织不能为空' }],
  password: [{ required: true, message: '登录密码不能为空' }, { min: 5, max: 20, message: '密码长度必须为 5 到 20 个字符' }],
  nickName: [{ required: true, message: '名称不能为空' }],
  sex: [{ required: true, message: '性别不能为空' }], status: [{ required: true, message: '请选择是否启用' }], isLocked: [{ required: true, message: '请选择是否冻结' }]
}
const columns = [
  { title: 'ID', dataIndex: 'userId', width: 90, fixed: 'left' },
  { title: '用户名', dataIndex: 'userName', key: 'username', width: 160 },
  { title: '手机号码', dataIndex: 'phonenumber', width: 140 },
  { title: '组织', key: 'dept', width: 160 },
  { title: '头像', key: 'avatar', width: 90 },
  { title: '名称', dataIndex: 'nickName', width: 150 },
  { title: '性别', dataIndex: 'sex', key: 'sex', width: 80 },
  { title: '登录信息', key: 'login', width: 280 },
  { title: '是否启用', dataIndex: 'status', key: 'enabled', width: 100 },
  { title: '是否冻结', dataIndex: 'isLocked', key: 'locked', width: 100 },
  { title: '邮箱', dataIndex: 'email', width: 210 },
  { title: '是否内置', dataIndex: 'isBuiltin', key: 'builtin', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'time', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 240, ellipsis: true },
  { title: '操作', key: 'operation', width: 260, fixed: 'right' }
]
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: keys => { selected.value = keys } }))

function params() { const value = { ...query }; if (dateRange.value?.length) [value.beginTime, value.endTime] = dateRange.value; return value }
async function load() { loading.value = true; try { const result = await listUser(params()); rows.value = result.rows || []; total.value = result.total || 0 } finally { loading.value = false } }
function search() { query.pageNum = 1; load() }
function resetQuery() { Object.assign(query, { pageNum: 1, userName: undefined, phonenumber: undefined, online: undefined, deptId: undefined, nickName: undefined, sex: undefined, loginIp: undefined, status: undefined, isLocked: undefined, email: undefined, isBuiltin: undefined }); dateRange.value = []; load() }
function pageChange({ page, pageSize }) { query.pageNum = page; query.pageSize = pageSize; load() }
function isOnline(record) { return record.online === '1' || record.online === true }
function isBuiltin(value) { return value === 'Y' || value === '0' || value === true }
function sexLabel(value) { return ({ '0': '男', '1': '女', '2': '未知' })[value] || '未知' }
function clear() { Object.assign(form, { userId: undefined, userName: '', phonenumber: '', deptId: undefined, password: initPassword.value, nickName: '', sex: '0', status: '0', isLocked: '1', email: '', isBuiltin: 'N', avatar: '', remark: '', roleIds: [], postIds: [], groupIds: [] }); formRef.value?.clearValidate?.() }
async function loadEditor(id) { const [user, groups] = await Promise.all([getUser(id), listGroups({ pageNum: 1, pageSize: 1000 })]); postOptions.value = user.posts || []; roleOptions.value = user.roles || []; groupOptions.value = (groups.rows || []).map(item => ({ label: item.groupCode ? `${item.groupName}[${item.groupCode}]` : item.groupName, value: item.groupId })); return user }
async function create() { clear(); const detail = await loadEditor(); title.value = '创建'; open.value = true; postOptions.value = detail.posts || []; roleOptions.value = detail.roles || [] }
async function fill(id, asCopy = false) { clear(); const detail = await loadEditor(id); Object.assign(form, detail.data || {}); form.postIds = detail.postIds || []; form.roleIds = detail.roleIds || []; form.groupIds = detail.groupIds || []; if (asCopy) { form.userId = undefined; form.userName = `${form.userName}_copy`; form.password = ''; form.isBuiltin = 'N' } else form.password = ''; title.value = asCopy ? '复制' : '修改'; open.value = true }
const edit = row => fill(row.userId)
const copy = row => fill(row.userId, true)
async function submit() { await formRef.value?.validate(); form.userId ? await updateUser(form) : await addUser(form); proxy.$modal.msgSuccess('保存成功'); close(); load() }
function close() { open.value = false; clear() }
async function unlock() { await proxy.$modal.confirm(`确认解冻所选用户 ${selected.value} 的登录状态吗？`); await unlockUsers(selected.value); proxy.$modal.msgSuccess('解冻成功'); load() }
async function remove() { const ids = selected.value; await proxy.$modal.confirm(`确认删除用户 ${ids} 吗？`); await delUser(ids); proxy.$modal.msgSuccess('删除成功'); selected.value = []; load() }
function openGoogle(row) { Object.assign(googleForm, { userId: row.userId, googleEnabled: row.googleEnabled || '1' }); googleOpen.value = true }
async function saveGoogle() { await toggleGoogleAuth(googleForm.userId, googleForm.googleEnabled); proxy.$modal.msgSuccess('保存成功'); googleOpen.value = false; load() }

onMounted(async () => { const tree = await deptTreeSelect(); deptOptions.value = tree.data || []; load() })
</script>

<style scoped>
.online-dot { display: inline-block; width: 8px; height: 8px; margin-right: 8px; border-radius: 50%; background: #bfbfbf; }
.online-dot.online { background: #52c41a; }
.login-info { display: flex; flex-direction: column; line-height: 22px; }
.legacy-drawer-footer { display: flex; justify-content: flex-end; }
.legacy-checkbox-list { display: flex; flex-wrap: wrap; gap: 12px 20px; width: 100%; }
.legacy-checkbox-list :deep(.ant-checkbox-wrapper) { margin-inline-start: 0; }
</style>
