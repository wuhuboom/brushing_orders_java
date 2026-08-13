<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" class="withdrawal-tabs mb8">
      <el-tab-pane :label="$t('member.withdrawal.realWithdrawal')" name="real" />
      <el-tab-pane :label="$t('member.withdrawal.fakeWithdrawal')" name="fake" />
    </el-tabs>

    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('member.withdrawal.code')" prop="code">
        <el-input
          v-model="queryParams.code"
          :placeholder="$t('member.withdrawal.enterCode')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.withdrawal.username')" prop="username">
        <el-input
          v-model="queryParams.username"
          :placeholder="$t('member.withdrawal.enterUsername')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.withdrawal.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          style="width: 180px"
          :placeholder="$t('member.withdrawal.selectStatus')"
          clearable
        >
          <el-option
            v-for="dict in withdraw_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('member.withdrawal.applicationTime')" prop="applicationTimeRange">
        <el-date-picker
          v-model="queryParams.applicationTimeRange"
          type="daterange"
          range-separator="-"
          value-format="YYYY-MM-DD"
          style="width: 320px"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('member.withdrawal.auditTime')" prop="auditTimeRange">
        <el-date-picker
          v-model="queryParams.auditTimeRange"
          type="daterange"
          range-separator="-"
          value-format="YYYY-MM-DD"
          style="width: 320px"
          clearable
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
      <right-toolbar
        v-model:showSearch="showSearch"
        :columns="toolbarColumns"
        @queryTable="getList"
        @update:columns="handleColumnsUpdate"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="withdrawalList"
      @selection-change="handleSelectionChange"
      :border="true"
      :key="tableKey"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        v-for="column in filteredColumns"
        :key="column.key"
        :label="column.label"
        :prop="column.prop"
        :width="column.width"
        :fixed="column.fixed"
        align="left"
      >

        <template v-if="column.key === '10'" #default="scope">
          <div v-if="scope.row.walletId">
            <div v-if="scope.row.bankWallet.type == '1'">
              <div>
                {{ $t("member.withdrawal.bankCode") }}：
                {{ scope.row.bankWallet.bankCode }}
              </div>
              <div>
                {{ $t("member.withdrawal.name") }}：
                {{ scope.row.bankWallet.name }}
              </div>
              <div>
                {{ $t("member.withdrawal.bankCardNumber") }}：
                {{ scope.row.bankWallet.bankCard }}
              </div>
              <div>
                {{ $t("member.withdrawal.accountType") }}：
                {{ scope.row.bankWallet.bankType }}
              </div>
            </div>
            <div v-else>
              <div>
                {{ $t("member.withdrawal.name") }}：{{
                  scope.row.bankWallet.name
                }}
              </div>
              <div>
                {{ $t("member.withdrawal.walletType") }}：{{
                  scope.row.bankWallet.walletType
                }}
              </div>
              <div>
                {{ $t("member.withdrawal.address") }}：{{
                  scope.row.bankWallet.walletAddress
                }}
              </div>
            </div>
          </div>
          <div v-else>
            <div>
              {{ $t("member.withdrawal.name") }}：{{ scope.row.withdrawName }}
            </div>
            <div>
              {{ $t("member.withdrawal.walletType") }}：{{
                scope.row.withdrawType
              }}
            </div>
            <div>
              {{ $t("member.withdrawal.address") }}：{{
                scope.row.withdrawAddress
              }}
            </div>
          </div>
        </template>
        <template
          v-if="column.key === '15' || column.key === '16'"
          #default="scope"
        >
          <span>{{
            parseTime(scope.row[column.prop], "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
        <template v-if="column.key === '18'" #default="scope">
          <div style="display: flex; justify-content: space-around">
            <dict-tag :options="withdraw_status" :value="scope.row.status" />
            <el-tag
              v-if="scope.row.status == '1'"
              type="success"
              v-hasPermi="['member:withdrawal:edit']"
              @click="handleUpdate(scope.row)"
            >
              {{ $t("member.withdrawal.approve") }}
            </el-tag>
            <el-tag
              type="danger"
              v-if="scope.row.status == '1'"
              @click="handleUpdateF(scope.row)"
              v-hasPermi="['member:withdrawal:edit']"
            >
              {{ $t("member.withdrawal.reject") }}
            </el-tag>
          </div>
        </template>
         <template v-if="column.key === '21'" #default="{ row }">
          <div>
            <div v-for="(ip, index) in row.ipAddress?.split(',')" :key="index">
              {{ ip }}
            </div>
          </div>
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

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form
        ref="withdrawalRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        label-position="top"
      >
        <el-form-item :label="$t('member.withdrawal.remark')" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="$t('member.withdrawal.enterRemark')"
          />
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

<script setup name="Withdrawal">
import {
  listWithdrawal,
  getWithdrawal,
  delWithdrawal,
  addWithdrawal,
  updateWithdrawal,
} from "@/api/member/withdrawal";
import { getPhoneFieldSet } from "@/api/member/member";
import {
  ref,
  reactive,
  toRefs,
  computed,
  onMounted,
  onUnmounted,
  watch,
  nextTick,
} from "vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

const { proxy } = getCurrentInstance();
const { withdraw_status, sys_yes_no } = proxy.useDict(
  "withdraw_status",
  "sys_yes_no"
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
const tableKey = ref(Date.now());
const showPhone = ref(null);
const activeTab = ref("real");

let intervalId = null;
let isPaused = false;

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    code: null,
    username: null,
    amount: null,
    creditedAmount: null,
    fee: null,
    applicationTime: null,
    auditTime: null,
    applicationTimeRange: null,
    auditTimeRange: null,
    isReal: "N",
    status: null,
  },
  rules: {
    code: [
      {
        required: true,
        message: t("member.withdrawal.codeRequired"),
        trigger: "blur",
      },
    ],
    amount: [
      {
        required: true,
        message: t("member.withdrawal.amountRequired"),
        trigger: "blur",
      },
    ],
    creditedAmount: [
      {
        required: true,
        message: t("member.withdrawal.creditedAmountRequired"),
        trigger: "blur",
      },
    ],
    fee: [
      {
        required: true,
        message: t("member.withdrawal.feeRequired"),
        trigger: "blur",
      },
    ],
    status: [
      {
        required: true,
        message: t("member.withdrawal.statusRequired"),
        trigger: "change",
      },
    ],
  },
});

const columns = ref([
  {
    key: "0",
    label: t("member.withdrawal.userId"),
    visible: true,
    prop: "id",
    width: "80",
  },
  {
    key: "1",
    label: t("member.withdrawal.withdrawalCode"),
    visible: true,
    prop: "code",
    width: "200",
  },
  {
    key: "2",
    label: t("member.withdrawal.userId"),
    visible: true,
    prop: "userId",
    width: "80",
  },
  {
    key: "3",
    label: t("member.withdrawal.username"),
    visible: true,
    prop: "username",
    width: "160",
  },
  {
    key: "5",
    label: t("member.withdrawal.phone"),
    visible: true,
    prop: "phone",
    width: "100",
  },
  {
    key: "6",
    label: t("member.withdrawal.userRemark"),
    visible: true,
    prop: "userRemark",
    width: "180",
  },
  {
    key: "7",
    label: t("member.withdrawal.parentUsername"),
    visible: true,
    prop: "parentUsername",
    width: "160",
  },
  {
    key: "9",
    label: t("member.withdrawal.dailyOrderCount"),
    visible: true,
    prop: "dailyOrderCount",
    width: "120",
  },
   {
    key: "21",
    labelKey: "member.index.ip",
    label: t("member.index.ip"),
    visible: true,
    prop: null,
    width: "180",
  },
  {
    key: "10",
    label: t("member.withdrawal.withdrawalAddress"),
    visible: true,
    prop: null,
    width: "200",
  },
  {
    key: "11",
    label: t("member.withdrawal.amount"),
    visible: true,
    prop: "amount",
    width: "180",
  },
  {
    key: "12",
    label: t("member.withdrawal.creditedAmount"),
    visible: true,
    prop: "creditedAmount",
    width: "180",
  },
  {
    key: "13",
    label: t("member.withdrawal.fee"),
    visible: true,
    prop: "fee",
    width: "180",
  },
  {
    key: "14",
    label: t("member.withdrawal.dailyWithdrawalCount"),
    visible: true,
    prop: "dailyWithdrawalCount",
    width: "120",
  },
  {
    key: "15",
    label: t("member.withdrawal.applicationTime"),
    visible: true,
    prop: "applicationTime",
    width: "160",
  },
  {
    key: "16",
    label: t("member.withdrawal.auditTime"),
    visible: true,
    prop: "auditTime",
    width: "160",
  },
  {
    key: "17",
    label: t("member.withdrawal.remark"),
    visible: true,
    prop: "remark",
    width: "180",
  },
  {
    key: "18",
    label: t("member.withdrawal.auditStatus"),
    visible: true,
    prop: null,
    width: "260",
    fixed: "right",
  },
]);

const { queryParams, form, rules } = toRefs(data);

const filteredColumns = computed(() => {
  return columns.value.filter((column) => column.visible);
});

const toolbarColumns = computed(() => {
  const sp =
    showPhone && typeof showPhone === "object" ? showPhone.value : showPhone;
  if (sp === undefined || sp === null) {
    return columns.value;
  }
  if (sp === false) {
    return columns.value.filter((col) => !(col.prop === "phone" || col.key === "5"));
  }
  return columns.value;
});

/** 更新列顺序 */
function handleColumnsUpdate(newColumns) {
  console.log("Received newColumns:", newColumns);
  // 验证并过滤无效列
  const validColumns = newColumns.filter(
    (column) =>
      column &&
      typeof column === "object" &&
      Object.prototype.hasOwnProperty.call(column, "visible") &&
      Object.prototype.hasOwnProperty.call(column, "key")
  );
  if (validColumns.length !== newColumns.length) {
    console.warn(
      "Filtered out invalid columns:",
      newColumns.filter(
        (column) =>
          !column ||
          typeof column !== "object" ||
          !Object.prototype.hasOwnProperty.call(column, "visible") ||
          !Object.prototype.hasOwnProperty.call(column, "key")
      )
    );
  }
  columns.value = validColumns;
  tableKey.value = Date.now(); // 强制表格重新渲染
}

/** 检查是否第一页 */
const isFirstPage = computed(() => {
  return queryParams.value.pageNum === 1;
});

/** 检查是否无筛选数据 */
const isNoFilter = computed(() => {
  return (
    !queryParams.value.code &&
    !queryParams.value.username &&
    !queryParams.value.status &&
    (!queryParams.value.applicationTimeRange || queryParams.value.applicationTimeRange.length === 0) &&
    (!queryParams.value.auditTimeRange || queryParams.value.auditTimeRange.length === 0)
  );
});

/** 轮询条件：真人 tab、第一页且无筛选 */
const shouldPoll = computed(() => {
  return activeTab.value === "real" && isFirstPage.value && isNoFilter.value;
});

/** 启动定时任务 */
function startPolling() {
  if (intervalId) return; // 已存在则不重复启动
  if (!shouldPoll.value) return; // 不满足条件不启动
  intervalId = setInterval(() => {
    pollList();
  }, 3000);
  console.log("Polling started");
}

/** 停止定时任务 */
function stopPolling() {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
    console.log("Polling stopped");
  }
}

