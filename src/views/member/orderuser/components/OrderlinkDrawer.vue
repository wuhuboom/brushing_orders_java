<template>
  <a-drawer
    v-model:open="visible"
    :title="title"
    width="85%"
    destroy-on-close
    :mask-closable="false"
  >
    <div class="orderlink-page">
      <div class="member-summary">
        <div class="member-summary-row">
          <span><strong>用户名：</strong>{{ user.username || "-" }}</span>
          <span><strong>手机号：</strong>{{ user.phoneNumber || "-" }}</span>
          <span><strong>余额：</strong>{{ formatAmount(user.balance) }}</span>
          <span><strong>任务进度：</strong>{{ taskProgressDisplay }}</span>
          <span><strong>最后登录时间：</strong>{{ parseTime(user.lastLoginTime) || "-" }}</span>
          <a-tooltip title="刷新用户信息和连单列表">
            <a-button type="text" class="summary-refresh" :loading="summaryLoading" @click="refreshAll">
              <ReloadOutlined />
            </a-button>
          </a-tooltip>
        </div>
        <div class="member-summary-tip">连单优先匹配ID小的商品!!!</div>
      </div>

      <ant-pro-table
        title="列表"
        :columns="orderlinkColumns"
        :data-source="orderlinkList"
        :loading="loading"
        row-key="id"
        :row-selection="rowSelection"
        :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
        :scroll="{ x: 1760 }"
        @page-change="handleAntPageChange"
        @refresh="refreshAll"
      >
        <template #search>
          <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
            <a-row :gutter="[20, 16]" align="middle">
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item label="连单ID">
                  <a-input
                    v-model:value="queryParams.linkOrderId"
                    allow-clear
                    placeholder="请输入连单ID"
                    @pressEnter="handleQuery"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item label="单数">
                  <a-input-number
                    v-model:value="queryParams.orderCount"
                    :min="1"
                    :controls="false"
                    placeholder="请输入单数"
                    class="full-width"
                    @pressEnter="handleQuery"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item label="价格类型">
                  <a-select v-model:value="queryParams.priceType" allow-clear placeholder="请选择价格类型">
                    <a-select-option
                      v-for="option in priceTypeOptions"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item label="状态">
                  <a-select v-model:value="queryParams.status" allow-clear placeholder="请选择状态">
                    <a-select-option
                      v-for="option in statusOptions"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="8">
                <a-form-item label="价格">
                  <a-space class="range-field" :size="8">
                    <a-input-number
                      v-model:value="queryParams.priceRange[0]"
                      :min="0"
                      :controls="false"
                      placeholder="最低价格"
                    />
                    <span class="range-separator">~</span>
                    <a-input-number
                      v-model:value="queryParams.priceRange[1]"
                      :min="0"
                      :controls="false"
                      placeholder="最高价格"
                    />
                  </a-space>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="9">
                <a-form-item label="创建时间">
                  <a-range-picker
                    v-model:value="queryParams.createTimeRange"
                    value-format="YYYY-MM-DD"
                    class="full-width"
                  />
                </a-form-item>
              </a-col>
              <a-col flex="auto" class="ant-pro-query-actions">
                <a-space>
                  <a-button @click="resetQuery">重置</a-button>
                  <a-button type="primary" @click="handleQuery">查询</a-button>
                </a-space>
              </a-col>
            </a-row>
          </a-form>
        </template>

        <template #toolbar>
          <a-space>
            <a-button
              type="primary"
              danger
              :disabled="multiple"
              @click="handleDelete()"
              v-hasPermi="['member:orderlink:remove']"
            >
              <template #icon><DeleteOutlined /></template>
              删除
            </a-button>
            <a-button type="primary" @click="handleAdd" v-hasPermi="['member:orderlink:add']">
              <template #icon><PlusOutlined /></template>
              新增
            </a-button>
          </a-space>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'productImage'">
            <image-preview :src="record.productImage" :width="50" :height="50" />
          </template>
          <template v-else-if="column.key === 'productTitle'">
            <a-typography-text :copyable="record.productTitle ? { text: record.productTitle } : false">
              {{ record.productTitle || "-" }}
            </a-typography-text>
          </template>
          <template v-else-if="column.key === 'priceType'">
            {{ priceTypeLabel(record.priceType) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusMeta(resolvedStatus(record)).color">
              {{ statusMeta(resolvedStatus(record)).label }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ parseTime(record.createTime) || "-" }}
          </template>
          <template v-else-if="column.key === 'updateTime'">
            {{ parseTime(record.updateTime || record.createTime) || "-" }}
          </template>
          <template v-else-if="column.key === 'updateBy'">
            {{ record.updateBy || record.createBy || "-" }}
          </template>
          <template v-else-if="column.key === 'operation'">
            <a-button
              type="link"
              class="cell-action"
              :disabled="!isEditableStatus(resolvedStatus(record))"
              @click="handleUpdate(record)"
              v-hasPermi="['member:orderlink:edit']"
            >
              修改
            </a-button>
          </template>
        </template>
      </ant-pro-table>
    </div>

    <a-modal
      v-model:open="editModalVisible"
      title="修改连单明细"
      width="520px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="editSaving"
      destroy-on-close
      @ok="submitEdit"
      @cancel="closeEditModal"
    >
      <a-form ref="editFormRef" :model="editForm" :rules="editRules" layout="vertical">
        <a-form-item label="价格类型" name="priceType">
          <a-radio-group v-model:value="editForm.priceType">
            <a-radio
              v-for="option in priceTypeOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="价格" name="price">
          <a-input-number
            v-model:value="editForm.price"
            :min="0"
            :controls="false"
            placeholder="请输入价格"
            class="full-width"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="addDrawerVisible"
      title="新增连单"
      width="85%"
      destroy-on-close
      :mask-closable="false"
    >
      <div class="orderlink-editor">
        <a-spin :spinning="loadingUser">
          <a-form
            ref="addFormRef"
            :model="form"
            :rules="addRules"
            layout="vertical"
            class="orderlink-base-form"
          >
            <a-row :gutter="20">
              <a-col :xs="24" :md="8">
                <a-form-item label="任务进度">
                  <a-input :value="taskProgressDisplay" disabled />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item label="单数" name="orderCount">
                  <a-input-number
                    v-model:value="form.orderCount"
                    :min="1"
                    :controls="false"
                    placeholder="请输入单数"
                    class="full-width"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item label="返佣倍数" name="commissionMultiple">
                  <a-input-number
                    v-model:value="form.commissionMultiple"
                    :min="1"
                    :controls="false"
                    placeholder="请输入返佣倍数"
                    class="full-width"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="20">
              <a-col :xs="24" :md="8">
                <a-form-item label="余额"><a-input :value="formatAmount(user.balance)" disabled /></a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item label="冻结余额"><a-input :value="formatAmount(user.frozenBalance)" disabled /></a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item label="总余额"><a-input :value="formatAmount(user.totalBalance)" disabled /></a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </a-spin>

        <ant-pro-table
          title="明细"
          :columns="detailColumns"
          :data-source="details"
          row-key="id"
          :pagination="false"
          :scroll="{ x: 900 }"
          :tool-options="{ refresh: false, density: false, columns: false, fullscreen: false }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'detailPriceType'">
              <a-select
                v-model:value="record.priceType"
                :disabled="!record.isEditing"
                size="small"
                class="full-width"
              >
                <a-select-option
                  v-for="option in priceTypeOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </template>
            <template v-else-if="column.key === 'detailPrice'">
              <a-input-number
                v-model:value="record.price"
                :min="0"
                :controls="false"
                :disabled="!record.isEditing"
                size="small"
                class="full-width"
              />
            </template>
            <template v-else-if="column.key === 'detailStatus'">
              <a-tag color="red">未完成</a-tag>
            </template>
            <template v-else-if="column.key === 'detailAction'">
              <a-space :size="4">
                <a-button
                  v-if="!record.isEditing"
                  type="link"
                  class="cell-action"
                  @click="startDetailEdit(record)"
                >
                  修改
                </a-button>
                <a-button
                  v-else
                  type="link"
                  class="cell-action"
                  @click="saveDetail(record)"
                >
                  保存
                </a-button>
                <a-button
                  v-if="record.isEditing"
                  type="link"
                  class="cell-action"
                  @click="cancelDetailEdit(record)"
                >
                  取消
                </a-button>
                <a-popconfirm
                  title="确认删除这条商品明细吗？"
                  ok-text="确 定"
                  cancel-text="取 消"
                  @confirm="removeDetail(record)"
                >
                  <a-button type="link" danger class="cell-action">删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </ant-pro-table>

        <ant-pro-table
          title="商品列表"
          :columns="goodsColumns"
          :data-source="goodsList"
          :loading="goodsLoading"
          row-key="id"
          :pagination="{ current: goodsQuery.pageNum, pageSize: goodsQuery.pageSize, total: goodsTotal }"
          :custom-row="goodsCustomRow"
          :row-class-name="goodsRowClass"
          :scroll="{ x: 1050 }"
          @page-change="handleGoodsPageChange"
          @refresh="fetchGoods"
        >
          <template #search>
            <a-form layout="horizontal" :model="goodsQuery" class="ant-pro-query-form">
              <a-row :gutter="[20, 16]" align="middle">
                <a-col :xs="24" :sm="12" :lg="6">
                  <a-form-item label="标题">
                    <a-input
                      v-model:value="goodsQuery.title"
                      allow-clear
                      placeholder="请输入标题"
                      @pressEnter="handleGoodsQuery"
                    />
                  </a-form-item>
                </a-col>
                <a-col :xs="24" :sm="12" :lg="6">
                  <a-form-item label="类目">
                    <a-select v-model:value="goodsQuery.typeId" allow-clear placeholder="请选择类目">
                      <a-select-option v-for="item in goodsTypes" :key="item.id" :value="item.id">
                        {{ item.title }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :xs="24" :sm="12" :lg="8">
                  <a-form-item label="价格">
                    <a-space class="range-field" :size="8">
                      <a-input-number
                        v-model:value="goodsQuery.priceRange[0]"
                        :min="0"
                        :controls="false"
                        placeholder="最低价格"
                      />
                      <span class="range-separator">~</span>
                      <a-input-number
                        v-model:value="goodsQuery.priceRange[1]"
                        :min="0"
                        :controls="false"
                        placeholder="最高价格"
                      />
                    </a-space>
                  </a-form-item>
                </a-col>
                <a-col flex="auto" class="ant-pro-query-actions">
                  <a-space>
                    <a-button @click="resetGoodsQuery">重置</a-button>
                    <a-button type="primary" @click="handleGoodsQuery">查询</a-button>
                  </a-space>
                </a-col>
              </a-row>
            </a-form>
          </template>

          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'goodsImage'">
              <image-preview :src="record.image" :width="50" :height="50" />
            </template>
            <template v-else-if="column.key === 'goodsCreateTime'">
              {{ parseTime(record.createTime) || "-" }}
            </template>
          </template>
        </ant-pro-table>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <a-button @click="closeAddDrawer">取 消</a-button>
          <a-popconfirm
            v-if="negativePriceCount > 1"
            :title="`当前设置用户会遇到${negativePriceCount}次负数，请注意!`"
            ok-text="确 定"
            cancel-text="取 消"
            @confirm="submitAdd"
          >
            <a-button type="primary" :loading="saving">确 定</a-button>
          </a-popconfirm>
          <a-button v-else type="primary" :loading="saving" @click="submitAdd">确 定</a-button>
        </div>
      </template>
    </a-drawer>
  </a-drawer>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref, toRefs, watch } from "vue";
import { DeleteOutlined, PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import {
  addOrderlink,
  delOrderlink,
  getOrderlink,
  listOrderlink,
  updateOrderlink,
} from "@/api/member/orderlink";
import { listGoods, typeList } from "@/api/member/goods";
import { getOrderuser } from "@/api/member/orderuser";
import { resolveDeleteIds } from "@/utils/management-rules";
import { parseTime } from "@/utils/common";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  userId: {
    type: [String, Number],
    default: null,
  },
  title: {
    type: String,
    default: "连单设置",
  },
});

