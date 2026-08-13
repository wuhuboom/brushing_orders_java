<template>
  <legacy-page
    title="IP重复会员列表"
    resource="duplicateIps"
    :columns="columns"
    :search-fields="searchFields"
    :advanced-search-fields="advancedSearchFields"
    :dicts="dicts"
    :scroll-x="5100"
  >
    <template #cell-lastLoginAddress="{ record }">
      {{ joinedLoginAddress(record) }}
    </template>
    <template #cell-parentInviteCode="{ record }">
      <span>{{ record.parentInviteCode || '-' }}</span>
      <a-button
        v-if="record.parentInviteCode"
        type="text"
        size="small"
        class="copy-button"
        aria-label="复制"
        @click="copyText(record.parentInviteCode)"
      >
        <CopyOutlined />
      </a-button>
    </template>
    <template #cell-inviteCode="{ record }">
      <span>{{ record.inviteCode || '-' }}</span>
      <a-button
        v-if="record.inviteCode"
        type="text"
        size="small"
        class="copy-button"
        aria-label="复制"
        @click="copyText(record.inviteCode)"
      >
        <CopyOutlined />
      </a-button>
    </template>
  </legacy-page>
</template>

<script setup name="Duplicateip">
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { CopyOutlined } from '@ant-design/icons-vue'
import { getLevel } from '@/api/member/orderuser'
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const { proxy } = getCurrentInstance()
const levelOptions = ref([])
const dicts = ['sys_enabled', 'sys_user_sex']
const yesNoOptions = [
  { label: '否', value: '0' },
  { label: '是', value: '1' }
]

const searchFields = [
  { label: '关键字', prop: 'keyword', placeholder: '用户名/手机号码/IP/邀请码', lg: 7 },
  { label: 'ID', prop: 'id', type: 'number', min: 1, precision: 0, lg: 7 },
  { label: '最后登录地址', prop: 'lastLoginAddress', lg: 6 }
]