/** 窗口可见性变化处理 */
function handleVisibilityChange() {
  if (document.hidden) {
    // 窗口隐藏，暂停
    if (intervalId && !isPaused) {
      stopPolling();
      isPaused = true;
    }
  } else {
    // 窗口可见，继续
    if (isPaused && shouldPoll.value) {
      startPolling();
      isPaused = false;
    }
  }
}

/** 手动查询提现记录列表（带 loading，总是更新数据） */
function getList() {
  loading.value = true;
  // 使用 addDateRange 以匹配后端参数结构（params[...]）
  let params = proxy.addDateRange(queryParams.value, queryParams.value.applicationTimeRange);
  // 如果 addDateRange 创建了 beginTime/endTime 但没有实际选择日期，移除这些空参数
  if (params && params.params) {
    if (!params.params.beginTime) {
      delete params.params.beginTime;
      delete params.params.endTime;
    }
  }

  // 添加审核时间区间参数（只有在选择了区间时才传）
  if (
    queryParams.value.auditTimeRange &&
    queryParams.value.auditTimeRange.length === 2 &&
    queryParams.value.auditTimeRange[0] &&
    queryParams.value.auditTimeRange[1]
  ) {
    params.params = params.params || {};
    params.params.auditBeginTime = queryParams.value.auditTimeRange[0];
    params.params.auditEndTime = queryParams.value.auditTimeRange[1];
  } else if (params && params.params) {
    delete params.params.auditBeginTime;
    delete params.params.auditEndTime;
  }

  console.log("Calling listWithdrawal with params:", params);
  listWithdrawal(params)
    .then((response) => {
      console.log(response);
      withdrawalList.value = response.rows;
      total.value = response.total;
      loading.value = false;
      console.log("Table data:", response);
    })
    .catch((err) => {
      console.error("listWithdrawal error:", err, "params:", params);
      loading.value = false;
    });
}

