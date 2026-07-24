<template>
  <legacy-page
    title="会员统计"
    resource="memberStatistics"
    :columns="columns"
    :search-fields="searchFields"
    :scroll-x="2400"
  />
</template>

<script setup name="Memberstat">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const searchFields = [
  { label: '用户名', prop: 'username' },
  { label: '手机号码', prop: 'phoneNumber' }
]

const columns = [
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '单数', dataIndex: 'orderNumber', width: 100 },
  { title: '签到天数', dataIndex: 'signDays', width: 110 },
  { title: '最后签到日期', dataIndex: 'lastSignDate', width: 140 },
  { title: '订单数量', dataIndex: 'totalOrderNumber', width: 120 },
  { title: '任务完成数量', dataIndex: 'taskCompleteNumber', width: 140 },
  { title: '重置次数', dataIndex: 'resetNumber', width: 120 },
  { title: '充值次数', dataIndex: 'rechargeNumber', width: 120 },
  { title: '充值金额', dataIndex: 'rechargeAmount', width: 130 },
  { title: '提现次数', dataIndex: 'withdrawalNumber', width: 120 },
  { title: '提现金额', dataIndex: 'withdrawalAmount', width: 130 },
  { title: '返佣次数', dataIndex: 'commissionNumber', width: 120 },
  { title: '返佣金额', dataIndex: 'commissionAmount', width: 130 },
  { title: '上级返佣次数', dataIndex: 'parentCommissionNumber', width: 140 },
  { title: '上级返佣金额', dataIndex: 'parentCommissionAmount', width: 140 },
  { title: '签到次数', dataIndex: 'signNumber', width: 120 },
  { title: '签到赠送金额', dataIndex: 'signAmount', width: 140 },
  { title: '赠送次数', dataIndex: 'giveNumber', width: 120 },
  { title: '赠送金额', dataIndex: 'giveAmount', width: 130 },
  { title: '弹窗次数', dataIndex: 'popupNumber', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 180 }
]
</script>
