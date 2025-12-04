<template>
  <el-drawer
    title="修改提现账户"
    v-model="visible"
    size="90%"
    with-header
    :destroy-on-close="false"
    :append-to-body="true"
    @close="handleClose"
  >
    <div class="app-container pa12">
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
        <el-form-item label="是否默认" prop="isDefault">
          <el-select
            v-model="queryParams.isDefault"
            placeholder="请选择是否默认"
            style="width: 220px"
            clearable
          >
            <el-option
              v-for="dict in user_yes_no"
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
            v-hasPermi="['member:withdrawalAcc:add']"
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
            v-hasPermi="['member:withdrawalAcc:edit']"
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
            v-hasPermi="['member:withdrawalAcc:remove']"
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
        :data="withdrawalAccList"
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
        <el-table-column
          label="出金类型"
          align="center"
          prop="withdrawalType"
        />
        <el-table-column label="参数" align="center" width="300">
          <template #default="scope">
            <div v-if="scope.row.type === '0'">
              <div>银行名称: {{ scope.row.bankName }}</div>
              <div>存款种类: {{ scope.row.depositType }}</div>
              <div>支行代码: {{ scope.row.branchCode }}</div>
              <div>支行名称: {{ scope.row.branchName }}</div>
              <div>银行账号: {{ scope.row.bankAccount }}</div>
              <div>账户持有人: {{ scope.row.accountHolder }}</div>
            </div>
            <div v-else-if="scope.row.type === '1'">
              <div>账户名称: {{ scope.row.accountName }}</div>
              <div>钱包名称: {{ scope.row.walletName }}</div>
              <div>钱包地址: {{ scope.row.walletAddress }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="是否默认" align="center" prop="isDefault">
          <template #default="scope">
            <dict-tag :options="user_yes_no" :value="scope.row.isDefault" />
          </template>
        </el-table-column>
        <el-table-column
          label="创建时间"
          align="center"
          prop="createTime"
          width="180"
        />
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
              v-hasPermi="['member:withdrawalAcc:edit']"
            ></el-button>
            <el-button
              circle
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['member:withdrawalAcc:remove']"
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

      <!-- 添加或修改提现账户对话框 -->
      <el-drawer :title="title" v-model="open" size="80%" append-to-body>
        <el-form
          ref="withdrawalAccRef"
          :model="form"
          :rules="rules"
          label-position="top"
          label-width="80px"
        >
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="类型" prop="type">
                <el-radio-group v-model="form.type" @change="handleTypeChange">
                  <el-radio
                    v-for="dict in order_zhlx"
                    :key="dict.value"
                    :label="dict.value"
                    >{{ dict.label }}</el-radio
                  >
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="出金类型" prop="withdrawalTypeId">
                <el-radio-group v-model="form.withdrawalTypeId">
                  <el-radio
                    v-for="dict in formWithdrawalTypes"
                    :key="dict.id"
                    :label="dict.id"
                    >{{ dict.name }}</el-radio
                  >
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="是否默认" prop="isDefault">
                <el-radio-group v-model="form.isDefault">
                  <el-radio
                    v-for="dict in user_yes_no"
                    :key="dict.value"
                    :label="dict.value"
                    >{{ dict.label }}</el-radio
                  >
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20" v-if="form.type === '0'">
            <el-col :span="8">
              <el-form-item label="银行名称" prop="bankName">
                <el-input
                  v-model="form.bankName"
                  placeholder="请输入银行名称"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="存款种类" prop="depositType">
                <el-input
                  v-model="form.depositType"
                  placeholder="请输入存款种类"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="支行代码" prop="branchCode">
                <el-input
                  v-model="form.branchCode"
                  placeholder="请输入支行代码"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20" v-if="form.type === '0'">
            <el-col :span="8">
              <el-form-item label="支行名称" prop="branchName">
                <el-input
                  v-model="form.branchName"
                  placeholder="请输入支行名称"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="银行账号" prop="bankAccount">
                <el-input
                  v-model="form.bankAccount"
                  placeholder="请输入银行账号"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="账户持有人" prop="accountHolder">
                <el-input
                  v-model="form.accountHolder"
                  placeholder="请输入账户持有人"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20" v-if="form.type === '1'">
            <el-col :span="24">
              <el-form-item label="账户名称" prop="accountName">
                <el-input
                  v-model="form.accountName"
                  placeholder="请输入账户名称"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20" v-if="form.type === '1'">
            <el-col :span="24">
              <el-form-item label="钱包名称" prop="walletName">
                <el-input
                  v-model="form.walletName"
                  placeholder="请输入钱包名称"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20" v-if="form.type === '1'">
            <el-col :span="24">
              <el-form-item label="钱包地址" prop="walletAddress">
                <el-input
                  v-model="form.walletAddress"
                  placeholder="请输入钱包地址"
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
      </el-drawer>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, toRefs, watch, getCurrentInstance } from "vue";
