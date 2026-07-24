<template>
  <a-drawer v-model:open="visible" title="彩金设置" width="90%">
    <div class="drawer-table-wrap ant-pro-member-page">
      <div v-if="loadingUser" class="drawer-user-loading">加载中...</div>
      <div v-else class="drawer-user-summary">
        <span><strong>用户名：</strong>{{ user.username || "-" }}</span>
        <span><strong>手机号：</strong>{{ user.phoneNumber || "-" }}</span>
        <span><strong>余额：</strong>{{ user.balance ?? 0 }}</span>
        <span><strong>任务进度：</strong>{{ user.taskProgress ?? 0 }} / {{ user.memberOrderCountPerDay ?? "-" }}</span>
      </div>

      <ant-pro-table
        title="彩金列表"
        :columns="bonusColumns"
        :data-source="bonusList"
        :loading="loading"
        row-key="id"
        :row-selection="rowSelection"
        :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
        :scroll="{ x: 1800 }"
        @page-change="handleAntPageChange"
        @refresh="getList"
      >
        <template #search>
          <a-form layout="horizontal" :model="queryParams">
            <a-row :gutter="24" align="middle">
              <a-col :span="7">
                <a-form-item label="金额">
                  <a-input v-model:value="queryParams.amount" placeholder="请输入金额" allow-clear @pressEnter="handleQuery" />
                </a-form-item>
              </a-col>
              <a-col :span="17" class="ant-pro-query-actions">
                <a-space>
                  <a-button @click="resetQuery">重 置</a-button>
                  <a-button type="primary" @click="handleQuery">查 询</a-button>
                </a-space>
              </a-col>
            </a-row>
          </a-form>
        </template>
        <template #toolbar>
          <a-button type="primary" @click="handleAdd" v-hasPermi="['member:bonus:add']">新 增</a-button>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'distributionType'"><dict-tag :options="fflx" :value="record.distributionType" /></template>
          <template v-else-if="column.dataIndex === 'pushType'"><dict-tag :options="tslx" :value="record.pushType" /></template>
          <template v-else-if="column.dataIndex === 'isReceived' || column.dataIndex === 'isDistributed'"><dict-tag :options="user_yes_no" :value="record[column.dataIndex]" /></template>
          <template v-else-if="['receivedTime','expiryTime','distributionTime','createTime'].includes(column.dataIndex)">{{ parseTime(record[column.dataIndex]) }}</template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space wrap>
              <a-button type="link" size="small" :disabled="record.isDistributed === '0'" @click="handleUpdate(record)" v-hasPermi="['member:bonus:edit']">修改</a-button>
              <a-button type="link" size="small" :disabled="!(record.isReceived === '1' && record.isDistributed === '1')" @click="handleReceive(record)">领取</a-button>
              <a-button type="link" size="small" :disabled="!(record.isReceived === '0' && record.isDistributed === '1')" @click="handleDistribute(record)">发放</a-button>
            </a-space>
          </template>
        </template>
      </ant-pro-table>

      <a-modal
        v-model:open="open"
        :title="title"
        width="800px"
        ok-text="确 定"
        cancel-text="取 消"
        @ok="submitForm"
        @cancel="cancel"
      >
        <a-form ref="bonusRef" :model="form" :rules="rules" layout="vertical">
          <a-row :gutter="[20, 0]">
            <a-col :span="12">
              <a-form-item label="余额">
                <a-input :value="user.balance" disabled />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="冻结余额">
                <a-input :value="user.frozenBalance" disabled />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="总余额">
                <a-input :value="user.totalBalance" disabled />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="任务进度">
                <a-input :value="taskProgressDisplay" disabled />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="单数" name="orderNum">
                <a-input v-model:value="form.orderNum" placeholder="请输入单数" :disabled="partialEdit" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="金额" name="amount">
                <a-input v-model:value="form.amount" placeholder="请输入金额" :disabled="partialEdit" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="动画时长" name="animationDuration">
                <a-input v-model:value="form.animationDuration" placeholder="请输入动画时长" :disabled="partialEdit" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="显示时长" name="displayDuration">
                <a-input v-model:value="form.displayDuration" placeholder="请输入显示时长" :disabled="partialEdit" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="发放类型" name="distributionType">
                <a-select v-model:value="form.distributionType" placeholder="请选择发放类型" :disabled="partialEdit">
                  <a-select-option v-for="dict in fflx" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="过期时间" name="expiryTime">
                <a-date-picker
                  v-model:value="form.expiryTime"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="请选择过期时间"
                  :disabled="partialEdit"
                  class="full-width"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="推送类型" name="pushType">
                <a-select v-model:value="form.pushType" placeholder="请选择推送类型" @change="handlePushTypeChange">
                  <a-select-option v-for="dict in tslx" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="form.pushType === '2'" :span="24">
              <a-form-item label="推送用户" name="toUsers">
                <a-select
                  v-model:value="form.toUsers"
                  placeholder="请选择推送用户"
                  mode="multiple"
                  show-search
                  allow-clear
                  class="full-width"
                >
                  <a-select-option v-for="u in usersList" :key="u.id" :value="u.id">
                    {{ u.username }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-modal>
    </div>
  </a-drawer>
</template>

<script setup>
import {
  ref,
  reactive,
  toRefs,
  watch,
  computed,
  getCurrentInstance,
} from "vue";
import {
  listBonus,
  getBonus,
  delBonus,
  addBonus,
  updateBonus,
} from "@/api/member/bonus";
import { getOrderuser, allUser } from "@/api/member/orderuser";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  userId: { type: [String, Number], default: null },
});
const emits = defineEmits(["update:modelValue", "success"]);

