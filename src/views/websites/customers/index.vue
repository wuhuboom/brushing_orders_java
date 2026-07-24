<template>
  <LegacyPage
    title="客户管理"
    resource="websiteCustomers"
    :columns="columns"
    :search-fields="searchFields"
    :dialog-fields="dialogFields"
    :toolbar="['add', 'delete']"
    :perms="perms"
    :list-request="listCustomers"
    :get-request="getCustomer"
    :add-request="addCustomer"
    :delete-request="deleteCustomer"
    :scroll-x="1250"
  />
</template>

<script setup name="WebsiteCustomers">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'
import { addCustomer, deleteCustomer, getCustomer, listCustomers } from '@/api/websites/customers'

const perms = { add: 'website:customer:add', remove: 'website:customer:remove' }
const searchFields = [
  { prop: 'firstName', label: '名' },
  { prop: 'lastName', label: '姓' },
  { prop: 'company', label: '公司' }
]
const columns = [
  { title: 'ID', dataIndex: 'id', width: 90 },
  { title: '名', dataIndex: 'firstName', width: 130 },
  { title: '姓', dataIndex: 'lastName', width: 130 },
  { title: '公司', dataIndex: 'company', width: 180 },
  { title: '邮箱', dataIndex: 'email', width: 220 },
  { title: '国家', dataIndex: 'country', width: 140 },
  { title: '内容', dataIndex: 'content', width: 260 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '操作', key: 'operation', fixed: 'right', width: 100 }
]
const dialogFields = [
  { prop: 'firstName', label: '名', required: true },
  { prop: 'lastName', label: '姓', required: true },
  { prop: 'company', label: '公司' },
  { prop: 'email', label: '邮箱', required: true },
  { prop: 'country', label: '国家', required: true },
  { prop: 'content', label: '内容', type: 'textarea', span: 24 }
]
</script>
