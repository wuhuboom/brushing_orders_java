<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="级别" prop="level">
        <el-input
          v-model="queryParams.level"
          placeholder="请输入级别"
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
          v-hasPermi="['member:level:add']"
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
          v-hasPermi="['member:level:edit']"
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
          v-hasPermi="['member:level:remove']"
          >删除</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['member:level:export']"
          >导出</el-button
        >
      </el-col>
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="levelList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="名称" align="center" prop="name" />
      <el-table-column label="级别" align="center" prop="level" />
      <el-table-column label="图标" align="center" prop="icon" width="100">
        <template #default="scope">
          <image-preview :src="scope.row.icon" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column label="价格" align="center" prop="price" />
      <el-table-column label="产品匹配" align="center">
        <template #default="scope">
          <div v-if="scope.row.productMatchEnabled == '1'">
            <el-button
              type="danger"
              link
              @click="openProductMatchDialog(scope.row)"
              >禁用</el-button
            >
          </div>
          <div v-else>
            <el-button
              type="success"
              link
              @click="openProductMatchDialog(scope.row)"
            >
              启用 : {{ scope.row.productMatchMin }}% -
              {{ scope.row.productMatchMax }}%
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="最低余额" align="center" prop="minBalance" />
      <el-table-column
        label="自动升级所需邀请人数"
        align="center"
        prop="inviteCount"
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
            v-hasPermi="['member:level:edit']"
          ></el-button>
          <el-button
            circle
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:level:remove']"
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

    <!-- 添加或修改等级对话框 -->
    <el-dialog :title="title" v-model="open" width="40%" append-to-body>
      <el-form
        ref="levelRef"
        :model="form"
        :rules="rules"
        label-position="top"
        label-width="80px"
      >
        <!-- 第一行 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="级别" prop="level">
              <el-input-number
                controls-position="right"
                v-model="form.level"
                :min="1"
                placeholder="请输入级别"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="图标" prop="icon">
              <image-upload v-model="form.icon" :limit="1" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第二行 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="会员等级价格" prop="price">
              <el-input-number
                v-model="form.price"
                controls-position="right"
                style="width: 100%"
                :min="0"
                placeholder="请输入会员等级价格"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最低余额" prop="minBalance">
              <el-input-number
                v-model="form.minBalance"
                controls-position="right"
                style="width: 100%"
                :min="0"
                placeholder="请输入最低余额"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="自动升级所需邀请人数" prop="inviteCount">
              <el-input-number
                v-model="form.inviteCount"
                style="width: 100%"
                controls-position="right"
                :min="0"
                placeholder="请输入自动升级所需邀请人数"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第三行 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="接单次数/天" prop="orderCountPerDay">
              <el-input-number
                v-model="form.orderCountPerDay"
                style="width: 100%"
                controls-position="right"
                :min="0"
                placeholder="请输入接单次数/天"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最低返佣百分比" prop="minCommissionRate">
              <el-input-number
                v-model="form.minCommissionRate"
                style="width: 100%"
                controls-position="right"
                :min="0"
                :max="100"
                placeholder="请输入最低返佣百分比"
              >
                <template #suffix>
                  <span>%</span>
                </template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最高返佣百分比" prop="maxCommissionRate">
              <el-input-number
                v-model="form.maxCommissionRate"
                controls-position="right"
                style="width: 100%"
                :min="0"
                :max="100"
                placeholder="请输入最高返佣百分比"
              >
                <template #suffix>
                  <span>%</span>
                </template>
              </el-input-number>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第四行 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item
              label="最低连单返佣百分比"
              prop="minContinuousCommissionRate"
            >
              <el-input-number
                v-model="form.minContinuousCommissionRate"
                controls-position="right"
                style="width: 100%"
                :min="0"
                :max="100"
                placeholder="请输入最低连单返佣百分比"
              >
                <template #suffix>
                  <span>%</span>
                </template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item
              label="最高连单返佣百分比"
              prop="maxContinuousCommissionRate"
            >
              <el-input-number
                v-model="form.maxContinuousCommissionRate"
                style="width: 100%"
                controls-position="right"
                :min="0"
                :max="100"
                placeholder="请输入最高连单返佣百分比"
              >
                <template #suffix>
                  <span>%</span>
                </template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="任务完成组数/天" prop="taskCountPerDay">
              <el-input-number
                v-model="form.taskCountPerDay"
                style="width: 100%"
                controls-position="right"
                :min="0"
                placeholder="请输入任务完成组数/天"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第五行 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="提现次数/天" prop="withdrawCountPerDay">
              <el-input-number
                v-model="form.withdrawCountPerDay"
                controls-position="right"
                style="width: 100%"
                :min="0"
                placeholder="请输入提现次数/天"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="提现手续费率" prop="withdrawFeeRate">
              <el-input-number
                v-model="form.withdrawFeeRate"
                controls-position="right"
                style="width: 100%"
                :min="0"
                :max="100"
                placeholder="请输入提现手续费率"
              >
                <template #suffix>
                  <span>%</span>
                </template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="提现最低单数" prop="minWithdrawAmount">
              <el-input-number
                v-model="form.minWithdrawAmount"
                :min="0"
                controls-position="right"
                style="width: 100%"
                placeholder="请输入提现最低单数"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第六行 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="提现限额/天" prop="withdrawLimitPerDay">
              <el-input-number
                v-model="form.withdrawLimitPerDay"
                :min="0"
                controls-position="right"
                style="width: 100%"
                placeholder="请输入提现限额/天"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最低提现金额" prop="minWithdraw">
              <el-input-number
                v-model="form.minWithdraw"
                :min="0"
                controls-position="right"
                style="width: 100%"
                placeholder="请输入最低提现金额"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最高提现金额" prop="maxWithdraw">
              <el-input-number
                v-model="form.maxWithdraw"
                :min="0"
                controls-position="right"
                style="width: 100%"
                placeholder="请输入最高提现金额"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 描述 -->
        <el-form-item label="描述">
          <editor v-model="form.description" :min-height="192" />
        </el-form-item>
      </el-form>

      <!-- 底部按钮 -->
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      :title="productMatchTitle"
      v-model="productMatchDialogVisible"
      width="500"
      append-to-body
    >
      <el-form
        :model="productMatchForm"
        ref="productMatchForRef"
        label-position="top"
        label-width="80px"
      >
        <!-- 单选框：启用/禁用 -->
        <el-form-item label="产品匹配" prop="productMatchEnabled">
          <el-radio-group v-model="productMatchForm.productMatchEnabled">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 最小匹配值 -->
        <el-form-item label="最小匹配值" prop="productMatchMin">
          <el-input-number
            v-model="productMatchForm.productMatchMin"
            :min="0"
            :max="100"
            controls-position="right"
            placeholder="请输入最小匹配值"
            style="width: 100%"
          >
            <template #suffix>
              <span>%</span>
            </template>
          </el-input-number>
        </el-form-item>

        <!-- 最大匹配值 -->
        <el-form-item label="最大匹配值" prop="productMatchMax">
          <el-input-number
            v-model="productMatchForm.productMatchMax"
            :min="0"
            :max="100"
            controls-position="right"
            placeholder="请输入最大匹配值"
            style="width: 100%"
          >
            <template #suffix>
              <span>%</span>
            </template>
          </el-input-number>
        </el-form-item>
      </el-form>

      <!-- 底部按钮 -->
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="updateProductMatch"
            >确 定</el-button
          >
          <el-button @click="closeProductMatchDialog">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Level">
