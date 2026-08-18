<template>
  <a-drawer
    v-model:open="visible"
    title="彩金设置"
    width="85%"
    :mask-closable="!mutationPending"
    :closable="!mutationPending"
    :keyboard="!mutationPending"
  >
    <div class="bonus-drawer ant-pro-member-page">
      <ant-pro-table
        title=""
        :columns="bonusColumns"
        :data-source="bonusList"
        :loading="loading"
        row-key="id"
        :row-selection="rowSelection"
        :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
        :scroll="{ x: 2120 }"
        @page-change="handleAntPageChange"
        @refresh="getList"
      >
        <template #search>
          <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
            <a-row :gutter="[24, 16]" align="middle">
              <a-col :xs="24" :sm="12" :lg="5">
                <a-form-item label="单数">
                  <a-input-number
                    v-model:value="queryParams.orderNum"
                    :min="1"
                    :precision="0"
                    placeholder="请输入单数"
                    class="full-width"
                    @pressEnter="handleQuery"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="7">
                <a-form-item label="金额">
                  <a-space-compact block>
                    <a-input-number
                      v-model:value="queryParams.minAmount"
                      :min="0"
                      placeholder="最低金额"
                      class="amount-input"
                    />
                    <a-input class="amount-separator" value="至" disabled />
                    <a-input-number
                      v-model:value="queryParams.maxAmount"
                      :min="0"
                      placeholder="最高金额"
                      class="amount-input"
                    />
                  </a-space-compact>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item label="发放类型">
                  <a-select
                    v-model:value="queryParams.distributionType"
                    :options="distributionOptions"
                    placeholder="请选择发放类型"
                    allow-clear
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

        <template #title>
          <div class="member-summary">
            <span class="member-summary-field">用户名: {{ user.username || "-" }}</span>
            <span class="member-summary-field">手机号码: {{ user.phoneNumber || "-" }}</span>
            <span class="member-summary-field">余额: {{ user.balance ?? 0 }}</span>
            <span class="member-summary-field">任务进度: {{ taskProgressDisplay }}</span>
            <span>最后登录时间: {{ parseTime(user.lastLoginTime) || "-" }}</span>
            <a-button
              type="link"
              class="summary-refresh"
              :loading="loadingUser"
              aria-label="刷新用户信息和彩金列表"
              @click="refreshAll"
            >
              <ReloadOutlined />
            </a-button>
          </div>
        </template>

        <template #toolbar>
          <a-button type="primary" @click="handleAdd" v-hasPermi="['member:bonus:add']">
            新增
          </a-button>
          <a-button
            danger
            :disabled="!ids.length || deleting"
            :loading="deleting"
            @click="handleDelete"
            v-hasPermi="['member:bonus:remove']"
          >
            删除
          </a-button>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'orderNum'">
            <a-button
              v-if="hasPermission('member:bonus:query')"
              v-hasPermi="['member:bonus:query']"
              type="link"
              size="small"
              @click="handleView(record)"
            >
              {{ record.orderNum }}
            </a-button>
            <span v-else>{{ record.orderNum }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'distributionType'">
            <a-tag color="blue">{{ distributionText(record.distributionType) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'pushType'">
            <a-button
              v-if="hasPermission('member:bonus:edit') && record.isReceived === '1'"
              v-hasPermi="['member:bonus:edit']"
              type="link"
              size="small"
              @click="handlePushEdit(record)"
            >
              {{ pushText(record.pushType) }}
            </a-button>
            <span v-else>{{ pushText(record.pushType) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'isReceived'">
            <a-tag :color="record.isReceived === '0' ? 'green' : 'default'">
              {{ record.isReceived === "0" ? "已领取" : "未领取" }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'isDistributed'">
            <a-tag :color="record.isDistributed === '0' ? 'green' : 'default'">
              {{ record.isDistributed === "0" ? "已发放" : "未发放" }}
            </a-tag>
          </template>
          <template v-else-if="timeColumns.includes(column.dataIndex)">
            {{ parseTime(record[column.dataIndex]) || "-" }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space wrap>
              <a-button
                type="link"
                size="small"
                :disabled="record.isReceived !== '1' || isReceiving(record)"
                :loading="isReceiving(record)"
                @click="handleReceive(record)"
                v-hasPermi="['member:bonus:receive']"
              >
                领取
              </a-button>
              <a-button
                type="link"
                size="small"
                :disabled="!canGive(record)"
                :loading="isGiving(record)"
                :title="giveDisabledReason(record)"
                @click="handleGive(record)"
                v-hasPermi="['member:bonus:give']"
              >
                发放
              </a-button>
              <a-button
                type="link"
                size="small"
                :disabled="record.isDistributed === '0' || record.isReceived === '0'"
                @click="handleUpdate(record)"
                v-hasPermi="['member:bonus:edit']"
              >
                修改
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleCopy(record)"
                v-hasPermi="['member:bonus:add']"
              >
                复制
              </a-button>
            </a-space>
          </template>
        </template>
      </ant-pro-table>

      <a-drawer
        v-model:open="dialogOpen"
        :title="dialogTitle"
        width="65%"
        :mask-closable="false"
        :destroy-on-close="false"
        :closable="!submitting"
        :keyboard="!submitting"
        @close="closeDialog"
      >
        <a-form ref="bonusRef" :model="form" :rules="rules" layout="vertical">
          <a-row :gutter="[20, 0]">
            <a-col :span="12">
              <a-form-item label="余额"><a-input :value="user.balance" disabled /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="冻结余额"><a-input :value="user.frozenBalance" disabled /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="总余额"><a-input :value="user.totalBalance" disabled /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="任务进度"><a-input :value="taskProgressDisplay" disabled /></a-form-item>
            </a-col>

            <a-col :span="12">
              <a-form-item label="单数" name="orderNum">
                <a-input-number
                  v-model:value="form.orderNum"
                  :min="1"
                  :precision="0"
                  placeholder="请输入单数"
                  :disabled="fieldDisabled('orderNum')"
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="金额" name="amount">
                <a-input-number
                  v-model:value="form.amount"
                  :min="0.01"
                  :precision="2"
                  placeholder="请输入金额"
                  :disabled="fieldDisabled('amount')"
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="动画时长（秒）" name="animationDuration">
                <a-input-number
                  v-model:value="form.animationDuration"
                  :min="0"
                  :precision="0"
                  placeholder="请输入动画时长"
                  :disabled="fieldDisabled('animationDuration')"
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="显示时长（秒）" name="displayDuration">
                <a-input-number
                  v-model:value="form.displayDuration"
                  :min="0"
                  :precision="0"
                  placeholder="请输入显示时长"
                  :disabled="fieldDisabled('displayDuration')"
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="发放类型" name="distributionType">
                <a-radio-group
                  v-model:value="form.distributionType"
                  :options="distributionOptions"
                  :disabled="fieldDisabled('distributionType')"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="过期时间" name="expiryTime">
                <a-date-picker
                  v-model:value="form.expiryTime"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="请选择过期时间"
                  :disabled="fieldDisabled('expiryTime')"
                  class="full-width"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="推送类型" name="pushType">
                <a-radio-group
                  v-model:value="form.pushType"
                  :options="pushOptions"
                  :disabled="fieldDisabled('pushType')"
                  @change="handlePushTypeChange"
                />
              </a-form-item>
            </a-col>
            <a-col v-if="form.pushType === '2'" :span="24">
              <a-form-item label="会员列表" name="toUsers">
                <a-select
                  v-model:value="form.toUsers"
                  mode="multiple"
                  show-search
                  allow-clear
                  :filter-option="false"
                  :options="userOptions"
                  :loading="loadingUsers"
                  :disabled="fieldDisabled('toUsers')"
                  placeholder="请输入用户名搜索会员"
                  class="full-width"
                  @search="handleUserSearch"
                  @focus="loadUsers()"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
        <template #footer>
          <div class="drawer-footer">
            <a-button :disabled="submitting" @click="closeDialog">
              {{ formMode === "view" ? "关闭" : "取消" }}
            </a-button>
            <a-button
              v-if="formMode !== 'view'"
              type="primary"
              :loading="submitting"
              :disabled="submitting"
              @click="submitForm"
            >
              确定
            </a-button>
          </div>
        </template>
      </a-drawer>
    </div>
  </a-drawer>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref, watch } from "vue";
import { ReloadOutlined } from "@ant-design/icons-vue";
import {
  addBonus,
  delBonus,
  getBonus,
  giveBonus,
  listBonus,
  receiveBonus,
  updateBonus,
} from "@/api/member/bonus";
import { getOrderuserOperationSummary, listOrderuser } from "@/api/member/orderuser";
import useUserStore from "@/store/modules/user";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  userId: { type: [String, Number], default: null },
});
const emit = defineEmits(["update:modelValue", "success"]);
const { proxy } = getCurrentInstance();
const userStore = useUserStore();

function hasPermission(permission) {
  const permissions = userStore.permissions || [];
  return permissions.includes("*:*:*") || permissions.includes(permission);
}

const distributionOptions = [
  { label: "立即发放", value: "1" },
  { label: "完成一组任务再发放", value: "2" },
];
const pushOptions = [
  { label: "不推送", value: "0" },
  { label: "全部会员", value: "1" },
  { label: "指定会员", value: "2" },
];
const timeColumns = ["receivedTime", "expiryTime", "distributionTime", "createTime"];
const immutableAfterReceive = [
  "orderNum",
  "amount",
  "animationDuration",
  "displayDuration",
  "distributionType",
];

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});
const loading = ref(false);
const loadingUser = ref(false);
const loadingUsers = ref(false);
const submitting = ref(false);
const deleting = ref(false);
const receivingIds = ref(new Set());
const givingIds = ref(new Set());
const mutationPending = computed(() =>
  submitting.value
  || deleting.value
  || receivingIds.value.size > 0
  || givingIds.value.size > 0
);
const bonusList = ref([]);
const total = ref(0);
const ids = ref([]);
const dialogOpen = ref(false);
const formMode = ref("create");
const dialogTitle = ref("创建");
const bonusRef = ref();
const userOptions = ref([]);
let userRequestSequence = 0;
let listRequestSequence = 0;
let optionRequestSequence = 0;
let recordRequestSequence = 0;
let submitRequestSequence = 0;
let searchTimer;

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userId: props.userId,
  orderNum: null,
  minAmount: null,
  maxAmount: null,
  distributionType: undefined,
});
const form = reactive(defaultForm());
const user = reactive({
  id: null,
  username: "",
  phoneNumber: "",
  balance: 0,
  frozenBalance: 0,
  totalBalance: 0,
  taskProgress: 0,
  memberOrderCountPerDay: null,
  lastLoginTime: null,
});

