<template>
  <a-drawer
    title="额外佣金设置"
    v-model:open="visible"
    width="85%"
    :destroy-on-close="false"
    :mask-closable="!mutationPending"
    :closable="!mutationPending"
    :keyboard="!mutationPending"
  >
    <div class="drawer-table-wrap ant-pro-member-page">
      <div v-if="loadingUser" class="drawer-user-loading">加载中...</div>
      <div v-else class="drawer-user-summary">
        <span><strong>用户名：</strong>{{ user.username || "-" }}</span>
        <span><strong>手机号：</strong>{{ user.phoneNumber || "-" }}</span>
        <span><strong>余额：</strong>{{ user.balance ?? 0 }}</span>
        <span><strong>任务进度：</strong>{{ taskProgressDisplay }}</span>
      </div>

      <ant-pro-table
        title="额外佣金列表"
        :columns="extraColumns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :row-selection="rowSelection"
        :pagination="false"
        :scroll="{ x: 1100 }"
        @refresh="fetchList"
      >
        <template #toolbar>
          <a-button type="primary" @click="handleAdd" v-hasPermi="['member:extracommission:add']">新增</a-button>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'isLocked'">
            <dict-tag :options="user_yes_no" :value="record.isLocked" />
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <dict-tag :options="goods_status" :value="record.status" />
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ record.createTime ? parseTime(record.createTime) : "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                :disabled="!canMutate(record)"
                :title="mutationDisabledReason(record)"
                @click="handleEdit(record)" v-hasPermi="['member:extracommission:edit']"
              >修改</a-button>
              <a-button
                type="link"
                danger
                size="small"
                :disabled="!canMutate(record) || isDeleting(record)"
                :loading="isDeleting(record)"
                :title="mutationDisabledReason(record)"
                @click="handleDelete(record)" v-hasPermi="['member:extracommission:remove']"
              >删除</a-button>
            </a-space>
          </template>
        </template>
      </ant-pro-table>

      <a-drawer
        :title="formTitle"
        v-model:open="formVisible"
        width="65%"
        :mask-closable="false"
        :destroy-on-close="false"
        :closable="!submitting"
        :keyboard="!submitting"
        @close="closeFormDrawer"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
        >
          <a-row :gutter="20">
            <a-col :span="12">
              <a-form-item label="余额">
                <a-input v-model:value="user.balance" disabled />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="冻结余额">
                <a-input v-model:value="user.frozenBalance" disabled />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="20">
            <a-col :span="12">
              <a-form-item label="总余额">
                <a-input v-model:value="user.totalBalance" disabled />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="任务进度">
                <a-input :value="taskProgressDisplay" disabled />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="单数" name="orderCount">
            <a-input-number
              v-model:value="form.orderCount"
              :min="1"
              :precision="0"
              placeholder="请输入单数"
              class="full-width"
            />
          </a-form-item>
          <a-form-item label="商品价格" name="productPrice">
            <a-input-number
              v-model:value="form.productPrice"
              :min="0.01"
              :precision="2"
              placeholder="请输入商品价格"
              class="full-width"
            />
          </a-form-item>
          <a-form-item label="金额" name="amount">
            <a-input-number
              v-model:value="form.amount"
              :min="0.01"
              :precision="2"
              placeholder="请输入金额"
              class="full-width"
            />
          </a-form-item>
          <a-form-item label="是否锁定" name="isLocked">
            <a-radio-group v-model:value="form.isLocked" disabled>
              <a-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :value="dict.value"
                >{{ dict.label }}</a-radio
              >
            </a-radio-group>
          </a-form-item>
        </a-form>
        <template #footer>
          <div class="drawer-footer">
            <a-button :disabled="submitting" @click="closeFormDrawer">取消</a-button>
            <a-button
              type="primary"
              :loading="submitting"
              :disabled="submitting"
              @click="submitForm"
            >确定</a-button>
          </div>
        </template>
      </a-drawer>
    </div>
  </a-drawer>
</template>

<script setup>
import { ref, reactive, watch, computed, getCurrentInstance } from "vue";
import {
  listExtracommission,
  getExtracommission,
  addExtracommission,
  updateExtracommission,
  delExtracommission,
} from "@/api/member/extracommission";
import { getOrderuserOperationSummary } from "@/api/member/orderuser";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  userId: { type: [String, Number], default: null },
});
const emits = defineEmits(["update:modelValue", "success", "open-orderinfo"]);

const { proxy } = getCurrentInstance();
const { user_yes_no, goods_status } = proxy.useDict(
  "user_yes_no",
  "goods_status"
);

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emits("update:modelValue", v),
});

const list = ref([]);
const loading = ref(false);
const selectedIds = ref([]);
const deletingIds = ref(new Set());
let listRequestSequence = 0;
let userRequestSequence = 0;
let formRequestSequence = 0;
let submitRequestSequence = 0;

