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
          v-model="queryParams.username"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phoneNumber">
        <el-input
          v-model="queryParams.phoneNumber"
          placeholder="请输入金额"
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
      :data="withdrawalList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="用户名" align="center" prop="username" />
      <el-table-column label="手机号" align="center" prop="phoneNumber" />
      <el-table-column
        label="上级用户名"
        align="center"
        prop="parentUsername"
      />
      <el-table-column label="提现账户" align="center" prop="amount">
        <template #default="scope">
          <div v-if="scope.row.withdrawalAccountInfo.type == '1'">
            <div>
              钱包名称：{{ scope.row.withdrawalAccountInfo.walletName }}
            </div>
            <div>
              钱包地址：{{ scope.row.withdrawalAccountInfo.walletAddress }}
            </div>
          </div>
          <div v-else>
            <div>银行名称：{{ scope.row.withdrawalAccountInfo.bankName }}</div>
            <div>
              银行账号：{{ scope.row.withdrawalAccountInfo.bankAccount }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="金额" align="center" prop="amount" />
      <el-table-column label="出金类型" align="center" prop="withdrawalType">
        <template #default="scope">
          <div>
            {{ scope.row.withdrawalAccountInfo.withdrawalType }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="apply_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remarks" />
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
      <el-table-column label="手续费" align="center" prop="fee" />
      <el-table-column
        label="操作"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            link
            type="primary"
            :disabled="scope.row.status !== '1'"
            @click="handleApprove(scope.row)"
            v-hasPermi="['member:withdrawal:edit']"
            >通过</el-button
          >
          <el-button
            link
            type="danger"
            :disabled="scope.row.status !== '1'"
            @click="handleReject(scope.row)"
            v-hasPermi="['member:withdrawal:edit']"
            >拒绝</el-button
          >
          <el-button
            link
            type="primary"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['member:withdrawal:edit']"
            >备注</el-button
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

    <!-- 添加或修改提现对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form
        ref="withdrawalRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="备注" prop="remarks">
          <el-input
            type="textarea"
            v-model="form.remarks"
            placeholder="请输入备注"
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

<script setup name="Withdrawal">
import {
  listWithdrawal,
  getWithdrawal,
  delWithdrawal,
  addWithdrawal,
  updateWithdrawal,
} from "@/api/member/withdrawal";

const { proxy } = getCurrentInstance();
const { transaction_type, apply_status, order_zhlx, user_yes_no } =
  proxy.useDict(
    "transaction_type",
    "apply_status",
    "order_zhlx",
    "user_yes_no"
  );

const withdrawalList = ref([]);
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
    status: null,
    remarks: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
    fee: null,
    withdrawalAccountId: null,
  },
  rules: {
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
    withdrawalType: [
      { required: true, message: "出金类型不能为空", trigger: "change" },
    ],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }],
    transactionType: [
      { required: true, message: "交易类型不能为空", trigger: "change" },
    ],
    orderNumber: [
      { required: true, message: "订单号不能为空", trigger: "blur" },
    ],
    isHidden: [
      { required: true, message: "是否隐藏不能为空", trigger: "blur" },
    ],
    fee: [{ required: true, message: "手续费不能为空", trigger: "blur" }],
    withdrawalAccountId: [
      { required: true, message: "提现账号ID不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询提现列表 */
function getList() {
  loading.value = true;
  listWithdrawal(queryParams.value).then((response) => {
    withdrawalList.value = response.rows;
    console.log(response.rows);
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
    status: null,
    createTime: null,
    remarks: null,
    transactionType: null,
    orderNumber: null,
    isHidden: null,
    fee: null,
    withdrawalAccountId: null,
  };
  proxy.resetForm("withdrawalRef");
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
  title.value = "添加提现";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getWithdrawal(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改提现";
  });
}

/** 通过操作 */
function handleApprove(row) {
  proxy.$modal
    .confirm("确认通过该提现申请？")
    .then(() => {
      const updateData = { id: row.id, status: 0 };
      updateWithdrawal(updateData).then((response) => {
        proxy.$modal.msgSuccess("通过成功");
        getList();
      });
    })
    .catch(() => {});
}

/** 拒绝操作 */
function handleReject(row) {
  proxy.$modal
    .confirm("确认拒绝该提现申请？")
    .then(() => {
      const updateData = { id: row.id, status: 2 };
      updateWithdrawal(updateData).then((response) => {
        proxy.$modal.msgSuccess("拒绝成功");
        getList();
      });
    })
    .catch(() => {});
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["withdrawalRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateWithdrawal(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addWithdrawal(form.value).then((response) => {
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
    .confirm('是否确认删除提现编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delWithdrawal(_ids);
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
    "member/withdrawal/export",
    {
      ...queryParams.value,
    },
    `withdrawal_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
