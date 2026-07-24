<template>
  <LegacyPage
    ref="pageRef"
    title="积分订单"
    resource="giftOrders"
    :columns="columns"
    :search-fields="searchFields"
    :dialog-fields="dialogFields"
    :perms="perms"
    :list-request="listGiftOrders"
    :get-request="getGiftOrder"
    :update-request="updateGiftOrder"
    allow-row-edit
    :scroll-x="1800"
  >
    <template #rowActions="{ record }">
      <a-popconfirm title="确认发货？" @confirm="changeStatus(record, 'ship')">
        <a-button type="link" size="small" :disabled="isOrderActionDisabled(record.status, 'ship')" v-hasPermi="['points:giftOrder:ship']">发货</a-button>
      </a-popconfirm>
      <a-popconfirm title="确认收货？" @confirm="changeStatus(record, 'receive')">
        <a-button type="link" size="small" :disabled="isOrderActionDisabled(record.status, 'receive')" v-hasPermi="['points:giftOrder:receive']">收货</a-button>
      </a-popconfirm>
      <a-popconfirm title="确认取消订单？" @confirm="changeStatus(record, 'cancel')">
        <a-button type="link" danger size="small" :disabled="isOrderActionDisabled(record.status, 'cancel')" v-hasPermi="['points:giftOrder:cancel']">取消</a-button>
      </a-popconfirm>
    </template>
  </LegacyPage>
</template>

<script setup name="PointsGiftOrders">
import LegacyPage from '@/views/member/legacy/LegacyPage.vue'
import { cancelGiftOrder, getGiftOrder, listGiftOrders, receiveGiftOrder, shipGiftOrder, updateGiftOrder } from '@/api/points/management'
import { isOrderActionDisabled } from '@/utils/management-rules'

const { proxy } = getCurrentInstance()
const pageRef = ref()
const statuses = [
  { label: '待处理', value: '1', elTagType: 'warning' },
  { label: '已发货', value: '2', elTagType: 'processing' },
  { label: '已收货', value: '3', elTagType: 'success' },
  { label: '已取消', value: '4', elTagType: 'danger' }
]
const perms = { edit: 'points:giftOrder:update' }
const searchFields = [
  { prop: 'orderNo', label: '订单号' },
  { prop: 'username', label: '用户名' },
  { prop: 'giftTitle', label: '礼品标题' },
  { prop: 'status', label: '状态', type: 'select', options: statuses },
  { label: '积分', type: 'numberRange', minProp: 'minPoints', maxProp: 'maxPoints', min: 0, precision: 2 },
  { prop: 'createTimeRange', label: '创建时间', type: 'daterange', startProp: 'startTime', endProp: 'endTime' }
]
const columns = [
  { title: '订单号', dataIndex: 'orderNo', width: 210 },
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '礼品图片', dataIndex: 'giftImage', type: 'image', width: 100 },
  { title: '礼品标题', dataIndex: 'giftTitle', width: 220 },
  { title: '积分', dataIndex: 'points', width: 100 },
  { title: '状态', dataIndex: 'status', options: statuses, width: 100 },
  { title: '收货人', dataIndex: 'consignee', width: 120 },
  { title: '电话', dataIndex: 'telephone', width: 140 },
  { title: '收货地址', dataIndex: 'deliveryAddress', width: 260 },
  { title: '创建时间', dataIndex: 'createTime', type: 'date', width: 170 },
  { title: '备注', dataIndex: 'remark', width: 200 },
  { title: '操作', key: 'operation', fixed: 'right', width: 290 }
]
const dialogFields = [{ prop: 'remark', label: '备注', type: 'textarea', required: true, span: 24 }]

async function changeStatus(record, action) {
  const requests = { ship: shipGiftOrder, receive: receiveGiftOrder, cancel: cancelGiftOrder }
  try {
    await requests[action](record.id)
    proxy.$modal.msgSuccess('操作成功')
    pageRef.value?.getList()
  } catch (_) {}
}
</script>
