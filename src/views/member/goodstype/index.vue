<template>
  <div class="app-container">
    <!-- <el-form
      :model="queryParams"
      ref="queryref"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('member.goodstype.name')" prop="name">
        <el-input
          v-model="queryParams.name"
          :placeholder="$t('member.goodstype.enterName')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.goodstype.sort')" prop="sort">
        <el-input
          v-model="queryParams.sort"
          :placeholder="$t('member.goodstype.enterSort')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.goodstype.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="$t('member.goodstype.selectStatus')"
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
        <el-button type="primary" icon="Search" @click="handleQuery"
          >{{$t('common.search')}}</el-button
        >
        <el-button icon="Refresh" @click="resetQuery">{{$t('common.reset')}}</el-button>
      </el-form-item>
    </el-form> -->

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['member:goodstype:add']"
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
          v-hasPermi="['member:goodstype:edit']"
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
          v-hasPermi="['member:goodstype:remove']"
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
      :data="goodstypeList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        :label="$t('member.goodstype.id')"
        align="center"
        prop="id"
      />
      <el-table-column
        :label="$t('member.goodstype.name')"
        align="center"
        prop="name"
      />
      <el-table-column
        :label="$t('member.goodstype.sort')"
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
            circle
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['member:goodstype:edit']"
          ></el-button>
          <el-button
            circle
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:goodstype:remove']"
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

    <!-- 添加或修改商品类别对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form
        ref="goodstypeRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item :label="$t('member.goodstype.name')" prop="name">
          <el-input
            v-model="form.name"
            :placeholder="$t('member.goodstype.enterName')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.goodstype.sort')" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            class="input-number-wide"
            :placeholder="$t('member.goodstype.enterSort')"
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

<script setup name="Goodstype">
import { reactive, toRefs } from "vue";
import { useI18n } from "vue-i18n";
import {
  listGoodstype,
  getGoodstype,
  delGoodstype,
  addGoodstype,
  updateGoodstype,
} from "@/api/member/goodstype";

const { t } = useI18n();

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const goodstypeList = ref([]);
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
    sort: null,
    status: null,
  },
  rules: {
    name: [
      {
        required: true,
        message: t("member.goodstype.nameRequired"),
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询商品类别列表 */
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
    name: null,
    sort: null,
    status: null,
    createTime: null,
    updateTime: null,
  };
  proxy.resetForm("goodstypeRef");
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
  title.value = t("member.goodstype.addGoodstype");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getGoodstype(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("member.goodstype.editGoodstype");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["goodstypeRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateGoodstype(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.goodstype.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addGoodstype(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.goodstype.addSuccess"));
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
    .confirm(t("member.goodstype.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delGoodstype(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("member.goodstype.deleteSuccess"));
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
    `${t("member.goodstype.exportFileName")}_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
