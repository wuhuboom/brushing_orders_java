<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="订单编号" prop="orderNumber">
        <el-input
          v-model="queryParams.orderNumber"
          placeholder="请输入订单编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户名" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
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

    <el-table
      v-loading="loading"
      :data="orderinfoList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="明细编号" align="center" prop="orderNumber" />
      <el-table-column label="用户名" align="center" prop="username" />
      <el-table-column label="类型" align="center" prop="type">
        <template #default="scope">
          <dict-tag :options="order_type" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column label="单数" align="center" prop="orderCount" />
      <el-table-column label="金额" align="center" prop="amount" />
      <el-table-column
        label="返佣百分比"
        align="center"
        prop="rebatePercentage"
      />
      <el-table-column label="返佣" align="center" prop="rebate" />
      <el-table-column
        label="上级返佣百分比"
        align="center"
        prop="upperRebatePercentage"
      />
      <el-table-column label="上级返佣" align="center" prop="upperRebate" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="order_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="过期时间"
        align="center"
        prop="expiryTime"
        width="180"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.expiryTime, "{y}-{m}-{d}") }}</span>
        </template>
      </el-table-column>
      <el-table-column label="商品图片" align="center" prop="productImage">
        <template #default="scope">
          <image-preview
            :src="scope.row.productImage"
            :width="50"
            :height="50"
          />
        </template>
      </el-table-column>
      <el-table-column label="商品标题" align="center" prop="productTitle" />
      <el-table-column
        label="额外佣金"
        align="center"
        prop="extraCommissionId"
      />
      <el-table-column label="备注" align="center" prop="remarks" />
      <!-- <el-table-column
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
            v-hasPermi="['member:orderinfo:edit']"
          ></el-button>
          <el-button
            circle
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:orderinfo:remove']"
          ></el-button>
        </template>
      </el-table-column> -->
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改订单对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form
        ref="orderinfoRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="订单标号" prop="orderNumber">
          <el-input v-model="form.orderNumber" placeholder="请输入订单标号" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option
              v-for="dict in order_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="单数" prop="orderCount">
          <el-input v-model="form.orderCount" placeholder="请输入单数" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="form.amount" placeholder="请输入金额" />
        </el-form-item>
        <el-form-item label="返佣百分比" prop="rebatePercentage">
          <el-input
            v-model="form.rebatePercentage"
            placeholder="请输入返佣百分比"
          />
        </el-form-item>
        <el-form-item label="返佣" prop="rebate">
          <el-input v-model="form.rebate" placeholder="请输入返佣" />
        </el-form-item>
        <el-form-item label="上级返佣百分比" prop="upperRebatePercentage">
          <el-input
            v-model="form.upperRebatePercentage"
            placeholder="请输入上级返佣百分比"
          />
        </el-form-item>
        <el-form-item label="上级返佣" prop="upperRebate">
          <el-input v-model="form.upperRebate" placeholder="请输入上级返佣" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in order_status"
              :key="dict.value"
              :label="dict.value"
              >{{ dict.label }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item label="过期时间" prop="expiryTime">
          <el-date-picker
            clearable
            v-model="form.expiryTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择过期时间"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item label="商品ID" prop="productId">
          <el-input v-model="form.productId" placeholder="请输入商品ID" />
        </el-form-item>
        <el-form-item label="额外佣金ID" prop="extraCommissionId">
          <el-input
            v-model="form.extraCommissionId"
            placeholder="请输入额外佣金ID"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remarks">
          <el-input v-model="form.remarks" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="评论ID" prop="commentId">
          <el-input v-model="form.commentId" placeholder="请输入评论ID" />
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

<script setup name="Orderinfo">
import {
  listOrderinfo,
  getOrderinfo,
  delOrderinfo,
  addOrderinfo,
  updateOrderinfo,
} from "@/api/member/orderinfo";

const { proxy } = getCurrentInstance();
const { order_status, order_type } = proxy.useDict(
  "order_status",
  "order_type"
);

const orderinfoList = ref([]);
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
    orderNumber: null,
    userId: null,
    type: null,
    orderCount: null,
    amount: null,
    rebatePercentage: null,
    rebate: null,
    upperRebatePercentage: null,
    upperRebate: null,
    status: null,
    expiryTime: null,
    productId: null,
    extraCommissionId: null,
    remarks: null,
    commentId: null,
  },
  rules: {
    orderNumber: [
      { required: true, message: "订单标号不能为空", trigger: "blur" },
    ],
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    type: [{ required: true, message: "类型不能为空", trigger: "change" }],
    orderCount: [{ required: true, message: "单数不能为空", trigger: "blur" }],
    amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
    rebatePercentage: [
      { required: true, message: "返佣百分比不能为空", trigger: "blur" },
    ],
    rebate: [{ required: true, message: "返佣不能为空", trigger: "blur" }],
    upperRebatePercentage: [
      { required: true, message: "上级返佣百分比不能为空", trigger: "blur" },
    ],
    upperRebate: [
      { required: true, message: "上级返佣不能为空", trigger: "blur" },
    ],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }],
    expiryTime: [
      { required: true, message: "过期时间不能为空", trigger: "blur" },
    ],
    productId: [{ required: true, message: "商品ID不能为空", trigger: "blur" }],
    extraCommissionId: [
      { required: true, message: "额外佣金ID不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询订单列表 */
function getList() {
  loading.value = true;
  listOrderinfo(queryParams.value).then((response) => {
    orderinfoList.value = response.rows;
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
    orderNumber: null,
    userId: null,
    type: null,
    orderCount: null,
    amount: null,
    rebatePercentage: null,
    rebate: null,
    upperRebatePercentage: null,
    upperRebate: null,
    status: null,
    expiryTime: null,
    productId: null,
    extraCommissionId: null,
    createTime: null,
    remarks: null,
    commentId: null,
  };
  proxy.resetForm("orderinfoRef");
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
  title.value = "添加订单";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getOrderinfo(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改订单";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["orderinfoRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateOrderinfo(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addOrderinfo(form.value).then((response) => {
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
    .confirm('是否确认删除订单编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delOrderinfo(_ids);
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
    "member/orderinfo/export",
    {
      ...queryParams.value,
    },
    `orderinfo_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
