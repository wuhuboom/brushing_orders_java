<template>
  <a-drawer
    v-model:open="visible"
    :title="title"
    width="85%"
    root-class-name="orderlink-drawer-root"
    destroy-on-close
    :mask-closable="false"
    :closable="!mutationPending"
    :keyboard="!mutationPending"
  >
    <div class="orderlink-page">
      <ant-pro-table
        title=""
        :columns="orderlinkColumns"
        :data-source="orderlinkList"
        :loading="loading"
        row-key="id"
        :row-selection="rowSelection"
        :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
        :scroll="{ x: 1732, y: 'calc(100vh - 440px)' }"
        @page-change="handleAntPageChange"
        @refresh="refreshAll()"
        @change="handleTableChange"
      >
        <template #search>
          <a-form
            layout="horizontal"
            :model="queryParams"
            class="ant-pro-query-form orderlink-query-form"
            :class="{ 'query-expanded': advancedSearchVisible }"
          >
            <a-row :gutter="[24, 16]" align="middle">
              <a-col :xs="24" :sm="12" :lg="8" :xl="8" :xxl="6">
                <a-form-item label="连单ID">
                  <a-input
                    v-model:value="queryParams.linkOrderId"
                    allow-clear
                    placeholder="请输入"
                    @pressEnter="handleQuery"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="8" :xl="8" :xxl="6">
                <a-form-item label="单数">
                  <a-input-number
                    v-model:value="queryParams.orderCount"
                    :min="1"
                    :controls="false"
                    placeholder="请输入"
                    class="full-width"
                    @pressEnter="handleQuery"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="8" :xl="8" :xxl="6" class="orderlink-product-title-query">
                <a-form-item label="商品标题">
                  <a-input
                    v-model:value="queryParams.productTitle"
                    allow-clear
                    placeholder="请输入"
                    @pressEnter="handleQuery"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="8" :xl="8" :xxl="6" class="ant-pro-query-actions orderlink-query-actions">
                <a-space>
                  <a-button @click="resetQuery">重 置</a-button>
                  <a-button type="primary" @click="handleQuery">查 询</a-button>
                  <a-button type="link" class="orderlink-expand-btn" @click="advancedSearchVisible = !advancedSearchVisible">
                    {{ advancedSearchVisible ? "收起" : "展开" }}
                    <UpOutlined v-if="advancedSearchVisible" />
                    <DownOutlined v-else />
                  </a-button>
                </a-space>
              </a-col>
            </a-row>

            <a-row v-if="advancedSearchVisible" :gutter="[24, 16]" class="orderlink-advanced-query-row">
              <a-col :xs="24" :sm="12" :lg="8" :xl="8" :xxl="6">
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
              <a-col :xs="24" :sm="12" :lg="8" :xl="8" :xxl="6">
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
              <a-col :xs="24" :sm="12" :lg="8" :xl="8" :xxl="6">
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
              <a-col :xs="24" :sm="12" :lg="8" :xl="8" :xxl="6">
                <a-form-item label="创建时间">
                  <a-range-picker
                    v-model:value="queryParams.createTimeRange"
                    value-format="YYYY-MM-DD"
                    class="full-width"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </template>

        <template #title>
          <div class="member-summary">
            <span class="member-summary-field">用户名: {{ user.username || "-" }}</span>
            <span class="member-summary-field">手机号码: {{ user.phoneNumber || "-" }}</span>
            <span class="member-summary-field">余额: {{ formatAmount(user.balance) }}</span>
            <span class="member-summary-field">任务进度: {{ taskProgressDisplay }}</span>
            <span>最后登录时间: {{ parseTime(user.lastLoginTime) || "-" }}</span>
            <a-tooltip title="刷新用户信息和连单列表">
              <a-button type="link" class="summary-refresh" :loading="summaryLoading" @click="refreshAll()">
                <ReloadOutlined />
              </a-button>
            </a-tooltip>
            <br />
            <span class="member-summary-tip">连单优先匹配ID小的商品!!!</span>
          </div>
        </template>

        <template #toolbar>
          <a-space>
            <a-button
              type="primary"
              danger
              :disabled="multiple || deleteSaving"
              :loading="deleteSaving"
              @click="handleDelete()"
              v-hasPermi="['member:orderlink:remove']"
            >
              <template #icon><DeleteOutlined /></template>
              删除
            </a-button>
            <a-button
              v-if="hasPermission('member:goods:list')"
              type="primary"
              :disabled="addOpening || saving"
              :loading="addOpening"
              @click="handleAdd"
              v-hasPermi="['member:orderlink:add']"
            >
              <template #icon><PlusOutlined /></template>
              创建
            </a-button>
          </a-space>
        </template>

        <template #emptyText>
          <a-empty :image="simpleEmptyImage" description="暂无数据" />
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
            {{ parseTime(record.updateTime) || "-" }}
          </template>
          <template v-else-if="column.key === 'updateBy'">
            {{ record.updateBy || "-" }}
          </template>
          <template v-else-if="column.key === 'operation'">
            <a-space :size="12">
              <a-button
                type="link"
                class="cell-action"
                :disabled="!isEditableStatus(resolvedStatus(record)) || deleteSaving || editSaving"
                :loading="editingRecordId === record.id"
                @click="handleUpdate(record)"
                v-hasPermi="['member:orderlink:edit']"
              >
                修改
              </a-button>
              <a-button
                type="link"
                danger
                class="cell-action"
                :disabled="!isEditableStatus(resolvedStatus(record)) || deleteSaving || editSaving"
                @click="handleDelete(record)"
                v-hasPermi="['member:orderlink:remove']"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </ant-pro-table>
    </div>

    <a-drawer
      v-model:open="editModalVisible"
      title="修改连单明细"
      width="85%"
      destroy-on-close
      :mask-closable="false"
      :closable="!editSaving"
      :keyboard="!editSaving"
      @close="closeEditModal"
    >
      <a-spin :spinning="editLoading">
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
      </a-spin>

      <template #footer>
        <div class="drawer-footer">
          <a-button :disabled="editSaving" @click="closeEditModal">取 消</a-button>
          <a-button
            type="primary"
            :loading="editSaving"
            :disabled="editLoading || editSaving"
            @click="submitEdit"
          >
            确 定
          </a-button>
        </div>
      </template>
    </a-drawer>

    <a-drawer
      v-model:open="addDrawerVisible"
      title="创建"
      width="85%"
      destroy-on-close
      :mask-closable="false"
      :closable="!saving"
      :keyboard="!saving"
      @close="closeAddDrawer"
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
                  :disabled="saving"
                  @click="startDetailEdit(record)"
                >
                  修改
                </a-button>
                <a-button
                  v-else
                  type="link"
                  class="cell-action"
                  :disabled="saving"
                  @click="saveDetail(record)"
                >
                  保存
                </a-button>
                <a-button
                  v-if="record.isEditing"
                  type="link"
                  class="cell-action"
                  :disabled="saving"
                  @click="cancelDetailEdit(record)"
                >
                  取消
                </a-button>
                <a-popconfirm
                  title="确认删除这条商品明细吗？"
                  ok-text="确 定"
                  cancel-text="取 消"
                  :disabled="saving"
                  @confirm="removeDetail(record)"
                >
                  <a-button type="link" danger class="cell-action" :disabled="saving">删除</a-button>
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
          @refresh="fetchGoods()"
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
          <a-button :disabled="saving" @click="closeAddDrawer">取 消</a-button>
          <a-popconfirm
            v-if="negativePriceCount > 1"
            :title="`当前设置用户会遇到${negativePriceCount}次负数，请注意!`"
            ok-text="确 定"
            cancel-text="取 消"
            :disabled="saving"
            @confirm="submitAdd"
          >
            <a-button type="primary" :loading="saving" :disabled="saving">确 定</a-button>
          </a-popconfirm>
          <a-button
            v-else
            type="primary"
            :loading="saving"
            :disabled="saving"
            @click="submitAdd"
          >
            确 定
          </a-button>
        </div>
      </template>
    </a-drawer>
  </a-drawer>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref, toRefs, watch } from "vue";
