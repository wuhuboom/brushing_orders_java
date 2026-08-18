<template>
  <div class="app-container ant-pro-member-page site-management-alignment-page withdrawal-type-page">
    <ant-pro-table
      title="出金类型"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="pagination"
      :scroll="{ x: 1722, y: 'calc(100vh - 440px)' }"
      @page-change="handlePageChange"
      @refresh="getList"
      @change="handleTableChange"
    >
      <template #search>
        <a-form layout="horizontal" :model="query" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="类型">
                <a-select v-model:value="query.type" allow-clear placeholder="请选择">
                  <a-select-option v-for="item in typeOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="名称">
                <a-input v-model:value="query.name" allow-clear placeholder="请输入" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col v-if="expanded" :xs="24" :sm="16" :md="10" :lg="8">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="createTimeRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['请选择', '请选择']"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重置</a-button>
                <a-button type="primary" @click="handleQuery">查询</a-button>
                <a-button type="link" @click="expanded = !expanded">
                  {{ expanded ? "收起" : "展开" }}
                  <UpOutlined v-if="expanded" />
                  <DownOutlined v-else />
                </a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-popconfirm
          title="删除选中的记录？"
          ok-text="确定"
          cancel-text="取消"
          :disabled="!selectedIds.length"
          @confirm="handleDelete"
        >
          <a-button type="primary" :disabled="!selectedIds.length" v-hasPermi="['member:withdrawaltype:remove']">
            <DeleteOutlined />删除
          </a-button>
        </a-popconfirm>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:withdrawaltype:add']">
          <PlusOutlined />创建
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'id'"><span class="withdrawal-type-id">{{ record.id }}</span></template>
        <template v-else-if="column.key === 'type'">
          <a-badge :status="String(record.type) === '2' ? 'success' : 'processing'" :text="typeText(record.type)" />
        </template>
        <template v-else-if="column.key === 'privateKey'">
          <a-badge :status="String(record.hasPrivateKey) === '1' ? 'success' : 'error'" :text="privateKeyText(record.hasPrivateKey)" />
        </template>
        <template v-else-if="column.key === 'icon'">
          <image-preview v-if="record.icon" :src="record.icon" :width="48" :height="48" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'parameters'">
          <a-typography-text v-if="parameterSummary(record)" :ellipsis="{ tooltip: parameterSummary(record) }" class="withdrawal-parameters">
            {{ parameterSummary(record) }}
          </a-typography-text>
        </template>
        <template v-else-if="column.key === 'createTime'">{{ formatDateTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'remarks'">{{ record.remarks || "-" }}</template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:withdrawaltype:edit']">修改</a-button>
            <a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:withdrawaltype:add']">复制</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-drawer
      v-model:open="open"
      :title="title"
      width="80%"
      size="large"
      :destroy-on-close="true"
      :body-style="{ paddingBottom: '24px' }"
      root-class-name="withdrawal-type-drawer"
      @close="cancel"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large" class="withdrawal-type-form">
        <a-row :gutter="[24, 0]">
          <a-col :span="12">
            <a-form-item label="类型" name="type">
              <a-radio-group v-model:value="form.type">
                <a-radio v-for="item in typeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="form.name" placeholder="名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="汇率" name="exchangeRate">
              <a-input v-model:value="form.exchangeRate" placeholder="汇率" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="序号" name="sortOrder">
              <a-input-number v-model:value="form.sortOrder" :precision="0" placeholder="序号" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="图标" name="icon">
              <image-upload v-model="form.icon" :limit="1" :is-show-tip="false" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row v-if="form.type !== '2'" :gutter="[24, 0]">
          <a-col v-for="field in bankFields" :key="field.prop" :span="12">
            <a-form-item :label="field.label" :name="field.prop">
              <a-input v-model:value="form[field.prop]" :placeholder="field.label" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row v-if="form.type === '2'" :gutter="[24, 0]">
          <a-col :span="24">
            <a-form-item label="接口服务地址" name="serviceUrl">
              <a-input v-model:value="form.serviceUrl" placeholder="接口服务地址" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="收款钱包地址" name="walletAddress">
              <a-input v-model:value="form.walletAddress" placeholder="收款钱包地址" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="货币合约地址" name="contractAddress">
              <a-input v-model:value="form.contractAddress" placeholder="货币合约地址" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="Abi" name="abi">
              <a-textarea v-model:value="form.abi" :rows="4" placeholder="Abi" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="网络" name="networkName">
              <a-textarea v-model:value="form.networkName" :rows="10" placeholder="网络" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="手续费钱包地址" name="feeWalletAddress">
              <a-input v-model:value="form.feeWalletAddress" placeholder="手续费钱包地址" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="手续费私钥（保存以后不可查看）" name="feePrivateKey">
              <a-input-password v-model:value="form.feePrivateKey" placeholder="手续费私钥（保存以后不可查看）" autocomplete="new-password" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="授权金额" name="authorizationAmount">
              <a-input-number v-model:value="form.authorizationAmount" placeholder="授权金额" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="手续费价格（默认：2000000000）" name="feePrice">
              <a-input-number v-model:value="form.feePrice" placeholder="手续费价格（默认：2000000000）" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="手续费限制（默认：300000）" name="feeLimit">
              <a-input-number v-model:value="form.feeLimit" placeholder="手续费限制（默认：300000）" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="钱包名称" name="walletName">
              <a-input v-model:value="form.walletName" placeholder="钱包名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="账户名称" name="accountName">
              <a-input v-model:value="form.accountName" placeholder="账户名称" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="form.remarks" :rows="3" placeholder="备注" />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="withdrawal-type-drawer-footer">
          <a-space>
            <a-button size="large" @click="cancel">取 消</a-button>
            <a-button type="primary" size="large" :loading="submitting" @click="submitForm">确 定</a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup name="Withdrawaltype">
import { DeleteOutlined, DownOutlined, PlusOutlined, UpOutlined } from "@ant-design/icons-vue";
import {
  addWithdrawaltype,
  delWithdrawaltype,
  getWithdrawaltype,
  listWithdrawaltype,
  updateWithdrawaltype,
} from "@/api/member/withdrawaltype";
import ImagePreview from "@/components/ImagePreview/index.vue";
import ImageUpload from "@/components/ImageUpload/index.vue";

const { proxy } = getCurrentInstance();
const typeOptions = [
  { label: "银行卡", value: "1" },
  { label: "网络", value: "2" },
];
const bankFields = [
  { label: "银行名称", prop: "bankName" },
  { label: "存款种类", prop: "depositType" },
  { label: "支行代码", prop: "branchCode" },
  { label: "支行名称", prop: "branchName" },
  { label: "银行账号", prop: "bankAccount" },
  { label: "账户持有人", prop: "accountHolder" },
];

const rows = ref([]);
const loading = ref(false);
const submitting = ref(false);
const open = ref(false);
const title = ref("");
const expanded = ref(false);
const total = ref(0);
const selectedIds = ref([]);
const createTimeRange = ref([]);
const formRef = ref();
const query = reactive({
  pageNum: 1,
  pageSize: 20,
  type: undefined,
  name: undefined,
  orderByColumn: "sortOrder",
  isAsc: "asc",
});
const form = reactive(emptyForm());

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 120, fixed: "left", sorter: true },
  { title: "类型", dataIndex: "type", key: "type", width: 100, sorter: true },
  { title: "名称", dataIndex: "name", width: 120, ellipsis: true },
  { title: "汇率", dataIndex: "exchangeRate", width: 200, ellipsis: true },
  { title: "序号", dataIndex: "sortOrder", key: "sortOrder", width: 100, sorter: true, defaultSortOrder: "ascend" },
  { title: "是否配置私钥", dataIndex: "hasPrivateKey", key: "privateKey", width: 100 },
  { title: "图标", dataIndex: "icon", key: "icon", width: 100 },
  { title: "参数", key: "parameters", width: 400 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 170, sorter: true },
  { title: "备注", dataIndex: "remarks", key: "remarks", width: 200, ellipsis: true },
  { title: "操作", key: "operation", width: 80, fixed: "right" },
];
const pagination = computed(() => ({
  current: query.pageNum,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: false,
  showQuickJumper: false,
  size: "small",
}));
const rowSelection = computed(() => ({
  selectedRowKeys: selectedIds.value,
  columnWidth: 32,
  fixed: true,
  onChange: (keys) => { selectedIds.value = keys; },
}));
const rules = {
  type: [{ required: true, message: "类型是必填项！", trigger: "change" }],
  name: [{ required: true, message: "名称是必填项！", trigger: "blur" }],
  exchangeRate: [{ required: true, message: "汇率是必填项！", trigger: "blur" }],
  sortOrder: [{ required: true, message: "序号是必填项！", trigger: "change" }],
};

