<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="分组列表" row-key="groupId" :columns="columns" :data-source="rows" :loading="loading"
      :row-selection="rowSelection" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total }" :scroll="{ x: 980 }"
      @page-change="pageChange" @refresh="load">
      <template #search><a-form layout="horizontal" size="large" class="ant-pro-query-form"><a-row :gutter="[24,16]" align="middle">
        <a-col :span="8"><a-form-item label="名称"><a-input v-model:value="query.groupName" allow-clear placeholder="请输入" @pressEnter="search" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" /></a-form-item></a-col>
        <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="search">查询</a-button></a-space></a-col>
      </a-row></a-form></template>
      <template #toolbar><a-button danger :disabled="!selected.length" v-hasPermi="['system:group:remove']" @click="remove()"><DeleteOutlined />删除</a-button><a-button type="primary" v-hasPermi="['system:group:add']" @click="create"><PlusOutlined />创建</a-button></template>
      <template #bodyCell="{ column, record }"><template v-if="column.key === 'time'">{{ parseTime(record.createTime) }}</template><template v-else-if="column.key === 'operation'"><a-space><a-button type="link" v-hasPermi="['system:group:edit']" @click="edit(record)">修改</a-button><a-button type="link" v-hasPermi="['system:group:add']" @click="copy(record)">复制</a-button></a-space></template></template>
    </ant-pro-table>

    <a-drawer v-model:open="open" :title="title" width="800px" destroy-on-close :body-style="{ paddingBottom: '72px' }" @close="close">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large">
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="名称" name="groupName"><a-input v-model:value="form.groupName" placeholder="名称" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" placeholder="备注" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="策略列表"><a-checkbox-group v-model:value="form.strategyIds" class="legacy-checkbox-list"><a-checkbox v-for="item in strategyOptions" :key="item.value" :value="item.value">{{ item.label }}</a-checkbox></a-checkbox-group></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="资源列表"><a-tree checkable check-strictly :tree-data="menuTree" :field-names="{ title: 'label', key: 'id', children: 'children' }" :checked-keys="checkedKeys" @check="onCheck" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer><div class="legacy-drawer-footer"><a-space><a-button size="large" @click="close">取消</a-button><a-button type="primary" size="large" @click="submit">确定</a-button></a-space></div></template>
    </a-drawer>
  </div>
</template>

<script setup name="SystemPermissionGroup">
import { addGroup, deleteGroups, getGroup, listGroups, listStrategies, updateGroup } from '@/api/system/alignment'
import { treeselect } from '@/api/system/menu'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { decorateLegacyResourceTree } from '@/utils/legacyResourceTree'
const { proxy } = getCurrentInstance()
const rows = ref([]), selected = ref([]), dateRange = ref([]), menuTree = ref([]), checkedKeys = ref([]), strategyOptions = ref([])
const loading = ref(false), total = ref(0), open = ref(false), title = ref(''), formRef = ref()
const query = reactive({ pageNum: 1, pageSize: 20, groupName: undefined })
const form = reactive({ groupId: undefined, groupName: '', strategyIds: [], menuIds: [], remark: '' })
const rules = { groupName: [{ required: true, message: '分组名称不能为空' }] }
const columns = [{ title:'ID',dataIndex:'groupId',width:90,fixed:'left'},{title:'名称',dataIndex:'groupName',width:220},{title:'创建时间',dataIndex:'createTime',key:'time',width:180},{title:'备注',dataIndex:'remark',ellipsis:true,width:300},{title:'操作',key:'operation',width:180,fixed:'right'}]
const rowSelection = computed(() => ({ selectedRowKeys:selected.value,onChange:keys => selected.value=keys }))
function params(){const p={...query};if(dateRange.value?.length)[p.beginTime,p.endTime]=dateRange.value;return p}
async function load(){loading.value=true;try{const res=await listGroups(params());rows.value=res.rows||[];total.value=res.total||0}finally{loading.value=false}}
function search(){query.pageNum=1;load()} function resetQuery(){query.pageNum=1;query.groupName=undefined;dateRange.value=[];load()} function pageChange({page,pageSize}){query.pageNum=page;query.pageSize=pageSize;load()}
async function loadOptions(){const [menus,strategies]=await Promise.all([treeselect(),listStrategies({pageNum:1,pageSize:1000})]);menuTree.value=decorateLegacyResourceTree(menus.data||[]);strategyOptions.value=(strategies.rows||[]).map(x=>({label:`${x.strategyName}[${x.strategyCode}]`,value:x.strategyId}))}
function resetForm(){Object.assign(form,{groupId:undefined,groupName:'',strategyIds:[],menuIds:[],remark:''});checkedKeys.value=[];formRef.value?.clearValidate?.()}
async function create(){resetForm();await loadOptions();title.value='创建';open.value=true}
async function fill(id,asCopy=false){resetForm();await loadOptions();const data=(await getGroup(id)).data||{};Object.assign(form,data);checkedKeys.value=data.menuIds||[];if(asCopy){form.groupId=undefined;form.groupName=`${data.groupName}-复制`};title.value=asCopy?'复制':'修改';open.value=true}
const edit=row=>fill(row?.groupId||selected.value[0]);const copy=row=>fill(row?.groupId||selected.value[0],true)
function onCheck(keys){checkedKeys.value=Array.isArray(keys)?keys:keys.checked||[]}
async function submit(){await formRef.value?.validate();form.menuIds=checkedKeys.value;form.groupId?await updateGroup(form):await addGroup(form);proxy.$modal.msgSuccess('保存成功');close();load()}
function close(){open.value=false;resetForm()}
async function remove(row){const ids=row?.groupId||selected.value;await proxy.$modal.confirm(`确认删除分组 ${ids} 吗？`);await deleteGroups(ids);proxy.$modal.msgSuccess('删除成功');selected.value=[];load()}
load()
</script>
<style scoped>
.legacy-checkbox-list { display: flex; flex-wrap: wrap; gap: 12px 20px; width: 100%; }
.legacy-checkbox-list :deep(.ant-checkbox-wrapper) { margin-inline-start: 0; }
.legacy-drawer-footer { display: flex; justify-content: flex-end; }
.ant-tree { padding: 4px 0; }
</style>
