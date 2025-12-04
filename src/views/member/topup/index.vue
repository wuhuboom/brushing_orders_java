<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('member.topup.username')" prop="username">
        <el-input
          v-model="queryParams.username"
          :placeholder="$t('member.topup.enterUsername')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.topup.code')" prop="code">
        <el-input
          v-model="queryParams.code"
          :placeholder="$t('member.topup.enterCode')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('member.topup.rechargeTime')"
        style="width: 400px"
      >
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          :start-placeholder="$t('common.startDate')"
          :end-placeholder="$t('common.endDate')"
        ></el-date-picker>
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
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="topupList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        :label="$t('member.topup.id')"
        align="center"
        prop="id"
      />
      <el-table-column
        :label="$t('member.topup.code')"
        align="center"
        prop="code"
      />
      <el-table-column
        :label="$t('member.topup.username')"
        align="center"
        prop="username"
      />
      <el-table-column
        :label="$t('member.topup.phone')"
        align="center"
        prop="phone"
      />
      <el-table-column
        :label="$t('member.topup.amount')"
        align="center"
        prop="amout"
      />
      <el-table-column
        :label="$t('member.topup.createTime')"
        align="center"
        prop="createTime"
        width="160"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
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

    <!-- 添加或修改充值记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="topupRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item :label="$t('member.topup.code')" prop="code">
          <el-input
            v-model="form.code"
            :placeholder="$t('member.topup.enterCode')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.topup.userId')" prop="userId">
          <el-input
            v-model="form.userId"
            :placeholder="$t('member.topup.enterUserId')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.topup.amount')" prop="amout">
          <el-input
            v-model="form.amout"
            :placeholder="$t('member.topup.enterAmount')"
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

<script setup name="Topup">
import {
  listTopup,
  getTopup,
  delTopup,
  addTopup,
  updateTopup,
} from "@/api/member/topup";
import { reactive, toRefs } from "vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

const { proxy } = getCurrentInstance();

const topupList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    code: null,
    username: null,
  },
  rules: {
    code: [
      {
        required: true,
        message: t("member.topup.codeRequired"),
        trigger: "blur",
      },
    ],
    userId: [
      {
        required: true,
        message: t("member.topup.userIdRequired"),
        trigger: "blur",
      },
    ],
    amout: [
      {
        required: true,
        message: t("member.topup.amountRequired"),
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询充值记录列表 */
function getList() {
  loading.value = true;
  listTopup(proxy.addDateRange(queryParams.value, dateRange.value)).then(
    (response) => {
      topupList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    }
  );
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
    code: null,
    userId: null,
    amout: null,
    createTime: null,
  };
  proxy.resetForm("topupRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
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
  title.value = t("member.topup.addRechargeRecord");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getTopup(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("member.topup.editRechargeRecord");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["topupRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateTopup(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.topup.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addTopup(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.topup.addSuccess"));
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
    .confirm(t("member.topup.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delTopup(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("member.topup.deleteSuccess"));
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/topup/export",
    {
      ...queryParams.value,
    },
    `${t("member.topup.exportFileName")}_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
