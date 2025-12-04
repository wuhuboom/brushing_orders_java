<template>
  <el-drawer
    :title="title"
    v-model="visible"
    size="92%"
    with-header
    :destroy-on-close="false"
  >
    <div class="app-container">
      <!-- 搜索表单 -->
      <el-form
        :model="queryParams"
        ref="queryRef"
        :inline="true"
        v-show="showSearch"
        label-width="68px"
      >
        <el-form-item label="连单ID" prop="linkOrderId">
          <el-input
            v-model="queryParams.linkOrderId"
            placeholder="请输入连单ID"
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
              <el-col :span="2.5">
                <el-button type="primary" plain icon="Plus" @click="handleAdd"
                  >新增</el-button
                >
              </el-col>
              <el-col :span="2.5">
                <el-button
                  type="danger"
                  plain
                  icon="Delete"
                  :disabled="multiple"
                  @click="handleDelete"
                  >删除</el-button
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
        :data="orderlinkList"
        @selection-change="handleSelectionChange"
        :border="true"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="自增ID" align="center" prop="id" />
        <el-table-column label="连单ID" align="center" prop="linkOrderId" />
        <el-table-column label="单数" align="center" prop="orderCount" />
        <el-table-column
          label="返佣倍数"
          align="center"
          prop="commissionMultiple"
        />
        <el-table-column label="商品图片" align="center" prop="productImage">
          <template #default="scope">
            <image-preview
              :src="scope.row.productImage"
              :width="50"
              :height="50"
            />
          </template>
        </el-table-column>
        <el-table-column label="商品标题" align="center" prop="productTitle" />

        <el-table-column label="价格类型" align="center" prop="priceType">
          <template #default="scope">
            <dict-tag :options="price_type" :value="scope.row.priceType" />
          </template>
        </el-table-column>
        <el-table-column label="价格" align="center" prop="price" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <el-tag v-if="scope.row.status == '1'" type="danger" effect="dark"
              >未完成</el-tag
            >
            <el-tag v-else type="success" effect="dark">已完成</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="创建时间"
          align="center"
          prop="createdTime"
          width="180"
        >
          <template #default="scope">
            <span>{{ parseTime(scope.row.createdTime) }}</span>
          </template>
        </el-table-column>
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
              @click="handleUpdate(scope.row)"
              v-hasPermi="['member:orderlink:edit']"
            ></el-button>
            <el-button
              circle
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['member:orderlink:remove']"
            ></el-button>
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
    </div>

    <!-- 修改连单对话框 -->
    <el-dialog :title="dialogTitle" v-model="open" width="500px" append-to-body>
      <el-form
        ref="orderlinkRef"
        :model="editForm"
        label-position="top"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="价格类型" prop="priceType">
          <el-radio-group v-model="editForm.priceType">
            <el-radio
              v-for="dict in price_type"
              :key="dict.value"
              :label="dict.value"
              >{{ dict.label }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number
            v-model="editForm.price"
            placeholder="请输入价格"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新增/修改连单抽屉 -->
    <el-drawer
      :title="addTitle"
      v-model="addDrawerVisible"
      size="85%"
      with-header
      :destroy-on-close="false"
    >
      <div class="app-container">
        <!-- 顶部：用户信息 + 调整表单 -->
        <div v-if="loadingUser" class="pa20" style="text-align: center">
          <el-spin />
        </div>

        <el-form
          v-else
          ref="addFormRef"
          :model="form"
          label-position="top"
          class="mb16"
          label-width="100px"
        >
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="任务进度" prop="taskProgress">
                <el-input v-model="taskProgressDisplay" :disabled="true" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="单数" prop="orderCount">
                <el-input-number
                  v-model="form.orderCount"
                  :min="1"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="返佣倍数" prop="commissionMultiple">
                <el-input-number
                  v-model="form.commissionMultiple"
                  :min="1"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="余额" prop="balance">
                <el-input v-model="user.balance" :disabled="true" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="冻结余额" prop="frozenBalance">
                <el-input v-model="user.frozenBalance" :disabled="true" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="总余额" prop="totalBalance">
                <el-input v-model="user.totalBalance" :disabled="true" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <!-- 明细：已选商品，可编辑价格类型与价格 -->
        <div class="mb12">
          <div
            style="
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 8px;
            "
          >
            <div><strong>明细</strong></div>
            <div style="display: flex; gap: 8px">
              <el-button
                size="small"
                type="primary"
                @click="onSaveAll"
                :loading="saving"
                >提交保存</el-button
              >
              <el-button size="small" @click="onCancelEdit">取消</el-button>
            </div>
          </div>

          <el-table :data="details" border style="width: 100%">
            <el-table-column
              prop="id"
              label="商品ID"
              width="120"
              align="center"
            />
            <el-table-column prop="title" label="商品标题" align="center" />
            <el-table-column label="价格类型" width="180" align="center">
              <template #default="scope">
                <el-select
                  v-model="scope.row.priceType"
                  placeholder="请选择"
                  size="small"
                  :disabled="!scope.row.isEditing"
                >
                  <el-option
                    v-for="opt in price_type"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="价格" width="180" align="center">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.price"
                  :min="0"
                  size="small"
                  :controls="false"
                  :disabled="!scope.row.isEditing"
                />
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template #default="scope">
                <el-tag type="danger" effect="dark">未完成</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center">
              <template #default="scope">
                <el-button
                  v-if="!scope.row.isEditing"
                  size="small"
                  type="primary"
                  @click="editDetail(scope.row)"
                  >编辑</el-button
                >
                <el-button
                  v-if="scope.row.isEditing"
                  size="small"
                  type="success"
                  @click="saveDetail(scope.row)"
                  >保存</el-button
                >
                <el-button
                  size="small"
                  type="danger"
                  @click="removeDetail(scope.row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 商品列表：用于从商品管理选择商品放入明细 -->
        <div>
          <div
            style="
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin: 20px 0 10px 0;
            "
          >
            <div><strong>商品列表</strong></div>
            <div style="display: flex; gap: 8px; align-items: center">
              <el-input
                v-model="goodsQuery.title"
                placeholder="搜索标题"
                size="small"
                clearable
                @keyup.enter="fetchGoods"
              />
              <el-button size="small" icon="Search" @click="fetchGoods"
                >搜索</el-button
              >
              <el-button size="small" icon="Refresh" @click="resetGoodsQuery"
                >重置</el-button
              >
            </div>
          </div>

          <el-table
            :data="goodsList"
            border
            @row-dblclick="addProductToDetails"
          >
            <el-table-column
              prop="id"
              label="商品ID"
              width="100"
              align="center"
            />
            <el-table-column prop="title" label="标题" align="center" />
            <el-table-column
              prop="typeTitle"
              label="类目"
              width="160"
              align="center"
            />
            <el-table-column
              prop="price"
              label="价格"
              width="160"
              align="center"
            />
            <el-table-column
              prop="image"
              label="图片"
              width="160"
              align="center"
            >
              <template #default="scope">
                <image-preview
                  :src="scope.row.image"
                  :width="50"
                  :height="50"
                />
              </template>
            </el-table-column>
            <el-table-column
              prop="createTime"
              label="创建时间"
              width="180"
              align="center"
            >
              <template #default="scope">
                <span>{{ parseTime(scope.row.createTime) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="goodsTotal > 0"
            :total="goodsTotal"
            v-model:page="goodsQuery.pageNum"
            v-model:limit="goodsQuery.pageSize"
            @pagination="fetchGoods"
          />
        </div>
      </div>
    </el-drawer>
  </el-drawer>
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
  listOrderlink,
  getOrderlink,
  delOrderlink,
  addOrderlink,
  updateOrderlink,
} from "@/api/member/orderlink";
import { getOrderuser } from "@/api/member/orderuser";
import { listGoods } from "@/api/member/goods";
import { parseTime } from "@/utils/ruoyi";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  userId: {
    type: [String, Number],
    default: null,
  },
  title: {
    type: String,
    default: "连单设置",
  },
});
const emit = defineEmits(["update:modelValue", "success"]);

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});

