<template>
  <div class="app-container">
    <!-- <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('member.level.nameZh')" prop="nameZh">
        <el-input
          v-model="queryParams.nameZh"
          :placeholder="$t('member.level.enterNameZh')"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['member:level:add']"
          >{{ $t("member.level.add") }}</el-button
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
          v-hasPermi="['member:level:remove']"
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
      :data="levelList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        :label="$t('member.level.primaryKeyId')"
        align="center"
        prop="id"
      />
      <el-table-column
        :label="$t('member.level.icon')"
        align="center"
        prop="icon"
        width="100"
      >
        <template #default="scope">
          <image-preview :src="scope.row.icon" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('member.level.nameZh')"
        align="center"
        prop="nameZh"
      />
      <el-table-column
        :label="$t('member.level.nameEn')"
        align="center"
        prop="nameEn"
      />
      <el-table-column
        :label="$t('member.level.price')"
        align="center"
        prop="price"
      />
      <el-table-column
        :label="$t('member.level.autoUpgradeInviteCount')"
        align="center"
        prop="autoUpgradeInviteCount"
        width="150px"
      />
      <el-table-column
        :label="$t('member.level.commissionRatio')"
        align="center"
        prop="commissionRatio"
        :formatter="
          (row) =>
            row.commissionRatio != null ? row.commissionRatio + '%' : ''
        "
      />
      <el-table-column
        :label="$t('member.level.streakCommissionRatio')"
        align="center"
        prop="streakCommissionRatio"
        :formatter="
          (row) =>
            row.streakCommissionRatio != null
              ? row.streakCommissionRatio + '%'
              : ''
        "
      />
      <el-table-column
        :label="$t('member.level.minBalance')"
        align="center"
        prop="minBalance"
      />
      <el-table-column
        :label="$t('member.level.orderCount')"
        align="center"
        prop="orderCount"
      />
      <el-table-column
        :label="$t('member.level.withdrawCount')"
        align="center"
        prop="withdrawCount"
      />
      <el-table-column
        :label="$t('member.level.withdrawLimit')"
        align="center"
        prop="withdrawLimit"
      />
      <el-table-column
        :label="$t('member.level.withdrawInterval')"
        align="center"
        prop="minWithdrawAmount"
      >
        <template #default="scope">
          <div>
            {{ scope.row.minWithdrawAmount }} -
            {{ scope.row.maxWithdrawAmount }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('member.level.withdrawFee')"
        align="center"
        prop="withdrawFee"
      />
      <el-table-column
        :label="$t('member.level.withdrawOrderPerDay')"
        align="center"
        prop="withdrawOrderPerDay"
        width="150px"
      />
      <el-table-column
        :label="$t('common.operation')"
        align="center"
        fixed="right"
        width="150px"
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

    <!-- 添加或修改会员等级抽屉 -->
    <el-drawer :title="title" v-model="open" size="50%" append-to-body :with-header="true">
      <el-form
        ref="levelRef"
        :model="form"
        label-position="top"
        :rules="rules"
        label-width="100px"
      >
        <!-- 独占一行：图标 -->
        <el-form-item :label="$t('member.level.icon')" prop="icon">
          <image-upload v-model="form.icon" :limit="1" />
        </el-form-item>

        <!-- 每行两个字段，使用 el-row + el-col -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="$t('member.level.nameZh')" prop="nameZh">
              <el-input
                v-model="form.nameZh"
                :placeholder="$t('member.level.enterNameZh')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('member.level.nameEn')" prop="nameEn">
              <el-input
                v-model="form.nameEn"
                :placeholder="$t('member.level.enterNameEn')"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="$t('member.level.price')" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.autoUpgradeInviteCount')"
              prop="autoUpgradeInviteCount"
            >
              <el-input-number
                v-model="form.autoUpgradeInviteCount"
                :min="0"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.commissionRatio')"
              prop="commissionRatio"
            >
              <el-input-number
                v-model="form.commissionRatio"
                :min="0"
                :step="0.1"
                :max="100"
                style="width: 100%"
              >
                <template #suffix>
                  <span>%</span>
                </template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.streakCommissionRatio')"
              prop="streakCommissionRatio"
            >
              <el-input-number
                v-model="form.streakCommissionRatio"
                :min="0"
                :step="0.1"
                :max="100"
                style="width: 100%"
              >
                <template #suffix>
                  <span>%</span>
                </template>
              </el-input-number>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.minBalance')"
              prop="minBalance"
            >
              <el-input-number
                v-model="form.minBalance"
                :min="0"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.orderCount')"
              prop="orderCount"
            >
              <el-input-number
                v-model="form.orderCount"
                :min="0"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.withdrawCount')"
              prop="withdrawCount"
            >
              <el-input-number
                v-model="form.withdrawCount"
                :min="0"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.withdrawLimit')"
              prop="withdrawLimit"
            >
              <el-input-number
                v-model="form.withdrawLimit"
                :min="0"
                :step="100"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.minWithdrawAmount')"
              prop="minWithdrawAmount"
            >
              <el-input-number
                v-model="form.minWithdrawAmount"
                :min="0"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.maxWithdrawAmount')"
              prop="maxWithdrawAmount"
            >
              <el-input-number
                v-model="form.maxWithdrawAmount"
                :min="0"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.withdrawFee')"
              prop="withdrawFee"
            >
              <el-input-number
                v-model="form.withdrawFee"
                :min="0"
                :step="0.1"
                :max="100"
                style="width: 100%"
              >
               <template #suffix>
                  <span>%</span>
                </template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.level.withdrawOrderPerDay')"
              prop="withdrawOrderPerDay"
            >
              <el-input-number
                v-model="form.withdrawOrderPerDay"
                :min="0"
                :step="1"
                style="width: 100%"
              >

              </el-input-number>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 独占一行：描述，使用 Tabs 展示多语种编辑 -->
        <el-form-item style="width: 100%;">
          <el-tabs v-model="activeTab" type="card" stretch >
            <el-tab-pane :label="$t('member.level.descriptionZh')" name="zh">
              <el-form-item :label="$t('member.level.descriptionZh')" >
                <editor v-model="form.descriptionZh" :min-height="200" style="width: 100%;" />
              </el-form-item>
            </el-tab-pane>
            <el-tab-pane :label="$t('member.level.descriptionEn')" name="en">
              <el-form-item :label="$t('member.level.descriptionEn')">
                <editor v-model="form.descriptionEn" :min-height="200" style="width: 100%;" />
              </el-form-item>
            </el-tab-pane>
            <el-tab-pane :label="$t('member.level.descriptionJa')" name="ja">
              <el-form-item :label="$t('member.level.descriptionJa')">
                <editor v-model="form.descriptionJa" :min-height="200" style="width: 100%;" />
              </el-form-item>
            </el-tab-pane>
            <el-tab-pane :label="$t('member.level.descriptionTh')" name="th">
              <el-form-item :label="$t('member.level.descriptionTh')">
                <editor v-model="form.descriptionTh" :min-height="200" style="width: 100%;" />
              </el-form-item>
            </el-tab-pane>
            <el-tab-pane :label="$t('member.level.descriptionKo')" name="ko">
              <el-form-item :label="$t('member.level.descriptionKo')">
                <editor v-model="form.descriptionKo" :min-height="200" style="width: 100%;" />
              </el-form-item>
            </el-tab-pane>
            <el-tab-pane :label="$t('member.level.descriptionZhTw')" name="zhTw">
              <el-form-item :label="$t('member.level.descriptionZhTw')">
                <editor v-model="form.descriptionZhTw" :min-height="200" style="width: 100%;" />
              </el-form-item>
            </el-tab-pane>
            <el-tab-pane :label="$t('member.level.descriptionPor')" name="por">
              <el-form-item :label="$t('member.level.descriptionPor')">
                <editor v-model="form.descriptionPor" :min-height="200" style="width: 100%;" />
              </el-form-item>
            </el-tab-pane>
            <el-tab-pane :label="$t('member.level.descriptionEs')" name="es">
              <el-form-item :label="$t('member.level.descriptionEs')">
                <editor v-model="form.descriptionEs" :min-height="200" style="width: 100%;" />
              </el-form-item>
            </el-tab-pane>
          </el-tabs>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ $t('common.confirm') }}</el-button>
          <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="Level">
