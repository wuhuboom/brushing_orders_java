<template>
  <legacy-page
    title="授权记录"
    resource="authRecords"
    :columns="columns"
    :search-fields="searchFields"
    :toolbar="toolbar"
    :dicts="dicts"
    :perms="perms"
    :scroll-x="2200"
  />
</template>

<script setup name="Authrecord">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'

const dicts = ['user_yes_no']
const toolbar = ['delete']
const perms = {
  remove: 'member:authrecord:remove'
}

const searchFields = [
  { label: '用户名', prop: 'username' },
  { label: '手机号码', prop: 'phoneNumber' },
  { label: '上级用户名', prop: 'parentUsername' },
  { label: '钱包地址', prop: 'walletAddress' },
  { label: '是否授权', prop: 'isApproved', type: 'select', dict: 'user_yes_no' }
]

const columns = [
  { title: 'ID', dataIndex: 'id', width: 90 },
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '手机号码', dataIndex: 'phoneNumber', width: 150 },
  { title: '上级用户名', dataIndex: 'parentUsername', width: 150 },
  { title: '余额信息', dataIndex: 'balanceInfo', width: 160 },
  { title: '名称', dataIndex: 'name', width: 140 },
  { title: '用户钱包地址', dataIndex: 'walletAddress', width: 220 },
  { title: '是否授权', dataIndex: 'isApproved', dict: 'user_yes_no', width: 120 },
  { title: '授权金额', dataIndex: 'approvedAmount', width: 130 },
  { title: '账户持有代币数量', dataIndex: 'tokenBalance', width: 170 },
  { title: '收款钱包地址', dataIndex: 'receiveWalletAddress', width: 220 },
  { title: '货币合约地址', dataIndex: 'currencyContractAddress', width: 220 },
  { title: '授权Hash', dataIndex: 'authHash', width: 220 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 180 },
  { title: '操作', key: 'operation', width: 120, fixed: 'right' }
]
</script>
