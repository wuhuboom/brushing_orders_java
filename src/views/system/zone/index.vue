<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryref"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('system.zone.name')" prop="name">
        <el-input
          v-model="queryParams.name"
          :placeholder="$t('system.zone.enterName')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('system.zone.tzName')" prop="tzName">
        <el-input
          v-model="queryParams.tzName"
          :placeholder="$t('system.zone.enterTzName')"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['system:zone:add']"
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
          v-hasPermi="['system:zone:edit']"
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
          v-hasPermi="['system:zone:remove']"
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
      :data="zoneList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        :label="$t('system.zone.name')"
        align="center"
        prop="name"
      />
      <el-table-column
        :label="$t('system.zone.tzName')"
        align="center"
        prop="tzName"
      />
      <el-table-column
        :label="$t('system.zone.status')"
        align="center"
        prop="status"
      >
        <template #default="scope">
          <div v-if="scope.row.status == '0'">
            {{ $t("system.zone.inUse") }}
          </div>
          <div v-else>--</div>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('common.operation')"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            circle
            type="success"
            icon="Check"
            :title="
              scope.row.status === 0
                ? $t('system.zone.currentlyInUse')
                : $t('system.zone.selectForUse')
            "
            v-if="scope.row.status !== 0"
            @click="handleSetActive(scope.row)"
            v-hasPermi="['system:zone:edit']"
          ></el-button>
          <el-button
            circle
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:zone:edit']"
          ></el-button>
          <el-button
            circle
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:zone:remove']"
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

    <!-- 添加或修改时区管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="zoneRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item :label="$t('system.zone.name')" prop="name">
          <el-input
            v-model="form.name"
            :placeholder="$t('system.zone.enterName')"
          />
        </el-form-item>
        <el-form-item :label="$t('system.zone.tzName')" prop="tzName">
          <el-input
            v-model="form.tzName"
            :placeholder="$t('system.zone.enterTzName')"
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

<script setup name="Zone">
import {
  listZone,
  getZone,
  delZone,
  addZone,
  updateZone,
  setActiveZone,
  getZoneActive,
} from "@/api/system/zone";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { refreshActiveTimeZone } from "@/utils/timezone-helper";

const { t } = useI18n();

const { proxy } = getCurrentInstance();

const zoneList = ref([]);
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
    tzName: null,
    status: null,
  },
  rules: {
    name: [
      {
        required: true,
        message: t("system.zone.nameRequired"),
        trigger: "blur",
      },
    ],
    tzName: [
      {
        required: true,
        message: t("system.zone.tzNameRequired"),
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

function handleSetActive(row) {
  ElMessageBox.confirm(
    t("system.zone.confirmSetActive", { zone: row.name || row.tzName }),
    t("common.prompt"),
    { type: "warning" }
  )
    .then(() => setActiveZone(row.id))
    .then(() => {
      ElMessage.success(t("system.zone.switchSuccess"));
      refreshActiveTimeZone();
      return getList();
    })
    .catch(() => {});
}

/** 查询时区管理列表 */
function getList() {
  loading.value = true;
  listZone(queryParams.value).then((response) => {
    zoneList.value = response.rows;
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
    tzName: null,
    status: null,
    createTime: null,
  };
  proxy.resetForm("zoneRef");
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
  title.value = t("system.zone.addZone");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getZone(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("system.zone.editZone");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["zoneRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateZone(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("system.zone.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addZone(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("system.zone.addSuccess"));
          refreshActiveTimeZone();
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
    .confirm(t("system.zone.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delZone(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("system.zone.deleteSuccess"));
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "system/zone/export",
    {
      ...queryParams.value,
    },
    `${t("system.zone.exportFileName")}_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
