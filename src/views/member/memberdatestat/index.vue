<template>
  <legacy-page
    title="每日统计列表"
    resource="memberDateStatistics"
    :columns="columns"
    :search-fields="searchFields"
    :advanced-search-fields="advancedSearchFields"
    :initial-query="{ isFake: '0' }"
    :dicts="['sys_enabled']"
    :scroll-x="2200"
  />
</template>

<script setup name="Memberdatestat">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const yesNoOptions = [
  { label: '否', value: '0' },
  { label: '是', value: '1' }
]

const searchFields = [
  {
    label: '统计日期',
    prop: 'statDateRange',
    type: 'daterange',
    showTime: false,
    startProp: 'startDate',
    endProp: 'endDate'
  },
  { label: '用户名', prop: 'username' },
  { label: '是否假人', prop: 'isFake', type: 'select', options: yesNoOptions }
]

const advancedSearchFields = [
  { label: '是否启用', prop: 'isEnabled', type: 'select', dict: 'sys_enabled' },
  { label: '允许邀请', prop: 'allowInvite', type: 'select', options: yesNoOptions },
  { label: '是否冻结', prop: 'isFrozen', type: 'select', options: yesNoOptions }
]

const columns = [
  { title: '统计日期', dataIndex: 'statDate', width: 130 },
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '订单数量', dataIndex: 'orderNumber', width: 120 },
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
  { title: '赠送金额', dataIndex: 'giveAmount', width: 130 }
]
</script>
