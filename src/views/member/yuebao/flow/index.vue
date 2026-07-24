<template>
  <legacy-page
    title="余额宝交易流水"
    resource="yuebaoFlows"
    :columns="columns"
    :search-fields="searchFields"
    :toolbar="toolbar"
    :dicts="dicts"
    :perms="perms"
    :scroll-x="1600"
  />
</template>

<script setup name="Yuebaoflow">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const dicts = ['user_yes_no', 'transaction_type']
const toolbar = ['show', 'hide']
const perms = {
  edit: 'member:yuebaoFlow:edit'
}

const searchFields = [
  { label: '流水编号', prop: 'serialCode' },
  { label: '用户名', prop: 'username' },
  { label: '交易类型', prop: 'transactionType', type: 'select', dict: 'transaction_type' },
  { label: '是否隐藏', prop: 'isHidden', type: 'select', dict: 'user_yes_no' }
]

const columns = [
  { title: '流水编号', dataIndex: 'serialCode', width: 180 },
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '交易类型', dataIndex: 'transactionType', dict: 'transaction_type', width: 140 },
  { title: '交易前余额', dataIndex: 'balanceBefore', width: 140 },
  { title: '金额', dataIndex: 'transactionAmount', width: 130 },
  { title: '交易后余额', dataIndex: 'balanceAfter', width: 140 },
  { title: '是否隐藏', dataIndex: 'isHidden', dict: 'user_yes_no', width: 120 },
  { title: '交易编号', dataIndex: 'transactionCode', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 180 }
]
</script>