const emit = defineEmits(["update:modelValue", "success"]);
const { proxy } = getCurrentInstance();
const { price_type } = proxy.useDict("price_type");

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

const fallbackPriceTypes = [
  { label: "商品价格", value: "0" },
  { label: "交易后负余额", value: "1" },
];

const priceTypeOptions = computed(() => {
  const options = price_type.value || [];
  if (!options.length) return fallbackPriceTypes;
  return options.map((item) => ({
    label: item.label,
    value: String(item.value),
  }));
});

const statusOptions = [
  { label: "已完成", value: "0", color: "green" },
  { label: "未完成", value: "1", color: "red" },
  { label: "已冻结", value: "2", color: "orange" },
  { label: "待提交", value: "3", color: "blue" },
];

const loading = ref(false);
const summaryLoading = ref(false);
const orderlinkList = ref([]);
const ids = ref([]);
const multiple = ref(true);
const total = ref(0);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    linkOrderId: null,
    orderCount: null,
    priceType: undefined,
    status: undefined,
    priceRange: [null, null],
    createTimeRange: [],
  },
});

const { queryParams } = toRefs(data);

const orderlinkColumns = [
  { title: "自增ID", dataIndex: "id", key: "id", width: 100, fixed: "left" },
  { title: "连单ID", dataIndex: "linkOrderId", key: "linkOrderId", width: 110 },
  { title: "单数", dataIndex: "orderCount", key: "orderCount", width: 90 },
  { title: "返佣倍数", dataIndex: "commissionMultiple", key: "commissionMultiple", width: 110 },
  { title: "商品图片", dataIndex: "productImage", key: "productImage", width: 100 },
  { title: "商品标题", dataIndex: "productTitle", key: "productTitle", width: 220 },
  { title: "价格类型", dataIndex: "priceType", key: "priceType", width: 130 },
  { title: "价格", dataIndex: "price", key: "price", width: 110 },
  { title: "状态", dataIndex: "status", key: "status", width: 100 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "最后修改人", dataIndex: "updateBy", key: "updateBy", width: 120 },
  { title: "最后修改时间", dataIndex: "updateTime", key: "updateTime", width: 180 },
  { title: "操作", key: "operation", width: 90, fixed: "right" },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  getCheckboxProps: (record) => ({
    disabled: !isEditableStatus(resolvedStatus(record)),
  }),
  onChange: (selectedKeys) => {
    ids.value = selectedKeys;
    multiple.value = selectedKeys.length === 0;
  },
}));

