<template>
  <div class="app-container ant-pro-member-page goods-page" :class="{ 'query-expanded': queryExpanded }">
    <ant-pro-table
      title="商品列表"
      :columns="goodsColumns"
      :data-source="goodsList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{ x: 1450, y: tableScrollHeight }"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      @page-change="handleAntPageChange"
      @refresh="getList"
      @change="handleTableChange"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :xl="6">
              <a-form-item label="标题">
                <a-input v-model:value="queryParams.title" allow-clear placeholder="请输入" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :xl="6">
              <a-form-item label="类目">
                <a-select v-model:value="queryParams.typeId" allow-clear placeholder="请选择">
                  <a-select-option v-for="item in typeDatas" :key="item.id" :value="item.id">{{ item.title }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :xl="6">
              <a-form-item label="是否启用">
                <a-select v-model:value="queryParams.isEnabled" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in goods_enabled" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :xl="6" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
                <a-button type="link" class="query-expand-button" @click="queryExpanded = !queryExpanded">
                  {{ queryExpanded ? '收起' : '展开' }}<DownOutlined :class="{ expanded: queryExpanded }" />
                </a-button>
              </a-space>
            </a-col>
            <a-col v-show="queryExpanded" :xs="24" :sm="12" :xl="6">
              <a-form-item label="价格">
                <a-input-group compact class="price-range">
                  <a-input-number v-model:value="queryParams.minPrice" :min="0" placeholder="请输入" />
                  <a-input class="price-range-separator" value="~" disabled />
                  <a-input-number v-model:value="queryParams.maxPrice" :min="0" placeholder="请输入" />
                </a-input-group>
              </a-form-item>
            </a-col>
            <a-col v-show="queryExpanded" :xs="24" :sm="12" :xl="6">
              <a-form-item label="创建时间">
                <a-range-picker v-model:value="dateRange" :placeholder="['请选择', '请选择']" value-format="YYYY-MM-DD" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-upload :show-upload-list="false" :before-upload="handleImport" accept="text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet">
          <a-button type="primary" v-hasPermi="['member:goods:add']"><template #icon><UploadOutlined /></template>上传</a-button>
        </a-upload>
        <a-button type="primary" :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:goods:remove']"><template #icon><DeleteOutlined /></template>删除</a-button>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:goods:add']"><template #icon><PlusOutlined /></template>创建</a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'id'"><span class="goods-id">{{ record.id }}</span></template>
        <template v-else-if="column.key === 'title'">
          <span>{{ record.title || '-' }}</span><a-button class="copy-title" type="link" aria-label="复制" @click="copyTitle(record.title)"><CopyOutlined /></a-button>
        </template>
        <template v-else-if="column.key === 'enabled'">
          <a-badge :status="isGoodsEnabled(record.isEnabled) ? 'success' : 'default'" :text="enabledText(record.isEnabled)" />
        </template>
        <template v-else-if="column.key === 'image'">
          <image-preview :src="record.image" :width="50" :height="50" />
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime ? parseTime(record.createTime) : "-" }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="8">
            <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:goods:edit']">修改</a-button>
            <a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:goods:add']">复制</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-drawer v-model:open="open" :title="title" width="70%" destroy-on-close class="goods-editor-drawer" @close="cancel">
      <a-form ref="goodsRef" :model="form" :rules="rules" layout="vertical" size="large" class="goods-editor-form">
        <a-row :gutter="[20, 0]">
          <a-col :span="24">
            <a-form-item label="标题" name="title">
              <a-textarea v-model:value="form.title" placeholder="标题" :rows="3" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="类目" name="typeId">
              <a-select v-model:value="form.typeId" placeholder="类目" allow-clear>
                <a-select-option v-for="item in typeDatas" :key="item.id" :value="item.id">
                  {{ item.title }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="是否启用" name="isEnabled">
              <a-radio-group v-model:value="form.isEnabled">
                <a-radio v-for="dict in editorEnabledOptions" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="价格" name="price">
              <a-input-number v-model:value="form.price" placeholder="价格" class="full-width" :min="0" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="序号" name="serialNumber">
              <a-input-number v-model:value="form.serialNumber" placeholder="序号" class="full-width" :min="0" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="图片" name="image">
              <image-upload v-model="form.image" :limit="1" :is-show-tip="false" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="二级标题" name="subTitle">
              <a-textarea v-model:value="form.subTitle" placeholder="二级标题" :rows="3" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="单价" name="unitPrice">
              <a-input-number v-model:value="form.unitPrice" placeholder="单价" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="数量" name="quantity">
              <a-input-number v-model:value="form.quantity" placeholder="数量" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="星级" name="starRating">
              <a-input-number v-model:value="form.starRating" placeholder="星级" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="评分" name="rating">
              <a-input-number v-model:value="form.rating" placeholder="评分" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="说明" name="description">
              <editor v-model="form.description" :min-height="200" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer><div class="goods-drawer-footer"><a-space><a-button size="large" @click="cancel">取 消</a-button><a-button type="primary" size="large" :loading="submitting" @click="submitForm">确 定</a-button></a-space></div></template>
    </a-drawer>
  </div>
</template>

<script setup name="Goods">
import {
  listGoods,
  getGoods,
  delGoods,
  addGoods,
  updateGoods,
  typeList,
  importGoods,
} from "@/api/member/goods";
import { CopyOutlined, DeleteOutlined, DownOutlined, PlusOutlined, UploadOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { goods_enabled } = proxy.useDict("goods_enabled");

const goodsList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const typeDatas = ref([]);
const goodsRef = ref();
const dateRange = ref([]);
const queryExpanded = ref(false);
const submitting = ref(false);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    title: null,
    typeId: null,
    isEnabled: null,
    minPrice: null,
    maxPrice: null,
    orderByColumn: null,
    isAsc: null,
  },
  rules: {
    title: [{ required: true, message: "标题不能为空", trigger: "blur" }],
    typeId: [{ required: true, message: "类目不能为空", trigger: "blur" }],
    isEnabled: [
      { required: true, message: "是否启用不能为空", trigger: "change" },
    ],
    price: [{ required: true, message: "价格不能为空", trigger: "blur" }],
    serialNumber: [
      { required: true, message: "序号不能为空", trigger: "blur" },
    ],
    image: [{ required: true, message: "图片不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

const goodsColumns = [
  { title: "ID", dataIndex: "id", key: "id", width: 80 },
  { title: "标题", dataIndex: "title", key: "title", width: 260 },
  { title: "类目", dataIndex: "typeTitle", key: "typeTitle", width: 140, sorter: true },
  { title: "是否启用", dataIndex: "isEnabled", key: "enabled", width: 120, sorter: true },
  { title: "价格", dataIndex: "price", key: "price", width: 120, sorter: true },
  { title: "序号", dataIndex: "serialNumber", key: "serialNumber", width: 140, sorter: true },
  { title: "图片", dataIndex: "image", key: "image", width: 120 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 190, sorter: true },
  { title: "操作", key: "operation", width: 160 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows),
}));
const tableScrollHeight = computed(() => queryExpanded.value ? "calc(100vh - 496px)" : "calc(100vh - 440px)");
const editorEnabledOptions = computed(() => {
  const options = unref(goods_enabled);
  if (!Array.isArray(options) || options.length === 0) return [{ label: "禁用", value: "1" }, { label: "启用", value: "0" }];
  return [...options].sort((left, right) => Number(right.value) - Number(left.value));
});

function dictText(options, value) {
  return proxy.selectDictLabel(options, value) || value || "-";
}

function isGoodsEnabled(value) {
  return String(value) === "0";
}

function enabledText(value) {
  const options = unref(goods_enabled);
  return options?.find((item) => String(item.value) === String(value))?.label ?? (isGoodsEnabled(value) ? "启用" : "禁用");
}

async function copyTitle(value) {
  if (!value) return;
  await navigator.clipboard.writeText(value);
  proxy.$modal.msgSuccess("复制成功");
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

const sortColumnMap = {
  typeTitle: "g.typeId",
  enabled: "g.isEnabled",
  price: "g.price",
  serialNumber: "g.serialNumber",
  createTime: "g.createTime",
};

function handleTableChange(_pagination, _filters, sorter) {
  queryParams.value.orderByColumn = sorter?.order ? sortColumnMap[sorter.columnKey] || null : null;
  queryParams.value.isAsc = sorter?.order === "ascend" ? "asc" : sorter?.order === "descend" ? "desc" : null;
  queryParams.value.pageNum = 1;
  getList();
}

/** 查询商品列表 */
function buildGoodsQuery() {
  const { minPrice, maxPrice, ...params } = queryParams.value;
  params.params = {
    ...(params.params || {}),
    beginPrice: minPrice ?? undefined,
    endPrice: maxPrice ?? undefined,
  };
  return proxy.addDateRange(params, dateRange.value);
}

function getList() {
  loading.value = true;
  listGoods(buildGoodsQuery()).then((response) => {
    goodsList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function getTypeList() {
  typeList().then((res) => {
    typeDatas.value = res.data;
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
    title: null,
    typeId: null,
    isEnabled: "0",
    price: null,
    serialNumber: null,
    image: null,
    subTitle: null,
    unitPrice: null,
    quantity: null,
    starRating: null,
    rating: null,
    description: null,
    createBy: null,
    createTime: null,
  };
  nextTick(() => goodsRef.value?.clearValidate?.());
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.title = null;
  queryParams.value.typeId = null;
  queryParams.value.isEnabled = null;
  queryParams.value.minPrice = null;
  queryParams.value.maxPrice = null;
  queryParams.value.orderByColumn = null;
  queryParams.value.isAsc = null;
  dateRange.value = [];
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
  getTypeList();
  open.value = true;
  title.value = "创建";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getTypeList();
  getGoods(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改";
  });
}

function handleCopy(row) {
  getGoods(row.id).then((response) => {
    const copied = { ...response.data };
    delete copied.id;
    delete copied.createTime;
    delete copied.updateTime;
    return addGoods(copied);
  }).then(() => {
    proxy.$modal.msgSuccess("复制成功");
    getList();
  });
}

function handleImport(file) {
  const formData = new FormData();
  formData.append("file", file);
  importGoods(formData).then(() => {
    proxy.$modal.msgSuccess("上传成功");
    getList();
  });
  return false;
}

/** 提交按钮 */
async function submitForm() {
  try {
    await goodsRef.value?.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    if (form.value.id != null) {
      await updateGoods(form.value);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addGoods(form.value);
      proxy.$modal.msgSuccess("新增成功");
    }
    open.value = false;
    getList();
  } finally {
    submitting.value = false;
  }
}

/** 删除按钮操作 */
function handleDelete(row = {}) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除商品编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delGoods(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/goods/export",
    {
      ...queryParams.value,
    },
    `goods_${new Date().getTime()}.xlsx`
  );
}

getTypeList();
getList();
</script>

<style scoped lang="scss">
.goods-page { margin-top: 44px; }
.goods-page :deep(.ant-table-body) { height: calc(100vh - 440px); }
.goods-page.query-expanded :deep(.ant-table-body) { height: calc(100vh - 496px); }
.goods-page :deep(.ant-pagination-item),
.goods-page :deep(.ant-pagination-prev),
.goods-page :deep(.ant-pagination-next) { min-width: 24px; height: 24px; line-height: 22px; }
.ant-pro-query-form :deep(.ant-select),
.ant-pro-query-form :deep(.ant-picker) { width: 100%; }
.query-expand-button { padding-inline: 4px; }
.query-expand-button .anticon { margin-left: 4px; transition: transform .2s; }
.query-expand-button .anticon.expanded { transform: rotate(180deg); }
.price-range { display: flex; width: 100%; }
.price-range :deep(.ant-input-number) { width: calc(50% - 18px); }
.price-range-separator { width: 36px !important; padding: 0; text-align: center; pointer-events: none; }
.goods-id { color: #1677ff; }
.copy-title { height: 22px; margin-left: 4px; padding: 0 2px !important; font-size: 14px; }
.full-width {
  width: 100%;
}
.goods-editor-form :deep(.ant-upload-list-picture-card .ant-upload-list-item-container),
.goods-editor-form :deep(.ant-upload.ant-upload-select-picture-card) { width: 102px; height: 102px; }
.goods-drawer-footer { text-align: right; }

@media (max-width: 768px) {
  .goods-page { margin-top: 12px; }
  .goods-page :deep(.ant-table-body),
  .goods-page.query-expanded :deep(.ant-table-body) { height: auto; }
  :global(.goods-editor-drawer .ant-drawer-content-wrapper) { width: 100% !important; }
}
</style>
