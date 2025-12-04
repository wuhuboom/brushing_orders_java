<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryref"
      :inline="true"
      v-show="showsearch"
      label-width="68px"
    >
      <el-form-item :label="$t('notice.orderTitle')" prop="noticeTitle">
        <el-input
          v-model="queryParams.noticeTitle"
          :placeholder="$t('notice.enterOrderTitle')"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('notice.operator')" prop="createBy">
        <el-input
          v-model="queryParams.createBy"
          :placeholder="$t('notice.enterOperator')"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('notice.type')" prop="noticeType">
        <el-select
          v-model="queryParams.noticeType"
          :placeholder="$t('notice.selectType')"
          clearable
          style="width: 200px"
        >
          <el-option
            v-for="dict in sys_notice_type"
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
          v-hasPermi="['system:notice:add']"
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
          v-hasPermi="['system:notice:edit']"
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
          v-hasPermi="['system:notice:remove']"
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
      :data="noticeList"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        :label="$t('notice.serialNumber')"
        align="center"
        prop="noticeId"
        width="100"
      />
      <el-table-column
        :label="$t('notice.noticeTitle')"
        align="center"
        prop="noticeTitle"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        :label="$t('notice.status')"
        align="center"
        prop="status"
      >
        <template #default="scope">
          <dict-tag :options="sys_notice_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('notice.creator')"
        align="center"
        prop="createBy"
      />
      <el-table-column
        :label="$t('notice.createTime')"
        align="center"
        prop="createTime"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
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
            v-hasPermi="['system:notice:edit']"
            >{{ $t("common.edit") }}</el-button
          >
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:notice:remove']"
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

    <!-- 添加或修改公告对话框 -->
    <el-dialog :title="title" v-model="open" width="780px" append-to-body>
      <el-form ref="noticeRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('notice.noticeTitle')" prop="noticeTitle">
              <el-input
                v-model="form.noticeTitle"
                :placeholder="$t('notice.enterNoticeTitle')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('notice.createTime')" prop="createTime">
              <el-date-picker
                v-model="form.createTime"
                type="datetime"
                :placeholder="$t('notice.selectCreateTime')"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('notice.status')">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in sys_notice_status"
                  :key="dict.value"
                  :value="dict.value"
                  >{{ dict.label }}</el-radio
                >
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('notice.content')">
              <editor v-model="form.noticeContent" :min-height="192" />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup name="Notice">
import { reactive, toRefs, ref } from "vue";
import { useI18n } from "vue-i18n";
import {
  listNotice,
  getNotice,
  delNotice,
  addNotice,
  updateNotice,
} from "@/api/system/notice";

const { t } = useI18n();

const { proxy } = getCurrentInstance();
const { sys_notice_status, sys_notice_type } = proxy.useDict(
  "sys_notice_status",
  "sys_notice_type"
);

const noticeList = ref([]);
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
    noticeTitle: undefined,
    createBy: undefined,
    status: undefined,
  },
  rules: {
    noticeTitle: [
      {
        required: true,
        message: t("notice.noticeTitleRequired"),
        trigger: "blur",
      },
    ],
    noticeType: [
      {
        required: true,
        message: t("notice.noticeTypeRequired"),
        trigger: "change",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询公告列表 */
function getList() {
  loading.value = true;
  listNotice(queryParams.value).then((response) => {
    noticeList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 表单重置 */
function reset() {
  form.value = {
    noticeId: undefined,
    noticeTitle: undefined,
    noticeType: undefined,
    noticeContent: undefined,
    status: "0",
  };
  proxy.resetForm("noticeRef");
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

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.noticeId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = t("notice.addNotice");
}

/**修改按钮操作 */
function handleUpdate(row) {
  reset();
  const noticeId = row.noticeId || ids.value;
  getNotice(noticeId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("notice.editNotice");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["noticeRef"].validate((valid) => {
    if (valid) {
      if (form.value.noticeId != undefined) {
        updateNotice(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("notice.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addNotice(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("notice.addSuccess"));
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const noticeIds = row.noticeId || ids.value;
  proxy.$modal
    .confirm(t("notice.deleteConfirm", { ids: noticeIds }))
    .then(function () {
      return delNotice(noticeIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("notice.deleteSuccess"));
    })
    .catch(() => {});
}

getList();
</script>