import {
  DeleteOutlined,
  DownOutlined,
  PlusOutlined,
  ReloadOutlined,
  UpOutlined,
} from "@ant-design/icons-vue";
import { Empty } from "ant-design-vue";
import {
  addOrderlink,
  delOrderlink,
  getOrderlink,
  listOrderlink,
  updateOrderlink,
} from "@/api/member/orderlink";
import { listGoods, typeList } from "@/api/member/goods";
import { getOrderuserOperationSummary } from "@/api/member/orderuser";
import { resolveDeleteIds } from "@/utils/management-rules";
import { parseTime } from "@/utils/common";
import { formatOrderlinkAmount as formatAmount } from "./orderlinkPresentation";
import useUserStore from "@/store/modules/user";

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
const userStore = useUserStore();
const { price_type } = proxy.useDict("price_type");
const simpleEmptyImage = Empty.PRESENTED_IMAGE_SIMPLE;

function hasPermission(permission) {
  const permissions = userStore.permissions || [];
  return permissions.includes("*:*:*") || permissions.includes(permission);
}

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
const advancedSearchVisible = ref(false);
const deleteSaving = ref(false);
const addOpening = ref(false);

let drawerSession = 0;
let listRequestSequence = 0;
let userRequestSequence = 0;
let summaryRequestSequence = 0;
let goodsRequestSequence = 0;
let goodsTypeRequestSequence = 0;