import {
  listLevel,
  getLevel,
  delLevel,
  addLevel,
  updateLevel,
} from "@/api/member/level";

const { proxy } = getCurrentInstance();

const levelList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const productMatchDialogVisible = ref(false);
const productMatchTitle = ref("");

const data = reactive({
  form: {},
  productMatchForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    level: null,
  },
  rules: {
    name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
    level: [{ required: true, message: "级别不能为空", trigger: "blur" }],
    icon: [{ required: true, message: "图标不能为空", trigger: "blur" }],
    price: [
      { required: true, message: "会员等级价格不能为空", trigger: "blur" },
    ],
    minBalance: [
      { required: true, message: "最低余额不能为空", trigger: "blur" },
    ],
    inviteCount: [
      {
        required: true,
        message: "自动升级所需邀请人数不能为空",
        trigger: "blur",
      },
    ],
    orderCountPerDay: [
      { required: true, message: "接单次数/天不能为空", trigger: "blur" },
    ],
    minCommissionRate: [
      { required: true, message: "最低返佣百分比不能为空", trigger: "blur" },
    ],
    maxCommissionRate: [
      { required: true, message: "最高返佣百分比不能为空", trigger: "blur" },
    ],
    minContinuousCommissionRate: [
      {
        required: true,
        message: "最低连单返佣百分比不能为空",
        trigger: "blur",
      },
    ],
    maxContinuousCommissionRate: [
      {
        required: true,
        message: "最高连单返佣百分比不能为空",
        trigger: "blur",
      },
    ],
    taskCountPerDay: [
      { required: true, message: "任务完成组数/天不能为空", trigger: "blur" },
    ],
    withdrawCountPerDay: [
      { required: true, message: "提现次数/天不能为空", trigger: "blur" },
    ],
    withdrawFeeRate: [
      { required: true, message: "提现手续费率不能为空", trigger: "blur" },
    ],
    minWithdrawAmount: [
      { required: true, message: "提现最低单数不能为空", trigger: "blur" },
    ],
    withdrawLimitPerDay: [
      { required: true, message: "提现限额/天不能为空", trigger: "blur" },
    ],
    minWithdraw: [
      { required: true, message: "最低提现金额不能为空", trigger: "blur" },
    ],
    maxWithdraw: [
      { required: true, message: "最高提现金额不能为空", trigger: "blur" },
    ],
    description: [{ required: true, message: "描述不能为空", trigger: "blur" }],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules, productMatchForm } = toRefs(data);

function openProductMatchDialog(row) {
  resetMatch();
  const _id = row.id || ids.value;
  getLevel(_id).then((response) => {
    productMatchForm.value = response.data;
    console.log(productMatchForm.value);
    productMatchTitle.value = "产品匹配";
    productMatchDialogVisible.value = true;
  });
}

function closeProductMatchDialog() {
  productMatchDialogVisible.value = false;
}

function updateProductMatch() {
  // 校验表单
  if (
    productMatchForm.productMatchMin === null ||
    productMatchForm.productMatchMax === null
  ) {
    return;
  }

  const params = {
    id: productMatchForm.value.id,
    productMatchMin: productMatchForm.value.productMatchMin,
    productMatchMax: productMatchForm.value.productMatchMax,
    productMatchEnabled: productMatchForm.value.productMatchEnabled,
  };

  updateLevel(params).then((response) => {
    productMatchDialogVisible.value = false;
    getList(); // 更新列表
    proxy.$modal.msgSuccess("修改成功");
  });
}

/** 查询等级列表 */
function getList() {
  loading.value = true;
  listLevel(queryParams.value).then((response) => {
    levelList.value = response.rows;
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
    level: null,
    icon: null,
    price: null,
    minBalance: null,
    inviteCount: null,
    orderCountPerDay: null,
    minCommissionRate: null,
    maxCommissionRate: null,
    minContinuousCommissionRate: null,
    maxContinuousCommissionRate: null,
    taskCountPerDay: null,
    withdrawCountPerDay: null,
    withdrawFeeRate: null,
    minWithdrawAmount: null,
    withdrawLimitPerDay: null,
    minWithdraw: null,
    maxWithdraw: null,
    description: null,
    createTime: null,
  };
  proxy.resetForm("levelRef");
}

function resetMatch() {
  form.value = {
    id: null,
    productMatchMin: null,
    productMatchMax: null,
    productMatchEnabled: null,
  };
  proxy.resetForm("productMatchForRef");
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
  title.value = "添加等级";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getLevel(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改等级";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["levelRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateLevel(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addLevel(form.value).then((response) => {
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
    .confirm('是否确认删除等级编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delLevel(_ids);
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
    "member/level/export",
    {
      ...queryParams.value,
    },
    `level_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