import { reactive, toRefs } from "vue";
import { useI18n } from "vue-i18n";
import {
  listLevel,
  getLevel,
  delLevel,
  addLevel,
  updateLevel,
} from "@/api/member/level";

const { t } = useI18n();

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
const activeTab = ref("zh");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    icon: null,
    nameZh: null,
    nameEn: null,
    price: null,
    autoUpgradeInviteCount: null,
    commissionRatio: null,
    streakCommissionRatio: null,
    minBalance: null,
    orderCount: null,
    withdrawCount: null,
    withdrawLimit: null,
    minWithdrawAmount: null,
    maxWithdrawAmount: null,
    withdrawFee: null,
    withdrawOrderPerDay: null,
    descriptionZh: null,
    descriptionEn: null,
  },
  rules: {
    icon: [
      {
        required: true,
        message: t("member.level.iconRequired"),
        trigger: "blur",
      },
    ],
    nameZh: [
      {
        required: true,
        message: t("member.level.nameZhRequired"),
        trigger: "blur",
      },
    ],
    nameEn: [
      {
        required: true,
        message: t("member.level.nameEnRequired"),
        trigger: "blur",
      },
    ],
    price: [
      {
        required: true,
        message: t("member.level.priceRequired"),
        trigger: "blur",
      },
    ],
    autoUpgradeInviteCount: [
      {
        required: true,
        message: t("member.level.autoUpgradeInviteCountRequired"),
        trigger: "blur",
      },
    ],
    commissionRatio: [
      {
        required: true,
        message: t("member.level.commissionRatioRequired"),
        trigger: "blur",
      },
    ],
    streakCommissionRatio: [
      {
        required: true,
        message: t("member.level.streakCommissionRatioRequired"),
        trigger: "blur",
      },
    ],
    minBalance: [
      {
        required: true,
        message: t("member.level.minBalanceRequired"),
        trigger: "blur",
      },
    ],
    orderCount: [
      {
        required: true,
        message: t("member.level.orderCountRequired"),
        trigger: "blur",
      },
    ],
    withdrawCount: [
      {
        required: true,
        message: t("member.level.withdrawCountRequired"),
        trigger: "blur",
      },
    ],
    withdrawLimit: [
      {
        required: true,
        message: t("member.level.withdrawLimitRequired"),
        trigger: "blur",
      },
    ],
    minWithdrawAmount: [
      {
        required: true,
        message: t("member.level.minWithdrawAmountRequired"),
        trigger: "blur",
      },
    ],
    maxWithdrawAmount: [
      {
        required: true,
        message: t("member.level.maxWithdrawAmountRequired"),
        trigger: "blur",
      },
    ],
    withdrawFee: [
      {
        required: true,
        message: t("member.level.withdrawFeeRequired"),
        trigger: "blur",
      },
    ],
    withdrawOrderPerDay: [
      {
        required: true,
        message: t("member.level.withdrawOrderPerDayRequired"),
        trigger: "blur",
      },
    ],
    descriptionZh: [
      {
        required: true,
        message: t("member.level.descriptionZhRequired"),
        trigger: "blur",
      },
    ],
    descriptionEn: [
      {
        required: true,
        message: t("member.level.descriptionEnRequired"),
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询会员等级列表 */
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
    icon: null,
    nameZh: null,
    nameEn: null,
    price: null,
    autoUpgradeInviteCount: null,
    commissionRatio: null,
    streakCommissionRatio: null,
    minBalance: null,
    orderCount: null,
    withdrawCount: null,
    withdrawLimit: null,
    minWithdrawAmount: null,
    maxWithdrawAmount: null,
    withdrawFee: null,
    withdrawOrderPerDay: null,
    descriptionZh: null,
    descriptionEn: null,
    descriptionJa: null,
    descriptionTh: null,
    descriptionKo: null,
    descriptionZhTw: null,
    descriptionPor: null,
    descriptionEs: null,
    createTime: null,
  };
  proxy.resetForm("levelRef");
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
  // 新增时默认打开中文标签（可根据需要改为其它语言）
  activeTab.value = "zh";
  open.value = true;
  title.value = t("member.level.addLevel");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getLevel(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("member.level.editLevel");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["levelRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateLevel(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.level.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addLevel(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.level.addSuccess"));
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
    .confirm(t("member.level.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delLevel(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("member.level.deleteSuccess"));
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
    `${t("member.level.exportFileName")}_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