function emptyForm() {
  return {
    id: undefined,
    type: undefined,
    name: undefined,
    exchangeRate: undefined,
    sortOrder: undefined,
    icon: undefined,
    bankName: undefined,
    depositType: undefined,
    branchCode: undefined,
    branchName: undefined,
    bankAccount: undefined,
    accountHolder: undefined,
    serviceUrl: undefined,
    walletAddress: undefined,
    contractAddress: undefined,
    abi: undefined,
    networkName: undefined,
    feeWalletAddress: undefined,
    feePrivateKey: undefined,
    hasPrivateKey: "0",
    authorizationAmount: undefined,
    feePrice: undefined,
    feeLimit: undefined,
    walletName: undefined,
    accountName: undefined,
    remarks: undefined,
  };
}
function toApiType(value) {
  return ({ "1": "0", "2": "1" })[String(value)] ?? value;
}
function fromApiType(value) {
  return ({ "0": "1", "1": "2" })[String(value)] ?? value;
}
function fromApiRecord(record) {
  return { ...record, type: fromApiType(record?.type) };
}
function toApiRecord(record) {
  return { ...record, type: toApiType(record?.type) };
}
function queryPayload() {
  const payload = { ...query };
  payload.type = toApiType(payload.type);
  if (createTimeRange.value?.length === 2) {
    payload.params = {
      beginTime: createTimeRange.value[0],
      endTime: createTimeRange.value[1],
    };
  }
  return payload;
}
async function getList() {
  loading.value = true;
  try {
    const response = await listWithdrawaltype(queryPayload());
    rows.value = (response.rows || []).map(fromApiRecord);
    total.value = response.total || 0;
  } finally {
    loading.value = false;
  }
}
function typeText(value) {
  return typeOptions.find((item) => item.value === String(value))?.label ?? "-";
}
function privateKeyText(value) {
  return String(value) === "1" ? "是" : "否";
}
function formatDateTime(value) {
  return value ? proxy.parseTime(value) : "-";
}
function parameterSummary(record) {
  const fields = String(record.type) === "1"
    ? bankFields
    : [
        { label: "接口服务地址", prop: "serviceUrl" },
        { label: "收款钱包地址", prop: "walletAddress" },
        { label: "货币合约地址", prop: "contractAddress" },
        { label: "网络", prop: "networkName" },
        { label: "钱包名称", prop: "walletName" },
        { label: "账户名称", prop: "accountName" },
      ];
  return fields
    .filter((field) => record[field.prop] !== null && record[field.prop] !== undefined && record[field.prop] !== "")
    .map((field) => `${field.label}：${record[field.prop]}`)
    .join("；");
}
function handlePageChange({ page, pageSize }) {
  query.pageNum = page;
  query.pageSize = pageSize;
  getList();
}
const sortColumnMap = { id: "id", type: "type", sortOrder: "sortOrder", createTime: "createTime" };
function handleTableChange(_pagination, _filters, sorter) {
  query.orderByColumn = sorter?.order ? sortColumnMap[sorter.columnKey] || undefined : undefined;
  query.isAsc = sorter?.order === "ascend" ? "asc" : sorter?.order === "descend" ? "desc" : undefined;
  query.pageNum = 1;
  getList();
}
function handleQuery() {
  query.pageNum = 1;
  getList();
}
function resetQuery() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 20,
    type: undefined,
    name: undefined,
    orderByColumn: "sortOrder",
    isAsc: "asc",
  });
  createTimeRange.value = [];
  getList();
}
function resetForm() {
  Object.assign(form, emptyForm());
  nextTick(() => formRef.value?.clearValidate());
}
function handleAdd() {
  resetForm();
  title.value = "创建";
  open.value = true;
}
async function loadRecord(id) {
  const response = await getWithdrawaltype(id);
  return fromApiRecord(response.data || {});
}
async function handleUpdate(row) {
  resetForm();
  const data = await loadRecord(row.id);
  Object.assign(form, data, { feePrivateKey: undefined });
  title.value = "修改";
  open.value = true;
}
async function handleCopy(row) {
  resetForm();
  const data = { ...(await loadRecord(row.id)) };
  delete data.id;
  delete data.createTime;
  delete data.updateTime;
  delete data.feePrivateKey;
  Object.assign(form, data, { hasPrivateKey: "0" });
  title.value = "创建";
  open.value = true;
}
function cancel() {
  open.value = false;
  resetForm();
}
async function submitForm() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    const payload = toApiRecord(form);
    if (form.id) {
      await updateWithdrawaltype(payload);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addWithdrawaltype(payload);
      proxy.$modal.msgSuccess("创建成功");
    }
    open.value = false;
    await getList();
  } finally {
    submitting.value = false;
  }
}
async function handleDelete() {
  await delWithdrawaltype(selectedIds.value.join(","));
  proxy.$modal.msgSuccess("删除成功");
  selectedIds.value = [];
  await getList();
}

