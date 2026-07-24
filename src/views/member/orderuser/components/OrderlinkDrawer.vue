<template>
  <a-drawer
    :title="title"
    v-model:open="visible"
    width="92%"
    :destroy-on-close="false"
  >
    <div class="app-container ant-pro-member-page">
      <ant-pro-table
        title="列表"
        :columns="orderlinkColumns"
        :data-source="orderlinkList"
        :loading="loading"
        row-key="id"
        :row-selection="rowSelection"
        :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
        :scroll="{ x: 1500 }"
        @page-change="handleAntPageChange"
        @refresh="getList"
      >
        <template #search>
          <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form" v-show="showSearch">
            <a-row :gutter="[24, 16]" align="middle">
              <a-col :xs="24" :sm="12" :md="8" :lg="7">
                <a-form-item label="连单ID">
                  <a-input v-model:value="queryParams.linkOrderId" allow-clear placeholder="请输入连单ID" @pressEnter="handleQuery" />
                </a-form-item>
              </a-col>
              <a-col flex="auto" class="ant-pro-query-actions">
                <a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="handleQuery">查询</a-button></a-space>
              </a-col>
            </a-row>
          </a-form>
        </template>
        <template #toolbar>
          <a-space>
            <a-button type="primary" @click="handleAdd">新增</a-button>
            <a-button danger :disabled="multiple" @click="handleDelete()">删除</a-button>
          </a-space>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'productImage'"><image-preview :src="record.productImage" :width="50" :height="50" /></template>
          <template v-else-if="column.key === 'priceType'">{{ dictText(price_type, record.priceType) }}</template>
          <template v-else-if="column.key === 'status'"><a-tag v-if="record.status == '1'" color="red">未完成</a-tag><a-tag v-else color="green">已完成</a-tag></template>
          <template v-else-if="column.key === 'createdTime'">{{ parseTime(record.createdTime) }}</template>
          <template v-else-if="column.key === 'operation'"><a-space><a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:orderlink:edit']">修改</a-button><a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['member:orderlink:remove']">删除</a-button></a-space></template>
        </template>
      </ant-pro-table>
    </div>

    <!-- 修改连单对话框 -->
    <a-modal
      :title="dialogTitle"
      v-model:open="open"
      width="500px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="dialogSaving"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form ref="orderlinkRef" :model="editForm" layout="vertical" :rules="rules">
        <a-form-item label="价格类型" name="priceType">
          <a-radio-group v-model:value="editForm.priceType">
            <a-radio v-for="dict in price_type" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="价格" name="price">
          <a-input-number v-model:value="editForm.price" placeholder="请输入价格" :min="0" class="full-width" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新增/修改连单抽屉 -->
    <a-drawer
      :title="addTitle"
      v-model:open="addDrawerVisible"
      width="85%"
      :destroy-on-close="false"
    >
      <div class="app-container ant-pro-member-page">
        <div v-if="loadingUser" class="pa20" style="text-align: center"><a-spin /></div>

        <a-form v-else ref="addFormRef" :model="form" layout="vertical" class="mb16">
          <a-row :gutter="20">
            <a-col :span="8"><a-form-item label="任务进度"><a-input :value="taskProgressDisplay" disabled /></a-form-item></a-col>
            <a-col :span="8"><a-form-item label="单数" name="orderCount"><a-input-number v-model:value="form.orderCount" :min="1" class="full-width" /></a-form-item></a-col>
            <a-col :span="8"><a-form-item label="返佣倍数" name="commissionMultiple"><a-input-number v-model:value="form.commissionMultiple" :min="1" class="full-width" /></a-form-item></a-col>
          </a-row>
          <a-row :gutter="20">
            <a-col :span="8"><a-form-item label="余额"><a-input v-model:value="user.balance" disabled /></a-form-item></a-col>
            <a-col :span="8"><a-form-item label="冻结余额"><a-input v-model:value="user.frozenBalance" disabled /></a-form-item></a-col>
            <a-col :span="8"><a-form-item label="总余额"><a-input v-model:value="user.totalBalance" disabled /></a-form-item></a-col>
          </a-row>
        </a-form>

        <ant-pro-table title="明细" :columns="detailColumns" :data-source="details" row-key="id" :pagination="false" :scroll="{ x: 900 }" :tool-options="{ refresh: false }">
          <template #toolbar><a-space><a-button type="primary" :loading="saving" @click="onSaveAll">提交保存</a-button><a-button @click="onCancelEdit">取消</a-button></a-space></template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'detailPriceType'"><a-select v-model:value="record.priceType" placeholder="请选择" size="small" :disabled="!record.isEditing" class="full-width"><a-select-option v-for="opt in price_type" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option></a-select></template>
            <template v-else-if="column.key === 'detailPrice'"><a-input-number v-model:value="record.price" :min="0" size="small" :controls="false" :disabled="!record.isEditing" class="full-width" /></template>
            <template v-else-if="column.key === 'detailStatus'"><a-tag color="red">未完成</a-tag></template>
            <template v-else-if="column.key === 'detailAction'"><a-space><a-button v-if="!record.isEditing" type="link" @click="editDetail(record)">编辑</a-button><a-button v-if="record.isEditing" type="link" @click="saveDetail(record)">保存</a-button><a-button type="link" danger @click="removeDetail(record)">删除</a-button></a-space></template>
          </template>
        </ant-pro-table>

        <ant-pro-table
          title="商品列表"
          :columns="goodsColumns"
          :data-source="goodsList"
          :loading="goodsLoading"
          row-key="id"
          :pagination="{ current: goodsQuery.pageNum, pageSize: goodsQuery.pageSize, total: goodsTotal }"
          :custom-row="goodsCustomRow"
          :scroll="{ x: 900 }"
          @page-change="handleGoodsPageChange"
          @refresh="fetchGoods"
        >
          <template #search><a-form layout="horizontal" :model="goodsQuery" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle"><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="标题"><a-input v-model:value="goodsQuery.title" allow-clear placeholder="搜索标题" @pressEnter="fetchGoods" /></a-form-item></a-col><a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetGoodsQuery">重置</a-button><a-button type="primary" @click="fetchGoods">查询</a-button></a-space></a-col></a-row></a-form></template>
          <template #bodyCell="{ column, record }"><template v-if="column.key === 'goodsImage'"><image-preview :src="record.image" :width="50" :height="50" /></template><template v-else-if="column.key === 'goodsCreateTime'">{{ parseTime(record.createTime) }}</template></template>
        </ant-pro-table>
      </div>
    </a-drawer>
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
  listOrderlink,
  getOrderlink,
  delOrderlink,
  addOrderlink,
  updateOrderlink,
} from "@/api/member/orderlink";
import { getOrderuser } from "@/api/member/orderuser";
import { listGoods } from "@/api/member/goods";
import { resolveDeleteIds } from "@/utils/management-rules";
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
const { price_type } = proxy.useDict("price_type");

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

