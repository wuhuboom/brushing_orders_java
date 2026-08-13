<template>
  <a-drawer v-model:open="visible" title="彩金设置" width="85%">
    <div class="bonus-drawer ant-pro-member-page">
      <div v-if="loadingUser" class="user-summary-loading">加载中...</div>
      <div v-else class="user-summary">
        <span><strong>用户名：</strong>{{ user.username || "-" }}</span>
        <span><strong>手机号：</strong>{{ user.phoneNumber || "-" }}</span>
        <span><strong>余额：</strong>{{ user.balance ?? 0 }}</span>
        <span><strong>任务进度：</strong>{{ taskProgressDisplay }}</span>
        <span><strong>最后登录：</strong>{{ parseTime(user.lastLoginTime) || "-" }}</span>
        <a-button size="small" @click="refreshAll">刷新</a-button>
      </div>

      <ant-pro-table
        title="彩金列表"
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

        <template #toolbar>
          <a-button type="primary" @click="handleAdd" v-hasPermi="['member:bonus:add']">
            新增
          </a-button>
          <a-button
            danger
            :disabled="!ids.length"
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
              v-if="hasPermission('member:bonus:edit')"
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
                :disabled="record.isReceived !== '1'"
                @click="handleReceive(record)"
                v-hasPermi="['member:bonus:receive']"
              >
                领取
              </a-button>
              <a-button
                type="link"
                size="small"
                :disabled="!canGive(record)"
                :title="giveDisabledReason(record)"
                @click="handleGive(record)"
                v-hasPermi="['member:bonus:give']"
              >
                发放
              </a-button>
              <a-button
                type="link"
                size="small"
                :disabled="record.isDistributed === '0'"
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

      <a-modal
        v-model:open="dialogOpen"
        :title="dialogTitle"
        width="800px"
        :footer="formMode === 'view' ? null : undefined"
        ok-text="确定"
        cancel-text="取消"
        @ok="submitForm"
        @cancel="closeDialog"
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
      </a-modal>
    </div>
  </a-drawer>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref, watch } from "vue";
import {
  addBonus,
  delBonus,
  getBonus,
  giveBonus,
  listBonus,
  receiveBonus,
  updateBonus,
} from "@/api/member/bonus";
import { getOrderuser, listOrderuser } from "@/api/member/orderuser";
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
const bonusList = ref([]);
const total = ref(0);
const ids = ref([]);
const dialogOpen = ref(false);
const formMode = ref("create");
const dialogTitle = ref("添加彩金");
const bonusRef = ref();
const userOptions = ref([]);

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
    if (visible.value) refreshAll();
  }
);
watch(visible, (open) => {
  if (open) refreshAll();
  else closeDialog();
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
  Object.assign(form, defaultForm(), { userId: props.userId });
  bonusRef.value?.clearValidate?.();
}

async function refreshAll() {
  queryParams.userId = props.userId;
  await Promise.all([fetchUser(props.userId), getList()]);
}

async function fetchUser(id) {
  if (!id) return;
  loadingUser.value = true;
  try {
    const response = await getOrderuser(id);
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
      memberOrderCountPerDay: value.memberLevel?.orderCountPerDay ?? null,
      lastLoginTime: value.lastLoginTime,
    });
  } finally {
    loadingUser.value = false;
  }
}