const rules = {
  orderNum: [{ required: true, message: "单数是必填项！", trigger: "change" }],
  amount: [{ required: true, message: "金额是必填项！", trigger: "change" }],
  animationDuration: [
    { required: true, message: "动画时长（秒）是必填项！", trigger: "change" },
  ],
  displayDuration: [
    { required: true, message: "显示时长（秒）是必填项！", trigger: "change" },
  ],
  distributionType: [{ required: true, message: "发放类型是必填项！", trigger: "change" }],
  pushType: [{ required: true, message: "推送类型是必填项！", trigger: "change" }],
  toUsers: [{
    validator: (_rule, value) => form.pushType !== "2" || (value && value.length)
      ? Promise.resolve()
      : Promise.reject(new Error("会员列表是必填项！")),
    trigger: "change",
  }],
};

const bonusColumns = [
  { title: "单数", dataIndex: "orderNum", align: "center", width: 90 },
  { title: "金额", dataIndex: "amount", align: "center", width: 110 },
  { title: "动画时长（秒）", dataIndex: "animationDuration", align: "center", width: 130 },
  { title: "显示时长（秒）", dataIndex: "displayDuration", align: "center", width: 130 },
  { title: "发放类型", dataIndex: "distributionType", align: "center", width: 170 },
  { title: "推送类型", dataIndex: "pushType", align: "center", width: 110 },
  { title: "推送会员", dataIndex: "pushUsersDisplay", width: 170, ellipsis: true },
  { title: "是否领取", dataIndex: "isReceived", align: "center", width: 100 },
  { title: "领取时间", dataIndex: "receivedTime", align: "center", width: 170 },
  { title: "是否发放", dataIndex: "isDistributed", align: "center", width: 100 },
  { title: "过期时间", dataIndex: "expiryTime", align: "center", width: 170 },
  { title: "发放时间", dataIndex: "distributionTime", align: "center", width: 170 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 170 },
  { title: "操作", dataIndex: "action", align: "center", width: 260, fixed: "right" },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  getCheckboxProps: (record) => ({ disabled: record.isReceived !== "1" }),
  onChange: (keys) => {
    ids.value = keys;
  },
}));
const taskProgressDisplay = computed(
  () => `${user.taskProgress ?? 0} / ${user.memberOrderCountPerDay ?? "-"}`
);
const taskGroupCompleted = computed(() => {
  const limit = Number(user.memberOrderCountPerDay);
  return Number.isFinite(limit)
    && limit > 0
    && Number(user.taskProgress || 0) >= limit;
});