function createMutationGuard(pendingRef) {
  let sequence = 0;
  let activeToken = null;
  return {
    acquire() {
      if (activeToken != null) return null;
      activeToken = ++sequence;
      pendingRef.value = true;
      return activeToken;
    },
    release(token) {
      if (activeToken !== token) return false;
      activeToken = null;
      pendingRef.value = false;
      return true;
    },
  };
}

const deleteMutationGuard = createMutationGuard(deleteSaving);

function sameId(left, right) {
  return String(left ?? "") === String(right ?? "");
}

function isCurrentDrawerRequest(session, targetUserId) {
  return visible.value
    && session === drawerSession
    && sameId(props.userId, targetUserId);
}

function normalizeTableResponse(response) {
  const rows = Array.isArray(response?.rows)
    ? response.rows
    : (Array.isArray(response?.data?.rows) ? response.data.rows : []);
  const rawTotal = response?.total ?? response?.data?.total ?? rows.length;
  const normalizedTotal = Number(rawTotal);
  return {
    rows,
    total: Number.isFinite(normalizedTotal) ? normalizedTotal : rows.length,
  };
}

function normalizeDetailResponse(response) {
  return response?.data && !Array.isArray(response.data) ? response.data : response;
}

function failureMessage(error, fallback) {
  const detail = error?.response?.data?.msg || error?.msg || error?.message;
  return detail && detail !== fallback ? `${fallback}：${detail}` : fallback;
}

function clearSelection() {
  ids.value = [];
  multiple.value = true;
}

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    linkOrderId: null,
    orderCount: null,
    productTitle: null,
    priceType: undefined,
    status: undefined,
    priceRange: [null, null],
    createTimeRange: [],
    orderByColumn: "ol.id",
    isAsc: "desc",
  },
});

const { queryParams } = toRefs(data);

const orderlinkColumns = [
  { title: "ID", dataIndex: "id", key: "id", width: 120, fixed: "left", sorter: true, defaultSortOrder: "descend" },
  { title: "连单ID", dataIndex: "linkOrderId", key: "linkOrderId", width: 100, fixed: "left", sorter: true },
  { title: "单数", dataIndex: "orderCount", key: "orderCount", width: 100, sorter: true },
  { title: "返佣倍数", dataIndex: "commissionMultiple", key: "commissionMultiple", width: 120, sorter: true },
  { title: "商品图片", dataIndex: "productImage", key: "productImage", width: 100 },
  { title: "商品标题", dataIndex: "productTitle", key: "productTitle", width: 200 },
  { title: "价格类型", dataIndex: "priceType", key: "priceType", width: 120, sorter: true },
  { title: "价格", dataIndex: "price", key: "price", width: 160, sorter: true },
  { title: "状态", dataIndex: "status", key: "status", width: 100 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 170, sorter: true },
  { title: "最后修改人", dataIndex: "updateBy", key: "updateBy", width: 120 },
  { title: "最后修改时间", dataIndex: "updateTime", key: "updateTime", width: 170, sorter: true },
  { title: "操作", key: "operation", width: 120, fixed: "right" },
];

