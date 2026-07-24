<template>
  <div class="commerce-page">
  <a-alert class="probability-tip" type="info" show-icon message="启用活动前，该活动启用奖品的中奖概率合计必须大于等于100%。" />
  <LegacyPage
    title="活动管理"
    resource="activities"
    :columns="columns"
    :search-fields="searchFields"
    :dialog-fields="dialogFields"
    :toolbar="['add', 'edit', 'delete']"
    :perms="perms"
    :list-request="listActivities"
    :get-request="getActivity"
    :add-request="addActivity"
    :update-request="updateActivity"
    :delete-request="deleteActivity"
    :scroll-x="1350"
  />
  </div>
</template>

<script setup name="ActivitiesManagement">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'
import { addActivity, deleteActivity, getActivity, listActivities, updateActivity } from '@/api/activities/management'

const yesNo = [{ label: '禁用', value: '0', elTagType: 'danger' }, { label: '启用', value: '1', elTagType: 'success' }]
const perms = { add: 'activity:activity:add', edit: 'activity:activity:edit', remove: 'activity:activity:remove' }
const searchFields = [
  { prop: 'title', label: '标题' },
  { prop: 'isEnabled', label: '是否启用', type: 'select', options: yesNo },
  { prop: 'createTimeRange', label: '创建时间', type: 'daterange', startProp: 'startTime', endProp: 'endTime' }
]
const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '标题', dataIndex: 'title', width: 220 },
  { title: '图片', dataIndex: 'image', type: 'image', width: 100 },
  { title: '是否启用', dataIndex: 'isEnabled', options: yesNo, width: 100 },
  { title: '序号', dataIndex: 'sortOrder', width: 90 },
  { title: '未中奖提示', dataIndex: 'noWinningTips', width: 260 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '操作', key: 'operation', fixed: 'right', width: 160 }
]
const dialogFields = [
  { prop: 'title', label: '标题', required: true },
  { prop: 'subTitle', label: '二级标题' },
  { prop: 'isEnabled', label: '是否启用', type: 'select', options: yesNo, required: true, defaultValue: '0' },
  { prop: 'sortOrder', label: '序号', type: 'number', min: 0, precision: 0, required: true, defaultValue: 0 },
  { prop: 'image', label: '图片', type: 'image', required: true, span: 24 },
  { prop: 'noWinningTips', label: '未中奖提示', type: 'textarea', required: true, span: 24 },
  { prop: 'ruleContent', label: '规则', type: 'editor', required: true, span: 24 },
  { prop: 'description', label: '描述', type: 'editor', required: true, span: 24 },
  { prop: 'remark', label: '备注', type: 'textarea', span: 24 }
]
</script>

<style scoped>.probability-tip { margin: 16px 16px 0; }</style>