const extraColumns = [
  { title: "ID", dataIndex: "id", align: "center", width: 90 },
  { title: "单数", dataIndex: "orderCount", align: "center", width: 120 },
  { title: "价格", dataIndex: "productPrice", align: "center", width: 120 },
  { title: "金额", dataIndex: "amount", align: "center", width: 120 },
  { title: "是否锁定", dataIndex: "isLocked", align: "center", width: 120 },
  { title: "状态", dataIndex: "status", align: "center", width: 120 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "操作", dataIndex: "action", align: "center", width: 140, fixed: "right" },
];

const rowSelection = computed(() => ({
  selectedRowKeys: selectedIds.value,
  onChange: (keys) => {
    selectedIds.value = keys;
  },
}));
const loadingUser = ref(false);
const user = reactive({
  id: null,
  username: null,
  phoneNumber: null,
  balance: 0,
  frozenBalance: 0,
  totalBalance: 0,
  taskProgress: 0,
  memberOrderCountPerDay: null,
});

const taskProgressDisplay = computed(() => {
  return `${user.taskProgress ?? 0} / ${user.memberOrderCountPerDay ?? "-"}`;
});

async function fetchUser(id) {
  const requestSequence = ++userRequestSequence;
  const targetId = normalizeIdentifier(id);
  if (!targetId) {
    resetUser();
    loadingUser.value = false;
    return;
  }
  loadingUser.value = true;
  try {
    const res = await getOrderuserOperationSummary(id);
    if (
      requestSequence !== userRequestSequence
      || !visible.value
      || normalizeIdentifier(props.userId) !== targetId
    ) return;
    const u = res.data || res;
    const balance = finiteNumber(u.balance);
    const frozenBalance = finiteNumber(u.frozenBalance ?? u.freezeBalance);
    Object.assign(user, {
      id: u.id,
      username: u.username,
      phoneNumber: u.phoneNumber,
      balance,
      frozenBalance,
      totalBalance: finiteNumber(u.totalBalance, balance + frozenBalance),
      taskProgress: u.taskProgress ?? 0,
      memberOrderCountPerDay: u.orderCountPerDay ?? null,
    });
  } catch (error) {
    if (requestSequence === userRequestSequence && visible.value) {
      proxy.$modal.msgError(error?.message || "加载会员信息失败");
    }
  } finally {
    if (requestSequence === userRequestSequence) loadingUser.value = false;
  }
}

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
  });
}

function finiteNumber(value, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

const formVisible = ref(false);
const formTitle = ref("");
const submitting = ref(false);
const mutationPending = computed(() => submitting.value || deletingIds.value.size > 0);

const formRef = ref(null);
const form = reactive({
  id: null,
  userId: normalizeIdentifier(props.userId),
  orderCount: null,
  productPrice: null,
  amount: null,
  isLocked: "1",
});

const rules = {
  orderCount: [
    { required: true, message: "单数不能为空", trigger: "change" },
    { validator: validatePositiveInteger, trigger: ["blur", "change"] },
  ],
  productPrice: [
    { required: true, message: "商品价格不能为空", trigger: "change" },
    { validator: (_rule, value) => validatePositiveNumber(value, "商品价格必须大于 0"), trigger: ["blur", "change"] },
  ],
  amount: [
    { required: true, message: "金额不能为空", trigger: "change" },
    { validator: (_rule, value) => validatePositiveNumber(value, "金额必须大于 0"), trigger: ["blur", "change"] },
  ],
  isLocked: [
    { required: true, message: "是否锁定不能为空", trigger: "change" },
  ],
};

function validatePositiveInteger(_rule, value) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 && Number.isInteger(number)
    ? Promise.resolve()
    : Promise.reject(new Error("单数必须为大于 0 的整数"));
}

function validatePositiveNumber(value, message) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0
    ? Promise.resolve()
    : Promise.reject(new Error(message));
}

async function fetchList() {
  const targetId = normalizeIdentifier(props.userId);
  const requestSequence = ++listRequestSequence;
  if (!targetId) {
    list.value = [];
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const res = await listExtracommission({ userId: targetId });
    if (
      requestSequence !== listRequestSequence
      || !visible.value
      || normalizeIdentifier(props.userId) !== targetId
    ) return;
    list.value = res.rows || res.data || [];
    selectedIds.value = [];
  } catch (error) {
    if (requestSequence === listRequestSequence && visible.value) {
      proxy.$modal.msgError(error?.message || "加载额外佣金列表失败");
    }
  } finally {
    if (requestSequence === listRequestSequence) loading.value = false;
  }
}

function resetFormData() {
  const defaults = {
    id: null,
    userId: normalizeIdentifier(props.userId),
    orderCount: null,
    productPrice: null,
    amount: null,
    isLocked: "1",
  };
  Object.keys(form).forEach((key) => {
    if (!(key in defaults)) delete form[key];
  });
  Object.assign(form, defaults);
  formRef.value?.clearValidate?.();
}

function handleAdd() {
  formRequestSequence += 1;
  resetFormData();
  formTitle.value = "创建";
  formVisible.value = true;
}