const orderlinkColumns = [
  { title: "自增ID", dataIndex: "id", width: 90 },
  { title: "连单ID", dataIndex: "linkOrderId", width: 120 },
  { title: "单数", dataIndex: "orderCount", width: 90 },
  { title: "返佣倍数", dataIndex: "commissionMultiple", width: 120 },
  { title: "商品图片", key: "productImage", dataIndex: "productImage", width: 120 },
  { title: "商品标题", dataIndex: "productTitle", width: 220 },
  { title: "价格类型", key: "priceType", dataIndex: "priceType", width: 120 },
  { title: "价格", dataIndex: "price", width: 100 },
  { title: "状态", key: "status", dataIndex: "status", width: 100 },
  { title: "创建时间", key: "createdTime", dataIndex: "createdTime", width: 180 },
  { title: "操作", key: "operation", width: 130, fixed: "right" },
];
const detailColumns = [
  { title: "商品ID", dataIndex: "id", width: 120 },
  { title: "商品标题", dataIndex: "title", width: 240 },
  { title: "价格类型", key: "detailPriceType", width: 180 },
  { title: "价格", key: "detailPrice", width: 180 },
  { title: "状态", key: "detailStatus", width: 120 },
  { title: "操作", key: "detailAction", width: 180, fixed: "right" },
];
const goodsColumns = [
  { title: "商品ID", dataIndex: "id", width: 100 },
  { title: "标题", dataIndex: "title", width: 260 },
  { title: "类目", dataIndex: "typeTitle", width: 160 },
  { title: "价格", dataIndex: "price", width: 120 },
  { title: "图片", key: "goodsImage", dataIndex: "image", width: 120 },
  { title: "创建时间", key: "goodsCreateTime", dataIndex: "createTime", width: 180 },
];
const rowSelection = computed(() => ({ selectedRowKeys: ids.value, onChange: (_keys, rows) => handleSelectionChange(rows) }));
function dictText(options, value) { return options.value?.find((item) => String(item.value) === String(value))?.label ?? value ?? "-"; }
function handleAntPageChange({ page, pageSize }) { queryParams.value.pageNum = page; queryParams.value.pageSize = pageSize; getList(); }
function handleGoodsPageChange({ page, pageSize }) { goodsQuery.pageNum = page; goodsQuery.pageSize = pageSize; fetchGoods(); }
function goodsCustomRow(record) { return { onDblclick: () => addProductToDetails(record) }; }
// 新增/修改连单抽屉相关
const addDrawerVisible = ref(false);
const addTitle = ref("新增连单");
const saving = ref(false);
const addFormRef = ref(null);

