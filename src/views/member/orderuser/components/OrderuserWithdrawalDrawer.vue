<template>
  <a-drawer
    v-model:open="visible"
    title="修改提现账户"
    width="85%"
    :destroy-on-close="false"
    :mask-closable="!submitting && !deleting"
    :closable="!submitting && !deleting"
    :keyboard="!submitting && !deleting"
    @close="handleClose"
  >
    <div class="drawer-table-wrap ant-pro-member-page">
      <ant-pro-table
        title="提现账户列表"
        :columns="withdrawalColumns"
        :data-source="withdrawalAccList"
        :loading="loading"
        row-key="id"
        :row-selection="rowSelection"
        :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
        :scroll="{ x: 1300 }"
        @page-change="handleAntPageChange"
        @refresh="getList"
      >
        <template #search>
          <a-form layout="horizontal" :model="queryParams">
            <a-row :gutter="24" align="middle">
              <a-col :span="7">
                <a-form-item label="类型">
                  <a-select v-model:value="queryParams.type" placeholder="请选择类型" allow-clear>
                    <a-select-option v-for="dict in order_zhlx" :key="dict.value" :value="dict.value">
                      {{ dict.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="7">
                <a-form-item label="是否默认">
                  <a-select v-model:value="queryParams.isDefault" placeholder="请选择是否默认" allow-clear>
                    <a-select-option v-for="dict in user_yes_no" :key="dict.value" :value="dict.value">
                      {{ dict.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="10" class="ant-pro-query-actions">
                <a-space>
                  <a-button @click="resetQuery">重 置</a-button>
                  <a-button type="primary" @click="handleQuery">查 询</a-button>
                </a-space>
              </a-col>
            </a-row>
          </a-form>
        </template>

        <template #toolbar>
          <a-space>
            <a-button type="primary" @click="handleAdd" v-hasPermi="['member:withdrawalAcc:add']">新 增</a-button>
            <a-button :disabled="single" @click="handleUpdate" v-hasPermi="['member:withdrawalAcc:edit']">修 改</a-button>
            <a-button
              danger
              :disabled="multiple || deleting"
              :loading="deleting"
              @click="handleDelete()"
              v-hasPermi="['member:withdrawalAcc:remove']"
            >删 除</a-button>
          </a-space>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'params'">
            <div v-if="record.type === '0'" class="table-detail-cell">
              <div>银行名称: {{ record.bankName }}</div>
              <div>存款种类: {{ record.depositType }}</div>
              <div>支行代码: {{ record.branchCode }}</div>
              <div>支行名称: {{ record.branchName }}</div>
              <div>银行账号: {{ record.bankAccount }}</div>
              <div>账户持有人: {{ record.accountHolder }}</div>
            </div>
            <div v-else-if="record.type === '1'" class="table-detail-cell">
              <div>账户名称: {{ record.accountName }}</div>
              <div>钱包名称: {{ record.walletName }}</div>
              <div>钱包地址: {{ record.walletAddress }}</div>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'attachment'">
            <a-image
              v-if="record.attachment"
              :src="record.attachment"
              :width="64"
            />
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'isDefault'">
            <dict-tag :options="user_yes_no" :value="record.isDefault" />
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ record.createTime ? parseTime(record.createTime) : "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['member:withdrawalAcc:edit']">修改</a-button>
              <a-button
                type="link"
                danger
                size="small"
                :disabled="deleting"
                :loading="deleting"
                @click="handleDelete(record)"
                v-hasPermi="['member:withdrawalAcc:remove']"
              >删除</a-button>
            </a-space>
          </template>
        </template>
      </ant-pro-table>

      <a-drawer
        v-model:open="open"
        :title="title"
        width="85%"
        :destroy-on-close="false"
        :mask-closable="!submitting"
        :closable="!submitting"
        :keyboard="!submitting"
        @close="handleFormClose"
      >
        <a-form ref="withdrawalAccRef" :model="form" :rules="rules" layout="vertical">
          <a-row :gutter="[20, 0]">
            <a-col :span="8">
              <a-form-item label="类型" name="type">
                <a-radio-group v-model:value="form.type" @change="handleTypeChange">
                  <a-radio v-for="dict in order_zhlx" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="出金类型" name="withdrawalTypeId">
                <a-radio-group v-model:value="form.withdrawalTypeId">
                  <a-radio v-for="dict in formWithdrawalTypes" :key="dict.id" :value="String(dict.id)">
                    {{ dict.name }}
                  </a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="是否默认" name="isDefault">
                <a-radio-group v-model:value="form.isDefault">
                  <a-radio v-for="dict in user_yes_no" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>

            <template v-if="form.type === '0'">
              <a-col :span="8">
                <a-form-item label="银行名称" name="bankName">
                  <a-input v-model:value="form.bankName" placeholder="请输入银行名称" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="存款种类" name="depositType">
                  <a-input v-model:value="form.depositType" placeholder="请输入存款种类" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="支行代码" name="branchCode">
                  <a-input v-model:value="form.branchCode" placeholder="请输入支行代码" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="支行名称" name="branchName">
                  <a-input v-model:value="form.branchName" placeholder="请输入支行名称" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="银行账号" name="bankAccount">
                  <a-input v-model:value="form.bankAccount" placeholder="请输入银行账号" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="账户持有人" name="accountHolder">
                  <a-input v-model:value="form.accountHolder" placeholder="请输入账户持有人" allow-clear />
                </a-form-item>
              </a-col>
            </template>

            <template v-if="form.type === '1'">
              <a-col :span="24">
                <a-form-item label="账户名称" name="accountName">
                  <a-input v-model:value="form.accountName" placeholder="请输入账户名称" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="钱包名称" name="walletName">
                  <a-input v-model:value="form.walletName" placeholder="请输入钱包名称" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="钱包地址" name="walletAddress">
                  <a-input v-model:value="form.walletAddress" placeholder="请输入钱包地址" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="附件" name="attachment">
                  <image-upload v-model="form.attachment" :limit="1" :is-show-tip="false" />
                </a-form-item>
              </a-col>
            </template>
          </a-row>
        </a-form>
        <template #footer>
          <div class="drawer-footer">
            <a-space>
              <a-button :disabled="submitting" @click="cancel">取 消</a-button>
              <a-button type="primary" :loading="submitting" @click="submitForm">确 定</a-button>
            </a-space>
          </div>
        </template>
      </a-drawer>
    </div>
  </a-drawer>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref, toRefs, watch } from "vue";
import {
  listWithdrawalAcc,
  getWithdrawalAcc,
  delWithdrawalAcc,
  addWithdrawalAcc,
  updateWithdrawalAcc,
  getType,
} from "@/api/member/withdrawalAcc";
import { resolveDeleteIds } from "@/utils/management-rules";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  userId: {
    type: [String, Number],
    default: null,
  },
});

const emit = defineEmits(["update:modelValue", "success"]);

const { proxy } = getCurrentInstance();
const { user_yes_no, order_zhlx } = proxy.useDict("user_yes_no", "order_zhlx");

const visible = ref(props.modelValue);
watch(
  () => props.modelValue,
  (v) => {
    visible.value = v;
  }
);
watch(visible, (v) => {
  emit("update:modelValue", v);
});

const loading = ref(false);
const submitting = ref(false);
const deleting = ref(false);
const withdrawalAccList = ref([]);
const total = ref(0);
const withdrawalAccRef = ref(null);
let listRequestToken = 0;
let detailRequestToken = 0;
const typeRequestTokens = {
  form: 0,
  query: 0,
};

const withdrawalColumns = [
  { title: "ID", dataIndex: "id", align: "center", width: 90 },
  { title: "出金类型", dataIndex: "withdrawalType", align: "center", width: 140 },
  { title: "参数", dataIndex: "params", align: "left", width: 320 },
  { title: "附件", dataIndex: "attachment", align: "center", width: 110 },
  { title: "是否默认", dataIndex: "isDefault", align: "center", width: 120 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "操作", dataIndex: "action", align: "center", width: 140, fixed: "right" },
];
const formWithdrawalTypes = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    type: null,
    isDefault: null,
  },
  rules: {},
});

