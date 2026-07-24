<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="列表"
      :columns="goodstypeColumns"
      :data-source="goodstypeList"
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
              <a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="handleQuery">查询</a-button></a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>
      <template #toolbar>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:goodstype:add']">新增</a-button>
        <a-button :disabled="single" @click="handleUpdate" v-hasPermi="['member:goodstype:edit']">修改</a-button>
        <a-button :disabled="single" @click="handleCopy" v-hasPermi="['member:goodstype:add']">复制</a-button>
        <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:goodstype:remove']">删除</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'enabled'">{{ dictText(goods_enabled, record.isEnabled) }}</template>
        <template v-else-if="column.key === 'image'"><image-preview :src="record.image" :width="50" :height="50" /></template>
        <template v-else-if="column.key === 'operation'"><a-space><a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:goodstype:edit']">修改</a-button><a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:goodstype:add']">复制</a-button><a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['member:goodstype:remove']">删除</a-button></a-space></template>
      </template>
    </ant-pro-table>

    <!-- 添加或修改类目管理对话框 -->
    <a-modal
      :title="title"
      v-model:open="open"
      width="500px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="submitting"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form ref="goodstypeRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="标题" name="title"><a-input v-model:value="form.title" placeholder="请输入标题" /></a-form-item>
        <a-form-item label="是否启用" name="isEnabled"><a-radio-group v-model:value="form.isEnabled"><a-radio v-for="dict in goods_enabled" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio></a-radio-group></a-form-item>
        <a-form-item label="序号" name="serialNumber"><a-input v-model:value="form.serialNumber" placeholder="请输入序号" /></a-form-item>
        <a-form-item label="图片" name="image"><image-upload v-model="form.image" :limit="1" /></a-form-item>
        <a-form-item label="二级标题" name="subTitle"><a-input v-model:value="form.subTitle" placeholder="请输入二级标题" /></a-form-item>
        <a-form-item label="备注" name="remarks"><a-textarea v-model:value="form.remarks" placeholder="请输入内容" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="Goodstype">
import {
  listGoodstype,
  getGoodstype,
  delGoodstype,
  addGoodstype,
  updateGoodstype,
} from "@/api/member/goodstype";

const { proxy } = getCurrentInstance();
const { goods_enabled } = proxy.useDict("goods_enabled");

const goodstypeList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const submitting = ref(false);
const goodstypeRef = ref();

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: null,
    serialNumber: null,
    image: null,
    subTitle: null,
    remarks: null,
  },
  rules: {
    title: [{ required: true, message: "标题不能为空", trigger: "blur" }],
    isEnabled: [
      { required: true, message: "是否启用不能为空", trigger: "change" },
    ],
    serialNumber: [
      { required: true, message: "序号不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

const goodstypeColumns = [
  { title: "ID", dataIndex: "id", width: 90 },
  { title: "标题", dataIndex: "title", width: 180 },
  { title: "是否启用", key: "enabled", dataIndex: "isEnabled", width: 120 },
  { title: "序号", dataIndex: "serialNumber", width: 100 },
  { title: "图片", key: "image", dataIndex: "image", width: 120 },
  { title: "二级标题", dataIndex: "subTitle", width: 180 },
  { title: "创建时间", dataIndex: "createTime", width: 180 },
  { title: "备注", dataIndex: "remarks", width: 160 },
  { title: "操作", key: "operation", width: 130, fixed: "right" },
];
const rowSelection = computed(() => ({ selectedRowKeys: ids.value, onChange: (_keys, rows) => handleSelectionChange(rows) }));
function dictText(options, value) { return options.value?.find((item) => String(item.value) === String(value))?.label ?? value ?? "-"; }
function handleAntPageChange({ page, pageSize }) { queryParams.value.pageNum = page; queryParams.value.pageSize = pageSize; getList(); }

/** 查询类目管理列表 */
function getList() {
  loading.value = true;
  listGoodstype(queryParams.value).then((response) => {
    goodstypeList.value = response.rows;
    total.value = response.total;
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
    title: null,
    isEnabled: "0",
    serialNumber: null,
    image: null,
    subTitle: null,
    remarks: null,
  };
  nextTick(() => goodstypeRef.value?.clearValidate?.());
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
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
  open.value = true;
  title.value = "添加类目管理";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getGoodstype(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改类目管理";
  });
}

function handleCopy(row) {
  const _id = row?.id || ids.value[0];
  getGoodstype(_id).then((response) => {
    const copied = { ...response.data };
    delete copied.id;
    delete copied.createTime;
    delete copied.updateTime;
    addGoodstype(copied).then(() => {
      proxy.$modal.msgSuccess("复制成功");
      getList();
    });
  });
}

/** 提交按钮 */
async function submitForm() {
  try {
    await goodstypeRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (form.value.id != null) {
      await updateGoodstype(form.value);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addGoodstype(form.value);
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
    .confirm('是否确认删除类目管理编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delGoodstype(_ids);
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
    "member/goodstype/export",
    {
      ...queryParams.value,
    },
    `goodstype_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
