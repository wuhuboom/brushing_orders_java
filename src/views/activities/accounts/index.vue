<template>
  <div class="commerce-page">
  <LegacyPage
    ref="pageRef"
    title="活动账户"
    resource="activityAccounts"
    :columns="columns"
    :search-fields="searchFields"
    :list-request="listActivityAccounts"
    :scroll-x="1050"
  >
    <template #rowActions="{ record }">
      <a-button type="link" size="small" @click="openAdjust(record)" v-hasPermi="['activity:account:adjust']">加减次数</a-button>
      <a-button type="link" size="small" @click="openPrizes(record)" v-hasPermi="['activity:account:prize']">奖品设置</a-button>
      <a-button type="link" size="small" @click="viewPartners(record)" v-hasPermi="['activity:partner:list']">参与记录</a-button>
    </template>
  </LegacyPage>

  <a-modal v-model:open="adjustOpen" title="加减次数" ok-text="确 定" cancel-text="取 消" @ok="submitAdjust">
    <a-form layout="vertical" :model="adjustForm">
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="用户名"><a-input :value="current?.username" disabled /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="手机号码"><a-input :value="current?.phoneNumber" disabled /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="可用抽奖次数"><a-input-number :value="current?.availableTimes" disabled class="full-width" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="已用抽奖次数"><a-input-number :value="current?.usedTimes" disabled class="full-width" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="操作类型" required><a-radio-group v-model:value="adjustForm.operationType"><a-radio value="subtract">减</a-radio><a-radio value="add">加</a-radio></a-radio-group></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="抽奖次数" required><a-input-number v-model:value="adjustForm.amount" :min="1" :precision="0" class="full-width" /></a-form-item></a-col>
        <a-col :span="24"><a-form-item label="备注"><a-textarea v-model:value="adjustForm.remark" /></a-form-item></a-col>
      </a-row>
    </a-form>
  </a-modal>

  <a-modal v-model:open="prizeOpen" title="奖品设置" width="1180px" :footer="null" destroy-on-close>
    <LegacyPage
      v-if="prizeOpen"
      title="奖品设置"
      resource="accountPrizes"
      :columns="prizeColumns"
      :search-fields="prizeSearchFields"
      :dialog-fields="prizeDialogFields"
      :toolbar="['add', 'edit', 'delete']"
      :perms="prizePerms"
      :list-request="listUserPrizes"
      :get-request="getAccountPrize"
      :add-request="addUserPrize"
      :update-request="updateUserPrize"
      :delete-request="deleteAccountPrize"
      :scroll-x="1200"
    />
  </a-modal>
  </div>
</template>

<script setup name="ActivityAccounts">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'
import {
  addAccountPrize, adjustActivityTimes, deleteAccountPrize, getAccountPrize, listAccountPrizes,
  listActivities, listActivityAccounts, listPrizes, updateAccountPrize
} from '@/api/activities/management'

const { proxy } = getCurrentInstance()
const router = useRouter()
const pageRef = ref()
const current = ref(null)
const adjustOpen = ref(false)
const prizeOpen = ref(false)
const adjustForm = reactive({ operationType: 'add', amount: 1, remark: '' })
const yesNo = [{ label: '否', value: '0', elTagType: 'info' }, { label: '是', value: '1', elTagType: 'success' }]
const prizeKinds = [{ label: '礼品', value: '1' }, { label: '现金', value: '2' }, { label: '抽奖次数', value: '3' }]
const activityOptions = reactive([])
const prizeOptions = reactive([])
const prizePerms = { add: 'activity:account:prize', edit: 'activity:account:prize', remove: 'activity:account:prize' }

const searchFields = [
  { prop: 'username', label: '用户名' },
  { prop: 'phoneNumber', label: '手机号码' },
  { label: '可用抽奖次数', type: 'numberRange', minProp: 'minAvailableTimes', maxProp: 'maxAvailableTimes', min: 0, precision: 0 }
]
const columns = [
  { title: 'ID', dataIndex: 'userId', width: 90 },
  { title: '用户名', dataIndex: 'username', width: 160 },
  { title: '手机号码', dataIndex: 'phoneNumber', width: 150 },
  { title: '可用抽奖次数', dataIndex: 'availableTimes', width: 140 },
  { title: '已用抽奖次数', dataIndex: 'usedTimes', width: 140 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '操作', key: 'operation', fixed: 'right', width: 280 }
]
const prizeSearchFields = [
  { prop: 'activityId', label: '活动', type: 'select', options: activityOptions },
  { prop: 'sortOrder', label: '序号', type: 'number', min: 0, precision: 0 },
  { prop: 'isWinning', label: '是否中奖', type: 'select', options: yesNo }
]
const prizeColumns = [
  { title: '活动', dataIndex: 'activityTitle', width: 180 },
  { title: '序号', dataIndex: 'sortOrder', width: 80 },
  { title: '是否中奖', dataIndex: 'isWinning', options: yesNo, width: 100 },
  { title: '奖品标题', dataIndex: 'prizeTitle', width: 200 },
  { title: '奖品图片', dataIndex: 'prizeImage', type: 'image', width: 100 },
  { title: '奖品类型', dataIndex: 'prizeKind', options: prizeKinds, width: 110 },
  { title: '奖品价格', dataIndex: 'prizePrice', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  { title: '操作', key: 'operation', fixed: 'right', width: 150 }
]
const prizeDialogFields = [
  { prop: 'activityId', label: '活动', type: 'select', options: activityOptions, required: true },
  { prop: 'prizeId', label: '奖品', type: 'select', options: prizeOptions, required: true },
  { prop: 'sortOrder', label: '序号', type: 'number', min: 0, precision: 0, required: true, defaultValue: 0 },
  { prop: 'isWinning', label: '是否中奖', type: 'select', options: yesNo, required: true, defaultValue: '0' },
  { prop: 'remark', label: '备注', type: 'textarea', span: 24 }
]

const listUserPrizes = (params) => listAccountPrizes(current.value.userId, params)
const addUserPrize = (data) => addAccountPrize(current.value.userId, data)
const updateUserPrize = (data) => updateAccountPrize(current.value.userId, data)

function openAdjust(record) {
  current.value = record
  Object.assign(adjustForm, { operationType: 'add', amount: 1, remark: '' })
  adjustOpen.value = true
}
function openPrizes(record) { current.value = record; prizeOpen.value = true }
function viewPartners(record) { router.push({ name: 'ActivityPartnerList', query: { username: record.username } }) }

async function submitAdjust() {
  if (!adjustForm.amount || adjustForm.amount <= 0) return proxy.$modal.msgError('次数必须大于0')
  try {
    await adjustActivityTimes(current.value.userId, adjustForm)
    proxy.$modal.msgSuccess('操作成功')
    adjustOpen.value = false
    pageRef.value?.getList()
  } catch (_) {}
}

Promise.all([
  listActivities({ pageNum: 1, pageSize: 1000 }),
  listPrizes({ pageNum: 1, pageSize: 1000 })
]).then(([activities, prizes]) => {
  activityOptions.splice(0, activityOptions.length, ...(activities.rows || []).map(item => ({ label: item.title, value: item.id })))
  prizeOptions.splice(0, prizeOptions.length, ...(prizes.rows || []).map(item => ({ label: `${item.activityTitle} / ${item.title}`, value: item.id })))
})
</script>

<style scoped>.full-width { width: 100%; }</style>
