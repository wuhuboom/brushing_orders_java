<template>
  <legacy-page
    title="数据统计"
    resource="websiteStatistics"
    :columns="columns"
    :search-fields="searchFields"
    :scroll-x="1800"
  />
</template>

<script setup name="Websitestat">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const searchFields = [
  { label: '统计日期', prop: 'statDate', placeholder: '请输入统计日期 YYYY-MM-DD' }
]

const columns = [
  { title: '统计日期', dataIndex: 'statDate', width: 130 },
  { title: '订单数量', dataIndex: 'orderNumber', width: 120 },
  { title: '任务完成数量', dataIndex: 'taskCompleteNumber', width: 140 },
  { title: '签到次数', dataIndex: 'signNumber', width: 120 },
  { title: '签到赠送金额', dataIndex: 'signAmount', width: 140 },
  { title: '充值次数', dataIndex: 'rechargeNumber', width: 120 },
  { title: '充值金额', dataIndex: 'rechargeAmount', width: 130 },
  { title: '提现次数', dataIndex: 'withdrawalNumber', width: 120 },
  { title: '提现金额', dataIndex: 'withdrawalAmount', width: 130 },
  { title: '返佣次数', dataIndex: 'commissionNumber', width: 120 },
  { title: '返佣金额', dataIndex: 'commissionAmount', width: 130 },
  { title: '上级返佣次数', dataIndex: 'parentCommissionNumber', width: 140 },
  { title: '上级返佣金额', dataIndex: 'parentCommissionAmount', width: 140 },
  { title: '赠送次数', dataIndex: 'giveNumber', width: 120 },
  { title: '赠送金额', dataIndex: 'giveAmount', width: 130 }
]
</script>