getList();
</script>

<style scoped lang="scss">
:global(body:has(.site-management-alignment-page)::-webkit-scrollbar) {
  width: 15px;
}

:global(body:has(.site-management-alignment-page) .copyright) {
  display: none;
}

:global(body:has(.site-management-alignment-page) .app-main) {
  padding-bottom: 0 !important;
}
.withdrawal-type-page {
  margin-top: 44px;
}

.withdrawal-type-page :deep(.ant-table-body) {
  height: calc(100vh - 440px);
}

.withdrawal-type-page :deep(.ant-table-body::-webkit-scrollbar) {
  width: 15px;
  height: 15px;
}

.withdrawal-type-page :deep(.ant-table-thead > tr > th),
.withdrawal-type-page :deep(.ant-table-tbody > tr > td) {
  padding: 12px 8px;
  font-size: 15px;
  line-height: 1.57143;
}

.withdrawal-type-page :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f0f0f0;
}

.withdrawal-type-form :deep(.ant-form-item) {
  margin-bottom: 24px;
}

.withdrawal-type-form :deep(textarea.ant-input) {
  padding: 7px 11px;
  font-size: 16px;
  line-height: 1.5715;
}

.withdrawal-type-id {
  color: #1677ff;
}

.withdrawal-parameters {
  max-width: 370px;
}

.withdrawal-type-form :deep(.ant-input-number) {
  width: 100%;
}

.withdrawal-type-form :deep(.ant-upload-list-picture-card .ant-upload-list-item-container),
.withdrawal-type-form :deep(.ant-upload.ant-upload-select-picture-card) {
  width: 102px;
  height: 102px;
}

.withdrawal-type-form :deep(.ant-upload-list-picture-card) {
  height: 102px;
}

:global(.withdrawal-type-drawer .ant-drawer-header) {
  height: 57px;
  min-height: 57px;
}

:global(.withdrawal-type-drawer .ant-drawer-body::-webkit-scrollbar) {
  width: 15px;
  height: 15px;
}

.withdrawal-type-drawer-footer {
  text-align: right;
}

@media (max-width: 992px) {
  .withdrawal-type-page {
    margin-top: 12px;
  }

  .withdrawal-type-page :deep(.ant-table-body) {
    height: auto;
  }

  :global(.withdrawal-type-drawer .ant-drawer-content-wrapper) {
    width: 100% !important;
  }
}
</style>