const user = reactive({
  id: null,
  username: null,
  phoneNumber: null,
  balance: 0,
  frozenBalance: 0,
  totalBalance: 0,
  taskProgress: 0,
  memberOrderCountPerDay: null,
  lastLoginTime: null,
});

const taskProgressDisplay = computed(
  () => `${user.taskProgress ?? 0} / ${user.memberOrderCountPerDay ?? 0}`,
);

function formatAmount(value) {
  const amount = Number(value || 0);
  return Number.isFinite(amount) ? amount.toFixed(2) : "0.00";
}

function priceTypeLabel(value) {
  return priceTypeOptions.value.find((item) => String(item.value) === String(value))?.label || value || "-";
}

function statusMeta(value) {
  return statusOptions.find((item) => item.value === String(value))
    || { label: value || "-", color: "default" };
}

function resolvedStatus(record) {
  return record?.displayStatus ?? record?.status;
}

function isEditableStatus(value) {
  return String(value) === "1";
}

function buildListParams() {
  const [minPrice, maxPrice] = queryParams.value.priceRange || [];
  const [beginDate, endDate] = queryParams.value.createTimeRange || [];
  return {
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize,
    linkOrderId: queryParams.value.linkOrderId,
    orderCount: queryParams.value.orderCount,
    priceType: queryParams.value.priceType,
    status: queryParams.value.status,
    userId: props.userId,
    params: {
      beginPrice: minPrice,
      endPrice: maxPrice,
      beginTime: beginDate ? `${beginDate} 00:00:00` : undefined,
      endTime: endDate ? `${endDate} 23:59:59` : undefined,
    },
  };
}

