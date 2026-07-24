<template>
  <legacy-page
    title="人才招聘"
    resource="recruitments"
    :columns="columns"
    :search-fields="searchFields"
    :dialog-fields="dialogFields"
    :toolbar="toolbar"
    :perms="perms"
    :scroll-x="1200"
  />
</template>

<script setup name="Recruitment">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const toolbar = ['delete', 'add']
const perms = {
  add: 'member:recruitment:add',
  edit: 'member:recruitment:edit',
  remove: 'member:recruitment:remove'
}

const searchFields = [
  { label: '名称', prop: 'name' }
]

const columns = [
  { title: '名称', dataIndex: 'name', width: 180 },
  { title: '序号', dataIndex: 'sortOrder', width: 100 },
  { title: '图标', dataIndex: 'icon', type: 'image', width: 110 },
  { title: '数量', dataIndex: 'quantity', width: 120 },
  { title: '薪资', dataIndex: 'salary', width: 160 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  { title: '操作', key: 'operation', width: 160, fixed: 'right' }
]

const dialogFields = [
  { label: '名称', prop: 'name', required: true },
  { label: '序号', prop: 'sortOrder', type: 'number', defaultValue: 0 },
  { label: '数量', prop: 'quantity', type: 'number', defaultValue: 0 },
  { label: '薪资', prop: 'salary' },
  { label: '图标', prop: 'icon', type: 'image', span: 24 },
  { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
]
</script>
