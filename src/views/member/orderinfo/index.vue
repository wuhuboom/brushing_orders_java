<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="订单明细列表"
      :columns="orderinfoColumns"
      :data-source="orderinfoList"
      :loading="loading"
      row-key="id"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 2570 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.username"
                  placeholder="请输入"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="createdDateRange"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  show-time
                  class="full-width"
                  :placeholder="['请选择', '请选择']"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="明细编号">
                <a-input
                  v-model:value="queryParams.orderNumber"
                  placeholder="请输入"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col flex="auto" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
                <a-button type="link" @click="advancedSearchVisible = !advancedSearchVisible">
                  {{ advancedSearchVisible ? "收起" : "展开" }}
                </a-button>
              </a-space>
            </a-col>
          </a-row>

          <a-row v-if="advancedSearchVisible" :gutter="[24, 16]" class="advanced-query-row">
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="类型">
                <a-select v-model:value="queryParams.type" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in order_type" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="单数">
                <a-input-number
                  v-model:value="queryParams.orderCount"
                  placeholder="请输入"
                  :min="0"
                  :precision="0"
                  class="full-width"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="金额">
                <a-space-compact block>
                  <a-input-number
                    v-model:value="queryParams.amountMin"
                    placeholder="请输入"
                    :precision="2"
                    class="amount-range-input"
                  />
                  <a-input disabled value="~" class="amount-range-separator" />
                  <a-input-number
                    v-model:value="queryParams.amountMax"
                    placeholder="请输入"
                    :precision="2"
                    class="amount-range-input"
                  />
                </a-space-compact>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="状态">
                <a-select v-model:value="queryParams.status" allow-clear placeholder="请选择">
                  <a-select-option
                    v-for="dict in orderStatusOptions"
                    :key="dict.value"
                    :value="dict.value"
                  >
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="过期时间">
                <a-date-picker
                  v-model:value="queryParams.expiryDate"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择"
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="商品标题">
                <a-input
                  v-model:value="queryParams.productTitle"
                  placeholder="请输入"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="额外佣金">
                <a-input-number
                  v-model:value="queryParams.extraCommission"
                  placeholder="请输入"
                  :precision="2"
                  class="full-width"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'type'">
          <dict-tag :options="order_type" :value="record.type" />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <dict-tag :options="orderStatusOptions" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'rebatePercentage'">
          {{ formatPercentage(record.rebatePercentage) }}
        </template>
        <template v-else-if="column.key === 'upperRebatePercentage'">
          {{ formatPercentage(record.upperRebatePercentage) }}
        </template>
        <template v-else-if="column.dataIndex === 'expiryTime'">
          {{ formatDateTime(record.expiryTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'productImage'">
          <image-preview
            v-if="record.productImage"
            :src="record.productImage"
            :width="50"
            :height="50"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'extraCommission'">
          {{ displayValue(record.extraCommissionAmount, 0) }}
        </template>
        <template v-else-if="column.key === 'remarks'">
          {{ displayValue(record.remarks) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="4">
            <a-button
              type="link"
              size="small"
              @click="handleUpdate(record)"
              v-hasPermi="['member:orderinfo:edit']"
            >修改</a-button>
            <a-button type="link" size="small" @click="openCommentDialog(record)">评论</a-button>
            <a-popconfirm
              title="取消选中的记录？"
              ok-text="确 定"
              cancel-text="取 消"
              :disabled="String(record.status) !== '1'"
              @confirm="handleCancelOrder(record)"
            >
              <a-button
                type="link"
                size="small"
                :disabled="String(record.status) !== '1'"
                v-hasPermi="['member:orderinfo:edit']"
              >取消</a-button>
            </a-popconfirm>
          </a-space>
        </template>
        <template v-else>
          {{ displayValue(record[column.dataIndex]) }}
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      v-model:open="editOpen"
      title="修改"
      width="520px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="submitting"
      @ok="submitForm"
      @cancel="closeEditDialog"
    >
      <a-form ref="orderinfoRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="过期时间" name="expiryTime">
          <a-date-picker
            v-model:value="form.expiryTime"
            value-format="YYYY-MM-DD HH:mm:ss"
            show-time
            placeholder="过期时间"
            class="full-width"
          />
        </a-form-item>
        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="form.remarks" placeholder="请输入" :rows="4" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="commentOpen" title="评论" width="620px" :footer="null">
      <a-table
        :columns="commentColumns"
        :data-source="commentRows"
        row-key="key"
        :pagination="false"
        :locale="{ emptyText: '暂无数据' }"
      />
    </a-modal>
  </div>
</template>

<script setup name="Orderinfo">
import {
  cancelOrderinfo,
  getOrderinfo,
  listOrderinfo,
  updateOrderinfo,
} from "@/api/member/orderinfo";

const { proxy } = getCurrentInstance();
const { order_status, order_type } = proxy.useDict("order_status", "order_type");

const orderinfoList = ref([]);
const loading = ref(true);
const total = ref(0);
const advancedSearchVisible = ref(false);
const createdDateRange = ref([]);
const editOpen = ref(false);
const commentOpen = ref(false);
const submitting = ref(false);
const orderinfoRef = ref();
const form = ref({ id: null, expiryTime: null, remarks: null });
const commentRows = ref([]);

const orderStatusOptions = computed(() => {
  const options = [...(order_status.value || [])];
  if (!options.some((item) => String(item.value) === "3")) {
    options.push({ label: "已取消", value: "3", elTagType: "info" });
  }
  const order = ["1", "2", "0", "3"];
  return options.sort(
    (left, right) => order.indexOf(String(left.value)) - order.indexOf(String(right.value))
  );
});

const orderinfoColumns = [
  { title: "明细编号", dataIndex: "orderNumber", key: "orderNumber", align: "center", fixed: "left", width: 210 },
  { title: "用户名", dataIndex: "username", align: "center", width: 140 },
  { title: "类型", dataIndex: "type", align: "center", width: 90 },
  { title: "单数", dataIndex: "orderCount", align: "center", width: 90 },
  { title: "金额", dataIndex: "amount", align: "center", width: 110 },
  { title: "返佣百分比", dataIndex: "rebatePercentage", key: "rebatePercentage", align: "center", width: 130 },
  { title: "返佣", dataIndex: "rebate", align: "center", width: 100 },
  { title: "上级返佣百分比", dataIndex: "upperRebatePercentage", key: "upperRebatePercentage", align: "center", width: 160 },
  { title: "上级返佣", dataIndex: "upperRebate", align: "center", width: 120 },
  { title: "状态", dataIndex: "status", align: "center", width: 110 },
  { title: "过期时间", dataIndex: "expiryTime", align: "center", width: 180 },
  { title: "商品图片", dataIndex: "productImage", align: "center", width: 110 },
  { title: "商品标题", dataIndex: "productTitle", align: "center", width: 300 },
  { title: "额外佣金", dataIndex: "extraCommissionAmount", key: "extraCommission", align: "center", width: 120 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "备注", dataIndex: "remarks", key: "remarks", align: "center", width: 180 },
  { title: "操作", key: "operation", align: "center", fixed: "right", width: 180 },
];

const commentColumns = [
  { title: "评论", dataIndex: "comment", align: "center" },
  { title: "评分", dataIndex: "rating", align: "center", width: 160 },
];

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  username: null,
  orderNumber: null,
  type: null,
  orderCount: null,
  amountMin: null,
  amountMax: null,
  status: null,
  expiryDate: null,
  productTitle: null,
  extraCommission: null,
});

const rules = {
  expiryTime: [{ required: true, message: "过期时间不能为空", trigger: "change" }],
};

function requestParams() {
  const {
    amountMin,
    amountMax,
    expiryDate,
    extraCommission,
    ...base
  } = queryParams;
  const params = {};
  if (amountMin !== null && amountMin !== undefined) params.amountMin = amountMin;
  if (amountMax !== null && amountMax !== undefined) params.amountMax = amountMax;
  if (expiryDate) params.expiryDate = expiryDate;
  if (extraCommission !== null && extraCommission !== undefined) {
    params.extraCommission = extraCommission;
  }
  if (createdDateRange.value?.length === 2) {
    params.beginTime = createdDateRange.value[0];
    params.endTime = createdDateRange.value[1];
  }
  return Object.keys(params).length ? { ...base, params } : { ...base };
}

async function getList() {
  loading.value = true;
  try {
    const response = await listOrderinfo(requestParams());
    orderinfoList.value = response.rows || [];
    total.value = response.total || 0;
  } finally {
    loading.value = false;
  }
}

function displayValue(value, fallback = "-") {
  return value === null || value === undefined || value === "" ? fallback : value;
}

function formatPercentage(value) {
  if (value === null || value === undefined || value === "") return "-";
  const number = Number(value);
  return Number.isFinite(number) ? `${number.toFixed(2)}%` : "-";
}

function formatDateTime(value) {
  return value === null || value === undefined || value === ""
    ? "-"
    : proxy.parseTime(value);
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  Object.assign(queryParams, {
    pageNum: 1,
    username: null,
    orderNumber: null,
    type: null,
    orderCount: null,
    amountMin: null,
    amountMax: null,
    status: null,
    expiryDate: null,
    productTitle: null,
    extraCommission: null,
  });
  createdDateRange.value = [];
  getList();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.pageNum = page;
  queryParams.pageSize = pageSize;
  getList();
}

async function handleUpdate(row) {
  const response = await getOrderinfo(row.id);
  const detail = response.data || row;
  form.value = {
    id: detail.id,
    expiryTime: detail.expiryTime
      ? proxy.parseTime(detail.expiryTime, "{y}-{m}-{d} {h}:{i}:{s}")
      : null,
    remarks: detail.remarks || null,
  };
  editOpen.value = true;
  nextTick(() => orderinfoRef.value?.clearValidate?.());
}

function closeEditDialog() {
  editOpen.value = false;
  form.value = { id: null, expiryTime: null, remarks: null };
}

async function submitForm() {
  try {
    await orderinfoRef.value?.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await updateOrderinfo({
      id: form.value.id,
      expiryTime: form.value.expiryTime,
      remarks: form.value.remarks,
    });
    proxy.$modal.msgSuccess("操作成功");
    closeEditDialog();
    getList();
  } finally {
    submitting.value = false;
  }
}

function openCommentDialog(record) {
  const comment = record.comment ?? record.commentContent ?? record.commentText;
  const rating = record.rating ?? record.commentRating ?? record.score;
  commentRows.value = comment !== null && comment !== undefined && comment !== ""
    ? [{ key: record.commentId || record.id, comment, rating: displayValue(rating) }]
    : [];
  commentOpen.value = true;
}

async function handleCancelOrder(record) {
  await cancelOrderinfo(record.id);
  proxy.$modal.msgSuccess("操作成功");
  getList();
}

getList();
</script>

<style scoped>
.full-width {
  width: 100%;
}

.amount-range-input {
  width: calc(50% - 18px);
}

.amount-range-separator {
  width: 36px;
  padding-inline: 0;
  text-align: center;
  pointer-events: none;
}

.advanced-query-row {
  margin-top: 16px;
}
</style>