const { queryParams, form, rules } = toRefs(data);

function clearSelection() {
  ids.value = [];
  single.value = true;
  multiple.value = true;
}

function getList() {
  const userId = props.userId;
  if (!visible.value || userId === null || userId === undefined || userId === "") {
    listRequestToken += 1;
    withdrawalAccList.value = [];
    total.value = 0;
    loading.value = false;
    clearSelection();
    return;
  }

  const requestToken = ++listRequestToken;
  clearSelection();
  loading.value = true;
  const params = {
    ...queryParams.value,
    userId,
  };
  return listWithdrawalAcc(params)
    .then((response) => {
      if (
        requestToken !== listRequestToken
        || !visible.value
        || String(props.userId) !== String(userId)
      ) return;
      withdrawalAccList.value = response.rows ?? response.data?.rows ?? [];
      total.value = response.total ?? response.data?.total ?? 0;
    })
    .catch(() => {})
    .finally(() => {
      if (requestToken === listRequestToken) loading.value = false;
    });
}

async function loadWithdrawalTypes(value, targetRef = formWithdrawalTypes, scope = "form") {
  const requestToken = ++typeRequestTokens[scope];
  if (value === null || value === undefined) {
    targetRef.value = [];
    return;
  }
  try {
    const response = await getType({ type: value });
    if (requestToken !== typeRequestTokens[scope]) return;
    const allTypes = (response.data ?? response.rows ?? response) || [];
    targetRef.value = allTypes.filter((item) => String(item.type) === String(value));
  } catch (error) {
    if (requestToken !== typeRequestTokens[scope]) return;
    console.error("Failed to fetch withdrawal types:", error);
    targetRef.value = [];
  }
}

