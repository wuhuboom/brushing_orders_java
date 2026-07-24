<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="列表" :columns="cusserviceColumns" :data-source="cusserviceList" :loading="loading" row-key="id" :row-selection="rowSelection" :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }" @page-change="handleAntPageChange" @refresh="getList">
      <template #search><a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle"><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="名称"><a-input v-model:value="queryParams.name" allow-clear placeholder="请输入名称" @pressEnter="handleQuery" /></a-form-item></a-col><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="序号"><a-input v-model:value="queryParams.sortOrder" allow-clear placeholder="请输入序号" @pressEnter="handleQuery" /></a-form-item></a-col><a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="handleQuery">查询</a-button></a-space></a-col></a-row></a-form></template>
      <template #toolbar><a-button type="primary" @click="handleAdd" v-hasPermi="['member:cusservice:add']">新增</a-button><a-button :disabled="single" @click="handleUpdate" v-hasPermi="['member:cusservice:edit']">修改</a-button><a-button :disabled="single" @click="openTranslationDialog(selectedRow)" v-hasPermi="['member:cusservice:edit']">国际化</a-button><a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:cusservice:remove']">删除</a-button></template>
      <template #bodyCell="{ column, record }"><template v-if="column.key === 'image'"><image-preview :src="record.image" :width="50" :height="50" /></template><template v-else-if="column.key === 'enabled'">{{ dictText(user_yes_no, record.isEnabled) }}</template><template v-else-if="column.key === 'operation'"><a-space><a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:cusservice:edit']">修改</a-button><a-button type="link" @click="openTranslationDialog(record)" v-hasPermi="['member:cusservice:edit']">国际化</a-button><a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['member:cusservice:remove']">删除</a-button></a-space></template></template>
    </ant-pro-table>
    <!-- 添加或修改客服对话框 -->
    <a-modal :title="title" v-model:open="open" width="500px" ok-text="确 定" cancel-text="取 消" :confirm-loading="submitting" @ok="submitForm" @cancel="cancel">
      <a-form ref="cusserviceRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="名称" name="name"><a-input v-model:value="form.name" placeholder="请输入名称" /></a-form-item>
        <a-form-item label="序号" name="sortOrder"><a-input v-model:value="form.sortOrder" placeholder="请输入序号" /></a-form-item>
        <a-form-item label="图片" name="image"><image-upload v-model="form.image" :limit="1" /></a-form-item>
        <a-form-item label="是否启用" name="isEnabled"><a-radio-group v-model:value="form.isEnabled"><a-radio v-for="dict in user_yes_no" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio></a-radio-group></a-form-item>
        <a-form-item label="链接" name="link"><a-input v-model:value="form.link" placeholder="请输入链接" /></a-form-item>
        <a-form-item label="备注" name="remarks"><a-input v-model:value="form.remarks" placeholder="请输入备注" /></a-form-item>
      </a-form>
    </a-modal>
    <translation-drawer
      v-model="translationOpen"
      :title="translationTitle"
      :translations="translationForm"
      type="customerService"
      @submit="submitTranslations"
    />
  </div>
</template>

<script setup name="Cusservice">
import {
  listCusservice,
  getCusservice,
  delCusservice,
  addCusservice,
  updateCusservice,
} from "@/api/member/cusservice";
import TranslationDrawer from "@/views/member/components/TranslationDrawer.vue";
import { createEmptyTranslations } from "@/views/member/components/translationLanguages";

const { proxy } = getCurrentInstance();
const { user_yes_no } = proxy.useDict("user_yes_no");

const cusserviceList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const submitting = ref(false);
const cusserviceRef = ref();
const translationOpen = ref(false);
const translationTitle = ref("");
const translationForm = ref(createEmptyTranslations());
const currentTranslationRow = ref(null);
const selectedRow = computed(() => cusserviceList.value.find((item) => item.id === ids.value[0]));

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    sortOrder: null,
    image: null,
    isEnabled: null,
    link: null,
    remarks: null,
  },
  rules: {
    name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
    sortOrder: [{ required: true, message: "序号不能为空", trigger: "blur" }],
    image: [{ required: true, message: "图片不能为空", trigger: "blur" }],
    isEnabled: [
      { required: true, message: "是否启用不能为空", trigger: "change" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

const cusserviceColumns = [
  { title: "ID", dataIndex: "id", width: 90 },
  { title: "名称", dataIndex: "name", width: 160 },
  { title: "序号", dataIndex: "sortOrder", width: 100 },
  { title: "图片", key: "image", dataIndex: "image", width: 120 },
  { title: "是否启用", key: "enabled", dataIndex: "isEnabled", width: 120 },
  { title: "链接", dataIndex: "link", width: 220 },
  { title: "备注", dataIndex: "remarks", width: 180 },
  { title: "操作", key: "operation", width: 210, fixed: "right" },
];
const rowSelection = computed(() => ({ selectedRowKeys: ids.value, onChange: (_keys, rows) => handleSelectionChange(rows) }));
function dictText(options, value) { return options.value?.find((item) => String(item.value) === String(value))?.label ?? value ?? "-"; }
function handleAntPageChange({ page, pageSize }) { queryParams.value.pageNum = page; queryParams.value.pageSize = pageSize; getList(); }

/** 查询客服列表 */
function getList() {
  loading.value = true;
  listCusservice(queryParams.value).then((response) => {
    cusserviceList.value = response.rows;
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
    name: null,
    sortOrder: null,
    image: null,
    isEnabled: null,
    link: null,
    remarks: null,
    createBy: null,
    createTime: null,
  };
  nextTick(() => cusserviceRef.value?.clearValidate?.());
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
  title.value = "添加客服";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getCusservice(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改客服";
  });
}

async function openTranslationDialog(row) {
  if (!row?.id) return;
  const response = await getCusservice(row.id);
  currentTranslationRow.value = response.data;
  translationForm.value = {
    ...createEmptyTranslations(),
    ...(response.data.translations || {}),
  };
  translationTitle.value = `${response.data.name || "客服"} - 国际化`;
  translationOpen.value = true;
}

async function submitTranslations(translations) {
  const row = currentTranslationRow.value;
  if (!row) return;
  const translationsId = translations.id || row.translationsId;
  await updateCusservice({
    id: row.id,
    translationsId,
    translations: { ...translations, id: translationsId },
  });
  proxy.$modal.msgSuccess("修改成功");
  translationOpen.value = false;
  currentTranslationRow.value = null;
  getList();
}

/** 提交按钮 */
async function submitForm() {
  try {
    await cusserviceRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (form.value.id != null) {
      await updateCusservice(form.value);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addCusservice(form.value);
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
    .confirm('是否确认删除客服编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delCusservice(_ids);
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
    "member/cusservice/export",
    {
      ...queryParams.value,
    },
    `cusservice_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
