<template>
  <LegacyPage
    title="积分流水"
    resource="flows"
    :columns="columns"
    :search-fields="searchFields"
    :list-request="listPointsFlows"
    :initial-query="initialQuery"
    :scroll-x="1300"
  />
</template>

<script setup name="PointsFlows">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'
import { listPointsFlows } from '@/api/points/management'

const route = useRoute()
const initialQuery = { username: route.query.username || null }
const searchFields = [
  { prop: 'username', label: '用户名' },
  { prop: 'phoneNumber', label: '手机号码' },
  { label: '积分', type: 'numberRange', minProp: 'minPoints', maxProp: 'maxPoints', precision: 2 }
]
const columns = [
  { title: '流水编号', dataIndex: 'flowNo', width: 210 },
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '手机号码', dataIndex: 'phoneNumber', width: 140 },
  { title: '交易前积分', dataIndex: 'beforePoints', width: 120 },
  { title: '积分', dataIndex: 'changePoints', width: 100 },
  { title: '交易后积分', dataIndex: 'afterPoints', width: 120 },
  { title: '交易编号', dataIndex: 'transactionNo', width: 210 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '备注', dataIndex: 'remark', width: 220 }
]
</script>
