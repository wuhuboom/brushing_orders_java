<template>
  <legacy-page
    title="公告管理"
    resource="bulletins"
    :columns="columns"
    :search-fields="searchFields"
    :dialog-fields="dialogFields"
    :toolbar="toolbar"
    :dicts="dicts"
    :perms="perms"
    :scroll-x="1200"
  />
</template>

<script setup name="Bulletin">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const dicts = ['user_yes_no']
const toolbar = ['delete', 'add', 'edit', 'i18n', 'copy']
const perms = {
  add: 'member:bulletin:add',
  edit: 'member:bulletin:edit',
  remove: 'member:bulletin:remove'
}

const searchFields = [
  { label: '标题', prop: 'title' },
  { label: '是否启用', prop: 'isEnabled', type: 'select', dict: 'user_yes_no' }
]

const columns = [
  { title: 'ID', dataIndex: 'id', width: 90 },
  { title: '标题', dataIndex: 'title', width: 240 },
  { title: '序号', dataIndex: 'sortOrder', width: 100 },
  { title: '是否启用', dataIndex: 'isEnabled', dict: 'user_yes_no', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 180 },
  { title: '操作', key: 'operation', width: 180, fixed: 'right' }
]

const dialogFields = [
  { label: '标题', prop: 'title', required: true, span: 24 },
  { label: '序号', prop: 'sortOrder', type: 'number', defaultValue: 0 },
  { label: '是否启用', prop: 'isEnabled', type: 'select', dict: 'user_yes_no', defaultValue: '0' },
  { label: '内容', prop: 'content', type: 'textarea', rows: 4, span: 24 },
  { label: '国际化内容', prop: 'i18nContent', type: 'textarea', rows: 4, span: 24 },
  { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
]
</script>