const { proxy } = getCurrentInstance();
const { tslx, user_yes_no, fflx } = proxy.useDict(
  "tslx",
  "user_yes_no",
  "fflx"
);

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emits("update:modelValue", v),
});

// Keep a runtime z-index/display fix when drawer is opened by parent
const bonusList = ref([]);
const open = ref(false);
const loading = ref(false);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const bonusColumns = [
  { title: "单数", dataIndex: "orderNum", align: "center", width: 100 },
  { title: "金额", dataIndex: "amount", align: "center", width: 120 },
  { title: "动画时长", dataIndex: "animationDuration", align: "center", width: 120 },
  { title: "显示时长", dataIndex: "displayDuration", align: "center", width: 120 },
  { title: "发放类型", dataIndex: "distributionType", align: "center", width: 120 },
  { title: "推送类型", dataIndex: "pushType", align: "center", width: 120 },
  { title: "推送会员", dataIndex: "pushUsersDisplay", align: "center", width: 180 },
  { title: "是否领取", dataIndex: "isReceived", align: "center", width: 120 },
  { title: "领取时间", dataIndex: "receivedTime", align: "center", width: 180 },
  { title: "是否发放", dataIndex: "isDistributed", align: "center", width: 120 },
  { title: "过期时间", dataIndex: "expiryTime", align: "center", width: 180 },
  { title: "发放时间", dataIndex: "distributionTime", align: "center", width: 180 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "操作", dataIndex: "action", align: "center", width: 180, fixed: "right" },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_, selectedRows) => handleSelectionChange(selectedRows),
}));
const total = ref(0);
const title = ref("");
const userData = ref({});

const usersList = ref([]);
const partialEdit = ref(false);
const bonusRef = ref(null);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: props.userId,
    amount: null,
  },
  rules: {
    orderNum: [{ required: true, message: "单数不能为空", trigger: "blur" }],
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
    animationDuration: [
      { required: true, message: "动画时长不能为空", trigger: "blur" },
    ],
    displayDuration: [
      { required: true, message: "显示时长不能为空", trigger: "blur" },
    ],
    distributionType: [
      { required: true, message: "发放类型不能为空", trigger: "change" },
    ],
    expiryTime: [
      { required: true, message: "过期时间不能为空", trigger: "blur" },
    ],
    pushType: [
      { required: true, message: "推送类型不能为空", trigger: "change" },
    ],
    toUsers: [],
  },
});

const { queryParams, form, rules } = toRefs(data);

