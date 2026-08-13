<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="充值记录列表"
      :columns="rechargeColumns"
      :data-source="rechargeList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 2200 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.username"
                  placeholder="请输入用户名"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="手机号码">
                <a-input
                  v-model:value="queryParams.phoneNumber"
                  placeholder="请输入手机号"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="上级用户名">
                <a-input
                  v-model:value="queryParams.parentUsername"
                  placeholder="请输入上级用户名"
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
              <a-form-item label="用户钱包地址">
                <a-input
                  v-model:value="queryParams.accountAddress"
                  allow-clear
                  placeholder="请输入"
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
              <a-form-item label="出金类型">
                <a-select v-model:value="queryParams.withdrawalType" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in order_zhlx" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="状态">
                <a-select v-model:value="queryParams.status" allow-clear placeholder="请选择状态">
                  <a-select-option v-for="dict in apply_status" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="是否假人">
                <a-select v-model:value="queryParams.isFake" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in user_yes_no" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="dateRange"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  show-time
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="交易类型">
                <a-select v-model:value="queryParams.transactionType" allow-clear placeholder="请选择交易类型">
                  <a-select-option v-for="dict in transaction_type" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="是否隐藏">
                <a-select v-model:value="queryParams.isHidden" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in user_yes_no" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-popconfirm
          title="显示"
          ok-text="确 定"
          cancel-text="取 消"
          :disabled="multiple"
          @confirm="handleHidden('1')"
        >
          <a-button :disabled="multiple" v-hasPermi="['member:recharge:edit']">
            <EyeOutlined />显示
          </a-button>
        </a-popconfirm>
        <a-popconfirm
          title="隐藏"
          ok-text="确 定"
          cancel-text="取 消"
          :disabled="multiple"
          @confirm="handleHidden('0')"
        >
          <a-button :disabled="multiple" v-hasPermi="['member:recharge:edit']">
            <EyeInvisibleOutlined />隐藏
          </a-button>
        </a-popconfirm>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'rechargeAccount'">
          {{ record.rechargeAccount || "-" }}
        </template>
        <template v-else-if="column.key === 'status'">
          <dict-tag :options="apply_status" :value="record.status" />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'transactionType'">
          <dict-tag :options="transaction_type" :value="record.transactionType" />
        </template>
        <template v-else-if="column.dataIndex === 'withdrawalType'">
          {{ dictText(order_zhlx, record.withdrawalType) }}
        </template>
        <template v-else-if="column.dataIndex === 'isHidden'">
          <dict-tag :options="user_yes_no" :value="record.isHidden" />
        </template>
        <template v-else-if="column.dataIndex === 'updateTime'">
          {{ parseTime(record.updateTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'remark' || column.dataIndex === 'updateBy'">
          {{ record[column.dataIndex] || "-" }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="4">
            <a-popconfirm
              title="通过选中的记录？"
              ok-text="确 定"
              cancel-text="取 消"
              :disabled="String(record.status) !== '1'"
              @confirm="handleApprove(record)"
            >
              <a-button
                type="link"
                size="small"
                :disabled="String(record.status) !== '1'"
                v-hasPermi="['member:recharge:edit']"
              >通过</a-button>
            </a-popconfirm>
            <a-button
              type="link"
              danger
              size="small"
              :disabled="String(record.status) !== '1'"
              @click="openReviewDialog(record, 'reject')"
              v-hasPermi="['member:recharge:edit']"
            >拒绝</a-button>
            <a-button
              type="link"
              size="small"
              @click="openReviewDialog(record, 'remark')"
              v-hasPermi="['member:recharge:edit']"
            >备注</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      :title="title"
      v-model:open="open"
      width="500px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="submitting"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form
        ref="rechargeRef"
        :model="form"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="form.remark"
            placeholder="请输入内容"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="Recharge">
import { EyeInvisibleOutlined, EyeOutlined } from "@ant-design/icons-vue";
import {
  listRecharge,
  delRecharge,
  addRecharge,
  updateRecharge,
  reviewRecharge,
} from "@/api/member/recharge";

const { proxy } = getCurrentInstance();
const { transaction_type, user_yes_no, apply_status, order_zhlx } = proxy.useDict(
  "transaction_type",
  "user_yes_no",
  "apply_status",
  "order_zhlx"
);

const rechargeList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const advancedSearchVisible = ref(false);
const dateRange = ref([]);
const title = ref("");
const dialogMode = ref("remark");
const submitting = ref(false);
const rechargeRef = ref();

const rechargeColumns = [
  { title: "用户名", dataIndex: "username", align: "center", width: 140 },
  { title: "手机号码", dataIndex: "phoneNumber", align: "center", width: 140 },
  { title: "上级用户名", dataIndex: "parentUsername", align: "center", width: 150 },
  { title: "充值账户", dataIndex: "rechargeAccount", key: "rechargeAccount", align: "center", width: 220 },
  { title: "金额", dataIndex: "amount", align: "center", width: 120 },
  { title: "出金类型", dataIndex: "withdrawalType", align: "center", width: 130 },
  { title: "赠送金额", dataIndex: "giftAmount", align: "center", width: 130 },
  { title: "到账金额", dataIndex: "receivedAmount", align: "center", width: 130 },
  { title: "转换后金额", dataIndex: "receivedAmount", key: "convertedAmount", align: "center", width: 140, hidden: true },
  { title: "状态", key: "status", dataIndex: "status", align: "center", width: 110 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "备注", dataIndex: "remark", align: "center", width: 150 },
  { title: "交易类型", dataIndex: "transactionType", align: "center", width: 130 },
  { title: "订单号", dataIndex: "orderNumber", align: "center", width: 180 },
  { title: "是否隐藏", dataIndex: "isHidden", align: "center", width: 120 },
  { title: "最后修改人", dataIndex: "updateBy", align: "center", width: 140 },
  { title: "最后修改时间", dataIndex: "updateTime", align: "center", width: 180 },
  { title: "操作", key: "operation", align: "center", fixed: "right", width: 180 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_, selectedRows) => handleSelectionChange(selectedRows),
}));

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    username: null,
    phoneNumber: null,
    parentUsername: null,
    accountAddress: null,
    amountMin: null,
    amountMax: null,
    isFake: null,
    userId: null,
    amount: null,
    withdrawalType: null,
    giftAmount: null,
    receivedAmount: null,
    status: null,
    createdTime: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
  },
  rules: {
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
    withdrawalType: [{ required: true, message: "充值类型不能为空", trigger: "change" }],
    receivedAmount: [{ required: true, message: "到账金额不能为空", trigger: "blur" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }],
    transactionType: [{ required: true, message: "交易类型不能为空", trigger: "change" }],
    orderNumber: [{ required: true, message: "订单号不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

function dictText(options, value) {
  return proxy.selectDictLabel(options, value) || value || "-";
}

/** 查询充值记录列表 */
function getList() {
  loading.value = true;
  const {
    amountMin,
    amountMax,
    ...base
  } = queryParams.value;
  const request = { ...base };
  const params = {};
  if (amountMin !== null && amountMin !== undefined) params.amountMin = amountMin;
  if (amountMax !== null && amountMax !== undefined) params.amountMax = amountMax;
  if (dateRange.value?.length === 2) {
    params.beginTime = dateRange.value[0];
    params.endTime = dateRange.value[1];
  }
  if (Object.keys(params).length) request.params = params;
  listRecharge(request).then((response) => {
    rechargeList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    userId: null,
    amount: null,
    withdrawalType: null,
    giftAmount: null,
    receivedAmount: null,
    status: null,
    createdTime: null,
    remark: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
  };
  nextTick(() => rechargeRef.value?.clearValidate?.());
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.username = null;
  queryParams.value.phoneNumber = null;
  queryParams.value.parentUsername = null;
  queryParams.value.accountAddress = null;
  queryParams.value.amountMin = null;
  queryParams.value.amountMax = null;
  queryParams.value.withdrawalType = null;
  queryParams.value.status = null;
  queryParams.value.isFake = null;
  queryParams.value.transactionType = null;
  queryParams.value.isHidden = null;
  dateRange.value = [];
  handleQuery();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "修改备注";
}

/** 修改按钮操作 */
function handleApprove(row) {
  return reviewRecharge({ id: row.id, status: "2" })
    .then(() => {
      proxy.$modal.msgSuccess("操作成功");
      getList();
    });
}

function openReviewDialog(row, mode) {
  reset();
  form.value = { ...row };
  dialogMode.value = mode;
  title.value = mode === "reject" ? "拒绝" : "备注";
  open.value = true;
}

/** 提交按钮 */
async function submitForm() {
  try {
    await rechargeRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (dialogMode.value === "reject") {
      await reviewRecharge({
        id: form.value.id,
        status: "3",
        remarks: form.value.remark,
      });
    } else if (form.value.id != null) {
      await updateRecharge(form.value);
    } else {
      await addRecharge(form.value);
    }
    proxy.$modal.msgSuccess("操作成功");
    open.value = false;
    getList();
  } finally {
    submitting.value = false;
  }
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(`是否确认删除充值记录编号为 "${_ids}" 的数据项？`)
    .then(function () {
      return delRecharge(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("操作成功");
    })
    .catch(() => {});
}

function handleHidden(isHidden) {
  Promise.all(ids.value.map((id) => updateRecharge({ id, isHidden }))).then(() => {
    proxy.$modal.msgSuccess("操作成功");
    getList();
  });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/recharge/export",
    {
      ...queryParams.value,
    },
    `recharge_${new Date().getTime()}.xlsx`
  );
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
  padding-inline: 8px;
  text-align: center;
}
</style>
