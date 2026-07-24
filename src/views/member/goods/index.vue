<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="商品列表"
      :columns="goodsColumns"
      :data-source="goodsList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="标题">
                <a-input v-model:value="queryParams.title" allow-clear placeholder="请输入标题" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="是否启用">
                <a-select v-model:value="queryParams.isEnabled" allow-clear placeholder="请选择是否启用">
                  <a-select-option v-for="dict in goods_enabled" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col flex="auto" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:goods:add']">新增</a-button>
        <a-button :disabled="single" @click="handleUpdate" v-hasPermi="['member:goods:edit']">修改</a-button>
        <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:goods:remove']">删除</a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'enabled'">
          <a-tag color="blue">{{ dictText(goods_enabled, record.isEnabled) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'image'">
          <image-preview :src="record.image" :width="50" :height="50" />
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="8">
            <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:goods:edit']">修改</a-button>
            <a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['member:goods:remove']">删除</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal v-model:open="open" :title="title" width="860px" destroy-on-close @ok="submitForm" @cancel="cancel">
      <a-form ref="goodsRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="[20, 0]">
          <a-col :span="24">
            <a-form-item label="标题" name="title">
              <a-textarea v-model:value="form.title" placeholder="请输入标题" :rows="2" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类目" name="typeId">
              <a-select v-model:value="form.typeId" placeholder="请选择商品类型" allow-clear>
                <a-select-option v-for="item in typeDatas" :key="item.id" :value="item.id">
                  {{ item.title }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否启用" name="isEnabled">
              <a-radio-group v-model:value="form.isEnabled">
                <a-radio v-for="dict in goods_enabled" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="价格" name="price">
              <a-input-number v-model:value="form.price" placeholder="请输入价格" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="序号" name="serialNumber">
              <a-input v-model:value="form.serialNumber" placeholder="请输入序号" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="图片" name="image">
              <image-upload v-model="form.image" :limit="1" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="二级标题" name="subTitle">
              <a-textarea v-model:value="form.subTitle" placeholder="请输入二级标题" :rows="3" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单价" name="unitPrice">
              <a-input-number v-model:value="form.unitPrice" placeholder="请输入单价" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数量" name="quantity">
              <a-input-number v-model:value="form.quantity" placeholder="请输入数量" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="星级" name="starRating">
              <a-input-number v-model:value="form.starRating" placeholder="请输入星级" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="评分" name="rating">
              <a-input-number v-model:value="form.rating" placeholder="请输入评分" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="说明" name="description">
              <a-textarea v-model:value="form.description" placeholder="请输入内容" :rows="3" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
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
} from "@/api/member/goods";

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

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: null,
    isEnabled: null,
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
  { title: "类目", dataIndex: "typeTitle", key: "typeTitle", width: 140 },
  { title: "是否启用", dataIndex: "isEnabled", key: "enabled", width: 120 },
  { title: "价格", dataIndex: "price", key: "price", width: 120 },
  { title: "序号", dataIndex: "serialNumber", key: "serialNumber", width: 140 },
  { title: "图片", dataIndex: "image", key: "image", width: 120 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 190 },
  { title: "操作", key: "operation", width: 160 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows),
}));

function dictText(options, value) {
  return proxy.selectDictLabel(options, value) || value || "-";
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

/** 查询商品列表 */
function getList() {
  loading.value = true;
  listGoods(queryParams.value).then((response) => {
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
  proxy.resetForm("goodsRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.title = null;
  queryParams.value.isEnabled = null;
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
  title.value = "添加商品";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getTypeList();
  getGoods(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改商品";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["goodsRef"]?.validate?.().then(() => {
      if (form.value.id != null) {
        updateGoods(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addGoods(form.value).then((response) => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
  }).catch(() => {});
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

getList();
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
