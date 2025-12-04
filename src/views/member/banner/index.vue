<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select
          v-model="queryParams.type"
          placeholder="请选择类型"
          style="width: 220px"
          clearable
        >
          <el-option
            v-for="dict in banner_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否启用" prop="isEnabled">
        <el-select
          v-model="queryParams.isEnabled"
          placeholder="请选择是否启用"
          style="width: 220px"
          clearable
        >
          <el-option
            v-for="dict in user_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery"
          >搜索</el-button
        >
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['member:banner:add']"
          >新增</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['member:banner:edit']"
          >修改</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['member:banner:remove']"
          >删除</el-button
        >
      </el-col>
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="bannerList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="名称" align="center" prop="name" />
      <el-table-column label="类型" align="center" prop="type">
        <template #default="scope">
          <dict-tag :options="banner_type" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column label="是否启用" align="center" prop="isEnabled">
        <template #default="scope">
          <dict-tag :options="user_yes_no" :value="scope.row.isEnabled" />
        </template>
      </el-table-column>
      <el-table-column label="序号" align="center" prop="sortOrder" />
      <el-table-column label="链接" align="center" prop="link" />
      <el-table-column label="媒体" align="center" prop="media" width="100">
        <template #default="scope">
          <media-preview :src="scope.row.media" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remarks" />
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
            v-hasPermi="['member:banner:edit']"
          ></el-button>
          <el-button
            circle
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:banner:remove']"
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

    <!-- 添加或修改横幅：用于存储横幅广告相关信息对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form
        ref="bannerRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option
              v-for="dict in banner_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="是否启用" prop="isEnabled">
          <el-radio-group v-model="form.isEnabled">
            <el-radio
              v-for="dict in user_yes_no"
              :key="dict.value"
              :label="dict.value"
              >{{ dict.label }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item label="序号" prop="sortOrder">
          <el-input v-model="form.sortOrder" placeholder="请输入序号" />
        </el-form-item>
        <el-form-item label="链接" prop="link">
          <el-input v-model="form.link" placeholder="请输入链接" />
        </el-form-item>
        <el-form-item label="媒体" prop="media">
          <media-upload v-model="form.media" :limit="1" />
        </el-form-item>
        <el-form-item label="备注" prop="remarks">
          <el-input v-model="form.remarks" placeholder="请输入备注" />
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
  proxy.resetForm("bannerRef");
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
function submitForm() {
  proxy.$refs["bannerRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateBanner(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addBanner(form.value).then((response) => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
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