const advancedSearchFields = computed(() => [
  { label: 'VIP等级', prop: 'vipId', type: 'select', options: levelOptions.value },
  { label: '上级用户名', prop: 'parentUsername' },
  {
    label: '用户名列表',
    prop: 'usernameList',
    tooltip: '多个用户名请使用英文逗号分隔'
  },
  {
    label: '余额',
    type: 'numberRange',
    minProp: 'balanceMin',
    maxProp: 'balanceMax',
    precision: 2
  },
  {
    label: '信誉分',
    type: 'numberRange',
    minProp: 'reputationMin',
    maxProp: 'reputationMax',
    precision: 0
  },
  { label: '是否启用', prop: 'isEnabled', type: 'select', dict: 'sys_enabled' },
  { label: '允许邀请', prop: 'allowInvite', type: 'select', options: yesNoOptions },
  { label: '是否冻结', prop: 'isFrozen', type: 'select', options: yesNoOptions },
  { label: '是否假人', prop: 'isFake', type: 'select', options: yesNoOptions },
  { label: '产品匹配', prop: 'productMatching', type: 'select', dict: 'sys_enabled' },
  { label: '账户状态', prop: 'accountStatus', type: 'select', dict: 'sys_enabled' },
  { label: '交易状态', prop: 'transactionStatus', type: 'select', dict: 'sys_enabled' },
  { label: '提现状态', prop: 'withdrawalStatus', type: 'select', dict: 'sys_enabled' },
  {
    label: '协助金提现状态',
    prop: 'assistWithdrawalStatus',
    type: 'select',
    dict: 'sys_enabled'
  },
  {
    label: '充值后禁止提现',
    prop: 'depositBlockWithdrawal',
    type: 'select',
    options: yesNoOptions
  },
  { label: '上级邀请码', prop: 'parentInviteCode' },
  { label: '性别', prop: 'gender', type: 'select', dict: 'sys_user_sex' },
  {
    label: '创建时间',
    prop: 'createTimeRange',
    type: 'daterange',
    showTime: false,
    startProp: 'beginCreateTime',
    endProp: 'endCreateTime'
  }
])

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 90 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 140 },
  { title: '最后登录地址', dataIndex: 'lastLoginAddress', key: 'lastLoginAddress', width: 300 },
  { title: '手机号码', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 150 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 180 },
  { title: 'VIP等级', dataIndex: 'vipName', key: 'vipName', width: 120 },
  { title: '上级用户名', dataIndex: 'parentUsername', key: 'parentUsername', width: 150 },
  { title: '任务进度', dataIndex: 'taskProgress', key: 'taskProgress', width: 120 },
  { title: '签到天数', dataIndex: 'signDays', key: 'signDays', width: 110 },
  { title: '余额', dataIndex: 'balance', key: 'balance', width: 120 },
  { title: '今日提现次数', dataIndex: 'todayWithdrawalCount', key: 'todayWithdrawalCount', width: 140 },
  { title: '直属下级数量', dataIndex: 'directChildrenCount', key: 'directChildrenCount', width: 140 },
  { title: '信誉分', dataIndex: 'reputationScore', key: 'reputationScore', width: 110 },
  { title: '今日签到次数', dataIndex: 'todaySignCount', key: 'todaySignCount', width: 140 },
  { title: '累计签到次数', dataIndex: 'totalSignCount', key: 'totalSignCount', width: 140 },
  { title: '总余额', dataIndex: 'totalBalance', key: 'totalBalance', width: 120 },
  { title: '冻结余额', dataIndex: 'frozenBalance', key: 'frozenBalance', width: 120 },
  { title: '提现金额', dataIndex: 'withdrawalAmount', key: 'withdrawalAmount', width: 130 },
  { title: '充值金额', dataIndex: 'rechargeAmount', key: 'rechargeAmount', width: 130 },
  { title: '今日重置次数', dataIndex: 'todayResetNumber', key: 'todayResetNumber', width: 140 },
  { title: '累计重置次数', dataIndex: 'totalResetNumber', key: 'totalResetNumber', width: 140 },
  { title: '今日佣金', dataIndex: 'todayCommission', key: 'todayCommission', width: 130 },
  { title: '今日上级佣金', dataIndex: 'todayParentCommission', key: 'todayParentCommission', width: 140 },
  { title: '是否启用', dataIndex: 'isEnabled', key: 'isEnabled', dict: 'sys_enabled', width: 120 },
  { title: '允许邀请', dataIndex: 'allowInvite', key: 'allowInvite', options: yesNoOptions, width: 120 },
  { title: '是否冻结', dataIndex: 'isFrozen', key: 'isFrozen', options: yesNoOptions, width: 120 },
  { title: '是否假人', dataIndex: 'isFake', key: 'isFake', options: yesNoOptions, width: 120 },
  { title: '产品匹配', dataIndex: 'productMatching', key: 'productMatching', dict: 'sys_enabled', width: 120 },
  { title: '账户状态', dataIndex: 'accountStatus', key: 'accountStatus', dict: 'sys_enabled', width: 120 },
  { title: '交易状态', dataIndex: 'transactionStatus', key: 'transactionStatus', dict: 'sys_enabled', width: 120 },
  { title: '提现状态', dataIndex: 'withdrawalStatus', key: 'withdrawalStatus', dict: 'sys_enabled', width: 120 },
  {
    title: '协助金提现状态',
    dataIndex: 'assistWithdrawalStatus',
    key: 'assistWithdrawalStatus',
    dict: 'sys_enabled',
    width: 150
  },
  {
    title: '充值后禁止提现',
    dataIndex: 'depositBlockWithdrawal',
    key: 'depositBlockWithdrawal',
    options: yesNoOptions,
    width: 150
  },
  { title: '上级邀请码', dataIndex: 'parentInviteCode', key: 'parentInviteCode', width: 140 },
  { title: '邀请码', dataIndex: 'inviteCode', key: 'inviteCode', width: 130 },
  { title: '性别', dataIndex: 'gender', key: 'gender', dict: 'sys_user_sex', width: 90 },
  { title: '最后登录时间', dataIndex: 'lastLoginTime', key: 'lastLoginTime', type: 'date', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', type: 'date', width: 180 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 180 }
]

function joinedLoginAddress(record) {
  const parts = [record.lastLoginIp, record.lastLoginAddress].filter(Boolean)
  return parts.length ? parts.join(' ') : '-'
}

async function copyText(value) {
  try {
    await navigator.clipboard.writeText(value || '')
  } catch {
    proxy.$modal.msgError('复制失败')
  }
}

onMounted(async () => {
  const response = await getLevel()
  levelOptions.value = (response.data || []).map((level) => ({
    label: level.name,
    value: level.id
  }))
})
</script>

<style scoped>
.copy-button {
  width: 24px;
  height: 24px;
  margin-left: 2px;
  padding: 0;
  color: #1677ff;
}
</style>