watch(
  () => props.userId,
  (id) => {
    queryParams.userId = id;
    if (visible.value) {
      submitRequestSequence += 1;
      submitting.value = false;
      closeDialog();
      refreshAll();
    }
  }
);
watch(visible, (open) => {
  if (open) {
    queryParams.userId = props.userId;
    refreshAll();
  } else {
    resetDrawerState();
  }
});

function defaultForm() {
  return {
    id: null,
    orderNum: null,
    userId: props.userId,
    amount: null,
    animationDuration: null,
    displayDuration: 0,
    distributionType: "2",
    expiryTime: null,
    pushType: "0",
    isDistributed: "1",
    isReceived: "1",
    toUsers: [],
  };
}

function resetForm() {
  const defaults = defaultForm();
  Object.keys(form).forEach((key) => {
    if (!(key in defaults)) delete form[key];
  });
  Object.assign(form, defaults);
  bonusRef.value?.clearValidate?.();
}

function resetUser() {
  Object.assign(user, {
    id: null,
    username: "",
    phoneNumber: "",
    balance: 0,
    frozenBalance: 0,
    totalBalance: 0,
    taskProgress: 0,
    memberOrderCountPerDay: null,
    lastLoginTime: null,
  });
}

function resetDrawerState() {
  userRequestSequence += 1;
  listRequestSequence += 1;
  optionRequestSequence += 1;
  recordRequestSequence += 1;
  submitRequestSequence += 1;
  clearTimeout(searchTimer);
  dialogOpen.value = false;
  submitting.value = false;
  deleting.value = false;
  receivingIds.value = new Set();
  givingIds.value = new Set();
  loading.value = false;
  loadingUser.value = false;
  loadingUsers.value = false;
  bonusList.value = [];
  total.value = 0;
  ids.value = [];
  userOptions.value = [];
  resetUser();
  resetForm();
}

