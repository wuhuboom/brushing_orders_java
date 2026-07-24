<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="横幅列表" :columns="bannerColumns" :data-source="bannerList" :loading="loading"
      row-key="id" :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      @page-change="handleAntPageChange" @refresh="getList">
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="6"><a-form-item label="名称"><a-input v-model:value="queryParams.name" allow-clear placeholder="请输入名称" @pressEnter="handleQuery" /></a-form-item></a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6"><a-form-item label="类型"><a-select v-model:value="queryParams.type" allow-clear placeholder="请选择类型"><a-select-option v-for="dict in banner_type" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option></a-select></a-form-item></a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6"><a-form-item label="是否启用"><a-select v-model:value="queryParams.isEnabled" allow-clear placeholder="请选择是否启用"><a-select-option v-for="dict in user_yes_no" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option></a-select></a-form-item></a-col>
            <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重 置</a-button><a-button type="primary" @click="handleQuery">查 询</a-button></a-space></a-col>
          </a-row>
        </a-form>
      </template>
      <template #toolbar>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:banner:add']">新增</a-button>
        <a-button :disabled="single" @click="handleUpdate" v-hasPermi="['member:banner:edit']">修改</a-button>
        <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:banner:remove']">删除</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">{{ dictText(banner_type, record.type) }}</template>
        <template v-else-if="column.key === 'enabled'">{{ dictText(user_yes_no, record.isEnabled) }}</template>
        <template v-else-if="column.key === 'media'"><media-preview :src="record.media" :width="50" :height="50" /></template>
        <template v-else-if="column.key === 'operation'"><a-space><a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:banner:edit']">修改</a-button><a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['member:banner:remove']">删除</a-button></a-space></template>
      </template>
    </ant-pro-table>

    <!-- 添加或修改横幅：用于存储横幅广告相关信息对话框 -->
    <a-modal
      :title="title"
      v-model:open="open"
      width="800px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="submitting"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form
        ref="bannerRef"
        :model="form"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入名称" />
        </a-form-item>
        <a-form-item label="类型" name="type">
          <a-select v-model:value="form.type" placeholder="请选择类型">
            <a-select-option
              v-for="dict in banner_type"
              :key="dict.value"
              :value="dict.value"
            >{{ dict.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="是否启用" name="isEnabled">
          <a-radio-group v-model:value="form.isEnabled">
            <a-radio
              v-for="dict in user_yes_no"
              :key="dict.value"
              :value="dict.value"
              >{{ dict.label }}</a-radio
            >
          </a-radio-group>
        </a-form-item>
        <a-form-item label="序号" name="sortOrder">
          <a-input v-model:value="form.sortOrder" placeholder="请输入序号" />
        </a-form-item>
        <a-form-item label="链接" name="link">
          <a-input v-model:value="form.link" placeholder="请输入链接" />
        </a-form-item>
        <a-form-item label="媒体" name="media">
          <media-upload v-model="form.media" :limit="1" />
        </a-form-item>
        <a-form-item label="备注" name="remarks">
          <a-input v-model:value="form.remarks" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="Banner">
import {
  listBanner,
  getBanner,
  delBanner,
  addBanner,
  updateBanner,
} from "@/api/member/banner";

import mediaPreview from "@/components/MediaPreview/index.vue";
import mediaUpload from "@/components/MediaUpload/index.vue";

const { proxy } = getCurrentInstance();
const { user_yes_no, banner_type } = proxy.useDict(
  "user_yes_no",
  "banner_type"
);

const bannerList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const submitting = ref(false);
const bannerRef = ref();

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    type: null,
    isEnabled: null,
    sortOrder: null,
    link: null,
    media: null,
    remarks: null,
  },
  rules: {
    name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
    type: [{ required: true, message: "类型不能为空", trigger: "change" }],
    isEnabled: [
      { required: true, message: "是否启用不能为空", trigger: "change" },
    ],
    sortOrder: [{ required: true, message: "序号不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

const bannerColumns = [
  { title: "ID", dataIndex: "id", key: "id", width: 80 },
  { title: "名称", dataIndex: "name", key: "name", width: 180 },
  { title: "类型", dataIndex: "type", key: "type", width: 130 },
  { title: "是否启用", dataIndex: "isEnabled", key: "enabled", width: 120 },
  { title: "序号", dataIndex: "sortOrder", key: "sortOrder", width: 100 },
  { title: "链接", dataIndex: "link", key: "link", width: 220 },
  { title: "媒体", dataIndex: "media", key: "media", width: 100 },
  { title: "备注", dataIndex: "remarks", key: "remarks" },
  { title: "操作", key: "operation", width: 150 },
];
const rowSelection = computed(() => ({ selectedRowKeys: ids.value, onChange: (_keys, rows) => handleSelectionChange(rows) }));
function dictText(options, value) { return proxy.selectDictLabel(options, value) || value || "-"; }
function handleAntPageChange({ page, pageSize }) { queryParams.value.pageNum = page; queryParams.value.pageSize = pageSize; getList(); }

/** 查询横幅：用于存储横幅广告相关信息列表 */
function getList() {
  loading.value = true;
  listBanner(queryParams.value).then((response) => {
    bannerList.value = response.rows;
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
    type: null,
    isEnabled: "0",
    sortOrder: null,
    link: null,
    media: null,
    remarks: null,
  };
  nextTick(() => bannerRef.value?.clearValidate?.());
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.name = null;
  queryParams.value.type = null;
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
  open.value = true;
  title.value = "添加横幅";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getBanner(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改横幅";
  });
}

/** 提交按钮 */
async function submitForm() {
  try {
    await bannerRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (form.value.id != null) {
      await updateBanner(form.value);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addBanner(form.value);
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
    .confirm(
      '是否确认删除横幅：用于存储横幅广告相关信息编号为"' + _ids + '"的数据项？'
    )
    .then(function () {
      return delBanner(_ids);
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
    "member/banner/export",
    {
      ...queryParams.value,
    },
    `banner_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