const rowSelection = computed(() => ({
  columnWidth: 32,
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

function resetUser() {
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
}

const taskProgressDisplay = computed(
  () => `${user.taskProgress ?? 0} / ${user.memberOrderCountPerDay ?? 0}`,
);

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

function buildListParams(targetUserId) {
  const [minPrice, maxPrice] = queryParams.value.priceRange || [];
  const [beginDate, endDate] = queryParams.value.createTimeRange || [];
  return {
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize,
    linkOrderId: queryParams.value.linkOrderId,
    orderCount: queryParams.value.orderCount,
    productTitle: queryParams.value.productTitle,
    priceType: queryParams.value.priceType,
    status: queryParams.value.status,
    orderByColumn: queryParams.value.orderByColumn,
    isAsc: queryParams.value.isAsc,
    userId: targetUserId,
    params: {
      beginPrice: minPrice,
      endPrice: maxPrice,
      beginTime: beginDate ? `${beginDate} 00:00:00` : undefined,
      endTime: endDate ? `${endDate} 23:59:59` : undefined,
    },
  };
}

async function getList(targetUserId = props.userId, session = drawerSession) {
  const requestSequence = ++listRequestSequence;
  clearSelection();
  if (!targetUserId) {
    orderlinkList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const response = await listOrderlink(buildListParams(targetUserId));
    if (!isCurrentDrawerRequest(session, targetUserId) || requestSequence !== listRequestSequence) {
      return;
    }
    const result = normalizeTableResponse(response);
    orderlinkList.value = result.rows;
    total.value = result.total;
  } catch (error) {
    if (isCurrentDrawerRequest(session, targetUserId) && requestSequence === listRequestSequence) {
      orderlinkList.value = [];
      total.value = 0;
      proxy.$modal.msgError(failureMessage(error, "连单列表加载失败"));
    }
  } finally {
    if (requestSequence === listRequestSequence) {
      loading.value = false;
    }
  }
}

async function fetchUser(id, session = drawerSession) {
  const requestSequence = ++userRequestSequence;
  if (!id) {
    resetUser();
    return;
  }

  loadingUser.value = true;
  try {
    const response = await getOrderuserOperationSummary(id);
    if (!isCurrentDrawerRequest(session, id) || requestSequence !== userRequestSequence) {
      return;
    }
    const record = normalizeDetailResponse(response) || {};
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
        record.orderCountPerDay
        ?? record.orderLimit
        ?? record.memberOrderCountPerDay
        ?? 0,
      lastLoginTime: record.lastLoginTime ?? record.lastLoginDate,
    });
  } catch (error) {
    if (isCurrentDrawerRequest(session, id) && requestSequence === userRequestSequence) {
      resetUser();
      proxy.$modal.msgError(failureMessage(error, "会员信息加载失败"));
    }
  } finally {
    if (requestSequence === userRequestSequence) {
      loadingUser.value = false;
    }
  }
}

async function refreshAll(targetUserId = props.userId, session = drawerSession) {
  if (!targetUserId || !isCurrentDrawerRequest(session, targetUserId)) return;
  const requestSequence = ++summaryRequestSequence;
  summaryLoading.value = true;
  try {
    await Promise.all([fetchUser(targetUserId, session), getList(targetUserId, session)]);
  } finally {
    if (requestSequence === summaryRequestSequence) {
      summaryLoading.value = false;
    }
  }
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList(props.userId, drawerSession);
}

const orderlinkSortColumnMap = {
  id: "ol.id",
  linkOrderId: "ol.linkOrderId",
  orderCount: "ol.orderCount",
  commissionMultiple: "ol.commissionMultiple",
  priceType: "ol.priceType",
  price: "ol.price",
  createTime: "ol.createTime",
  updateTime: "ol.updateTime",
};

function handleTableChange(_pagination, _filters, sorter) {
  const activeSorter = Array.isArray(sorter)
    ? sorter.find((item) => item?.order)
    : sorter;
  queryParams.value.orderByColumn = activeSorter?.order
    ? orderlinkSortColumnMap[activeSorter.columnKey] || null
    : null;
  queryParams.value.isAsc = activeSorter?.order === "ascend"
    ? "asc"
    : activeSorter?.order === "descend"
      ? "desc"
      : null;
  queryParams.value.pageNum = 1;
  getList(props.userId, drawerSession);
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList(props.userId, drawerSession);
}

function resetQueryState() {
  Object.assign(queryParams.value, {
    pageNum: 1,
    pageSize: 10,
    linkOrderId: null,
    orderCount: null,
    productTitle: null,
    priceType: undefined,
    status: undefined,
    priceRange: [null, null],
    createTimeRange: [],
    orderByColumn: "ol.id",
    isAsc: "desc",
  });
  advancedSearchVisible.value = false;
}

function resetQuery() {
  resetQueryState();
  getList(props.userId, drawerSession);
}

async function handleDelete(row) {
  const mutationToken = deleteMutationGuard.acquire();
  if (mutationToken == null) return;
  const session = drawerSession;
  const targetUserId = props.userId;
  try {
    const targetIds = resolveDeleteIds(row, ids.value);
    if (!targetIds.length) {
      proxy.$modal.msgWarning("请选择要删除的数据");
      return;
    }

    const targetsAreCurrentAndEditable = () => {
      const currentRecords = new Map(
        orderlinkList.value.map((record) => [String(record.id), record]),
      );
      return targetIds.every((id) => {
        const record = currentRecords.get(String(id));
        return record && isEditableStatus(resolvedStatus(record));
      });
    };
    if (!targetsAreCurrentAndEditable()) {
      clearSelection();
      proxy.$modal.msgWarning("所选连单状态已变化，请刷新后重试");
      return;
    }

    try {
      await proxy.$modal.confirm(`是否确认删除选中的 ${targetIds.length} 条连单明细？`);
    } catch {
      return;
    }

    if (!isCurrentDrawerRequest(session, targetUserId) || !targetsAreCurrentAndEditable()) {
      if (isCurrentDrawerRequest(session, targetUserId)) {
        clearSelection();
        proxy.$modal.msgWarning("所选连单状态已变化，请刷新后重试");
      }
      return;
    }

    await delOrderlink(targetIds.join(","));
    if (isCurrentDrawerRequest(session, targetUserId)) {
      clearSelection();
      proxy.$modal.msgSuccess("删除成功");
      await getList(targetUserId, session);
    }
    emit("success");
  } catch (error) {
    if (isCurrentDrawerRequest(session, targetUserId)) {
      proxy.$modal.msgError(failureMessage(error, "删除失败"));
    }
  } finally {
    deleteMutationGuard.release(mutationToken);
  }
}

const editModalVisible = ref(false);
const editLoading = ref(false);
const editSaving = ref(false);
const editMutationGuard = createMutationGuard(editSaving);
const editingRecordId = ref(null);
const editFormRef = ref();
let editRequestSequence = 0;
let editTargetUserId = null;
let editTargetSession = 0;
const editForm = reactive({
  id: null,
  priceType: undefined,
  price: null,
});
const editRules = {
  priceType: [{ required: true, message: "请选择价格类型", trigger: "change" }],
  price: [
    { required: true, message: "请输入价格", trigger: "blur" },
    {
      validator: async (_rule, value) => {
        if (!Number.isFinite(Number(value)) || Number(value) < 0) {
          throw new Error("请输入有效价格");
        }
      },
      trigger: "blur",
    },
  ],
};

async function handleUpdate(row) {
  if (!isEditableStatus(resolvedStatus(row)) || editingRecordId.value != null || editSaving.value) {
    return;
  }
  const requestSequence = ++editRequestSequence;
  const session = drawerSession;
  const targetUserId = props.userId;
  editingRecordId.value = row.id;
  editLoading.value = true;
  editTargetUserId = targetUserId;
  editTargetSession = session;
  Object.assign(editForm, { id: null, priceType: undefined, price: null });
  editModalVisible.value = true;
  try {
    const response = await getOrderlink(row.id);
    if (
      requestSequence !== editRequestSequence
      || !editModalVisible.value
      || !isCurrentDrawerRequest(session, targetUserId)
    ) {
      return;
    }
    const record = normalizeDetailResponse(response) || {};
    if (!sameId(record.id, row.id) || !isEditableStatus(record.status)) {
      proxy.$modal.msgWarning("该连单已完成或锁定，不能修改");
      closeEditModal();
      await getList(targetUserId, session);
      return;
    }
    Object.assign(editForm, {
      id: record.id,
      priceType: String(record.priceType),
      price: record.price,
    });
  } catch (error) {
    if (requestSequence === editRequestSequence && editModalVisible.value) {
      proxy.$modal.msgError(failureMessage(error, "连单详情加载失败"));
      closeEditModal();
    }
  } finally {
    if (requestSequence === editRequestSequence) {
      editLoading.value = false;
      editingRecordId.value = null;
    }
  }
}

async function submitEdit() {
  if (editLoading.value || !editForm.id) return;
  const mutationToken = editMutationGuard.acquire();
  if (mutationToken == null) return;
  const session = editTargetSession;
  const targetUserId = editTargetUserId;
  const editorSequence = editRequestSequence;
  let succeeded = false;
  try {
    try {
      await editFormRef.value?.validate();
    } catch {
      return;
    }

    if (!isCurrentEditSession(session, targetUserId, editorSequence)) {
      return;
    }

    const currentRecord = orderlinkList.value.find((record) => sameId(record.id, editForm.id));
    if (!currentRecord || !isEditableStatus(resolvedStatus(currentRecord))) {
      proxy.$modal.msgWarning("该连单已完成或锁定，不能修改");
      resetEditModalState();
      await getList(targetUserId, session);
      return;
    }

    const payload = {
      id: editForm.id,
      priceType: String(editForm.priceType),
      price: editForm.price,
    };
    await updateOrderlink(payload);
    if (isCurrentEditSession(session, targetUserId, editorSequence)) {
      proxy.$modal.msgSuccess("修改成功");
      await getList(targetUserId, session);
    }
    emit("success");
    succeeded = true;
  } catch (error) {
    if (isCurrentEditSession(session, targetUserId, editorSequence)) {
      proxy.$modal.msgError(failureMessage(error, "修改失败"));
    }
  } finally {
    editMutationGuard.release(mutationToken);
  }
  if (succeeded && isCurrentEditSession(session, targetUserId, editorSequence)) {
    resetEditModalState();
  }
}

function isCurrentEditSession(session, targetUserId, editorSequence) {
  return editModalVisible.value
    && editorSequence === editRequestSequence
    && session === editTargetSession
    && sameId(targetUserId, editTargetUserId)
    && isCurrentDrawerRequest(session, targetUserId);
}

function resetEditModalState() {
  editRequestSequence += 1;
  editModalVisible.value = false;
  editLoading.value = false;
  editingRecordId.value = null;
  editFormRef.value?.resetFields();
  Object.assign(editForm, { id: null, priceType: undefined, price: null });
  editTargetUserId = null;
  editTargetSession = 0;
}

function closeEditModal() {
  if (editSaving.value) {
    editModalVisible.value = true;
    return;
  }
  resetEditModalState();
}

const addDrawerVisible = ref(false);
const loadingUser = ref(false);
const saving = ref(false);
const addMutationGuard = createMutationGuard(saving);
const mutationPending = computed(
  () => deleteSaving.value || editSaving.value || saving.value,
);
const addFormRef = ref();
const form = reactive({
  orderCount: 1,
  commissionMultiple: 1,
});
const positiveIntegerValidator = async (_rule, value) => {
  if (!Number.isInteger(Number(value)) || Number(value) < 1) {
    throw new Error("请输入大于 0 的整数");
  }
};
const addRules = {
  orderCount: [
    { required: true, message: "请输入单数", trigger: "blur" },
    { validator: positiveIntegerValidator, trigger: "blur" },
  ],
  commissionMultiple: [
    { required: true, message: "请输入返佣倍数", trigger: "blur" },
    { validator: positiveIntegerValidator, trigger: "blur" },
  ],
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

async function fetchGoodsTypes(session = drawerSession, targetUserId = props.userId) {
  if (goodsTypes.value.length) return;
  const requestSequence = ++goodsTypeRequestSequence;
  try {
    const response = await typeList();
    if (
      requestSequence !== goodsTypeRequestSequence
      || !addDrawerVisible.value
      || !isCurrentDrawerRequest(session, targetUserId)
    ) {
      return;
    }
    const records = Array.isArray(response?.data)
      ? response.data
      : normalizeTableResponse(response).rows;
    goodsTypes.value = records;
  } catch (error) {
    if (requestSequence === goodsTypeRequestSequence && addDrawerVisible.value) {
      proxy.$modal.msgError(failureMessage(error, "商品类目加载失败"));
    }
  }
}

async function fetchGoods(session = drawerSession, targetUserId = props.userId) {
  const requestSequence = ++goodsRequestSequence;
  const [beginPrice, endPrice] = goodsQuery.priceRange || [];
  goodsLoading.value = true;
  try {
    const response = await listGoods({
      pageNum: goodsQuery.pageNum,
      pageSize: goodsQuery.pageSize,
      title: goodsQuery.title,
      typeId: goodsQuery.typeId,
      isEnabled: "0",
      orderByColumn: "g.price",
      isAsc: "desc",
      params: {
        beginPrice,
        endPrice,
      },
    });
    if (
      requestSequence !== goodsRequestSequence
      || !addDrawerVisible.value
      || !isCurrentDrawerRequest(session, targetUserId)
    ) {
      return;
    }
    const result = normalizeTableResponse(response);
    goodsList.value = result.rows;
    goodsTotal.value = result.total;
  } catch (error) {
    if (requestSequence === goodsRequestSequence && addDrawerVisible.value) {
      goodsList.value = [];
      goodsTotal.value = 0;
      proxy.$modal.msgError(failureMessage(error, "商品列表加载失败"));
    }
  } finally {
    if (requestSequence === goodsRequestSequence) {
      goodsLoading.value = false;
    }
  }
}

function handleGoodsPageChange({ page, pageSize }) {
  goodsQuery.pageNum = page;
  goodsQuery.pageSize = pageSize;
  fetchGoods(drawerSession, props.userId);
}

function handleGoodsQuery() {
  goodsQuery.pageNum = 1;
  fetchGoods(drawerSession, props.userId);
}

function resetGoodsQuery() {
  Object.assign(goodsQuery, {
    pageNum: 1,
    pageSize: 10,
    title: "",
    typeId: undefined,
    priceRange: [null, null],
  });
  fetchGoods(drawerSession, props.userId);
}

function goodsCustomRow(record) {
  return {
    onClick: () => addProductToDetails(record),
  };
}

function goodsRowClass(record) {
  return details.value.some((item) => sameId(item.id, record.id))
    ? "goods-row-selected"
    : "goods-row-clickable";
}

function defaultPriceType() {
  return priceTypeOptions.value.find((item) => item.label === "商品价格")?.value
    ?? priceTypeOptions.value[0]?.value
    ?? "0";
}

function addProductToDetails(record) {
  if (saving.value) return;
  if (details.value.some((item) => sameId(item.id, record.id))) {
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
  if (saving.value) return;
  details.value = details.value.filter((item) => !sameId(item.id, record.id));
}

function startDetailEdit(record) {
  if (saving.value) return;
  record.originalPriceType = record.priceType;
  record.originalPrice = record.price;
  record.isEditing = true;
}

function saveDetail(record) {
  if (saving.value) return;
  if (!priceTypeOptions.value.some((option) => sameId(option.value, record.priceType))) {
    proxy.$modal.msgError("请选择有效价格类型");
    return;
  }
  if (!Number.isFinite(Number(record.price)) || Number(record.price) < 0) {
    proxy.$modal.msgError("请输入有效价格");
    return;
  }
  record.isEditing = false;
  record.originalPriceType = undefined;
  record.originalPrice = undefined;
}

function cancelDetailEdit(record) {
  if (saving.value) return;
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

function resetGoodsState() {
  goodsRequestSequence += 1;
  goodsList.value = [];
  goodsTotal.value = 0;
  goodsLoading.value = false;
  Object.assign(goodsQuery, {
    pageNum: 1,
    pageSize: 10,
    title: "",
    typeId: undefined,
    priceRange: [null, null],
  });
}

let addTargetUserId = null;
let addTargetSession = 0;
let addDrawerSequence = 0;

async function handleAdd() {
  if (addOpening.value || saving.value) return;
  if (!props.userId || !isCurrentDrawerRequest(drawerSession, props.userId)) {
    proxy.$modal.msgWarning("请先选择会员");
    return;
  }

  const session = drawerSession;
  const targetUserId = props.userId;
  const editorSequence = ++addDrawerSequence;
  addTargetSession = session;
  addTargetUserId = targetUserId;
  resetGoodsState();
  resetAddForm();
  addDrawerVisible.value = true;
  addOpening.value = true;
  try {
    await Promise.all([
      fetchUser(targetUserId, session),
      fetchGoodsTypes(session, targetUserId),
      fetchGoods(session, targetUserId),
    ]);
  } finally {
    if (
      editorSequence === addDrawerSequence
      && addTargetSession === session
      && sameId(addTargetUserId, targetUserId)
    ) {
      addOpening.value = false;
    }
  }
}

function isCurrentAddSession(session, targetUserId, editorSequence) {
  return addDrawerVisible.value
    && editorSequence === addDrawerSequence
    && session === addTargetSession
    && sameId(targetUserId, addTargetUserId)
    && isCurrentDrawerRequest(session, targetUserId);
}

function resetAddDrawerState() {
  addDrawerSequence += 1;
  addDrawerVisible.value = false;
  addOpening.value = false;
  addTargetUserId = null;
  addTargetSession = 0;
  resetGoodsState();
  addFormRef.value?.resetFields();
  resetAddForm();
}

function closeAddDrawer() {
  if (saving.value) {
    addDrawerVisible.value = true;
    return;
  }
  resetAddDrawerState();
}

async function submitAdd() {
  if (addOpening.value) return;
  const mutationToken = addMutationGuard.acquire();
  if (mutationToken == null) return;
  const session = addTargetSession;
  const targetUserId = addTargetUserId;
  const editorSequence = addDrawerSequence;
  let succeeded = false;
  try {
    try {
      await addFormRef.value?.validate();
    } catch {
      return;
    }

    if (!isCurrentAddSession(session, targetUserId, editorSequence)) {
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

    const hasInvalidDetail = details.value.some((item) => (
      item.id == null
      || !priceTypeOptions.value.some((option) => sameId(option.value, item.priceType))
      || !Number.isFinite(Number(item.price))
      || Number(item.price) < 0
    ));
    if (hasInvalidDetail) {
      proxy.$modal.msgError("商品明细包含无效的价格类型或价格");
      return;
    }

    const payload = {
      userId: targetUserId,
      orderCount: Number(form.orderCount),
      commissionMultiple: Number(form.commissionMultiple),
      status: "1",
      details: details.value.map((item) => ({
        goodsId: item.id,
        priceType: String(item.priceType),
        price: item.price,
      })),
    };
    await addOrderlink(payload);
    if (isCurrentAddSession(session, targetUserId, editorSequence)) {
      proxy.$modal.msgSuccess("新增成功");
      await getList(targetUserId, session);
    }
    emit("success");
    succeeded = true;
  } catch (error) {
    if (isCurrentAddSession(session, targetUserId, editorSequence)) {
      proxy.$modal.msgError(failureMessage(error, "新增失败"));
    }
  } finally {
    addMutationGuard.release(mutationToken);
  }
  if (succeeded && isCurrentAddSession(session, targetUserId, editorSequence)) {
    resetAddDrawerState();
  }
}

function resetDrawerState() {
  listRequestSequence += 1;
  userRequestSequence += 1;
  summaryRequestSequence += 1;
  goodsRequestSequence += 1;
  goodsTypeRequestSequence += 1;
  clearSelection();
  resetQueryState();
  orderlinkList.value = [];
  total.value = 0;
  loading.value = false;
  summaryLoading.value = false;
  loadingUser.value = false;
  resetUser();
  resetEditModalState();
  resetAddDrawerState();
}

function beginDrawerSession(targetUserId) {
  drawerSession += 1;
  resetDrawerState();
  if (targetUserId) {
    refreshAll(targetUserId, drawerSession);
  }
}

watch(
  [() => props.modelValue, () => props.userId],
  ([open, targetUserId], [wasOpen, previousUserId] = []) => {
    if (open && targetUserId) {
      if (!wasOpen || !sameId(targetUserId, previousUserId)) {
        beginDrawerSession(targetUserId);
      }
      return;
    }

    drawerSession += 1;
    resetDrawerState();
  },
  { immediate: true },
);
</script>

<style scoped>
:global(.orderlink-drawer-root) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
}

:global(.orderlink-drawer-root .ant-drawer-header) {
  font-size: 16px;
  line-height: 24px;
}

:global(.orderlink-drawer-root .ant-drawer-title) {
  font-size: 16px;
  line-height: 24px;
}

.orderlink-page,
.orderlink-editor {
  min-width: 0;
}

.orderlink-page,
.orderlink-page :deep(*) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
}

.orderlink-page :deep(.ant-pro-search-card .ant-card-body) {
  padding: 24px 0;
}

.orderlink-page :deep(.ant-pro-table-card .ant-card-body) {
  padding: 0;
}

.orderlink-page :deep(.ant-pro-table-toolbar) {
  min-height: 96px;
}

.orderlink-page :deep(.ant-pro-table-title) {
  flex: 0 0 50%;
  min-width: 0;
  color: rgba(0, 0, 0, 0.88);
}

.orderlink-page :deep(.ant-pro-table-actions) {
  flex: 1 1 50%;
  min-width: 0;
  justify-content: flex-end;
}

.orderlink-query-form :deep(.ant-form-item) {
  flex-wrap: nowrap;
  margin-bottom: 0;
}

.orderlink-query-form :deep(.ant-form-item-label) {
  flex: 0 0 80px;
}

.orderlink-query-form :deep(.ant-form-item-control) {
  flex: 1 1 0;
  max-width: calc(100% - 80px);
}

.orderlink-advanced-query-row {
  margin-top: 16px;
}

.orderlink-expand-btn {
  padding-right: 0;
}

.member-summary {
  padding: 0;
  margin: 0;
  color: rgba(0, 0, 0, 0.88);
  font-size: 16px;
  font-weight: 700;
  line-height: 16px;
  background: transparent;
  border: 0;
}

.member-summary > span {
  white-space: nowrap;
}

.member-summary-field {
  margin-right: 10px;
}

.member-summary-tip {
  color: #ff0000;
  font-size: 16px;
  font-weight: 700;
  line-height: 16px;
}

.summary-refresh {
  color: #1677ff;
}

.orderlink-page :deep(.ant-table-thead > tr > th),
.orderlink-page :deep(.ant-table-tbody > tr > td) {
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "微软雅黑", Arial, sans-serif;
  font-size: 15px;
  line-height: 23.5714px;
}

.orderlink-page :deep(.ant-table-thead > tr > th) {
  height: 48.36px;
  padding: 12px 8px;
  font-weight: 600;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.orderlink-page :deep(.ant-table-column-sorters),
.orderlink-page :deep(.ant-table-column-sorter) {
  height: 23.5625px;
}

.orderlink-page :deep(.ant-table-tbody > tr > td) {
  padding: 12px 8px;
  border-bottom: 1px solid #f0f0f0;
}

.orderlink-page :deep(.ant-table-body) {
  min-height: calc(100vh - 440px);
}

.orderlink-page :deep(.ant-table-body::-webkit-scrollbar) {
  width: 15px;
  height: 15px;
}

.orderlink-page :deep(.ant-table-cell-scrollbar) {
  width: 15px !important;
}

.orderlink-page :deep(.ant-table-header col:last-child) {
  width: 15px !important;
}

.orderlink-page :deep(.ant-table-expanded-row-fixed) {
  position: static !important;
  width: auto !important;
  padding: 0 !important;
  margin: 0 !important;
  overflow: visible !important;
}

.orderlink-page :deep(.ant-empty) {
  margin: 32px 8px;
  color: rgba(0, 0, 0, 0.45);
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "微软雅黑", Arial, sans-serif;
  font-size: 15px;
  line-height: 23.5714px;
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

@media (max-width: 1599px) {
  .orderlink-query-form:not(.query-expanded) .orderlink-product-title-query {
    display: none;
  }
}

</style>
