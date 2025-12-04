<template>
  <el-drawer title="彩金设置" v-model="visible" size="90%" append-to-body>
    <div class="app-container">
      <el-form
        :model="queryParams"
        ref="queryRef"
        :inline="true"
        v-show="showSearch"
        label-width="68px"
      >
        <el-form-item label="金额" prop="amount">
          <el-input
            v-model="queryParams.amount"
            placeholder="请输入金额"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery"
            >搜索</el-button
          >
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div v-if="loadingUser" class="pa20" style="text-align: center">
        <el-spin />
      </div>
      <template v-else>
        <el-row :gutter="10" class="mb8" justify="space-between" align="middle">
          <!-- 左侧：用户基本信息 -->
          <el-col :span="12">
            <el-row :gutter="10">
              <el-col :span="4">
                <div><strong>用户名：</strong> {{ user.username || "-" }}</div>
              </el-col>
              <el-col :span="3">
                <div>
                  <strong>手机号：</strong> {{ user.phoneNumber || "-" }}
                </div>
              </el-col>
              <el-col :span="3">
                <div><strong>余额：</strong> {{ user.balance ?? 0 }}</div>
              </el-col>
              <el-col :span="4">
                <div>
                  <strong>任务进度：</strong>
                  {{ user.taskProgress ?? 0 }} /
                  {{ user.memberOrderCountPerDay ?? "-" }}
                </div>
              </el-col>
              <el-col :span="8">
                <div><strong>最后登录时间：</strong> 2025-10-28 11:28:14</div>
              </el-col>
            </el-row>
          </el-col>

          <!-- 右侧：操作栏 -->
          <el-col :span="12" class="text-right">
            <el-row :gutter="10" justify="end">
              <el-col :span="5">
                <el-button type="primary" plain icon="Plus" @click="handleAdd"
                  >新增</el-button
                >
              </el-col>
              <el-col :span="3">
                <right-toolbar
                  v-model:showSearch="showSearch"
                  @queryTable="getList"
                />
              </el-col>
            </el-row>
          </el-col>
        </el-row>
      </template>

      <el-table
        v-loading="loading"
        :data="bonusList"
        @selection-change="handleSelectionChange"
        :border="true"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="单数" align="center" prop="orderNum" />
        <el-table-column label="金额" align="center" prop="amount" />
        <el-table-column
          label="动画时长"
          align="center"
          prop="animationDuration"
        />
        <el-table-column
          label="显示时长"
          align="center"
          prop="displayDuration"
        />
        <el-table-column
          label="发放类型"
          align="center"
          prop="distributionType"
        >
          <template #default="scope">
            <dict-tag :options="fflx" :value="scope.row.distributionType" />
          </template>
        </el-table-column>
        <el-table-column label="推送类型" align="center" prop="pushType">
          <template #default="scope">
            <dict-tag :options="tslx" :value="scope.row.pushType" />
          </template>
        </el-table-column>
        <el-table-column label="推送会员" align="center">
          <template #default="scope">
            {{ scope.row.pushUsersDisplay }}
          </template>
        </el-table-column>
        <el-table-column label="是否领取" align="center" prop="isReceived">
          <template #default="scope">
            <dict-tag :options="user_yes_no" :value="scope.row.isReceived" />
          </template>
        </el-table-column>
        <el-table-column
          label="领取时间"
          align="center"
          prop="receivedTime"
          width="180"
        >
          <template #default="scope">
            <span>{{ parseTime(scope.row.receivedTime, "{y}-{m}-{d}") }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否发放" align="center" prop="isDistributed">
          <template #default="scope">
            <dict-tag :options="user_yes_no" :value="scope.row.isDistributed" />
          </template>
        </el-table-column>
        <el-table-column
          label="过期时间"
          align="center"
          prop="expiryTime"
          width="180"
        >
          <template #default="scope">
            <span>{{ parseTime(scope.row.expiryTime, "{y}-{m}-{d}") }}</span>
          </template>
        </el-table-column>

        <el-table-column
          label="发放时间"
          align="center"
          prop="distributionTime"
          width="180"
        >
          <template #default="scope">
            <span>{{
              parseTime(scope.row.distributionTime, "{y}-{m}-{d}")
            }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="创建时间"
          align="center"
          prop="createTime"
          width="180"
        >
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime, "{y}-{m}-{d}") }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          align="center"
          class-name="small-padding fixed-width"
        >
          <template #default="scope">
            <el-button
              link
              type="primary"
              @click="handleUpdate(scope.row)"
              :disabled="scope.row.isDistributed === '0'"
              v-hasPermi="['member:bonus:edit']"
              >修改</el-button
            >
            <el-button
              link
              type="primary"
              @click="handleReceive(scope.row)"
              :disabled="
                !(
                  scope.row.isReceived === '1' &&
                  scope.row.isDistributed === '1'
                )
              "
              >领取</el-button
            >
            <el-button
              link
              type="primary"
              @click="handleDistribute(scope.row)"
              :disabled="
                !(
                  scope.row.isReceived === '0' &&
                  scope.row.isDistributed === '1'
                )
              "
              >发放</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />

      <!-- 添加或修改彩金对话框 -->
      <el-dialog :title="title" v-model="open" width="800" append-to-body>
        <el-form
          ref="bonusRef"
          :model="form"
          :rules="rules"
          label-width="80px"
          label-position="top"
        >
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="余额" prop="balance">
                <el-input v-model="user.balance" :disabled="true" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="冻结余额" prop="frozenBalance">
                <el-input v-model="user.frozenBalance" :disabled="true" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="总余额" prop="totalBalance">
                <el-input v-model="user.totalBalance" :disabled="true" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="任务进度" prop="taskProgress">
                <el-input v-model="taskProgressDisplay" :disabled="true" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="单数" prop="orderNum">
                <el-input
                  v-model="form.orderNum"
                  placeholder="请输入单数"
                  :disabled="partialEdit"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="金额" prop="amount">
                <el-input
                  v-model="form.amount"
                  placeholder="请输入金额"
                  :disabled="partialEdit"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="动画时长" prop="animationDuration">
                <el-input
                  v-model="form.animationDuration"
                  placeholder="请输入动画时长"
                  :disabled="partialEdit"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="显示时长" prop="displayDuration">
                <el-input
                  v-model="form.displayDuration"
                  placeholder="请输入显示时长"
                  :disabled="partialEdit"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="发放类型" prop="distributionType">
                <el-select
                  v-model="form.distributionType"
                  placeholder="请选择发放类型"
                  :disabled="partialEdit"
                >
                  <el-option
                    v-for="dict in fflx"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="过期时间" prop="expiryTime">
                <el-date-picker
                  clearable
                  v-model="form.expiryTime"
                  type="datetime"
                  style="width: 100%"
                  placeholder="请选择过期时间"
                  :disabled="partialEdit"
                >
                </el-date-picker>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="推送类型" prop="pushType">
            <el-select
              v-model="form.pushType"
              placeholder="请选择推送类型"
              @change="handlePushTypeChange"
            >
              <el-option
                v-for="dict in tslx"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item
            label="推送用户"
            prop="toUsers"
            v-if="form.pushType === '2'"
          >
            <el-select
              v-model="form.toUsers"
              placeholder="请选择推送用户"
              filterable
              multiple
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="u in usersList"
                :key="u.id"
                :label="u.username"
                :value="u.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button type="primary" @click="submitForm">确 定</el-button>
            <el-button @click="cancel">取 消</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </el-drawer>
