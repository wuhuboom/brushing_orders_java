<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="类型" prop="type">
        <el-select
          v-model="queryParams.type"
          placeholder="请选择类型"
          clearable
          style="width: 220px"
        >
          <el-option
            v-for="dict in order_zhlx"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入名称"
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
          v-hasPermi="['member:withdrawaltype:add']"
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
          v-hasPermi="['member:withdrawaltype:edit']"
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
          v-hasPermi="['member:withdrawaltype:remove']"
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
      :data="withdrawaltypeList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="类型" align="center" prop="type">
        <template #default="scope">
          <dict-tag :options="order_zhlx" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column label="名称" align="center" prop="name" />
      <el-table-column label="汇率" align="center" prop="exchangeRate" />
      <el-table-column label="序号" align="center" prop="sortOrder" />
      <el-table-column label="图标" align="center" prop="icon" width="100">
        <template #default="scope">
          <image-preview :src="scope.row.icon" :width="50" :height="50" />
        </template>
      </el-table-column>

      <el-table-column label="创建时间" align="center" prop="createTime" />
      <el-table-column label="备注" align="center" prop="remarks" />
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
            v-hasPermi="['member:withdrawaltype:edit']"
          ></el-button>
          <el-button
            circle
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:withdrawaltype:remove']"
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

    <!-- 添加或修改出金类型对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form
        ref="withdrawaltypeRef"
        :model="form"
        :rules="rules"
        label-position="top"
        label-width="80px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-radio-group v-model="form.type">
                <el-radio
                  v-for="dict in order_zhlx"
                  :key="dict.value"
                  :label="dict.value"
                  >{{ dict.label }}</el-radio
                >
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="汇率" prop="exchangeRate">
              <el-input v-model="form.exchangeRate" placeholder="请输入汇率" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="序号" prop="sortOrder">
              <el-input v-model="form.sortOrder" placeholder="请输入序号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="图标" prop="icon">
              <image-upload v-model="form.icon" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 银行卡字段 (type === '0') -->
        <el-row v-if="form.type === '0'" :gutter="20">
          <el-col :span="12">
            <el-form-item label="银行名称" prop="bankName">
              <el-input v-model="form.bankName" placeholder="请输入银行名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存款种类" prop="depositType">
              <el-input
                v-model="form.depositType"
                placeholder="请输入存款种类"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.type === '0'" :gutter="20">
          <el-col :span="12">
            <el-form-item label="支行代码" prop="branchCode">
              <el-input
                v-model="form.branchCode"
                placeholder="请输入支行代码"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支行名称" prop="branchName">
              <el-input
                v-model="form.branchName"
                placeholder="请输入支行名称"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.type === '0'" :gutter="20">
          <el-col :span="12">
            <el-form-item label="银行账号" prop="bankAccount">
              <el-input
                v-model="form.bankAccount"
                placeholder="请输入银行账号"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账户持有人" prop="accountHolder">
              <el-input
                v-model="form.accountHolder"
                placeholder="请输入账户持有人"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 钱包字段 (type === '1') -->
        <el-row v-if="form.type === '1'" :gutter="20">
          <el-col :span="12">
            <el-form-item label="账户名称" prop="accountName">
              <el-input
                v-model="form.accountName"
                placeholder="请输入账户名称"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="钱包名称" prop="walletName">
              <el-input
                v-model="form.walletName"
                placeholder="请输入钱包名称"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.type === '1'" :gutter="20">
          <el-col :span="24">
            <el-form-item label="钱包地址" prop="walletAddress">
              <el-input
                v-model="form.walletAddress"
                placeholder="请输入钱包地址"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注" prop="remarks">
              <el-input
                type="textarea"
                v-model="form.remarks"
                placeholder="请输入备注"
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

<script setup name="Withdrawaltype">
import {
  listWithdrawaltype,
  getWithdrawaltype,
  delWithdrawaltype,
  addWithdrawaltype,
  updateWithdrawaltype,
} from "@/api/member/withdrawaltype";

const { proxy } = getCurrentInstance();
const { order_zhlx } = proxy.useDict("order_zhlx");

const withdrawaltypeList = ref([]);
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
    type: null,
    name: null,
    exchangeRate: null,
    sortOrder: null,
    icon: null,
    bankName: null,
    depositType: null,
    branchCode: null,
    branchName: null,
    bankAccount: null,
    accountHolder: null,
    accountName: null,
    walletName: null,
    walletAddress: null,
    remarks: null,
  },
  rules: {
    type: [{ required: true, message: "类型不能为空", trigger: "change" }],
    name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
    exchangeRate: [
      { required: true, message: "汇率不能为空", trigger: "blur" },
    ],
    sortOrder: [{ required: true, message: "序号不能为空", trigger: "blur" }],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询出金类型列表 */
function getList() {
  loading.value = true;
  listWithdrawaltype(queryParams.value).then((response) => {
    withdrawaltypeList.value = response.rows;
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
    type: "0",
    name: null,
    exchangeRate: null,
    sortOrder: null,
    icon: null,
    bankName: null,
    depositType: null,
    branchCode: null,
    branchName: null,
    bankAccount: null,
    accountHolder: null,
    accountName: null,
    walletName: null,
    walletAddress: null,
    remarks: null,
    createTime: null,
  };
  proxy.resetForm("withdrawaltypeRef");
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
  title.value = "添加出金类型";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getWithdrawaltype(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改出金类型";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["withdrawaltypeRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateWithdrawaltype(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addWithdrawaltype(form.value).then((response) => {
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
    .confirm('是否确认删除出金类型编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delWithdrawaltype(_ids);
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
    "member/withdrawaltype/export",
    {
      ...queryParams.value,
    },
    `withdrawaltype_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
