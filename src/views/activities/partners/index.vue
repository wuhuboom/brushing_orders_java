<template>
  <LegacyPage
    title="参与记录"
    resource="partners"
    :columns="columns"
    :search-fields="searchFields"
    :toolbar="['show', 'hide']"
    :perms="perms"
    :list-request="listPartners"
    :hidden-request="updatePartnerHidden"
    :initial-query="initialQuery"
    :scroll-x="1800"
  />
</template>

<script setup name="ActivityPartners">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'
import { listActivities, listPartners, updatePartnerHidden } from '@/api/activities/management'

const route = useRoute()
const initialQuery = { username: route.query.username || null }
const yesNo = [{ label: '否', value: '0', elTagType: 'info' }, { label: '是', value: '1', elTagType: 'success' }]
const prizeKinds = [{ label: '礼品', value: '1' }, { label: '现金', value: '2' }, { label: '抽奖次数', value: '3' }]
const activityOptions = reactive([])
const perms = { edit: 'activity:partner:hidden' }
const searchFields = [
  { prop: 'activityId', label: '活动', type: 'select', options: activityOptions },
  { prop: 'username', label: '用户名' },
  { prop: 'phoneNumber', label: '手机号码' },
  { prop: 'isWinning', label: '是否中奖', type: 'select', options: yesNo },
  { prop: 'isHidden', label: '是否隐藏', type: 'select', options: yesNo },
  { prop: 'isVerified', label: '是否授权', type: 'select', options: yesNo }
]
const columns = [
  { title: '活动', dataIndex: 'activityTitle', width: 180 },
  { title: '用户名', dataIndex: 'username', width: 150 },
  { title: '手机号码', dataIndex: 'phoneNumber', width: 150 },
  { title: '是否中奖', dataIndex: 'isWinning', options: yesNo, width: 100 },
  { title: '奖品图片', dataIndex: 'prizeImage', type: 'image', width: 100 },
  { title: '奖品标题', dataIndex: 'prizeTitle', width: 220 },
  { title: '是否隐藏', dataIndex: 'isHidden', options: yesNo, width: 100 },
  { title: '是否授权', dataIndex: 'isVerified', options: yesNo, width: 100 },
  { title: '奖品类型', dataIndex: 'prizeKind', options: prizeKinds, width: 110 },
  { title: '奖品价格', dataIndex: 'prizePrice', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '备注', dataIndex: 'remark', width: 200 }
]

listActivities({ pageNum: 1, pageSize: 1000 }).then((response) => {
  activityOptions.splice(0, activityOptions.length, ...(response.rows || []).map(item => ({ label: item.title, value: item.id })))
})
</script>