async function refreshAll() {
  queryParams.userId = props.userId;
  await Promise.all([fetchUser(props.userId), getList()]);
}

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
    const response = await getOrderuserOperationSummary(id);
    if (
      requestSequence !== userRequestSequence
      || !visible.value
      || normalizeIdentifier(props.userId) !== targetId
    ) return;
    const value = response.data || response;
    const balance = Number(value.balance || 0);
    const frozenBalance = Number(value.frozenBalance ?? value.freezeBalance ?? 0);
    Object.assign(user, {
      id: value.id,
      username: value.username,
      phoneNumber: value.phoneNumber,
      balance: value.balance ?? 0,
      frozenBalance: value.frozenBalance ?? value.freezeBalance ?? 0,
      totalBalance: value.totalBalance ?? balance + frozenBalance,
      taskProgress: value.taskProgress ?? 0,
      memberOrderCountPerDay: value.orderCountPerDay ?? null,
      lastLoginTime: value.lastLoginTime,
    });
  } catch (error) {
    if (requestSequence === userRequestSequence && visible.value) {
      proxy.$modal.msgError(error?.message || "加载会员信息失败");
    }
  } finally {
    if (requestSequence === userRequestSequence) loadingUser.value = false;
  }
}

async function getList() {
  const targetId = normalizeIdentifier(queryParams.userId);
  const requestSequence = ++listRequestSequence;
  if (!targetId) {
    bonusList.value = [];
    total.value = 0;
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const response = await listBonus({ ...queryParams, userId: targetId });
    if (
      requestSequence !== listRequestSequence
      || !visible.value
      || normalizeIdentifier(props.userId) !== targetId
    ) return;
    bonusList.value = (response.rows || []).map((row) => ({
      ...row,
      id: normalizeIdentifier(row.id),
      pushUsersDisplay: pushUsersText(row),
    }));
    total.value = response.total || 0;
    ids.value = [];
  } catch (error) {
    if (requestSequence === listRequestSequence && visible.value) {
      proxy.$modal.msgError(error?.message || "加载彩金列表失败");
    }
  } finally {
    if (requestSequence === listRequestSequence) loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: queryParams.pageSize,
    userId: props.userId,
    orderNum: null,
    minAmount: null,
    maxAmount: null,
    distributionType: undefined,
  });
  getList();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.pageNum = page;
  queryParams.pageSize = pageSize;
  getList();
}

