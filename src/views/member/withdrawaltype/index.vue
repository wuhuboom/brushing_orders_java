<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="出金类型"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="pagination"
      :scroll="{ x: 1750 }"
      @page-change="handlePageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="query" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="5">
              <a-form-item label="类型">
                <a-select v-model:value="query.type" allow-clear placeholder="请选择">
                  <a-select-option v-for="item in typeOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="5">
              <a-form-item label="名称">
                <a-input v-model:value="query.name" allow-clear placeholder="请输入" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="16" :md="10" :lg="8">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="createTimeRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['请选择', '请选择']"
                  style="width: 100%"
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
        <a-popconfirm
          title="删除选中的记录？"
          ok-text="确定"
          cancel-text="取消"
          :disabled="!selectedIds.length"
          @confirm="handleDelete"
        >
          <a-button danger :disabled="!selectedIds.length" v-hasPermi="['member:withdrawaltype:remove']">
            <DeleteOutlined />删除
          </a-button>
        </a-popconfirm>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:withdrawaltype:add']">
          <PlusOutlined />创建
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">{{ typeText(record.type) }}</template>
        <template v-else-if="column.key === 'privateKey'">{{ privateKeyText(record.hasPrivateKey) }}</template>
        <template v-else-if="column.key === 'icon'">
          <image-preview v-if="record.icon" :src="record.icon" :width="48" :height="48" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'parameters'">
          <a-typography-text :ellipsis="{ tooltip: parameterSummary(record) }" style="max-width: 250px">
            {{ parameterSummary(record) || "-" }}
          </a-typography-text>
        </template>
        <template v-else-if="column.key === 'createTime'">{{ formatDateTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:withdrawaltype:edit']">修改</a-button>
            <a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:withdrawaltype:add']">复制</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      v-model:open="open"
      :title="title"
      width="900px"
      ok-text="确定"
      cancel-text="取消"
      :confirm-loading="submitting"
      :destroy-on-close="true"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="20">
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
              <a-input v-model:value="form.name" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="汇率" name="exchangeRate">
              <a-input v-model:value="form.exchangeRate" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="序号" name="sortOrder">
              <a-input-number v-model:value="form.sortOrder" :precision="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="图标" name="icon">
              <image-upload v-model="form.icon" :limit="1" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row v-if="form.type === '0'" :gutter="20">
          <a-col v-for="field in bankFields" :key="field.prop" :span="12">
            <a-form-item :label="field.label" :name="field.prop">
              <a-input v-model:value="form[field.prop]" placeholder="请输入" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row v-if="form.type === '1'" :gutter="20">
          <a-col :span="12">
            <a-form-item label="接口服务地址" name="serviceUrl">
              <a-input v-model:value="form.serviceUrl" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="收款钱包地址" name="walletAddress">
              <a-input v-model:value="form.walletAddress" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="货币合约地址" name="contractAddress">
              <a-input v-model:value="form.contractAddress" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Abi" name="abi">
              <a-input v-model:value="form.abi" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="网络" name="networkName">
              <a-input v-model:value="form.networkName" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手续费钱包地址" name="feeWalletAddress">
              <a-input v-model:value="form.feeWalletAddress" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="手续费私钥（保存以后不可查看）" name="feePrivateKey">
              <a-input-password v-model:value="form.feePrivateKey" placeholder="请输入" autocomplete="new-password" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="授权金额" name="authorizationAmount">
              <a-input-number v-model:value="form.authorizationAmount" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手续费价格（默认：2000000000）" name="feePrice">
              <a-input-number v-model:value="form.feePrice" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手续费限制（默认：300000）" name="feeLimit">
              <a-input-number v-model:value="form.feeLimit" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="钱包名称" name="walletName">
              <a-input v-model:value="form.walletName" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="账户名称" name="accountName">
              <a-input v-model:value="form.accountName" placeholder="请输入" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="form.remarks" :rows="3" placeholder="请输入" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="Withdrawaltype">
import { DeleteOutlined, PlusOutlined } from "@ant-design/icons-vue";
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
  { label: "银行卡", value: "0" },
  { label: "网络", value: "1" },
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
const total = ref(0);
const selectedIds = ref([]);
const createTimeRange = ref([]);
const formRef = ref();
const query = reactive({ pageNum: 1, pageSize: 20, type: undefined, name: undefined });
const form = reactive(emptyForm());

const columns = [
  { title: "ID", dataIndex: "id", width: 90 },
  { title: "类型", dataIndex: "type", key: "type", width: 110 },
  { title: "名称", dataIndex: "name", width: 150 },
  { title: "汇率", dataIndex: "exchangeRate", width: 110 },
  { title: "序号", dataIndex: "sortOrder", width: 90 },
  { title: "是否配置私钥", dataIndex: "hasPrivateKey", key: "privateKey", width: 140 },
  { title: "图标", dataIndex: "icon", key: "icon", width: 100 },
  { title: "参数", key: "parameters", width: 280 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "备注", dataIndex: "remarks", width: 180 },
  { title: "操作", key: "operation", width: 140, fixed: "right" },
];
const pagination = computed(() => ({
  current: query.pageNum,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: false,
  showQuickJumper: false,
}));
const rowSelection = computed(() => ({
  selectedRowKeys: selectedIds.value,
  onChange: (keys) => { selectedIds.value = keys; },
}));
const rules = {
  type: [{ required: true, message: "类型是必填项！", trigger: "change" }],
  name: [{ required: true, message: "标题是必填项！", trigger: "blur" }],
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
function queryPayload() {
  const payload = { ...query };
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
    rows.value = response.rows || [];
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
  const fields = String(record.type) === "0"
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
function handleQuery() {
  query.pageNum = 1;
  getList();
}
function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 20, type: undefined, name: undefined });
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
  return response.data || {};
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
    const payload = { ...form };
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
