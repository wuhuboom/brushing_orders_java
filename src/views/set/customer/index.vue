<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryref"
      :inline="true"
      v-show="showSearch"
      label-width="100px"
    >
      <el-form-item :label="$t('customer.name')" prop="name">
        <el-input
          v-model="queryParams.name"
          :placeholder="$t('customer.enterName')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('customer.linkUrl')" prop="linkUrl">
        <el-input
          v-model="queryParams.linkUrl"
          :placeholder="$t('customer.enterLinkUrl')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('customer.sort')" prop="sort">
        <el-input
          v-model="queryParams.sort"
          :placeholder="$t('customer.enterSort')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('customer.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="$t('customer.selectStatus')"
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
          v-hasPermi="['set:customer:add']"
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
          v-hasPermi="['set:customer:edit']"
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
          v-hasPermi="['set:customer:remove']"
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
      :data="customerList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        :label="$t('customer.primaryKeyId')"
        align="center"
        prop="id"
      />
      <el-table-column
        :label="$t('customer.name')"
        align="center"
        prop="name"
      />
      <el-table-column
        :label="$t('customer.linkUrl')"
        align="center"
        prop="linkUrl"
      />
      <el-table-column
        :label="$t('customer.iconUrl')"
        align="center"
        prop="iconUrl"
        width="100"
      >
        <template #default="scope">
          <image-preview :src="scope.row.iconUrl" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('customer.sort')"
        align="center"
        prop="sort"
      />
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
            v-hasPermi="['set:customer:edit']"
            >{{ $t("common.edit") }}</el-button
          >
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['set:customer:remove']"
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

    <!-- 添加或修改客服管理对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form
        ref="customerRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item :label="$t('customer.name')" prop="name">
          <el-input
            v-model="form.name"
            :placeholder="$t('customer.enterName')"
          />
        </el-form-item>
        <el-form-item :label="$t('customer.nameZh')" prop="nameZh">
          <el-input
            v-model="form.nameZh"
            :placeholder="$t('customer.enterNameZh')"
          />
        </el-form-item>
        <el-form-item :label="$t('customer.nameJp')" prop="nameJp">
          <el-input
            v-model="form.nameJp"
            :placeholder="$t('customer.enterNameJp')"
          />
        </el-form-item>
        <el-form-item :label="$t('customer.nameKo')" prop="nameKo">
          <el-input
            v-model="form.nameKo"
            :placeholder="$t('customer.enterNameKo')"
          />
        </el-form-item>
        <el-form-item :label="$t('customer.nameTh')" prop="nameTh">
          <el-input
            v-model="form.nameTh"
            :placeholder="$t('customer.enterNameTh')"
          />
        </el-form-item>
        <el-form-item :label="$t('customer.nameZhTw')" prop="nameZhTw">
          <el-input
            v-model="form.nameZhTw"
            :placeholder="$t('customer.enterNameZhTw')"
          />
        </el-form-item>
        <el-form-item :label="$t('customer.namePor')" prop="namePor">
          <el-input v-model="form.namePor" />
        </el-form-item>
        <el-form-item :label="$t('customer.nameEs')" prop="nameEs">
          <el-input
            v-model="form.nameEs"
            :placeholder="$t('customer.enterNameEs')"
          />
        </el-form-item>
        <el-form-item :label="$t('customer.linkUrl')" prop="linkUrl">
          <el-input
            v-model="form.linkUrl"
            :placeholder="$t('customer.enterLinkUrl')"
          />
        </el-form-item>
        <el-form-item :label="$t('customer.iconUrl')" prop="iconUrl">
          <image-upload v-model="form.iconUrl" :limit="1" />
        </el-form-item>
        <el-form-item :label="$t('customer.sort')" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            class="input-number-wide"
            :placeholder="$t('customer.enterSort')"
            style="width: 80%"
          />
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

<script setup name="Customer">
import { reactive, toRefs } from "vue";
import { useI18n } from "vue-i18n";
import {
  listCustomer,
  getCustomer,
  delCustomer,
  addCustomer,
  updateCustomer,
} from "@/api/set/customer";

const { t } = useI18n();

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const customerList = ref([]);
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
    linkUrl: null,
    iconUrl: null,
    sort: null,
    status: null,
  },
  rules: {
    name: [
      { required: true, message: t("customer.nameRequired"), trigger: "blur" },
    ],
    linkUrl: [
      {
        required: true,
        message: t("customer.linkUrlRequired"),
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询客服管理列表 */
function getList() {
  loading.value = true;
  listCustomer(queryParams.value).then((response) => {
    customerList.value = response.rows;
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
    linkUrl: null,
    iconUrl: null,
    sort: null,
    status: "0",
    createTime: null,
    updateTime: null,
    nameZh: null,
    nameJp: null,
    nameKo: null,
    nameTh: null,
    nameZhTw: null,
    namePor: null,
    nameEs: null,
  };
  proxy.resetForm("customerRef");
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
  title.value = t("customer.addCustomer");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getCustomer(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("customer.editCustomer");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["customerRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateCustomer(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("customer.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addCustomer(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("customer.addSuccess"));
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
    .confirm(t("customer.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delCustomer(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("customer.deleteSuccess"));
    })
    .catch(() => {});
}

getList();
</script>