async function getList() {
  if (!queryParams.userId) return;
  loading.value = true;
  try {
    const response = await listBonus({ ...queryParams });
    bonusList.value = (response.rows || []).map((row) => ({
      ...row,
      pushUsersDisplay: pushUsersText(row),
    }));
    total.value = response.total || 0;
    ids.value = [];
  } finally {
    loading.value = false;
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
  resetForm();
  formMode.value = "create";
  dialogTitle.value = "添加彩金";
  dialogOpen.value = true;
}

async function loadRecord(row, mode, title) {
  const response = await getBonus(row.id);
  resetForm();
  Object.assign(form, response.data || response);
  form.toUsers = normalizeUserIds(form.toUsers);
  formMode.value = mode;
  dialogTitle.value = title;
  dialogOpen.value = true;
  if (form.pushType === "2") await loadUsers();
  bonusRef.value?.clearValidate?.();
}

function handleView(row) {
  loadRecord(row, "view", "查看彩金");
}

function handleUpdate(row) {
  loadRecord(row, "edit", "修改彩金");
}

function handlePushEdit(row) {
  loadRecord(row, "push", "修改推送设置");
}

async function handleCopy(row) {
  const response = await getBonus(row.id);
  resetForm();
  Object.assign(form, response.data || response, {
    id: null,
    isReceived: "1",
    isDistributed: "1",
    receivedTime: null,
    distributionTime: null,
    createTime: null,
  });
  form.toUsers = normalizeUserIds(form.toUsers);
  formMode.value = "create";
  dialogTitle.value = "复制彩金";
  dialogOpen.value = true;
  if (form.pushType === "2") await loadUsers();
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
  dialogOpen.value = false;
  resetForm();
}

async function submitForm() {
  try {
    await bonusRef.value?.validate?.();
    const payload = {
      ...form,
      userId: props.userId,
      toUsers: form.pushType === "2" ? form.toUsers.join(",") : null,
    };
    if (form.id) {
      await updateBonus(payload);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addBonus(payload);
      proxy.$modal.msgSuccess("新增成功");
    }
    dialogOpen.value = false;
    await getList();
    emit("success");
  } catch (error) {
    if (error?.errorFields) return;
    return;
  }
}

function handlePushTypeChange() {
  if (form.pushType !== "2") form.toUsers = [];
  else loadUsers();
  bonusRef.value?.clearValidate?.(["toUsers"]);
}

async function loadUsers(keyword = "") {
  const normalizedKeyword = typeof keyword === "string" ? keyword.trim() : "";
  loadingUsers.value = true;
  try {
    const response = await listOrderuser({
      pageNum: 1,
      pageSize: 100,
      username: normalizedKeyword || undefined,
    });
    const rows = response.rows || [];
    const selected = normalizeUserIds(form.toUsers).map((id) => ({
      value: id,
      label: String(id),
    }));
    const fetched = rows.map((item) => ({
      value: Number(item.id),
      label: item.username,
    }));
    userOptions.value = [...selected, ...fetched].filter(
      (item, index, all) => all.findIndex((value) => value.value === item.value) === index
    );
  } finally {
    loadingUsers.value = false;
  }
}

let searchTimer;
function handleUserSearch(value) {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => loadUsers(value), 300);
}

async function handleReceive(row) {
  try {
    await proxy.$modal.confirm("确认将该彩金标记为已领取吗？");
    await receiveBonus(row.id);
    proxy.$modal.msgSuccess("领取成功");
    await getList();
  } catch (_) {
    // Cancelled confirmations do not require feedback.
  }
}

async function handleGive(row) {
  try {
    await proxy.$modal.confirm("确认发放该彩金并增加会员余额吗？");
    await giveBonus(row.id);
    proxy.$modal.msgSuccess("发放成功");
    await Promise.all([getList(), fetchUser(props.userId)]);
  } catch (_) {
    // Cancelled confirmations do not require feedback.
  }
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
  try {
    await proxy.$modal.confirm(`确认删除选中的 ${ids.value.length} 条彩金记录吗？`);
    await delBonus(ids.value.join(","));
    proxy.$modal.msgSuccess("删除成功");
    await getList();
  } catch (_) {
    // Cancelled confirmations do not require feedback.
  }
}

function normalizeUserIds(value) {
  if (Array.isArray(value)) return value.map(Number).filter(Number.isFinite);
  if (!value) return [];
  return String(value).split(",").map(Number).filter(Number.isFinite);
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

.user-summary,
.user-summary-loading {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 24px;
  padding: 0 4px 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid #f0f0f0;
  color: #595959;
}

.user-summary {
  flex-wrap: wrap;
}

.user-summary strong {
  color: #262626;
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

:deep(.ant-btn-link) {
  padding-inline: 0;
}
</style>