async function handleTypeChange(eventOrValue) {
  const value = eventOrValue?.target?.value ?? eventOrValue;
  form.value.withdrawalType = null;
  form.value.withdrawalTypeId = null;
  if (value === "0") {
    form.value.accountName = null;
    form.value.walletName = null;
    form.value.walletAddress = null;
    form.value.attachment = null;
  } else if (value === "1") {
    form.value.bankName = null;
    form.value.depositType = null;
    form.value.branchCode = null;
    form.value.branchName = null;
    form.value.bankAccount = null;
    form.value.accountHolder = null;
  }
  await loadWithdrawalTypes(value, formWithdrawalTypes, "form");
  updateRules();
  withdrawalAccRef.value?.clearValidate?.();
}

function updateRules() {
  const newRules = {
    type: [{ required: true, message: "类型不能为空", trigger: "change" }],
    isDefault: [{ required: true, message: "是否默认不能为空", trigger: "change" }],
    withdrawalTypeId: [{ required: true, message: "出金类型不能为空", trigger: "change" }],
  };

  if (form.value.type === "0") {
    Object.assign(newRules, {
      bankName: [{ required: true, message: "银行名称不能为空", trigger: "blur" }],
      bankAccount: [{ required: true, message: "银行账号不能为空", trigger: "blur" }],
      accountHolder: [{ required: true, message: "账户持有人不能为空", trigger: "blur" }],
    });
  } else if (form.value.type === "1") {
    Object.assign(newRules, {
      walletAddress: [{ required: true, message: "钱包地址不能为空", trigger: "blur" }],
    });
  }

  rules.value = newRules;
}

function cancel() {
  if (submitting.value) return;
  open.value = false;
  reset();
  formWithdrawalTypes.value = [];
}

function handleFormClose() {
  if (submitting.value) return;
  detailRequestToken += 1;
  typeRequestTokens.form += 1;
  reset();
  formWithdrawalTypes.value = [];
}

function handleClose() {
  open.value = false;
  resetDrawerState();
}

function createDefaultForm(userId = null) {
  return {
    id: null,
    userId,
    type: null,
    withdrawalType: null,
    withdrawalTypeId: null,
    isDefault: "1",
    bankName: null,
    depositType: null,
    branchCode: null,
    branchName: null,
    bankAccount: null,
    accountHolder: null,
    accountName: null,
    walletName: null,
    walletAddress: null,
    attachment: null,
    createTime: null,
  };
}

function reset() {
  form.value = createDefaultForm();
  updateRules();
  withdrawalAccRef.value?.clearValidate?.();
}

function resetQueryState(userId = null) {
  Object.assign(queryParams.value, {
    pageNum: 1,
    pageSize: 10,
    userId,
    type: null,
    isDefault: null,
  });
}

function resetDrawerState(userId = null) {
  listRequestToken += 1;
  detailRequestToken += 1;
  typeRequestTokens.form += 1;
  typeRequestTokens.query += 1;
  loading.value = false;
  open.value = false;
  reset();
  resetQueryState(userId);
  clearSelection();
  withdrawalAccList.value = [];
  total.value = 0;
  formWithdrawalTypes.value = [];
  title.value = "";
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  resetQueryState(props.userId);
  handleQuery();
}

const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_, selectedRows) => handleSelectionChange(selectedRows),
}));

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const open = ref(false);
const title = ref("");

async function handleAdd() {
  const userId = props.userId;
  if (userId === null || userId === undefined || userId === "") return;
  detailRequestToken += 1;
  reset();
  form.value.userId = userId;
  await loadWithdrawalTypes(form.value.type, formWithdrawalTypes, "form");
  if (!visible.value || String(props.userId) !== String(userId)) return;
  open.value = true;
  title.value = "创建";
}

