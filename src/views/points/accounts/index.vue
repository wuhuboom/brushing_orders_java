<template>
  <div class="commerce-page">
  <LegacyPage
    ref="pageRef"
    title="积分账户"
    resource="accounts"
    :columns="columns"
    :search-fields="searchFields"
    :list-request="listPointsAccounts"
    :scroll-x="1100"
  >
    <template #rowActions="{ record }">
      <a-button type="link" size="small" @click="openAdjust(record)" v-hasPermi="['points:account:adjust']">加减积分</a-button>
      <a-button type="link" size="small" @click="viewFlows(record)" v-hasPermi="['points:flow:list']">查看积分流水</a-button>
    </template>
  </LegacyPage>

  <a-modal v-model:open="open" title="加减积分" ok-text="确 定" cancel-text="取 消" @ok="submit">
    <a-form layout="vertical" :model="form">
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="用户名"><a-input :value="current?.username" disabled /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="手机号码"><a-input :value="current?.phoneNumber" disabled /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="可用积分"><a-input-number :value="current?.availablePoints" disabled class="full-width" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="冻结积分"><a-input-number :value="current?.frozenPoints" disabled class="full-width" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="操作类型" required><a-radio-group v-model:value="form.operationType"><a-radio value="subtract">减</a-radio><a-radio value="add">加</a-radio></a-radio-group></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="积分" required><a-input-number v-model:value="form.amount" :min="0.01" :precision="2" class="full-width" /></a-form-item></a-col>
        <a-col :span="24"><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" /></a-form-item></a-col>
      </a-row>
    </a-form>
  </a-modal>
  </div>
</template>

<script setup name="PointsAccounts">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'
import { adjustPoints, listPointsAccounts } from '@/api/points/management'

const { proxy } = getCurrentInstance()
const router = useRouter()
const pageRef = ref()
const open = ref(false)
const current = ref(null)
const form = reactive({ operationType: 'add', amount: 1, remark: '' })
const searchFields = [
  { prop: 'username', label: '用户名' },
  { prop: 'phoneNumber', label: '手机号码' },
  { label: '可用积分', type: 'numberRange', minProp: 'minAvailablePoints', maxProp: 'maxAvailablePoints', min: 0, precision: 2 }
]
const columns = [
  { title: 'ID', dataIndex: 'userId', width: 90 },
  { title: '用户名', dataIndex: 'username', width: 150 },
  { title: '手机号码', dataIndex: 'phoneNumber', width: 150 },
  { title: '可用积分', dataIndex: 'availablePoints', width: 120 },
  { title: '积分', dataIndex: 'points', width: 100 },
  { title: '冻结积分', dataIndex: 'frozenPoints', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '操作', key: 'operation', fixed: 'right', width: 230 }
]

function openAdjust(record) {
  current.value = record
  form.operationType = 'add'
  form.amount = 1
  form.remark = ''
  open.value = true
}

async function submit() {
  if (!form.amount || form.amount <= 0) return proxy.$modal.msgError('积分必须大于0')
  try {
    await adjustPoints(current.value.userId, form)
    proxy.$modal.msgSuccess('操作成功')
    open.value = false
    pageRef.value?.getList()
  } catch (_) {}
}

function viewFlows(record) {
  router.push({ name: 'PointsFlowList', query: { username: record.username } })
}
</script>

<style scoped>.full-width { width: 100%; }</style>