function handleAdd() {
  recordRequestSequence += 1;
  resetForm();
  formMode.value = "create";
  dialogTitle.value = "创建";
  dialogOpen.value = true;
}

async function loadRecord(row, mode, title) {
  const requestSequence = ++recordRequestSequence;
  try {
    const response = await getBonus(row.id);
    if (requestSequence !== recordRequestSequence || !visible.value) return;
    resetForm();
    Object.assign(form, response.data || response);
    form.expiryTime = formatExpiryInput(form.expiryTime);
    form.userId = normalizeIdentifier(props.userId);
    form.toUsers = normalizeUserIds(form.toUsers);
    formMode.value = mode;
    dialogTitle.value = title;
    dialogOpen.value = true;
    if (form.pushType === "2") await loadUsers();
    bonusRef.value?.clearValidate?.();
  } catch (error) {
    if (requestSequence === recordRequestSequence) {
      proxy.$modal.msgError(error?.message || "加载彩金详情失败");
    }
  }
}

function handleView(row) {
  loadRecord(row, "view", "查看彩金");
}

function handleUpdate(row) {
  if (row.isReceived !== "1" || row.isDistributed === "0") {
    proxy.$modal.msgWarning("已领取或已发放的彩金不能修改");
    return;
  }
  loadRecord(row, "edit", "修改彩金");
}

function handlePushEdit(row) {
  if (row.isReceived !== "1") {
    proxy.$modal.msgWarning("已领取的彩金不能修改推送设置");
    return;
  }
  loadRecord(row, "push", "修改推送设置");
}

async function handleCopy(row) {
  const requestSequence = ++recordRequestSequence;
  try {
    const response = await getBonus(row.id);
    if (requestSequence !== recordRequestSequence || !visible.value) return;
    resetForm();
    Object.assign(form, response.data || response, {
      id: null,
      userId: normalizeIdentifier(props.userId),
      isReceived: "1",
      isDistributed: "1",
      receivedTime: null,
      distributionTime: null,
      createTime: null,
    });
    form.expiryTime = formatExpiryInput(form.expiryTime);
    form.toUsers = normalizeUserIds(form.toUsers);
    formMode.value = "create";
    dialogTitle.value = "复制彩金";
    dialogOpen.value = true;
    if (form.pushType === "2") await loadUsers();
  } catch (error) {
    if (requestSequence === recordRequestSequence) {
      proxy.$modal.msgError(error?.message || "加载彩金详情失败");
    }
  }
}

function fieldDisabled(field) {
  if (formMode.value === "view") return true;
  if (formMode.value === "push") {
    return !["displayDuration", "pushType", "toUsers", "expiryTime"].includes(field);
  }
  return formMode.value === "edit"
    && form.isReceived === "0"
    && immutableAfterReceive.includes(field);
}

function closeDialog() {
  recordRequestSequence += 1;
  optionRequestSequence += 1;
  clearTimeout(searchTimer);
  dialogOpen.value = false;
  userOptions.value = [];
  resetForm();
}

async function submitForm() {
  if (submitting.value) return;
  submitting.value = true;
  try {
    await bonusRef.value?.validate?.();
  } catch (error) {
    submitting.value = false;
    if (!error?.errorFields) {
      proxy.$modal.msgError(error?.message || "彩金设置校验失败");
    }
    return;
  }

  const requestSequence = ++submitRequestSequence;
  try {
    const payload = buildBonusPayload();
    let successMessage;
    if (form.id != null) {
      await updateBonus(payload);
      successMessage = "修改成功";
    } else {
      await addBonus(payload);
      successMessage = "新增成功";
    }
    if (requestSequence !== submitRequestSequence || !visible.value) return;
    proxy.$modal.msgSuccess(successMessage);
    closeDialog();
    await getList();
    emit("success");
  } catch (error) {
    if (requestSequence === submitRequestSequence) {
      proxy.$modal.msgError(error?.message || "保存彩金设置失败");
    }
  } finally {
    if (requestSequence === submitRequestSequence) submitting.value = false;
  }
}

