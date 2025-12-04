<template>
  <el-drawer title="额外佣金设置" v-model="visible" size="90%" append-to-body>
    <div>
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
                <div><strong>最后登录时间：</strong> -</div>
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
                  @queryTable="fetchList"
                />
              </el-col>
            </el-row>
          </el-col>
        </el-row>
      </template>

      <el-table
        v-loading="loading"
        :data="list"
        @selection-change="handleSelectionChange"
        :border="true"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="ID" align="center" prop="id" />
        <el-table-column label="单数" align="center" prop="orderCount" />
        <el-table-column label="价格" align="center" prop="productPrice" />
        <el-table-column label="金额" align="center" prop="amount" />
        <el-table-column label="是否锁定" align="center" prop="isLocked">
          <template #default="scope">
            <dict-tag :options="user_yes_no" :value="scope.row.isLocked" />
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="goods_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" />
        <el-table-column
          label="操作"
          align="center"
          class-name="small-padding fixed-width"
        >
          <template #default="scope">
            <el-button
              circle
              type="primary"
              icon="Edit"
              @click="handleEdit(scope.row)"
            ></el-button>
            <el-button
              circle
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
            ></el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog
        :title="formTitle"
        v-model="formVisible"
        width="800px"
        append-to-body
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
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

          <el-form-item label="用户id" prop="userId">
            <el-input v-model="form.userId" disabled />
          </el-form-item>
          <el-form-item label="订单数" prop="orderCount">
            <el-input v-model="form.orderCount" placeholder="请输入订单数" />
          </el-form-item>
          <el-form-item label="商品价格" prop="productPrice">
            <el-input
              v-model="form.productPrice"
              placeholder="请输入商品价格"
            />
          </el-form-item>
          <el-form-item label="金额" prop="amount">
            <el-input v-model="form.amount" placeholder="请输入金额" />
          </el-form-item>
          <el-form-item label="是否锁定" prop="isLocked">
            <el-radio-group v-model="form.isLocked">
              <el-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button type="primary" @click="submitForm">确 定</el-button>
            <el-button @click="formVisible = false">取 消</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </el-drawer>
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
    // 当抽屉打开时初始化列表（只获取对应用户的数据）
    fetchList();
    // 同步获取用户信息用于展示
    fetchUser(props.userId);
  }
});

// 当 userId 变化且抽屉已打开时刷新
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
    // 将用户 id 填到表单
    form.userId = u.id;
  } catch (err) {
    console.error(err);
  } finally {
    loadingUser.value = false;
  }
}

const formVisible = ref(false);
const formTitle = ref("");

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
  userId: [{ required: true, message: "用户id不能为空", trigger: "blur" }],
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
  // 请求只带 userId，按需返回该用户的额外佣金记录
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

function submitForm() {
  formRef.value.validate((valid) => {
    if (!valid) return;
    if (form.id != null) {
      updateExtracommission(form).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        formVisible.value = false;
        fetchList();
        emits("success");
      });
    } else {
      addExtracommission(form).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        formVisible.value = false;
        fetchList();
        emits("success");
      });
    }
  });
}

function handleDelete(row) {
  const _id = row.id;
  proxy.$modal
    .confirm('是否确认删除额外佣金设置编号为"' + _id + '"的数据项？')
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

// 当抽屉关闭时清理
watch(visible, (val) => {
  if (!val) {
    // 清理表单及列表缓存
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
