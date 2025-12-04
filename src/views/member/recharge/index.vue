<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="用户名" prop="userId">
        <el-input
          v-model="queryParams.userName"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号码" prop="userId">
        <el-input
          v-model="queryParams.userName"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>

      <el-form-item label="订单号" prop="orderNumber">
        <el-input
          v-model="queryParams.orderNumber"
          placeholder="请输入订单号"
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
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="rechargeList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户名" align="center" prop="username" />
      <el-table-column label="手机号" align="center" prop="phoneNumber" />
      <el-table-column
        label="上级用户名"
        align="center"
        prop="parentUsername"
      />
      <el-table-column label="金额" align="center" prop="amount" />
      <el-table-column label="出金类型" align="center" prop="withdrawalType" />
      <el-table-column label="赠送金额" align="center" prop="giftAmount" />
      <el-table-column label="到账金额" align="center" prop="receivedAmount" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-tag effect="dark" type="success">已通过 </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="交易类型" align="center" prop="transactionType">
        <template #default="scope">
          <dict-tag
            :options="transaction_type"
            :value="scope.row.transactionType"
          />
        </template>
      </el-table-column>
      <el-table-column label="订单号" align="center" prop="orderNumber" />
      <el-table-column label="是否隐藏" align="center" prop="isHidden">
        <template #default="scope">
          <dict-tag :options="user_yes_no" :value="scope.row.isHidden" />
        </template>
      </el-table-column>
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
            v-hasPermi="['member:recharge:edit']"
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

    <!-- 添加或修改充值记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form
        ref="rechargeRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
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

<script setup name="Recharge">
import {
  listRecharge,
  getRecharge,
  delRecharge,
  addRecharge,
  updateRecharge,
} from "@/api/member/recharge";

const { proxy } = getCurrentInstance();
const { transaction_type, user_yes_no } = proxy.useDict(
  "transaction_type",
  "user_yes_no"
);

const rechargeList = ref([]);
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
    userId: null,
    amount: null,
    withdrawalType: null,
    giftAmount: null,
    receivedAmount: null,
    status: null,
    createdTime: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
  },
  rules: {
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
    withdrawalType: [
      { required: true, message: "出金类型不能为空", trigger: "change" },
    ],
    receivedAmount: [
      { required: true, message: "到账金额不能为空", trigger: "blur" },
    ],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }],
    transactionType: [
      { required: true, message: "交易类型不能为空", trigger: "change" },
    ],
    orderNumber: [
      { required: true, message: "订单号不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询充值记录列表 */
function getList() {
  loading.value = true;
  listRecharge(queryParams.value).then((response) => {
    rechargeList.value = response.rows;
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
    userId: null,
    amount: null,
    withdrawalType: null,
    giftAmount: null,
    receivedAmount: null,
    status: null,
    createdTime: null,
    remark: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
  };
  proxy.resetForm("rechargeRef");
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
  title.value = "添加充值记录";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getRecharge(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改备注";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["rechargeRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateRecharge(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addRecharge(form.value).then((response) => {
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
    .confirm('是否确认删除充值记录编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delRecharge(_ids);
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
    "member/recharge/export",
    {
      ...queryParams.value,
    },
    `recharge_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