async function handleEdit(row) {
  if (!canMutate(row)) return;
  const requestSequence = ++formRequestSequence;
  try {
    const res = await getExtracommission(row.id);
    if (requestSequence !== formRequestSequence || !visible.value) return;
    const data = res.data || res;
    resetFormData();
    Object.assign(form, {
      id: normalizeIdentifier(data.id),
      userId: normalizeIdentifier(props.userId),
      orderCount: data.orderCount,
      productPrice: data.productPrice,
      amount: data.amount,
      isLocked: String(data.isLocked ?? "1"),
    });
    formTitle.value = "修改额外佣金";
    formVisible.value = true;
  } catch (error) {
    if (requestSequence === formRequestSequence) {
      proxy.$modal.msgError(error?.message || "加载额外佣金详情失败");
    }
  }
}

async function submitForm() {
  if (submitting.value) return;
  submitting.value = true;
  try {
    await formRef.value?.validate();
  } catch (error) {
    submitting.value = false;
    if (!error?.errorFields) {
      proxy.$modal.msgError(error?.message || "额外佣金设置校验失败");
    }
    return;
  }
  if (!normalizeIdentifier(props.userId)) {
    submitting.value = false;
    proxy.$modal.msgError("用户ID不能为空");
    return;
  }

  const requestSequence = ++submitRequestSequence;
  try {
    const payload = buildExtraCommissionPayload();
    let successMessage;
    if (form.id != null) {
      await updateExtracommission(payload);
      successMessage = "修改成功";
    } else {
      await addExtracommission(payload);
      successMessage = "新增成功";
    }
    if (requestSequence !== submitRequestSequence || !visible.value) return;
    proxy.$modal.msgSuccess(successMessage);
    closeFormDrawer();
    await fetchList();
    emits("success");
  } catch (error) {
    if (requestSequence === submitRequestSequence) {
      proxy.$modal.msgError(error?.message || "保存额外佣金设置失败");
    }
  } finally {
    if (requestSequence === submitRequestSequence) submitting.value = false;
  }
}

function buildExtraCommissionPayload() {
  const payload = {
    userId: normalizeIdentifier(props.userId),
    orderCount: Number(form.orderCount),
    productPrice: Number(form.productPrice),
    amount: Number(form.amount),
  };
  if (form.id != null) payload.id = normalizeIdentifier(form.id);
  return payload;
}

function closeFormDrawer() {
  formRequestSequence += 1;
  formVisible.value = false;
  resetFormData();
}

function canMutate(row) {
  return String(row?.status) === "1" && String(row?.isLocked) === "1";
}

function mutationDisabledReason(row) {
  return canMutate(row) ? "" : "已绑定或已完成的额外佣金不能修改或删除";
}

function isDeleting(row) {
  return deletingIds.value.has(normalizeIdentifier(row?.id));
}

async function handleDelete(row) {
  const id = normalizeIdentifier(row?.id);
  if (!id || !canMutate(row) || deletingIds.value.has(id)) return;
  setDeleting(id, true);
  let confirmed = false;
  try {
    await proxy.$modal.confirm(`是否确认删除额外佣金设置编号为"${id}"的数据项？`);
    confirmed = true;
    await delExtracommission(id);
    proxy.$modal.msgSuccess("删除成功");
    await fetchList();
    emits("success");
  } catch (error) {
    if (confirmed || !isConfirmationCancel(error)) {
      proxy.$modal.msgError(error?.message || "删除额外佣金设置失败");
    }
  } finally {
    setDeleting(id, false);
  }
}

function setDeleting(id, deleting) {
  const next = new Set(deletingIds.value);
  if (deleting) next.add(id);
  else next.delete(id);
  deletingIds.value = next;
}

function isConfirmationCancel(error) {
  return error == null
    || error === "cancel"
    || error === "close"
    || error?.type === "cancel"
    || error?.type === "close";
}

function normalizeIdentifier(value) {
  return value == null ? "" : String(value).trim();
}

function refreshAll() {
  return Promise.all([fetchList(), fetchUser(props.userId)]);
}

function resetDrawerState() {
  listRequestSequence += 1;
  userRequestSequence += 1;
  formRequestSequence += 1;
  submitRequestSequence += 1;
  formVisible.value = false;
  submitting.value = false;
  deletingIds.value = new Set();
  loading.value = false;
  loadingUser.value = false;
  list.value = [];
  selectedIds.value = [];
  resetUser();
  resetFormData();
}

watch(visible, (open) => {
  if (open) refreshAll();
  else resetDrawerState();
});

watch(
  () => props.userId,
  (id) => {
    if (!visible.value) return;
    formRequestSequence += 1;
    submitRequestSequence += 1;
    submitting.value = false;
    formVisible.value = false;
    resetFormData();
    Promise.all([fetchList(), fetchUser(id)]);
  }
);
</script>

<style scoped>
.full-width {
  width: 100%;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>