async function handleUpdate(row = {}) {
  reset();
  const selectedId = Array.isArray(ids.value) ? ids.value[0] : ids.value;
  const targetId = row.id ?? selectedId;
  if (targetId === null || targetId === undefined) return;
  const userId = props.userId;
  if (userId === null || userId === undefined || userId === "") return;
  const requestToken = ++detailRequestToken;
  let response;
  try {
    response = await getWithdrawalAcc(targetId);
  } catch {
    return;
  }
  if (
    requestToken !== detailRequestToken
    || !visible.value
    || String(props.userId) !== String(userId)
  ) return;
  const record = response.data ?? response;
  form.value = {
    ...createDefaultForm(userId),
    id: record.id,
    userId: userId ?? record.userId,
    type: record.type == null ? null : String(record.type),
    withdrawalTypeId: record.withdrawalTypeId == null
      ? null
      : String(record.withdrawalTypeId),
    isDefault: String(record.isDefault ?? "1"),
    bankName: record.bankName ?? null,
    depositType: record.depositType ?? null,
    branchCode: record.branchCode ?? null,
    branchName: record.branchName ?? null,
    bankAccount: record.bankAccount ?? null,
    accountHolder: record.accountHolder ?? null,
    accountName: record.accountName ?? null,
    walletName: record.walletName ?? null,
    walletAddress: record.walletAddress ?? null,
    attachment: record.attachment ?? null,
  };
  await loadWithdrawalTypes(form.value.type, formWithdrawalTypes, "form");
  if (
    requestToken !== detailRequestToken
    || !visible.value
    || String(props.userId) !== String(userId)
  ) return;
  updateRules();
  withdrawalAccRef.value?.clearValidate?.();
  open.value = true;
  title.value = "修改提现账户";
}

function buildWithdrawalPayload() {
  const payload = {
    id: form.value.id,
    userId: props.userId,
    type: form.value.type,
    withdrawalTypeId: form.value.withdrawalTypeId,
    isDefault: form.value.isDefault,
  };
  if (form.value.type === "0") {
    Object.assign(payload, {
      bankName: form.value.bankName,
      depositType: form.value.depositType,
      branchCode: form.value.branchCode,
      branchName: form.value.branchName,
      bankAccount: form.value.bankAccount,
      accountHolder: form.value.accountHolder,
    });
  } else if (form.value.type === "1") {
    Object.assign(payload, {
      accountName: form.value.accountName,
      walletName: form.value.walletName,
      walletAddress: form.value.walletAddress,
      attachment: form.value.attachment,
    });
  }
  return payload;
}

async function submitForm() {
  if (submitting.value || !withdrawalAccRef.value) return;
  submitting.value = true;
  try {
    await withdrawalAccRef.value.validate();
    const payload = buildWithdrawalPayload();
    if (payload.id != null) {
      await updateWithdrawalAcc(payload);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addWithdrawalAcc(payload);
      proxy.$modal.msgSuccess("新增成功");
    }
    open.value = false;
    reset();
    await getList();
    emit("success");
  } catch {
    // Form validation and request errors are already surfaced by their owners.
  } finally {
    submitting.value = false;
  }
}

function handleDelete(row) {
  if (deleting.value) return;
  const _ids = resolveDeleteIds(row, ids.value);
  if (!_ids.length) {
    proxy.$modal.msgWarning("请选择要删除的数据");
    return;
  }
  deleting.value = true;
  proxy.$modal
    .confirm(`是否确认删除提现账户编号为"${_ids}"的数据项？`)
    .then(() => delWithdrawalAcc(Array.isArray(_ids) ? _ids.join(",") : _ids))
    .then(() => {
      handleSelectionChange([]);
      getList();
      proxy.$modal.msgSuccess("删除成功");
      emit("success");
    })
    .catch(() => {})
    .finally(() => {
      deleting.value = false;
    });
}

function handleExport() {
  proxy.download(
    "member/withdrawalAcc/export",
    {
      ...queryParams.value,
    },
    `withdrawalAcc_${new Date().getTime()}.xlsx`
  );
}

watch(
  () => props.userId,
  (id, previousId) => {
    if (!visible.value || String(id) === String(previousId)) return;
    resetDrawerState(id ?? null);
    if (id != null) getList();
  }
);

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.userId != null) {
      resetDrawerState(props.userId);
      getList();
    } else {
      resetDrawerState();
    }
  },
  { immediate: true }
);

</script>

<style scoped>
.drawer-footer {
  text-align: right;
}
</style>
