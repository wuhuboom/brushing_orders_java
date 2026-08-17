<template>
  <a-drawer
    v-model:open="visible"
    title="修改提现账户"
    width="90%"
    :destroy-on-close="false"
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
            <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:withdrawalAcc:remove']">删 除</a-button>
          </a-space>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'type'">
            <dict-tag :options="order_zhlx" :value="record.type" />
          </template>
          <template v-else-if="column.dataIndex === 'params'">
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
          <template v-else-if="column.dataIndex === 'isDefault'">
            <dict-tag :options="user_yes_no" :value="record.isDefault" />
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ record.createTime ? parseTime(record.createTime) : "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['member:withdrawalAcc:edit']">修改</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['member:withdrawalAcc:remove']">删除</a-button>
            </a-space>
          </template>
        </template>
      </ant-pro-table>

      <a-drawer v-model:open="open" :title="title" width="80%" :destroy-on-close="false">
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
                  <a-radio v-for="dict in formWithdrawalTypes" :key="dict.id" :value="dict.id">
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
            </template>
          </a-row>
        </a-form>
        <template #footer>
          <div class="drawer-footer">
            <a-space>
              <a-button @click="cancel">取 消</a-button>
              <a-button type="primary" @click="submitForm">确 定</a-button>
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
    type: String,
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
const withdrawalAccList = ref([]);
const total = ref(0);
const withdrawalAccRef = ref(null);

const withdrawalColumns = [
  { title: "ID", dataIndex: "id", align: "center", width: 90 },
  { title: "类型", dataIndex: "type", align: "center", width: 120 },
  { title: "出金类型", dataIndex: "withdrawalType", align: "center", width: 140 },
  { title: "参数", dataIndex: "params", align: "left", width: 320 },
  { title: "是否默认", dataIndex: "isDefault", align: "center", width: 120 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "操作", dataIndex: "action", align: "center", width: 140, fixed: "right" },
];
const queryWithdrawalTypes = ref([]);
const formWithdrawalTypes = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    type: null,
    withdrawalType: null,
    isDefault: null,
    bankName: null,
    depositType: null,
    branchCode: null,
    branchName: null,
    bankAccount: null,
    accountHolder: null,
    accountName: null,
    walletName: null,
    walletAddress: null,
  },
  rules: {},
});

const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  const params = {
    ...queryParams.value,
    userId: props.userId ?? queryParams.value.userId,
  };
  listWithdrawalAcc(params)
    .then((response) => {
      withdrawalAccList.value = response.rows ?? response.data?.rows ?? [];
      total.value = response.total ?? response.data?.total ?? 0;
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
}

async function loadWithdrawalTypes(value, targetRef = formWithdrawalTypes) {
  if (value === null || value === undefined) {
    targetRef.value = [];
    return;
  }
  try {
    const response = await getType(value);
    const allTypes = (response.data ?? response.rows ?? response) || [];
    targetRef.value = allTypes.filter((item) => item.type === value);
  } catch (error) {
    console.error("Failed to fetch withdrawal types:", error);
    targetRef.value = [];
  }
}

async function handleTypeChange(eventOrValue) {
  const value = eventOrValue?.target?.value ?? eventOrValue;
  if (value === "0") {
    form.value.withdrawalType = null;
    form.value.accountName = null;
    form.value.walletName = null;
    form.value.walletAddress = null;
  } else if (value === "1") {
    form.value.bankName = null;
    form.value.depositType = null;
    form.value.branchCode = null;
    form.value.branchName = null;
    form.value.bankAccount = null;
    form.value.accountHolder = null;
  }
  await loadWithdrawalTypes(value, formWithdrawalTypes);
  updateRules();
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
      depositType: [{ required: true, message: "存款种类不能为空", trigger: "blur" }],
      branchCode: [{ required: true, message: "支行代码不能为空", trigger: "blur" }],
      branchName: [{ required: true, message: "支行名称不能为空", trigger: "blur" }],
      bankAccount: [{ required: true, message: "银行账号不能为空", trigger: "blur" }],
      accountHolder: [{ required: true, message: "账户持有人不能为空", trigger: "blur" }],
    });
  } else if (form.value.type === "1") {
    Object.assign(newRules, {
      accountName: [{ required: true, message: "账户名称不能为空", trigger: "blur" }],
      walletName: [{ required: true, message: "钱包名称不能为空", trigger: "blur" }],
      walletAddress: [{ required: true, message: "钱包地址不能为空", trigger: "blur" }],
    });
  }

  rules.value = newRules;
}

function cancel() {
  open.value = false;
  reset();
}

function handleClose() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    id: null,
    userId: null,
    type: "0",
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
    createTime: null,
  };
  updateRules();
  withdrawalAccRef.value?.clearValidate?.();
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  Object.assign(queryParams.value, {
    pageNum: 1,
    type: null,
    withdrawalType: null,
    isDefault: null,
    bankName: null,
    depositType: null,
    branchCode: null,
    branchName: null,
    bankAccount: null,
    accountHolder: null,
    accountName: null,
    walletName: null,
    walletAddress: null,
  });
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
  reset();
  form.value.userId = props.userId;
  await loadWithdrawalTypes(form.value.type, formWithdrawalTypes);
  open.value = true;
  title.value = "添加提现账户";
}

function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getWithdrawalAcc(_id).then(async (response) => {
    form.value = response.data ?? response;
    await loadWithdrawalTypes(form.value.type, formWithdrawalTypes);
    updateRules();
    open.value = true;
    title.value = "修改提现账户";
  });
}

function submitForm() {
  withdrawalAccRef.value?.validate?.().then(() => {
    form.value.userId = form.value.userId || props.userId;
    if (form.value.id != null) {
      updateWithdrawalAcc(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addWithdrawalAcc(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

function handleDelete(row) {
  const _ids = resolveDeleteIds(row, ids.value);
  if (!_ids.length) {
    proxy.$modal.msgWarning("请选择要删除的数据");
    return;
  }
  proxy.$modal
    .confirm(`是否确认删除提现账户编号为"${_ids}"的数据项？`)
    .then(() => delWithdrawalAcc(Array.isArray(_ids) ? _ids.join(",") : _ids))
    .then(() => {
      handleSelectionChange([]);
      getList();
      proxy.$modal.msgSuccess("删除成功");
      emit("success");
    })
    .catch(() => {});
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
  (id) => {
    if (id != null && visible.value) {
      queryParams.value.userId = id;
      queryParams.value.pageNum = 1;
      getList();
    }
  }
);

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      queryParams.value.userId = props.userId ?? queryParams.value.userId;
      getList();
    } else {
      open.value = false;
    }
  }
);

watch(
  () => queryParams.value.type,
  async (newVal) => {
    await loadWithdrawalTypes(newVal, queryWithdrawalTypes);
    queryParams.value.withdrawalType = null;
  }
);

reset();
getList();
</script>

<style scoped>
.drawer-footer {
  text-align: right;
}
</style>
