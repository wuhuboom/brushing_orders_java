<template>
  <a-drawer
    title="额外佣金设置"
    v-model:open="visible"
    width="90%"
    :destroy-on-close="false"
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
          <a-button type="primary" @click="handleAdd">新 增</a-button>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'isLocked'">
            <dict-tag :options="user_yes_no" :value="record.isLocked" />
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <dict-tag :options="goods_status" :value="record.status" />
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">修改</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </ant-pro-table>

      <a-modal
        :title="formTitle"
        v-model:open="formVisible"
        width="800px"
        ok-text="确 定"
        cancel-text="取 消"
        :confirm-loading="submitting"
        @ok="submitForm"
        @cancel="formVisible = false"
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

          <a-form-item label="用户ID" name="userId">
            <a-input v-model:value="form.userId" disabled />
          </a-form-item>
          <a-form-item label="订单数" name="orderCount">
            <a-input v-model:value="form.orderCount" placeholder="请输入订单数" />
          </a-form-item>
          <a-form-item label="商品价格" name="productPrice">
            <a-input
              v-model:value="form.productPrice"
              placeholder="请输入商品价格"
            />
          </a-form-item>
          <a-form-item label="金额" name="amount">
            <a-input v-model:value="form.amount" placeholder="请输入金额" />
          </a-form-item>
          <a-form-item label="是否锁定" name="isLocked">
            <a-radio-group v-model:value="form.isLocked">
              <a-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :value="dict.value"
                >{{ dict.label }}</a-radio
              >
            </a-radio-group>
          </a-form-item>
        </a-form>
      </a-modal>
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
import { getOrderuser } from "@/api/member/orderuser";

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
watch(visible, (val) => {
  if (val) {
    // 褰撴娊灞夋墦寮€鏃跺垵濮嬪寲鍒楄〃锛堝彧鑾峰彇瀵瑰簲鐢ㄦ埛鐨勬暟鎹級
    fetchList();
    // 鍚屾鑾峰彇鐢ㄦ埛淇℃伅鐢ㄤ簬灞曠ず
    fetchUser(props.userId);
  }
});

// 褰?userId 鍙樺寲涓旀娊灞夊凡鎵撳紑鏃跺埛鏂?
watch(
  () => props.userId,
  (id) => {
    if (visible.value) {
      fetchList();
      fetchUser(id);
    }
  }
);

const list = ref([]);
const loading = ref(false);
const selectedIds = ref([]);

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
  onChange: (_, selectedRows) => handleSelectionChange(selectedRows),
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
    // 灏嗙敤鎴?id 濉埌琛ㄥ崟
    form.userId = u.id;
  } catch (err) {
    console.error(err);
  } finally {
    loadingUser.value = false;
  }
}

const formVisible = ref(false);
const formTitle = ref("");
const submitting = ref(false);

const formRef = ref(null);
const form = reactive({
  id: null,
  userId: null,
  orderCount: null,
  productPrice: null,
  amount: null,
  isLocked: "1",
  createTime: null,
});

const rules = {
  userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
  orderCount: [{ required: true, message: "订单数不能为空", trigger: "blur" }],
  productPrice: [
    { required: true, message: "商品价格不能为空", trigger: "blur" },
  ],
  amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
  isLocked: [
    { required: true, message: "是否锁定不能为空", trigger: "change" },
  ],
  status: [{ required: true, message: "状态不能为空", trigger: "change" }],
};

function fetchList() {
  if (!props.userId) {
    list.value = [];
    return;
  }
  loading.value = true;
  // 璇锋眰鍙甫 userId锛屾寜闇€杩斿洖璇ョ敤鎴风殑棰濆浣ｉ噾璁板綍
  listExtracommission({ userId: props.userId })
    .then((res) => {
      list.value = res.rows || res.data || [];
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map((item) => item.id);
}

function resetFormData() {
  form.id = null;
  form.userId = props.userId;
  form.orderCount = null;
  form.productPrice = null;
  form.amount = null;
  form.isLocked = "1";
  form.status = null;
  form.createTime = null;
}

function handleAdd() {
  resetFormData();
  formTitle.value = "新增额外佣金";
  formVisible.value = true;
}

function handleEdit(row) {
  resetFormData();
  const _id = row.id;
  getExtracommission(_id).then((res) => {
    const data = res.data || res;
    Object.assign(form, data);
    formTitle.value = "修改额外佣金";
    formVisible.value = true;
  });
}

async function submitForm() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (form.id != null) {
      await updateExtracommission(form);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addExtracommission(form);
      proxy.$modal.msgSuccess("新增成功");
    }
    formVisible.value = false;
    fetchList();
    emits("success");
  } finally {
    submitting.value = false;
  }
}

function handleDelete(row) {
  const _id = row.id;
  proxy.$modal
    .confirm(`是否确认删除额外佣金设置编号为"${_id}"的数据项？`)
    .then(() => {
      return delExtracommission(_id);
    })
    .then(() => {
      proxy.$modal.msgSuccess("删除成功");
      fetchList();
      emits("success");
    })
    .catch(() => {});
}

// 褰撴娊灞夊叧闂椂娓呯悊
watch(visible, (val) => {
  if (!val) {
    // 娓呯悊琛ㄥ崟鍙婂垪琛ㄧ紦瀛?
    list.value = [];
    resetFormData();
  }
});
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
.small-padding {
  padding: 6px;
}
.fixed-width {
  width: 140px;
}
</style>




