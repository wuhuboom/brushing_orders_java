<template>
  <legacy-page
    title="IP重复会员"
    resource="duplicateIps"
    :columns="columns"
    :search-fields="searchFields"
    :dicts="dicts"
    :scroll-x="3600"
  />
</template>

<script setup name="Duplicateip">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const dicts = ['user_yes_no']
const searchFields = [
  { label: '关键字', prop: 'keyword', placeholder: '用户名/手机/IP/邀请码' },
  { label: 'IP', prop: 'ip' }
]

const columns = [
  { title: 'ID', dataIndex: 'id', width: 90 },
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '最后登录地址', dataIndex: 'lastLoginAddress', width: 180 },
  { title: '最后登录IP', dataIndex: 'lastLoginIp', width: 150 },
  { title: '手机号码', dataIndex: 'phoneNumber', width: 150 },
  { title: '邮箱', dataIndex: 'email', width: 180 },
  { title: 'VIP等级', dataIndex: 'vipName', width: 130 },
  { title: '上级用户名', dataIndex: 'parentUsername', width: 150 },
  { title: '任务进度', dataIndex: 'taskProgress', width: 120 },
  { title: '签到天数', dataIndex: 'signDays', width: 120 },
  { title: '余额', dataIndex: 'balance', width: 120 },
  { title: '今日提现次数', dataIndex: 'todayWithdrawalCount', width: 140 },
  { title: '直属下级数量', dataIndex: 'directChildrenCount', width: 140 },
  { title: '信誉分', dataIndex: 'reputationScore', width: 110 },
  { title: '今日签到次数', dataIndex: 'todaySignCount', width: 140 },
  { title: '累计签到次数', dataIndex: 'totalSignCount', width: 140 },
  { title: '总余额', dataIndex: 'totalBalance', width: 120 },
  { title: '冻结余额', dataIndex: 'frozenBalance', width: 120 },
  { title: '提现金额', dataIndex: 'withdrawalAmount', width: 130 },
  { title: '充值金额', dataIndex: 'rechargeAmount', width: 130 },
  { title: '今日重置次数', dataIndex: 'todayResetNumber', width: 140 },
  { title: '累计重置次数', dataIndex: 'totalResetNumber', width: 140 },
  { title: '今日佣金', dataIndex: 'todayCommission', width: 130 },
  { title: '今日上级佣金', dataIndex: 'todayParentCommission', width: 140 },
  { title: '是否启用', dataIndex: 'isEnabled', dict: 'user_yes_no', width: 120 },
  { title: '允许邀请', dataIndex: 'allowInvite', dict: 'user_yes_no', width: 120 },
  { title: '是否冻结', dataIndex: 'isFrozen', dict: 'user_yes_no', width: 120 },
  { title: '是否假人', dataIndex: 'isFake', dict: 'user_yes_no', width: 120 },
  { title: '产品匹配', dataIndex: 'productMatching', dict: 'user_yes_no', width: 120 },
  { title: '账户状态', dataIndex: 'accountStatus', width: 120 },
  { title: '交易状态', dataIndex: 'transactionStatus', width: 120 },
  { title: '提现状态', dataIndex: 'withdrawalStatus', width: 120 },
  { title: '协助金提现状态', dataIndex: 'assistWithdrawalStatus', width: 150 },
  { title: '充值后禁止提现', dataIndex: 'depositBlockWithdrawal', dict: 'user_yes_no', width: 150 },
  { title: '上级邀请码', dataIndex: 'parentInviteCode', width: 140 },
  { title: '邀请码', dataIndex: 'inviteCode', width: 130 },
  { title: '性别', dataIndex: 'gender', width: 90 },
  { title: '最后登录时间', dataIndex: 'lastLoginTime', type: 'date', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 180 }
]
</script>