const { proxy } = getCurrentInstance();
const { sys_common_status, price_type } = proxy.useDict(
  "sys_common_status",
  "price_type"
);

const orderlinkList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    linkOrderId: null,
    userId: null,
  },
});

const { queryParams } = toRefs(data);

// 新增/修改连单抽屉相关
const addDrawerVisible = ref(false);
const addTitle = ref("新增连单");
const saving = ref(false);
const addFormRef = ref(null);

const formData = reactive({
  form: {
    id: null,
    userId: props.userId ?? null,
    orderCount: 1,
    commissionMultiple: 1,
    username: null,
    phoneNumber: null,
    taskProgress: null,
    balance: 0,
    frozenBalance: 0,
    totalBalance: 0,
  },
});

const details = ref([]); // { id, title, priceType, price, status, isEditing }

const goodsList = ref([]);
const goodsQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  title: "",
});
const goodsTotal = ref(0);
const goodsLoading = ref(false);

const { form } = toRefs(formData);

// 计算属性：任务进度显示
const taskProgressDisplay = computed(() => {
  return `${form.taskProgress ?? 0} / ${form.memberOrderCountPerDay ?? "-"}`;
});

function getList() {
  loading.value = true;
  const params = {
    ...queryParams.value,
    userId: props.userId ?? queryParams.value.userId,
  };
  listOrderlink(params)
    .then((response) => {
      orderlinkList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
}

// fetch user basic info
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

// goods list
function fetchGoods() {
  goodsLoading.value = true;
  listGoods({
    ...goodsQuery,
  })
    .then((res) => {
      goodsList.value = res.rows || res.data?.rows || res.data || [];
      goodsTotal.value = res.total ?? res.data?.total ?? 0;
    })
    .catch(() => {})
    .finally(() => {
      goodsLoading.value = false;
    });
}

function resetGoodsQuery() {
  Object.assign(goodsQuery, { title: "", pageNum: 1 });
  fetchGoods();
}

// add product to details
function addProductToDetails(row) {
  const exists = details.value.find((d) => d.id === row.id);
  if (exists) {
    proxy.$modal.msgInfo("商品已添加至明细");
    return;
  }
  let defaultPriceType = "";
  const found = price_type.value.find((opt) => opt.label === "商品价格");
  if (found) {
    defaultPriceType = found.value;
  } else if (price_type.value.length > 0) {
    defaultPriceType = price_type.value[0].value;
  }
  console.log(defaultPriceType);
  details.value.push({
    id: row.id,
    title: row.title,
    priceType: defaultPriceType,
    price: row.price ?? 0,
    status: 1,
    isEditing: false,
  });
}

// remove detail
function removeDetail(row) {
  details.value = details.value.filter((d) => d.id !== row.id);
}

// edit detail
function editDetail(row) {
  row.isEditing = true;
}

// save detail
function saveDetail(row) {
  if (row.price == null || row.price === "" || isNaN(Number(row.price))) {
    proxy.$modal.msgError("请填写有效价格");
    return;
  }
  row.isEditing = false;
}

// cancel edit
function onCancelEdit() {
  details.value.forEach((d) => (d.isEditing = false));
  addDrawerVisible.value = false;
}

// save all
async function onSaveAll() {
  if (!props.userId) {
    proxy.$modal.msgError("缺少用户ID");
    return;
  }
  if (!form.value.orderCount || form.value.orderCount < 1) {
    proxy.$modal.msgError("请填写有效的单数");
    return;
  }
  if (!details.value.length) {
    proxy.$modal.msgError("请添加至少一个商品到明细");
    return;
  }
  for (const d of details.value) {
    if (d.price == null || d.price === "" || isNaN(Number(d.price))) {
      proxy.$modal.msgError("明细中存在无效价格");
      return;
    }
  }

  const payload = {
    userId: props.userId,
    orderCount: form.value.orderCount,
    commissionMultiple: form.value.commissionMultiple,
    details: details.value.map((d) => ({
      goodsId: d.id,
      priceType: d.priceType,
      price: d.price,
    })),
  };

  saving.value = true;
  try {
    if (form.value.id) {
      await updateOrderlink({ ...payload, id: form.value.id });
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addOrderlink(payload);
      proxy.$modal.msgSuccess("新增成功");
    }
    addDrawerVisible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    proxy.$modal.msgError("保存失败");
  } finally {
    saving.value = false;
  }
}

// edit orderlink
async function handleInnerUpdate(orderlinkId) {
  try {
    const res = await getOrderlink(orderlinkId);
    const data = res.data || res;
    form.id = data.id;
    form.userId = data.userId;
    form.orderCount = data.orderCount;
    form.commissionMultiple = data.commissionMultiple;
    const items = data.details || [];
    details.value = items.map((it) => ({
      id: it.goodsId ?? it.productId ?? it.id,
      title: it.title ?? "",
      priceType: it.priceType,
      price: it.price ?? 0,
      status: it.status ?? 1,
      isEditing: false,
    }));
  } catch (e) {
    console.error(e);
  }
}

function resetForm() {
  form.id = null;
  form.userId = props.userId ?? null;
  form.orderCount = 1;
  form.commissionMultiple = 1;
  form.username = null;
  form.phoneNumber = null;
  form.taskProgress = null;
  form.balance = 0;
  form.frozenBalance = 0;
  form.totalBalance = 0;
  details.value = [];
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm && proxy.$refs.queryRef.resetFields();
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

// outer handleAdd
function handleAdd() {
  resetForm();
  addTitle.value = "新增连单";
  addDrawerVisible.value = true;
  fetchGoods();
  if (!user.id && props.userId) {
    fetchUser(props.userId);
  }
}

// outer handleUpdate - 打开对话框
function handleUpdate(row) {
  Object.assign(editForm, {
    priceType: row.priceType || "",
    price: row.price || 0,
  });
  dialogTitle.value = "修改";
  currentRowId.value = row.id;
  open.value = true;
}

// 修改连单对话框相关
const open = ref(false);
const dialogTitle = ref("修改");
const currentRowId = ref(null);
const orderlinkRef = ref(null);
const editForm = reactive({
  priceType: "",
  price: 0,
});
const rules = reactive({
  priceType: [{ required: true, message: "请选择价格类型", trigger: "change" }],
  price: [{ required: true, message: "请输入价格", trigger: "blur" }],
});

// submit form
function submitForm() {
  orderlinkRef.value.validate((valid) => {
    if (valid) {
      const payload = {
        ...editForm,
        id: currentRowId.value,
      };
      updateOrderlink(payload)
        .then(() => {
          open.value = false;
          proxy.$modal.msgSuccess("修改成功");
          getList();
        })
        .catch(() => {
          proxy.$modal.msgError("修改失败");
        });
    } else {
      console.log("error submit!");
    }
  });
}

// cancel
function cancel() {
  open.value = false;
  orderlinkRef.value.resetFields();
}

function handleDelete(row) {
  const _ids = row ? [row.id] : ids.value;
  proxy.$modal
    .confirm('是否确认删除连单编号为"' + _ids + '"的数据项？')
    .then(() => delOrderlink(_ids.join(",")))
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
      emit("success");
    })
    .catch(() => {});
}

watch(
  () => props.userId,
  (id) => {
    queryParams.value.userId = id ?? queryParams.value.userId;
    if (visible.value) {
      fetchUser(id);
      getList();
    }
  }
);

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      queryParams.value.userId = props.userId ?? queryParams.value.userId;
      fetchUser(props.userId);
      getList();
    } else {
      addDrawerVisible.value = false;
    }
  }
);

watch(
  () => addDrawerVisible.value,
  (val) => {
    if (!val) {
      getList();
    }
  }
);

getList();
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
.mb12 {
  margin-bottom: 12px;
}
.pa20 {
  padding: 20px;
}
.button-flex {
  display: flex;
  gap: 8px;
}
.dialog-footer {
  padding: 0;
}
.mb16 {
  margin-bottom: 16px;
}
.app-container {
  padding: 12px 18px;
}
</style>