async function getList() {
  if (!props.userId) {
    orderlinkList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const response = await listOrderlink(buildListParams());
    orderlinkList.value = response.rows || [];
    total.value = response.total || 0;
  } finally {
    loading.value = false;
  }
}

async function fetchUser(id) {
  if (!id) {
    Object.assign(user, {
      id: null,
      username: null,
      phoneNumber: null,
      balance: 0,
      frozenBalance: 0,
      totalBalance: 0,
      taskProgress: 0,
      memberOrderCountPerDay: null,
      lastLoginTime: null,
    });
    return;
  }

  loadingUser.value = true;
  try {
    const response = await getOrderuser(id);
    const record = response.data || response;
    const balance = Number(record.balance || 0);
    const frozenBalance = Number(record.frozenBalance ?? record.freezeBalance ?? 0);
    Object.assign(user, {
      id: record.id,
      username: record.username,
      phoneNumber: record.phoneNumber ?? record.phone,
      balance,
      frozenBalance,
      totalBalance: Number(record.totalBalance ?? balance + frozenBalance),
      taskProgress: record.taskProgress ?? record.orderSeq ?? 0,
      memberOrderCountPerDay:
        record.memberLevel?.orderCountPerDay
        ?? record.orderLimit
        ?? record.memberOrderCountPerDay
        ?? 0,
      lastLoginTime: record.lastLoginTime ?? record.lastLoginDate,
    });
  } finally {
    loadingUser.value = false;
  }
}

