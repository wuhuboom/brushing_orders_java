<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('banner.bannerName')" prop="name">
        <el-input
          v-model="queryParams.name"
          :placeholder="$t('banner.enterBannerName')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('banner.linkUrl')" prop="linkUrl">
        <el-input
          v-model="queryParams.linkUrl"
          :placeholder="$t('banner.enterLinkUrl')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('banner.sort')" prop="sort">
        <el-input
          v-model="queryParams.sort"
          :placeholder="$t('banner.enterSort')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('banner.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="$t('banner.selectStatus')"
          clearable
        >
          <el-option
            v-for="dict in sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">{{
          $t("common.search")
        }}</el-button>
        <el-button icon="Refresh" @click="resetQuery">{{
          $t("common.reset")
        }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['set:banner:add']"
          >{{ $t("common.add") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['set:banner:edit']"
          >{{ $t("common.edit") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['set:banner:remove']"
          >{{ $t("common.delete") }}</el-button
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
      <el-table-column
        :label="$t('banner.primaryKeyId')"
        align="center"
        prop="id"
      />
      <el-table-column
        :label="$t('banner.bannerName')"
        align="center"
        prop="name"
      />
      <el-table-column
        :label="$t('banner.imageUrl')"
        align="center"
        prop="imageUrl"
        width="100"
      >
        <template #default="scope">
          <image-preview :src="scope.row.imageUrl" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('banner.linkUrl')"
        align="center"
        prop="linkUrl"
      />
      <el-table-column :label="$t('banner.sort')" align="center" prop="sort" />
      <el-table-column
        :label="$t('banner.status')"
        align="center"
        prop="status"
      >
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('common.operation')"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['set:banner:edit']"
            >{{ $t("common.edit") }}</el-button
          >
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['set:banner:remove']"
            >{{ $t("common.delete") }}</el-button
          >
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

    <!-- 添加或修改轮播图管理对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="bannerRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="$t('banner.bannerName')" prop="name">
          <el-input
            v-model="form.name"
            :placeholder="$t('banner.enterBannerName')"
          />
        </el-form-item>
        <el-form-item :label="$t('banner.imageUrl')" prop="imageUrl">
          <image-upload v-model="form.imageUrl" :limit="1" />
        </el-form-item>
        <el-form-item :label="$t('banner.linkUrl')" prop="linkUrl">
          <el-input
            v-model="form.linkUrl"
            :placeholder="$t('banner.enterLinkUrl')"
          />
        </el-form-item>
        <el-form-item :label="$t('banner.sort')" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            class="input-number-wide"
            :placeholder="$t('banner.enterSort')"
            style="width: 80%"
          />
        </el-form-item>
        <el-form-item :label="$t('banner.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="parseInt(dict.value)"
              >{{ dict.label }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{
            $t("common.confirm")
          }}</el-button>
          <el-button @click="cancel">{{ $t("common.cancel") }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Banner">
import { reactive, toRefs, ref } from "vue";
import { useI18n } from "vue-i18n";
import {
  listBanner,
  getBanner,
  delBanner,
  addBanner,
  updateBanner,
} from "@/api/set/banner";

const { t } = useI18n();

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

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
    imageUrl: null,
    linkUrl: null,
    sort: null,
    status: null,
  },
  rules: {
    name: [
      { required: true, message: t("banner.nameRequired"), trigger: "blur" },
    ],
    imageUrl: [
      {
        required: true,
        message: t("banner.imageUrlRequired"),
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询轮播图管理列表 */
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
    imageUrl: null,
    linkUrl: null,
    sort: null,
    status: "0",
    createTime: null,
    updateTime: null,
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
  proxy.resetForm("queryref");
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
  title.value = t("banner.addBanner");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getBanner(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("banner.editBanner");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["bannerRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateBanner(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("common.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addBanner(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("common.addSuccess"));
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
    .confirm(t("banner.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delBanner(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("common.deleteSuccess"));
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "set/banner/export",
    {
      ...queryParams.value,
    },
    `${t("banner.exportFileName")}_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