import {
  listWithdrawalAcc,
  getWithdrawalAcc,
  delWithdrawalAcc,
  addWithdrawalAcc,
  updateWithdrawalAcc,
  getType,
} from "@/api/member/withdrawalAcc";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  userId: {
    type: String,
    default: null,
  },
});

const emit = defineEmits(["update:modelValue", "success"]);

const { proxy } = getCurrentInstance();
const { user_yes_no, order_zhlx } = proxy.useDict("user_yes_no", "order_zhlx");

const visible = ref(props.modelValue);
watch(
  () => props.modelValue,
  (v) => {
    visible.value = v;
  }
);
watch(visible, (v) => {
  emit("update:modelValue", v);
});

const loading = ref(false);
const withdrawalAccList = ref([]);
const total = ref(0);

const queryWithdrawalTypes = ref([]);
const formWithdrawalTypes = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    type: null,
    withdrawalType: null,
    isDefault: null,
    bankName: null,
    depositType: null,
    branchCode: null,
    branchName: null,
    bankAccount: null,
    accountHolder: null,
    accountName: null,
    walletName: null,
    walletAddress: null,
  },
  rules: {
    type: [{ required: true, message: "类型不能为空", trigger: "change" }],
    isDefault: [
      { required: true, message: "是否默认不能为空", trigger: "change" },
    ],
    withdrawalTypeId: [
      { required: true, message: "出金类型不能为空", trigger: "change" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  const params = {
    ...queryParams.value,
    userId: props.userId ?? queryParams.value.userId,
  };
  listWithdrawalAcc(params)
    .then((response) => {
      withdrawalAccList.value = response.rows ?? response.data?.rows ?? [];
      total.value = response.total ?? response.data?.total ?? 0;
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
}

// 类型变化处理
const handleTypeChange = async (value) => {
  // 清空相关字段
  if (value === "0") {
    form.value.withdrawalType = null;
    form.value.accountName = null;
    form.value.walletName = null;
    form.value.walletAddress = null;
  } else if (value === "1") {
    form.value.bankName = null;
    form.value.depositType = null;
    form.value.branchCode = null;
    form.value.branchName = null;
    form.value.bankAccount = null;
    form.value.accountHolder = null;
  }
  // 加载出金类型
  if (value !== null && value !== undefined) {
    try {
      const response = await getType(value);
      const allTypes = (response.data ?? response.rows ?? response) || [];
      const filteredTypes = allTypes.filter((item) => item.type === value);
      formWithdrawalTypes.value = filteredTypes;
    } catch (error) {
      console.error("Failed to fetch withdrawal types:", error);
      formWithdrawalTypes.value = [];
    }
  } else {
    formWithdrawalTypes.value = [];
  }
  // 更新规则
  updateRules();
};

// 更新验证规则
function updateRules() {
  const newRules = {
    type: [{ required: true, message: "类型不能为空", trigger: "change" }],
    isDefault: [
      { required: true, message: "是否默认不能为空", trigger: "change" },
    ],
  };

  if (form.value.type === "0") {
    // 银行卡必填
    newRules.bankName = [
      { required: true, message: "银行名称不能为空", trigger: "blur" },
    ];
    newRules.depositType = [
      { required: true, message: "存款种类不能为空", trigger: "blur" },
    ];
    newRules.branchCode = [
      { required: true, message: "支行代码不能为空", trigger: "blur" },
    ];
    newRules.branchName = [
      { required: true, message: "支行名称不能为空", trigger: "blur" },
    ];
    newRules.bankAccount = [
      { required: true, message: "银行账号不能为空", trigger: "blur" },
    ];
    newRules.accountHolder = [
      { required: true, message: "账户持有人不能为空", trigger: "blur" },
    ];
  } else if (form.value.type === "1") {
    // 网络必填
    newRules.accountName = [
      { required: true, message: "账户名称不能为空", trigger: "blur" },
    ];

    newRules.walletName = [
      { required: true, message: "钱包名称不能为空", trigger: "blur" },
    ];
    newRules.walletAddress = [
      { required: true, message: "钱包地址不能为空", trigger: "blur" },
    ];
  }

  Object.assign(rules.value, newRules);
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 抽屉关闭
function handleClose() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    userId: null,
    type: "0", // 默认选中银行类型
    withdrawalType: null,
    isDefault: "1", // 默认值"1"
    bankName: null,
    depositType: null,
    branchCode: null,
    branchName: null,
    bankAccount: null,
    accountHolder: null,
    accountName: null,
    walletName: null,
    walletAddress: null,
    createTime: null,
  };
  updateRules();
  proxy.resetForm && proxy.resetForm("withdrawalAccRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm && proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const showSearch = ref(true);
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
const open = ref(false);
const title = ref("");
function handleAdd() {
  reset();
  form.value.userId = props.userId;
  open.value = true;
  title.value = "添加提现账户";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getWithdrawalAcc(_id).then((response) => {
    form.value = response.data ?? response;
    updateRules();
    open.value = true;
    title.value = "修改提现账户";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["withdrawalAccRef"].validate((valid) => {
    if (valid) {
      // 确保userId被添加到form中
      form.value.userId = form.value.userId || props.userId;
      if (form.value.id != null) {
        updateWithdrawalAcc(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addWithdrawalAcc(form.value).then(() => {
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
  const _ids = row ? [row.id] : ids.value;
  proxy.$modal
    .confirm('是否确认删除提现账户编号为"' + _ids + '"的数据项？')
    .then(() => delWithdrawalAcc(Array.isArray(_ids) ? _ids.join(",") : _ids))
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
      emit("success");
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/withdrawalAcc/export",
    {
      ...queryParams.value,
    },
    `withdrawalAcc_${new Date().getTime()}.xlsx`
  );
}

watch(
  () => props.userId,
  (id) => {
    if (id != null && visible.value) {
      queryParams.value.userId = id;
      queryParams.value.pageNum = 1;
      getList();
    }
  }
);

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      queryParams.value.userId = props.userId ?? queryParams.value.userId;
      getList();
    } else {
      open.value = false;
    }
  }
);

// 监听查询类型的变化，加载对应的出金类型
watch(
  () => queryParams.value.type,
  async (newVal) => {
    if (newVal !== null && newVal !== undefined) {
      try {
        const response = await getType(newVal);
        const allTypes = (response.data ?? response.rows ?? response) || [];
        const filteredTypes = allTypes.filter((item) => item.type === newVal);
        queryWithdrawalTypes.value = filteredTypes;
        queryParams.value.withdrawalType = null;
      } catch (error) {
        console.error("Failed to fetch query withdrawal types:", error);
        queryWithdrawalTypes.value = [];
      }
    } else {
      queryWithdrawalTypes.value = [];
      queryParams.value.withdrawalType = null;
    }
  }
);

// 监听表单类型的变化，加载对应的出金类型
watch(
  () => form.value.type,
  (newVal) => {
    handleTypeChange(newVal);
  }
);

getList();
</script>

<style scoped>
.pa12 {
  padding: 12px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>
