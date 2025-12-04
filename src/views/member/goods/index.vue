<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否启用" prop="isEnabled">
        <el-select
          v-model="queryParams.isEnabled"
          placeholder="请选择是否启用"
          clearable
          style="width: 220px"
        >
          <el-option
            v-for="dict in goods_enabled"
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
          v-hasPermi="['member:goods:add']"
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
          v-hasPermi="['member:goods:edit']"
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
          v-hasPermi="['member:goods:remove']"
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
      :data="goodsList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="标题" align="center" prop="title" />
      <el-table-column label="类目" align="center" prop="typeTitle" />
      <el-table-column label="是否启用" align="center" prop="isEnabled">
        <template #default="scope">
          <dict-tag :options="goods_enabled" :value="scope.row.isEnabled" />
        </template>
      </el-table-column>
      <el-table-column label="价格" align="center" prop="price" />
      <el-table-column label="序号" align="center" prop="serialNumber" />
      <el-table-column label="图片" align="center" prop="image" width="100">
        <template #default="scope">
          <image-preview :src="scope.row.image" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" />

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

    <!-- 添加或修改商品对话框 -->
    <el-dialog :title="title" v-model="open" width="40%" append-to-body>
      <el-form
        ref="goodsRef"
        label-position="top"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <!-- 第一行 -->
        <el-row :gutter="20">
          <el-col :span="22">
            <el-form-item label="标题" prop="title">
              <el-input
                type="textarea"
                v-model="form.title"
                placeholder="请输入标题"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第二行 -->
        <el-row :gutter="20">
          <el-col :span="11">
            <el-form-item label="类目" prop="typeId">
              <el-select
                v-model="form.typeId"
                placeholder="请选择商品类型"
                clearable
              >
                <el-option
                  v-for="item in typeDatas"
                  :key="item.id"
                  :label="item.title"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="11">
            <el-form-item label="是否启用" prop="isEnabled">
              <el-radio-group v-model="form.isEnabled">
                <el-radio
                  v-for="dict in goods_enabled"
                  :key="dict.value"
                  :label="dict.value"
                  >{{ dict.label }}</el-radio
                >
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="11">
            <el-form-item label="价格" prop="price">
              <el-input-number
                v-model="form.price"
                placeholder="请输入价格"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="11">
            <el-form-item label="序号" prop="serialNumber">
              <el-input v-model="form.serialNumber" placeholder="请输入序号" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第三行 -->
        <el-row :gutter="20">
          <el-col :span="11">
            <el-form-item label="图片" prop="image">
              <image-upload v-model="form.image" :limit="1" />
            </el-form-item>
          </el-col>
          <el-col :span="11">
            <el-form-item label="二级标题" prop="subTitle">
              <el-input
                type="textarea"
                v-model="form.subTitle"
                placeholder="请输入二级标题"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第四行 -->
        <el-row :gutter="20">
          <el-col :span="11">
            <el-form-item label="单价" prop="unitPrice">
              <el-input-number
                v-model="form.unitPrice"
                controls-position="right"
                placeholder="请输入单价"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="11">
            <el-form-item label="数量" prop="quantity">
              <el-input-number
                v-model="form.quantity"
                controls-position="right"
                placeholder="请输入数量"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第五行 -->
        <el-row :gutter="20">
          <el-col :span="11">
            <el-form-item label="星级" prop="starRating">
              <el-input-number
                v-model="form.starRating"
                controls-position="right"
                placeholder="请输入星级"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="11">
            <el-form-item label="评分" prop="rating">
              <el-input-number
                v-model="form.rating"
                controls-position="right"
                placeholder="请输入评分"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第六行 -->
        <el-row :gutter="20">
          <el-col :span="22">
            <el-form-item label="说明" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                placeholder="请输入内容"
              />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup name="Goods">
import {
  listGoods,
  getGoods,
  delGoods,
  addGoods,
  updateGoods,
  typeList,
} from "@/api/member/goods";

const { proxy } = getCurrentInstance();
const { goods_enabled } = proxy.useDict("goods_enabled");

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
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: null,
    isEnabled: null,
  },
  rules: {
    title: [{ required: true, message: "标题不能为空", trigger: "blur" }],
    typeId: [{ required: true, message: "类目不能为空", trigger: "blur" }],
    isEnabled: [
      { required: true, message: "是否启用不能为空", trigger: "change" },
    ],
    price: [{ required: true, message: "价格不能为空", trigger: "blur" }],
    serialNumber: [
      { required: true, message: "序号不能为空", trigger: "blur" },
    ],
    image: [{ required: true, message: "图片不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询商品列表 */
function getList() {
  loading.value = true;
  listGoods(queryParams.value).then((response) => {
    goodsList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function getTypeList() {
  typeList().then((res) => {
    typeDatas.value = res.data;
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
    title: null,
    typeId: null,
    isEnabled: "0",
    price: null,
    serialNumber: null,
    image: null,
    subTitle: null,
    unitPrice: null,
    quantity: null,
    starRating: null,
    rating: null,
    description: null,
    createBy: null,
    createTime: null,
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
  getTypeList();
  open.value = true;
  title.value = "添加商品";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getTypeList();
  getGoods(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改商品";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["goodsRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateGoods(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addGoods(form.value).then((response) => {
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
    .confirm('是否确认删除商品编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delGoods(_ids);
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
    "member/goods/export",
    {
      ...queryParams.value,
    },
    `goods_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