async function refreshAll() {
  summaryLoading.value = true;
  try {
    await Promise.all([fetchUser(props.userId), getList()]);
  } finally {
    summaryLoading.value = false;
  }
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  Object.assign(queryParams.value, {
    pageNum: 1,
    linkOrderId: null,
    orderCount: null,
    priceType: undefined,
    status: undefined,
    priceRange: [null, null],
    createTimeRange: [],
  });
  getList();
}

async function handleDelete(row) {
  const targetIds = resolveDeleteIds(row, ids.value);
  if (!targetIds.length) {
    proxy.$modal.msgWarning("请选择要删除的数据");
    return;
  }

  try {
    await proxy.$modal.confirm(`是否确认删除选中的 ${targetIds.length} 条连单明细？`);
    await delOrderlink(targetIds.join(","));
    ids.value = [];
    multiple.value = true;
    proxy.$modal.msgSuccess("删除成功");
    await getList();
    emit("success");
  } catch {
    // Cancelled by the user.
  }
}

const editModalVisible = ref(false);
const editSaving = ref(false);
const editFormRef = ref();
const editForm = reactive({
  id: null,
  priceType: undefined,
  price: null,
});
const editRules = {
  priceType: [{ required: true, message: "请选择价格类型", trigger: "change" }],
  price: [{ required: true, message: "请输入价格", trigger: "blur" }],
};

async function handleUpdate(row) {
  if (!isEditableStatus(resolvedStatus(row))) return;
  const response = await getOrderlink(row.id);
  const record = response.data || response;
  Object.assign(editForm, {
    id: record.id,
    priceType: String(record.priceType),
    price: record.price,
  });
  editModalVisible.value = true;
}

async function submitEdit() {
  try {
    await editFormRef.value?.validate();
  } catch {
    return;
  }

  editSaving.value = true;
  try {
    await updateOrderlink({ ...editForm });
    proxy.$modal.msgSuccess("修改成功");
    editModalVisible.value = false;
    await getList();
    emit("success");
  } finally {
    editSaving.value = false;
  }
}

