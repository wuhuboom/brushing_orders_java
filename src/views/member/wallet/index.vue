<template>
  <legacy-page
    title="钱包管理"
    resource="wallets"
    :columns="columns"
    :search-fields="searchFields"
    :dialog-fields="dialogFields"
    :toolbar="toolbar"
    :dicts="dicts"
    :perms="perms"
    :scroll-x="1300"
  />
</template>

<script setup name="Wallet">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const dicts = ['user_yes_no']
const toolbar = ['delete', 'add']
const perms = {
  add: 'member:wallet:add',
  edit: 'member:wallet:edit',
  remove: 'member:wallet:remove'
}

const searchFields = [
  { label: '名称', prop: 'name' },
  { label: '是否启用', prop: 'isEnabled', type: 'select', dict: 'user_yes_no' }
]

const columns = [
  { title: '名称', dataIndex: 'name', width: 180 },
  { title: '是否启用', dataIndex: 'isEnabled', dict: 'user_yes_no', width: 120 },
  { title: '序号', dataIndex: 'sortOrder', width: 100 },
  { title: '图片', dataIndex: 'image', type: 'image', width: 110 },
  { title: '链接', dataIndex: 'link', width: 240 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  { title: '操作', key: 'operation', width: 160, fixed: 'right' }
]

const dialogFields = [
  { label: '名称', prop: 'name', required: true },
  { label: '是否启用', prop: 'isEnabled', type: 'select', dict: 'user_yes_no', defaultValue: '0' },
  { label: '序号', prop: 'sortOrder', type: 'number', defaultValue: 0 },
  { label: '链接', prop: 'link', span: 24 },
  { label: '图片', prop: 'image', type: 'image', span: 24 },
  { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
]
</script>