watch(
  () => props.userId,
  (id) => {
    queryParams.value.userId = id;
    if (visible.value) {
      queryParams.value.pageNum = 1;
      getList();
      fetchUser(id);
    }
  }
);

watch(visible, async (v) => {
  if (v) {
    await loadUsers();
    queryParams.value.pageNum = 1;
    queryParams.value.userId = props.userId;
    getList();
  }
});

const handlePushTypeChange = (val) => {
  if (val !== "2") {
    form.value.toUsers = [];
    rules.value.toUsers = [];
  } else {
    rules.value.toUsers = [
      { required: true, message: "请选择推送用户", trigger: "change" },
    ];
  }
  bonusRef.value?.clearValidate?.(["toUsers"]);
};

function loadUsers() {
  allUser()
    .then((response) => {
      usersList.value = response.rows || response.data || response;
    })
    .catch(() => {
      usersList.value = [];
    });
}

function getList() {
  loading.value = true;
  listBonus(queryParams.value)
    .then((response) => {
      bonusList.value = (response.rows || []).map((row) => {
        if (row.pushType === "2" && row.toUsers) {
          const ids = row.toUsers
            .split(",")
            .map((id) => parseInt(id.trim()))
            .filter((id) => id && !isNaN(id));
          const usernames = ids
            .map(
              (id) => usersList.value.find((u) => u.id === id)?.username || id
            )
            .join(", ");
          row.pushUsersDisplay = usernames || "-";
        } else {
          row.pushUsersDisplay = "-";
        }
        return row;
      });
      total.value = response.total;
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
}

// 鍙栨秷鎸夐挳
function cancel() {
  open.value = false;
  reset();
}

// 琛ㄥ崟閲嶇疆
function reset() {
  form.value = {
    id: null,
    orderNum: null,
    userId: null,
    amount: null,
    animationDuration: null,
    displayDuration: null,
    distributionType: "1",
    expiryTime: null,
    pushType: null,
    createTime: null,
    isDistributed: null,
    isReceived: null,
    distributionTime: null,
    toUsers: [],
  };
  rules.value.toUsers = [];
  partialEdit.value = false;
  bonusRef.value?.clearValidate?.();
}

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
    });
    return;
  }
  loadingUser.value = true;
  try {
    const res = await getOrderuser(id);
    const u = res.data || res;
    user.id = u.id;
    user.username = u.username;
    user.phoneNumber = u.phoneNumber;
    user.balance = u.balance ?? 0;
    user.frozenBalance = u.frozenBalance ?? u.freezeBalance ?? 0;
    user.totalBalance = u.totalBalance ?? user.balance + user.frozenBalance;
    user.taskProgress = u.taskProgress ?? 0;
    user.memberOrderCountPerDay = u.memberLevel?.orderCountPerDay ?? null;
    // 鏇存柊form涓殑鍙瀛楁
    form.username = user.username;
    form.phoneNumber = user.phoneNumber;
    form.taskProgress = user.taskProgress;
    form.memberOrderCountPerDay = user.memberOrderCountPerDay;
    form.balance = user.balance;
    form.frozenBalance = user.frozenBalance;
    form.totalBalance = user.totalBalance;
    form.userId = u.id;
  } catch (err) {
    console.error(err);
  } finally {
    loadingUser.value = false;
  }
}
const taskProgressDisplay = computed(() => {
  return `${user.taskProgress ?? 0} / ${user.memberOrderCountPerDay ?? "-"}`;
});

/** 鎼滅储鎸夐挳鎿嶄綔 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 閲嶇疆鎸夐挳鎿嶄綔 */
function resetQuery() {
  proxy.resetForm && proxy.resetForm("queryRef");
  handleQuery();
}

// 澶氶€夋閫変腑鏁版嵁
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

/** 鏂板鎸夐挳鎿嶄綔 */
function handleAdd() {
  reset();
  // 榛樿鎶婂綋鍓嶄紶鍏ョ殑userId濉厖鍒拌〃鍗?
  form.value.userId = queryParams.value.userId;
  open.value = true;
  title.value = "添加彩金";
}