function closeEditModal() {
  editModalVisible.value = false;
  editFormRef.value?.resetFields();
}

const addDrawerVisible = ref(false);
const loadingUser = ref(false);
const saving = ref(false);
const addFormRef = ref();
const form = reactive({
  orderCount: 1,
  commissionMultiple: 1,
});
const addRules = {
  orderCount: [{ required: true, message: "请输入单数", trigger: "blur" }],
  commissionMultiple: [{ required: true, message: "请输入返佣倍数", trigger: "blur" }],
};

const details = ref([]);
const detailColumns = [
  { title: "商品ID", dataIndex: "id", key: "id", width: 110 },
  { title: "商品标题", dataIndex: "title", key: "title", width: 260 },
  { title: "价格类型", key: "detailPriceType", width: 190 },
  { title: "价格", key: "detailPrice", width: 160 },
  { title: "状态", key: "detailStatus", width: 100 },
  { title: "操作", key: "detailAction", width: 180, fixed: "right" },
];

const negativePriceCount = computed(
  () => details.value.filter((item) => priceTypeLabel(item.priceType).includes("负余额")).length,
);

const goodsList = ref([]);
const goodsTotal = ref(0);
const goodsLoading = ref(false);
const goodsTypes = ref([]);
const goodsQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  title: "",
  typeId: undefined,
  priceRange: [null, null],
});

const goodsColumns = [
  { title: "商品ID", dataIndex: "id", key: "id", width: 110 },
  { title: "标题", dataIndex: "title", key: "title", width: 300 },
  { title: "类目", dataIndex: "typeTitle", key: "typeTitle", width: 150 },
  { title: "价格", dataIndex: "price", key: "price", width: 130 },
  { title: "序号", dataIndex: "serialNumber", key: "serialNumber", width: 110 },
  { title: "图片", dataIndex: "image", key: "goodsImage", width: 100 },
  { title: "创建时间", dataIndex: "createTime", key: "goodsCreateTime", width: 180 },
];

async function fetchGoodsTypes() {
  if (goodsTypes.value.length) return;
  const response = await typeList();
  goodsTypes.value = response.data || [];
}

async function fetchGoods() {
  const [beginPrice, endPrice] = goodsQuery.priceRange || [];
  goodsLoading.value = true;
  try {
    const response = await listGoods({
      pageNum: goodsQuery.pageNum,
      pageSize: goodsQuery.pageSize,
      title: goodsQuery.title,
      typeId: goodsQuery.typeId,
      isEnabled: "0",
      params: {
        beginPrice,
        endPrice,
      },
    });
    goodsList.value = response.rows || [];
    goodsTotal.value = response.total || 0;
  } finally {
    goodsLoading.value = false;
  }
}

function handleGoodsPageChange({ page, pageSize }) {
  goodsQuery.pageNum = page;
  goodsQuery.pageSize = pageSize;
  fetchGoods();
}

function handleGoodsQuery() {
  goodsQuery.pageNum = 1;
  fetchGoods();
}

function resetGoodsQuery() {
  Object.assign(goodsQuery, {
    pageNum: 1,
    title: "",
    typeId: undefined,
    priceRange: [null, null],
  });
  fetchGoods();
}

function goodsCustomRow(record) {
  return {
    onClick: () => addProductToDetails(record),
  };
}

function goodsRowClass(record) {
  return details.value.some((item) => item.id === record.id) ? "goods-row-selected" : "goods-row-clickable";
}

function defaultPriceType() {
  return priceTypeOptions.value.find((item) => item.label === "商品价格")?.value
    ?? priceTypeOptions.value[0]?.value
    ?? "0";
}

