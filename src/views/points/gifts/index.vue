<template>
  <div class="commerce-page">
  <LegacyPage
    ref="pageRef"
    title="礼品管理"
    resource="gifts"
    :columns="columns"
    :search-fields="searchFields"
    :dialog-fields="dialogFields"
    :toolbar="['add', 'edit', 'copy', 'delete']"
    :perms="perms"
    :list-request="listGifts"
    :get-request="getGift"
    :add-request="addGift"
    :update-request="updateGift"
    :delete-request="deleteGift"
    :scroll-x="1550"
  >
    <template #rowActions="{ record }">
      <a-button type="link" size="small" @click="openAdjust(record, 'stock')" v-hasPermi="['points:gift:adjust']">加减库存</a-button>
      <a-button type="link" size="small" @click="openAdjust(record, 'sales')" v-hasPermi="['points:gift:adjust']">加减销量</a-button>
    </template>
  </LegacyPage>

  <a-modal v-model:open="adjustOpen" :title="adjustKind === 'stock' ? '加减库存' : '加减销量'" ok-text="确 定" cancel-text="取 消" @ok="submitAdjust">
    <a-form layout="vertical" :model="adjustForm">
      <a-form-item label="礼品标题"><a-input :value="currentGift?.title" disabled /></a-form-item>
      <a-form-item label="当前数值">
        <a-input-number :value="adjustKind === 'stock' ? currentGift?.stock : currentGift?.saleVolume" disabled class="full-width" />
      </a-form-item>
      <a-form-item label="操作类型" required>
        <a-radio-group v-model:value="adjustForm.operationType"><a-radio value="add">加</a-radio><a-radio value="subtract">减</a-radio></a-radio-group>
      </a-form-item>
      <a-form-item :label="adjustKind === 'stock' ? '库存数量' : '销量'" required>
        <a-input-number v-model:value="adjustForm.amount" :min="1" :precision="0" class="full-width" />
      </a-form-item>
    </a-form>
  </a-modal>
  </div>
</template>

<script setup name="PointsGifts">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'
import { addGift, adjustGift, deleteGift, getGift, listGifts, updateGift } from '@/api/points/management'
import { signedAmount } from '@/utils/management-rules'

const { proxy } = getCurrentInstance()
const pageRef = ref()
const adjustOpen = ref(false)
const adjustKind = ref('stock')
const currentGift = ref(null)
const adjustForm = reactive({ operationType: 'add', amount: 1 })
const yesNo = [{ label: '禁用', value: '0', elTagType: 'danger' }, { label: '启用', value: '1', elTagType: 'success' }]
const giftKinds = [{ label: '礼品', value: '1' }, { label: '现金', value: '2' }]

const perms = { add: 'points:gift:add', edit: 'points:gift:edit', remove: 'points:gift:remove' }
const searchFields = [
  { prop: 'title', label: '标题' },
  { prop: 'kind', label: '类型', type: 'select', options: giftKinds },
  { prop: 'isEnabled', label: '是否启用', type: 'select', options: yesNo }
]
const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '图片', dataIndex: 'image', type: 'image', width: 90 },
  { title: '标题', dataIndex: 'title', width: 220 },
  { title: '类型', dataIndex: 'kind', options: giftKinds, width: 90 },
  { title: '是否启用', dataIndex: 'isEnabled', options: yesNo, width: 100 },
  { title: '积分', dataIndex: 'points', width: 100 },
  { title: '价格', dataIndex: 'price', width: 100 },
  { title: '库存', dataIndex: 'stock', width: 90 },
  { title: '可用库存', dataIndex: 'availableStock', width: 100 },
  { title: '冻结库存', dataIndex: 'frozenStock', width: 100 },
  { title: '销量', dataIndex: 'saleVolume', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '操作', key: 'operation', fixed: 'right', width: 320 }
]
const dialogFields = [
  { prop: 'title', label: '标题', required: true },
  { prop: 'subTitle', label: '二级标题' },
  { prop: 'kind', label: '类型', type: 'select', options: giftKinds, required: true, defaultValue: '1' },
  { prop: 'isEnabled', label: '是否启用', type: 'select', options: yesNo, required: true, defaultValue: '1' },
  { prop: 'points', label: '积分', type: 'number', min: 0, precision: 2, required: true, defaultValue: 0 },
  { prop: 'price', label: '价格', type: 'number', min: 0, precision: 2, required: true, defaultValue: 0 },
  { prop: 'image', label: '图片', type: 'image', required: true, span: 24 },
  { prop: 'remark', label: '备注', type: 'textarea', span: 24 }
]

function openAdjust(record, kind) {
  currentGift.value = record
  adjustKind.value = kind
  adjustForm.operationType = 'add'
  adjustForm.amount = 1
  adjustOpen.value = true
}

async function submitAdjust() {
  if (!adjustForm.amount || adjustForm.amount <= 0) return proxy.$modal.msgError('数量必须大于0')
  const delta = signedAmount(adjustForm.operationType, adjustForm.amount)
  try {
    await adjustGift(currentGift.value.id, {
      stockDelta: adjustKind.value === 'stock' ? delta : 0,
      salesDelta: adjustKind.value === 'sales' ? delta : 0,
      version: currentGift.value.version
    })
    proxy.$modal.msgSuccess('操作成功')
    adjustOpen.value = false
    pageRef.value?.getList()
  } catch (_) {}
}
</script>

<style scoped>.full-width { width: 100%; }</style>