/** 淇敼鎸夐挳鎿嶄綔 */
function handleUpdate(row) {
  const _id = row?.id || ids.value;
  getBonus(_id).then((response) => {
    form.value = { ...response.data };
    // 澶勭悊toUsers鍥炴樉锛氬鏋滄槸瀛楃涓诧紝杞崲涓篒D鏁扮粍
    if (form.value.toUsers && typeof form.value.toUsers === "string") {
      const userIds = form.value.toUsers
        .split(",")
        .map((id) => parseInt(id.trim()))
        .filter((id) => id && !isNaN(id));
      form.value.toUsers = userIds;
    } else if (Array.isArray(form.value.toUsers)) {
      // 濡傛灉宸茬粡鏄暟缁勶紝纭繚鏄暟瀛?
      form.value.toUsers = form.value.toUsers
        .map((id) => parseInt(id))
        .filter((id) => id && !isNaN(id));
    } else {
      form.value.toUsers = [];
    }
    // 鏍规嵁pushType璁剧疆rules
    handlePushTypeChange(form.value.pushType);
    // 鍔犺浇瀵瑰簲鐢ㄦ埛鐨勮缁嗕俊鎭?
    if (form.value.userId) {
      fetchUser(form.value.userId);
    }
    // 璁剧疆partialEdit锛氬鏋滃凡棰嗗彇 (isReceived === '0')锛屽垯partial edit
    partialEdit.value = form.value.isReceived === "0";
    bonusRef.value?.clearValidate?.();
    open.value = true;
    title.value = "修改彩金";
  });
}

/** 棰嗗彇鎿嶄綔 */
function handleReceive(row) {
  proxy.$modal
    .confirm("是否确认领取？")
    .then(() => {
      const params = {
        id: row.id,
        isReceived: "0",
        receivedTime: new Date(),
      };
      updateBonus(params).then(() => {
        proxy.$modal.msgSuccess("领取成功");
        getList();
      });
    })
    .catch(() => {});
}

/** 鍙戞斁鎿嶄綔 */
function handleDistribute(row) {
  proxy.$modal
    .confirm("是否确认发放？")
    .then(() => {
      const params = {
        id: row.id,
        isDistributed: "0",
        distributionTime: new Date(),
      };
      updateBonus(params).then(() => {
        proxy.$modal.msgSuccess("发放成功");
        getList();
      });
    })
    .catch(() => {});
}

/** 鎻愪氦鎸夐挳 */
function submitForm() {
  bonusRef.value?.validate?.().then(() => {
      // 杞崲toUsers涓洪€楀彿鍒嗛殧鐨勫瓧绗︿覆锛堜粎褰損ushType='2'鏃讹級
      if (
        form.value.pushType === "2" &&
        form.value.toUsers &&
        form.value.toUsers.length > 0
      ) {
        form.value.toUsers = form.value.toUsers.join(",");
      } else {
        form.value.toUsers = null;
      }
      if (form.value.id != null) {
        updateBonus(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
          emits("success");
        });
      } else {
        addBonus(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
          emits("success");
        });
      }
  }).catch(() => {});
}

/** 瀵煎嚭鎸夐挳鎿嶄綔 */
function handleExport() {
  proxy.download &&
    proxy.download(
      "member/bonus/export",
      { ...queryParams.value },
      `bonus_${new Date().getTime()}.xlsx`
    );
}
</script>

<style scoped>
.app-container {
  padding: 16px;
}
.mb8 {
  margin-bottom: 8px;
}
.button-flex {
  display: flex;
  gap: 8px;
}
.small-padding {
  padding: 4px;
}
.user-info-flex {
  display: flex;
  flex-wrap: nowrap;
  gap: 20px;
  align-items: center;
}
.user-info-flex .info-item {
  white-space: nowrap;
  flex-shrink: 0;
}
.action-toolbar {
  display: flex;
  flex-wrap: nowrap;
  gap: 10px;
  justify-content: flex-end;
  align-items: center;
}

.full-width {
  width: 100%;
}
</style>