function addProductToDetails(record) {
  if (details.value.some((item) => item.id === record.id)) {
    proxy.$modal.msgInfo("该商品已添加到明细");
    return;
  }
  details.value.push({
    id: record.id,
    title: record.title,
    priceType: defaultPriceType(),
    price: record.price ?? 0,
    status: "1",
    isEditing: false,
    originalPriceType: undefined,
    originalPrice: undefined,
  });
}

function removeDetail(record) {
  details.value = details.value.filter((item) => item.id !== record.id);
}

function startDetailEdit(record) {
  record.originalPriceType = record.priceType;
  record.originalPrice = record.price;
  record.isEditing = true;
}

function saveDetail(record) {
  if (record.price == null || record.price === "" || Number.isNaN(Number(record.price))) {
    proxy.$modal.msgError("请输入有效价格");
    return;
  }
  record.isEditing = false;
  record.originalPriceType = undefined;
  record.originalPrice = undefined;
}

function cancelDetailEdit(record) {
  record.priceType = record.originalPriceType;
  record.price = record.originalPrice;
  record.isEditing = false;
  record.originalPriceType = undefined;
  record.originalPrice = undefined;
}

function resetAddForm() {
  Object.assign(form, {
    orderCount: Math.max(Number(user.taskProgress || 0) + 1, 1),
    commissionMultiple: 1,
  });
  details.value = [];
}

async function handleAdd() {
  resetAddForm();
  addDrawerVisible.value = true;
  await Promise.all([fetchUser(props.userId), fetchGoodsTypes(), fetchGoods()]);
}

function closeAddDrawer() {
  addDrawerVisible.value = false;
  details.value = [];
  addFormRef.value?.resetFields();
}

async function submitAdd() {
  try {
    await addFormRef.value?.validate();
  } catch {
    return;
  }

  if (!details.value.length) {
    proxy.$modal.msgError("请至少添加一个商品到明细");
    return;
  }
  if (details.value.some((item) => item.isEditing)) {
    proxy.$modal.msgWarning("请先保存正在编辑的商品明细");
    return;
  }

  saving.value = true;
  try {
    await addOrderlink({
      userId: props.userId,
      orderCount: form.orderCount,
      commissionMultiple: form.commissionMultiple,
      details: details.value.map((item) => ({
        goodsId: item.id,
        priceType: item.priceType,
        price: item.price,
      })),
    });
    proxy.$modal.msgSuccess("新增成功");
    closeAddDrawer();
    await getList();
    emit("success");
  } finally {
    saving.value = false;
  }
}

watch(
  () => props.userId,
  (id) => {
    if (visible.value) {
      ids.value = [];
      multiple.value = true;
      queryParams.value.pageNum = 1;
      refreshAll();
    } else if (!id) {
      orderlinkList.value = [];
    }
  },
);

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      ids.value = [];
      multiple.value = true;
      queryParams.value.pageNum = 1;
      refreshAll();
    } else {
      addDrawerVisible.value = false;
      editModalVisible.value = false;
    }
  },
);
</script>

<style scoped>
.orderlink-page,
.orderlink-editor {
  min-width: 0;
}

.member-summary {
  padding: 12px 18px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.member-summary-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 18px;
  min-height: 28px;
}

.member-summary-row span {
  white-space: nowrap;
}

.member-summary-tip {
  margin-top: 6px;
  color: #ff4d4f;
}

.summary-refresh {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #1677ff;
}

.full-width {
  width: 100%;
}

.range-field {
  display: flex;
  width: 100%;
}

.range-field :deep(.ant-input-number) {
  flex: 1;
  min-width: 0;
}

.range-separator {
  color: #8c8c8c;
}

.cell-action {
  height: 24px;
  padding: 0;
}

.orderlink-base-form {
  margin-bottom: 12px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:deep(.goods-row-clickable),
:deep(.goods-row-selected) {
  cursor: pointer;
}

:deep(.goods-row-clickable:hover > td) {
  background: #f5faff !important;
}

:deep(.goods-row-selected > td) {
  background: #e6f4ff !important;
}

@media (max-width: 900px) {
  .member-summary-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