const form = reactive({
  id: null,
  userId: props.userId ?? null,
  orderCount: 1,
  commissionMultiple: 1,
  username: null,
  phoneNumber: null,
  taskProgress: null,
  memberOrderCountPerDay: null,
  balance: 0,
  frozenBalance: 0,
  totalBalance: 0,
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
  if (!form.orderCount || form.orderCount < 1) {
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
    orderCount: form.orderCount,
    commissionMultiple: form.commissionMultiple,
    details: details.value.map((d) => ({
      goodsId: d.id,
      priceType: d.priceType,
      price: d.price,
    })),
  };

  saving.value = true;
  try {
    if (form.id) {
      await updateOrderlink({ ...payload, id: form.id });
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
  queryParams.value.linkOrderId = null;
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
const dialogSaving = ref(false);
const editForm = reactive({
  priceType: "",
  price: 0,
});
const rules = reactive({
  priceType: [{ required: true, message: "请选择价格类型", trigger: "change" }],
  price: [{ required: true, message: "请输入价格", trigger: "blur" }],
});

// submit form
async function submitForm() {
  try {
    await orderlinkRef.value?.validate();
  } catch {
    return;
  }

  const payload = {
    ...editForm,
    id: currentRowId.value,
  };
  dialogSaving.value = true;
  try {
    await updateOrderlink(payload);
    open.value = false;
    proxy.$modal.msgSuccess("修改成功");
    getList();
  } catch {
    proxy.$modal.msgError("修改失败");
  } finally {
    dialogSaving.value = false;
  }
}

// cancel
function cancel() {
  open.value = false;
  orderlinkRef.value?.resetFields();
}

function handleDelete(row) {
  const _ids = resolveDeleteIds(row, ids.value);
  if (!_ids.length) {
    proxy.$modal.msgWarning("请选择要删除的数据");
    return;
  }
  proxy.$modal
    .confirm('是否确认删除连单编号为"' + _ids + '"的数据项？')
    .then(() => delOrderlink(_ids.join(",")))
    .then(() => {
      handleSelectionChange([]);
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