/** 轮询查询提现记录列表（无 loading，仅变化时更新） */
function pollList() {
  // 轮询时使用 addDateRange 构造与 Topup 相同的参数结构
  let params = proxy.addDateRange(queryParams.value, queryParams.value.applicationTimeRange);
  if (
    queryParams.value.auditTimeRange &&
    queryParams.value.auditTimeRange.length === 2 &&
    queryParams.value.auditTimeRange[0] &&
    queryParams.value.auditTimeRange[1]
  ) {
    params.params = params.params || {};
    params.params.auditBeginTime = queryParams.value.auditTimeRange[0];
    params.params.auditEndTime = queryParams.value.auditTimeRange[1];
  }

  console.log("Polling listWithdrawal with params:", params);
  listWithdrawal(params)
    .then((response) => {
      const newRows = response.rows;
      const newTotal = response.total;
      // 使用 Set 比较 ID 集合（忽略顺序）
      const currentIdsSet = new Set(withdrawalList.value.map((row) => row.id));
      const newIdsSet = new Set(newRows.map((row) => row.id));
      const isDataChanged =
        newIdsSet.size !== currentIdsSet.size ||
        ![...newIdsSet].every((id) => currentIdsSet.has(id)) ||
        newTotal !== total.value;
      if (isDataChanged) {
        withdrawalList.value = newRows;
        total.value = newTotal;
        tableKey.value = Date.now(); // 仅变化时强制重绘
      } else {
      }
    })
    .catch((error) => {
      console.error("Polling error:", error);
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
    code: null,
    amount: null,
    creditedAmount: null,
    fee: null,
    applicationTime: null,
    auditTime: null,
    remark: null,
    status: null,
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
  queryParams.value.applicationTimeRange = [];
  queryParams.value.auditTimeRange = [];
  proxy.resetForm("queryRef");
  queryParams.value.isReal = activeTab.value === "real" ? "N" : "Y";
  handleQuery();
}

watch(activeTab, (name) => {
  queryParams.value.isReal = name === "real" ? "N" : "Y";
  queryParams.value.pageNum = 1;
  getList();
});

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
  title.value = t("member.withdrawal.addWithdrawalRecord");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getWithdrawal(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    form.value.status = "0";
    title.value = t("member.withdrawal.approveAudit");
  });
}
function handleUpdateF(row) {
  reset();
  const _id = row.id || ids.value;
  getWithdrawal(_id).then((response) => {
    form.value = response.data;
    form.value.status = "2";
    open.value = true;
    title.value = t("member.withdrawal.rejectAudit");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["withdrawalRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateWithdrawal(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.withdrawal.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addWithdrawal(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.withdrawal.addSuccess"));
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
    .confirm(t("member.withdrawal.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delWithdrawal(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("member.withdrawal.deleteSuccess"));
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
    `${t("member.withdrawal.exportFileName")}_${new Date().getTime()}.xlsx`
  );
}

onMounted(() => {
  getPhoneFieldSet()
    .then((res) => {
      const val = res && res.data;
      const str = val === null || val === undefined ? "" : String(val).toLowerCase();
      const visible =
        val === true || val === 1 || ["true", "1", "y", "yes"].includes(str);
      showPhone.value = visible;
      const phoneCol = columns.value.find((c) => c.prop === "phone" || c.key === "5");
      if (phoneCol) {
        phoneCol.visible = visible;
      }
    })
    .catch(() => {});

  getList();
  if (shouldPoll.value) {
    startPolling();
  }
  document.addEventListener("visibilitychange", handleVisibilityChange);
  // 监听条件变化，自动启动/停止轮询
  watch(shouldPoll, (newVal) => {
    if (newVal) {
      startPolling();
    } else {
      stopPolling();
    }
  });
});

onUnmounted(() => {
  stopPolling();
  document.removeEventListener("visibilitychange", handleVisibilityChange);
});
</script>

<style scoped>
.withdrawal-tabs :deep(.el-tabs__content) {
  display: none;
}
</style>