function buildBonusPayload() {
  const payload = {
    userId: normalizeIdentifier(props.userId),
    orderNum: Number(form.orderNum),
    amount: Number(form.amount),
    animationDuration: Number(form.animationDuration),
    displayDuration: Number(form.displayDuration),
    distributionType: String(form.distributionType),
    expiryTime: toExpiryTimestamp(form.expiryTime),
    pushType: String(form.pushType),
    toUsers: form.pushType === "2" ? normalizeUserIds(form.toUsers).join(",") : null,
  };
  if (form.id != null) payload.id = normalizeIdentifier(form.id);
  return payload;
}

function handlePushTypeChange() {
  if (form.pushType !== "2") form.toUsers = [];
  else loadUsers();
  bonusRef.value?.clearValidate?.(["toUsers"]);
}

async function loadUsers(keyword = "") {
  const normalizedKeyword = typeof keyword === "string" ? keyword.trim() : "";
  const requestSequence = ++optionRequestSequence;
  loadingUsers.value = true;
  try {
    const response = await listOrderuser({
      pageNum: 1,
      pageSize: 100,
      username: normalizedKeyword || undefined,
    });
    if (requestSequence !== optionRequestSequence || !dialogOpen.value) return;
    const rows = response.rows || [];
    const selected = normalizeUserIds(form.toUsers).map((id) => ({
      value: id,
      label: String(id),
    }));
    const fetched = rows
      .filter((item) => !normalizedKeyword
        || String(item.username || "").includes(normalizedKeyword))
      .map((item) => ({
        value: normalizeIdentifier(item.id),
        label: item.username,
      }))
      .filter((item) => item.value);
    userOptions.value = [...selected, ...fetched].filter(
      (item, index, all) => all.findIndex((value) => value.value === item.value) === index
    );
  } catch (error) {
    if (requestSequence === optionRequestSequence) {
      proxy.$modal.msgError(error?.message || "加载会员列表失败");
    }
  } finally {
    if (requestSequence === optionRequestSequence) loadingUsers.value = false;
  }
}

function handleUserSearch(value) {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => loadUsers(value), 300);
}

async function handleReceive(row) {
  const key = normalizeIdentifier(row.id);
  if (!key || receivingIds.value.has(key)) return;
  setRowPending(receivingIds, key, true);
  let confirmed = false;
  try {
    await proxy.$modal.confirm("确认将该彩金标记为已领取吗？");
    confirmed = true;
    await receiveBonus(row.id);
    proxy.$modal.msgSuccess("领取成功");
    await getList();
    emit("success");
  } catch (error) {
    reportMutationError(error, "领取彩金失败", confirmed);
  } finally {
    setRowPending(receivingIds, key, false);
  }
}

async function handleGive(row) {
  const key = normalizeIdentifier(row.id);
  if (!key || givingIds.value.has(key)) return;
  setRowPending(givingIds, key, true);
  let confirmed = false;
  try {
    await proxy.$modal.confirm("确认发放该彩金并增加会员余额吗？");
    confirmed = true;
    await giveBonus(row.id);
    proxy.$modal.msgSuccess("发放成功");
    await Promise.all([getList(), fetchUser(props.userId)]);
    emit("success");
  } catch (error) {
    reportMutationError(error, "发放彩金失败", confirmed);
  } finally {
    setRowPending(givingIds, key, false);
  }
}

function isReceiving(row) {
  return receivingIds.value.has(normalizeIdentifier(row.id));
}

function isGiving(row) {
  return givingIds.value.has(normalizeIdentifier(row.id));
}

function setRowPending(target, key, pending) {
  const next = new Set(target.value);
  if (pending) next.add(key);
  else next.delete(key);
  target.value = next;
}

function canGive(row) {
  if (row.isReceived !== "0" || row.isDistributed !== "1") return false;
  return row.distributionType !== "2" || taskGroupCompleted.value;
}

