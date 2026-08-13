<template>
  <legacy-page
    title="业绩统计列表"
    resource="performanceStatistics"
    :columns="columns"
    :search-fields="searchFields"
    :toolbar="toolbar"
    :perms="perms"
    export-path="member/legacy/performanceStatistics/export"
    :scroll-x="1800"
  />
</template>

<script setup name="Performancestat">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const toolbar = ['export']
const perms = {
  export: 'member:performance:export'
}

const searchFields = [
  {
    label: '统计日期',
    prop: 'statDateRange',
    type: 'daterange',
    showTime: false,
    startProp: 'startDate',
    endProp: 'endDate'
  },
  { label: '用户名', prop: 'username' }
]

const columns = [
  { title: '统计日期', dataIndex: 'statDate', width: 130 },
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '注册会员数量', dataIndex: 'registerMemberNumber', width: 140 },
  { title: '订单数量', dataIndex: 'orderNumber', width: 120 },
  { title: '投注人数', dataIndex: 'bettorNumber', width: 120 },
  { title: '任务完成数量', dataIndex: 'taskCompleteNumber', width: 140 },
  { title: '重置次数', dataIndex: 'resetNumber', width: 120 },
  { title: '充值金额', dataIndex: 'rechargeAmount', width: 130 },
  { title: '提现金额', dataIndex: 'withdrawalAmount', width: 130 },
  { title: '返佣金额', dataIndex: 'commissionAmount', width: 130 },
  { title: '上级返佣金额', dataIndex: 'parentCommissionAmount', width: 140 },
  { title: '签到赠送金额', dataIndex: 'signAmount', width: 140 },
  { title: '赠送金额', dataIndex: 'giveAmount', width: 130 }
]
</script>