</template>

<script setup>
import {
  ref,
  reactive,
  toRefs,
  watch,
  nextTick,
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
watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      nextTick(() => {
        try {
          const wrappers = document.querySelectorAll(".el-drawer__wrapper");
          if (wrappers.length) {
            const w = wrappers[wrappers.length - 1];
            w.style.zIndex = "20000";
            w.style.display = "block";
          }
        } catch (e) {
          console.error("Failed to adjust el-drawer wrapper styles", e);
        }
      });
    }
  }
);

const bonusList = ref([]);
const open = ref(false);
const loading = ref(false);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const userData = ref({});

const usersList = ref([]);
const partialEdit = ref(false);

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
  nextTick(() => {
    proxy.$refs.bonusRef?.clearValidate("toUsers");
  });
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

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
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
  proxy.resetForm && proxy.resetForm("bonusRef");
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
    // 更新form中的只读字段
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

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm && proxy.resetForm("queryRef");
  handleQuery();
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
  // 默认把当前传入的userId填充到表单
  form.value.userId = queryParams.value.userId;
  open.value = true;
  title.value = "添加彩金";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  const _id = row?.id || ids.value;
  getBonus(_id).then((response) => {
    form.value = { ...response.data };
    // 处理toUsers回显：如果是字符串，转换为ID数组
    if (form.value.toUsers && typeof form.value.toUsers === "string") {
      const userIds = form.value.toUsers
        .split(",")
        .map((id) => parseInt(id.trim()))
        .filter((id) => id && !isNaN(id));
      form.value.toUsers = userIds;
    } else if (Array.isArray(form.value.toUsers)) {
      // 如果已经是数组，确保是数字
      form.value.toUsers = form.value.toUsers
        .map((id) => parseInt(id))
        .filter((id) => id && !isNaN(id));
    } else {
      form.value.toUsers = [];
    }
    // 根据pushType设置rules
    handlePushTypeChange(form.value.pushType);
    // 加载对应用户的详细信息
    if (form.value.userId) {
      fetchUser(form.value.userId);
    }
    // 设置partialEdit：如果已领取 (isReceived === '0')，则partial edit
    partialEdit.value = form.value.isReceived === "0";
    nextTick(() => {
      proxy.$refs.bonusRef?.clearValidate();
    });
    open.value = true;
    title.value = "修改彩金";
  });
}

/** 领取操作 */
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

/** 发放操作 */
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

/** 提交按钮 */
function submitForm() {
  proxy.$refs["bonusRef"]?.validate?.((valid) => {
    if (valid) {
      // 转换toUsers为逗号分隔的字符串（仅当pushType='2'时）
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
    }
  });
}

/** 导出按钮操作 */
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
</style>
