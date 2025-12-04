<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="商店名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入商店名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="VIP等级" prop="vipLevel">
        <el-input
          v-model="queryParams.vipLevel"
          placeholder="请输入VIP等级"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['member:shop:add']"
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
          v-hasPermi="['member:shop:edit']"
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
          v-hasPermi="['member:shop:remove']"
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
      :data="shopList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="商店名称" align="center" prop="name" />
      <el-table-column label="商店图标" align="center" prop="icon" width="100">
        <template #default="scope">
          <image-preview :src="scope.row.icon" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column label="VIP等级" align="center" prop="vipLevel" />
      <el-table-column label="最小交易金额" align="center" prop="minMoney" />
      <el-table-column label="最大交易金额" align="center" prop="maxMoney" />
      <el-table-column
        label="自动升级最小订单数"
        align="center"
        prop="autoVip"
      />
      <el-table-column
        label="自动升级最大订单数"
        align="center"
        prop="maxAutoVip"
      />
      <el-table-column
        label="佣金百分比"
        align="center"
        prop="commissionPercentage"
      />
      <el-table-column label="备注" align="center" prop="remark" />
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
            v-hasPermi="['member:shop:edit']"
          ></el-button>
          <el-button
            circle
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:shop:remove']"
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

    <!-- 添加或修改商品店铺对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="shopRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="商店名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商店名称" />
        </el-form-item>
        <el-form-item label="商店图标" prop="icon">
          <image-upload v-model="form.icon" :limit="1" />
        </el-form-item>
        <el-form-item label="VIP等级" prop="vipLevel">
          <el-input-number
            v-model="form.vipLevel"
            placeholder="请输入VIP等级"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="最小交易金额" prop="minMoney">
          <el-input-number
            v-model="form.minMoney"
            placeholder="请输入最小交易金额"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="最大交易金额" prop="maxMoney">
          <el-input-number
            v-model="form.maxMoney"
            placeholder="请输入最大交易金额"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="佣金百分比" prop="commissionPercentage">
          <el-input-number
            v-model="form.commissionPercentage"
            placeholder="请输入佣金百分比"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="自动升级最小订单数" prop="autoVip">
          <el-input-number
            v-model="form.autoVip"
            placeholder="自动升级订单数"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="自动升级最大订单数" prop="maxAutoVip">
          <el-input-number
            v-model="form.maxAutoVip"
            placeholder="自动升级订单数"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            placeholder="请输入内容"
          />
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

<script setup name="Shop">
import {
  listShop,
  getShop,
  delShop,
  addShop,
  updateShop,
} from "@/api/member/shop";

const { proxy } = getCurrentInstance();

const shopList = ref([]);
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
    icon: null,
    vipLevel: null,
    minMoney: null,
    maxMoney: null,
    commissionPercentage: null,
  },
  rules: {
    name: [{ required: true, message: "商店名称不能为空", trigger: "blur" }],
    icon: [{ required: true, message: "商店图标不能为空", trigger: "blur" }],
    vipLevel: [
      { required: true, message: "VIP 等级不能为空", trigger: "blur" },
    ],
    autoVip: [
      { required: true, message: "自动升级最小单数不能为空", trigger: "blur" },
    ],
    maxAutoVip: [
      { required: true, message: "自动升级最大单数不能为空", trigger: "blur" },
    ],
    minMoney: [
      { required: true, message: "最小交易金额不能为空", trigger: "blur" },
    ],
    maxMoney: [
      { required: true, message: "最大交易金额不能为空", trigger: "blur" },
    ],
    commissionPercentage: [
      { required: true, message: "佣金百分比不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询商品店铺列表 */
function getList() {
  loading.value = true;
  listShop(queryParams.value).then((response) => {
    shopList.value = response.rows;
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
    icon: null,
    vipLevel: null,
    minMoney: null,
    maxMoney: null,
    commissionPercentage: null,
    remark: null,
    createTime: null,
    updateTime: null,
  };
  proxy.resetForm("shopRef");
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
  title.value = "添加商品店铺";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getShop(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改商品店铺";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["shopRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateShop(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addShop(form.value).then((response) => {
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
    .confirm('是否确认删除商品店铺编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delShop(_ids);
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
    "member/shop/export",
    {
      ...queryParams.value,
    },
    `shop_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
