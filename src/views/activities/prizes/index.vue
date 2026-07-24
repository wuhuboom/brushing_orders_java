<template>
  <a-alert class="probability-tip" type="warning" show-icon message="同一活动启用奖品的中奖概率合计需达到100%，单个奖品概率范围为0–100。" />
  <LegacyPage
    title="奖品管理"
    resource="prizes"
    :columns="columns"
    :search-fields="searchFields"
    :dialog-fields="dialogFields"
    :toolbar="['add', 'edit', 'delete']"
    :perms="perms"
    :list-request="listPrizes"
    :get-request="getPrize"
    :add-request="addPrize"
    :update-request="updatePrize"
    :delete-request="deletePrize"
    :scroll-x="1500"
  />
</template>

<script setup name="ActivityPrizes">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'
import { addPrize, deletePrize, getPrize, listActivities, listPrizes, updatePrize } from '@/api/activities/management'

const yesNo = [{ label: '禁用', value: '0', elTagType: 'danger' }, { label: '启用', value: '1', elTagType: 'success' }]
const prizeKinds = [{ label: '礼品', value: '1' }, { label: '现金', value: '2' }, { label: '抽奖次数', value: '3' }]
const activityOptions = reactive([])
const perms = { add: 'activity:prize:add', edit: 'activity:prize:edit', remove: 'activity:prize:remove' }
const searchFields = [
  { prop: 'activityId', label: '活动', type: 'select', options: activityOptions },
  { prop: 'title', label: '标题' },
  { prop: 'kind', label: '类型', type: 'select', options: prizeKinds }
]
const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '活动', dataIndex: 'activityTitle', width: 180 },
  { title: '图片', dataIndex: 'image', type: 'image', width: 100 },
  { title: '标题', dataIndex: 'title', width: 220 },
  { title: '类型', dataIndex: 'kind', options: prizeKinds, width: 100 },
  { title: '价格', dataIndex: 'price', width: 100 },
  { title: '中奖概率（%）', dataIndex: 'probability', width: 140 },
  { title: '是否启用', dataIndex: 'isEnabled', options: yesNo, width: 100 },
  { title: '序号', dataIndex: 'sortOrder', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  { title: '操作', key: 'operation', fixed: 'right', width: 160 }
]
const dialogFields = [
  { prop: 'activityId', label: '活动', type: 'select', options: activityOptions, required: true, span: 24 },
  { prop: 'probability', label: '中奖概率（%）', type: 'number', min: 0, precision: 4, required: true, defaultValue: 0 },
  { prop: 'title', label: '标题', required: true },
  { prop: 'subTitle', label: '二级标题' },
  { prop: 'kind', label: '类型', type: 'select', options: prizeKinds, required: true, defaultValue: '1' },
  { prop: 'price', label: '价格', type: 'number', min: 0, precision: 2, required: true, defaultValue: 0 },
  { prop: 'isEnabled', label: '是否启用', type: 'select', options: yesNo, required: true, defaultValue: '1' },
  { prop: 'sortOrder', label: '序号', type: 'number', min: 0, precision: 0, required: true, defaultValue: 0 },
  { prop: 'image', label: '图片', type: 'image', required: true, span: 24 },
  { prop: 'remark', label: '备注', type: 'textarea', span: 24 }
]

listActivities({ pageNum: 1, pageSize: 1000 }).then((response) => {
  activityOptions.splice(0, activityOptions.length, ...(response.rows || []).map(item => ({ label: item.title, value: item.id })))
})
</script>

<style scoped>.probability-tip { margin: 16px 16px 0; }</style>
