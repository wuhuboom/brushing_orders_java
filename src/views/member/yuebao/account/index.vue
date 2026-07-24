<template>
  <legacy-page
    title="余额宝账户"
    resource="yuebaoAccounts"
    :columns="columns"
    :search-fields="searchFields"
    :dialog-fields="dialogFields"
    :dicts="dicts"
    :perms="perms"
    :scroll-x="1700"
  />
</template>

<script setup name="Yuebaoaccount">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const dicts = ['user_yes_no']
const perms = {
  edit: 'member:yuebaoAccount:edit'
}

const searchFields = [
  { label: '用户名', prop: 'username' },
  { label: '手机号码', prop: 'phoneNumber' },
  { label: '是否启用', prop: 'isEnabled', type: 'select', dict: 'user_yes_no' }
]

const columns = [
  { title: 'ID', dataIndex: 'id', width: 90 },
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '手机号码', dataIndex: 'phoneNumber', width: 150 },
  { title: '余额', dataIndex: 'balance', width: 120 },
  { title: '总余额', dataIndex: 'totalBalance', width: 120 },
  { title: '冻结余额', dataIndex: 'frozenBalance', width: 120 },
  { title: '是否启用', dataIndex: 'isEnabled', dict: 'user_yes_no', width: 120 },
  { title: '是否启用加息', dataIndex: 'isInterestEnabled', dict: 'user_yes_no', width: 140 },
  { title: '加息日利率(‱)', dataIndex: 'interestDailyRate', width: 150 },
  { title: '开始日期', dataIndex: 'startDate', width: 130 },
  { title: '结束日期', dataIndex: 'endDate', width: 130 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 180 },
  { title: '操作', key: 'operation', width: 100, fixed: 'right' }
]

const dialogFields = [
  { label: '用户ID', prop: 'userId', required: true },
  { label: '余额', prop: 'balance', type: 'number', precision: 2, defaultValue: 0 },
  { label: '总余额', prop: 'totalBalance', type: 'number', precision: 2, defaultValue: 0 },
  { label: '冻结余额', prop: 'frozenBalance', type: 'number', precision: 2, defaultValue: 0 },
  { label: '是否启用', prop: 'isEnabled', type: 'select', dict: 'user_yes_no', defaultValue: '0' },
  { label: '是否启用加息', prop: 'isInterestEnabled', type: 'select', dict: 'user_yes_no', defaultValue: '0' },
  { label: '加息日利率(‱)', prop: 'interestDailyRate', type: 'number', precision: 4, defaultValue: 0 },
  { label: '开始日期', prop: 'startDate', type: 'date' },
  { label: '结束日期', prop: 'endDate', type: 'date' },
  { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
]
</script>
