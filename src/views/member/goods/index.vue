<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryref"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('member.goods.productName')" prop="name">
        <el-input
          v-model="queryParams.name"
          :placeholder="$t('member.goods.enterProductName')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.goods.priceRange')">
        <el-input
          v-model.number="queryParams.minPrice"
          :placeholder="$t('member.goods.minPrice')"
          style="width: 150px"
          type="number"
        />
        <span style="margin: 0 10px">-</span>
        <el-input
          v-model.number="queryParams.maxPrice"
          :placeholder="$t('member.goods.maxPrice')"
          style="width: 150px"
          type="number"
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
          v-hasPermi="['member:goods:add']"
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
          v-hasPermi="['member:goods:edit']"
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
          v-hasPermi="['member:goods:remove']"
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
      :data="goodsList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        :label="$t('member.goods.id')"
        align="center"
        prop="id"
      />
      <el-table-column
        :label="$t('member.goods.name')"
        align="center"
        prop="name"
      />
      <el-table-column
        :label="$t('member.goods.typeName')"
        align="center"
        prop="typeName"
      />
      <el-table-column
        :label="$t('member.goods.coverUrl')"
        align="center"
        prop="coverUrl"
        width="100"
      >
        <template #default="scope">
          <image-preview :src="scope.row.coverUrl" :width="50" :height="50" />
        </template>
      </el-table-column>

      <el-table-column
        :label="$t('member.goods.price')"
        align="center"
        prop="price"
      />
      <el-table-column
        :label="$t('member.goods.description')"
        align="center"
        prop="description"
      />
      <el-table-column
        :label="$t('member.goods.status')"
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
            circle
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['member:goods:edit']"
          ></el-button>
          <el-button
            circle
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:goods:remove']"
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

    <!-- 添加或修改商品列表对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="goodsRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item :label="$t('member.goods.coverUrl')" prop="coverUrl">
          <image-upload v-model="form.coverUrl" :limit="1" />
        </el-form-item>
        <el-form-item :label="$t('member.goods.name')" prop="name">
          <el-input
            v-model="form.name"
            :placeholder="$t('member.goods.enterName')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.goods.typeId')" prop="typeId">
          <el-select
            v-model="form.typeId"
            :placeholder="$t('member.goods.selectType')"
            clearable
          >
            <el-option
              v-for="item in typeDatas"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="$t('member.goods.price')" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            class="input-number-wide"
            :placeholder="$t('member.goods.enterPrice')"
            style="width: 80%"
          />
        </el-form-item>
        <el-form-item
          :label="$t('member.goods.description')"
          prop="description"
        >
          <el-input
            v-model="form.description"
            type="textarea"
            :placeholder="$t('member.goods.enterDescription')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.goods.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
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

<script setup name="Goods">
import { reactive, toRefs, ref } from "vue";
import { useI18n } from "vue-i18n";
import {
  listGoods,
  getGoods,
  delGoods,
  addGoods,
  updateGoods,
  listType,
} from "@/api/member/goods";

const { t } = useI18n();

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const goodsList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const typeDatas = ref([]);

const data = reactive({
  form: {
    id: null,
    name: null,
    coverUrl: null,
    price: null,
    description: null,
    status: "0",
    typeId: null, // 新增 typeId 字段
    createTime: null,
    updateTime: null,
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    status: null,
    minPrice: null,
    maxPrice: null,
  },
  rules: {
    name: [
      {
        required: true,
        message: t("member.goods.nameRequired"),
        trigger: "blur",
      },
    ],
    price: [
      {
        required: true,
        message: t("member.goods.priceRequired"),
        trigger: "blur",
      },
    ],
    coverUrl: [
      {
        required: true,
        message: t("member.goods.coverUrlRequired"),
        trigger: "blur",
      },
    ],
    typeId: [
      {
        required: true,
        message: t("member.goods.typeIdRequired"),
        trigger: "change",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询商品列表列表 */
function getList() {
  loading.value = true;
  listGoods(queryParams.value).then((response) => {
    goodsList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 获取商品类型列表
function getTypeList() {
  listType().then((res) => {
    // 修改为调用 listType
    typeDatas.value = res.data;
  });
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    name: null,
    coverUrl: null,
    price: null,
    description: null,
    status: "0",
    typeId: null, // 重置时清空 typeId
    createTime: null,
    updateTime: null,
  };
  proxy.resetForm("goodsRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.minPrice = null;
  queryParams.value.maxPrice = null;
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
  getTypeList(); // 新增时加载商品类型
  open.value = true;
  title.value = t("member.goods.addGoods");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getTypeList(); // 修改时加载商品类型
  getGoods(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("member.goods.editGoods");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["goodsRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateGoods(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.goods.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addGoods(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.goods.addSuccess"));
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
    .confirm(t("member.goods.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delGoods(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("member.goods.deleteSuccess"));
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/goods/export",
    {
      ...queryParams.value,
    },
    `${t("member.goods.exportFileName")}_${new Date().getTime()}.xlsx`
  );
}

getList();
getTypeList(); // 初始加载商品类型
</script>