function giveDisabledReason(row) {
  if (row.isReceived !== "0") return "请先由管理员手动领取";
  if (row.isDistributed !== "1") return "彩金已发放";
  if (row.distributionType === "2" && !taskGroupCompleted.value) {
    return "完成当前任务组后才可由管理员发放";
  }
  return "";
}

async function handleDelete() {
  if (deleting.value || !ids.value.length) return;
  deleting.value = true;
  let confirmed = false;
  try {
    await proxy.$modal.confirm(`确认删除选中的 ${ids.value.length} 条彩金记录吗？`);
    confirmed = true;
    await delBonus(ids.value.join(","));
    proxy.$modal.msgSuccess("删除成功");
    await getList();
    emit("success");
  } catch (error) {
    reportMutationError(error, "删除彩金失败", confirmed);
  } finally {
    deleting.value = false;
  }
}

function normalizeUserIds(value) {
  const values = Array.isArray(value) ? value : value ? String(value).split(",") : [];
  return values.map(normalizeIdentifier).filter(Boolean);
}

function normalizeIdentifier(value) {
  return value == null ? "" : String(value).trim();
}

function formatExpiryInput(value) {
  if (value === null || value === undefined || value === "") return null;
  if (typeof value === "string" && /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(value)) {
    return value;
  }
  const numericValue = typeof value === "string" && /^\d+$/.test(value)
    ? Number(value)
    : value;
  const date = new Date(numericValue);
  if (Number.isNaN(date.getTime())) return null;
  const pad = (part) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function toExpiryTimestamp(value) {
  if (value === null || value === undefined || value === "") return null;
  if (typeof value === "number" && Number.isFinite(value)) return value;
  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/);
  const timestamp = match
    ? new Date(
        Number(match[1]),
        Number(match[2]) - 1,
        Number(match[3]),
        Number(match[4]),
        Number(match[5]),
        Number(match[6]),
      ).getTime()
    : Number(value);
  if (!Number.isFinite(timestamp)) throw new Error("过期时间格式无效");
  return timestamp;
}

function reportMutationError(error, fallbackMessage, confirmed = false) {
  if (!confirmed && isConfirmationCancel(error)) return;
  proxy.$modal.msgError(error?.message || fallbackMessage);
}

function isConfirmationCancel(error) {
  return error == null
    || error === "cancel"
    || error === "close"
    || error?.type === "cancel"
    || error?.type === "close";
}

function pushUsersText(row) {
  if (row.pushType === "0") return "-";
  if (row.pushType === "1") return "全部会员";
  return normalizeUserIds(row.toUsers).join(", ") || "-";
}

function distributionText(value) {
  return distributionOptions.find((item) => item.value === String(value))?.label || "-";
}

function pushText(value) {
  return pushOptions.find((item) => item.value === String(value))?.label || "-";
}
</script>

<style scoped>
.bonus-drawer {
  min-width: 0;
}

.bonus-drawer :deep(.ant-pro-search-card .ant-card-body) {
  padding: 24px 0;
}

.bonus-drawer :deep(.ant-pro-table-card .ant-card-body) {
  padding: 0;
}

.bonus-drawer :deep(.ant-pro-table-toolbar) {
  padding: 16px 0;
}

.bonus-drawer :deep(.ant-pro-table-title) {
  flex: 0 0 50%;
  min-width: 0;
  color: rgba(0, 0, 0, 0.88);
}

.bonus-drawer :deep(.ant-pro-table-actions) {
  flex: 1 1 50%;
  min-width: 0;
  justify-content: flex-end;
}

.member-summary {
  padding: 0;
  margin: 0;
  color: rgba(0, 0, 0, 0.88);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
  font-size: 16px;
  font-weight: 700;
  line-height: 16px;
  background: transparent;
  border: 0;
}

.member-summary-field {
  margin-right: 10px;
}

.summary-refresh {
  color: #1677ff;
  font-family: inherit;
}

.full-width {
  width: 100%;
}

.amount-input {
  width: calc(50% - 20px);
}

.amount-separator {
  width: 40px;
  padding-inline: 0;
  text-align: center;
  pointer-events: none;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:deep(.ant-pro-table .ant-btn-link) {
  padding-inline: 0;
}
</style>
