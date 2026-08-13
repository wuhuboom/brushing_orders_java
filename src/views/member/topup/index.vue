<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="100px"
    >
      <el-form-item :label="$t('member.topup.username')" prop="username">
        <el-input
          v-model="queryParams.username"
          :placeholder="$t('member.topup.enterUsername')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.topup.code')" prop="code">
        <el-input
          v-model="queryParams.code"
          :placeholder="$t('member.topup.enterCode')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.topup.agentUsername')" prop="agentUsername">
        <el-input
          v-model="queryParams.agentUsername"
          :placeholder="$t('member.topup.enterAgentUsername')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.index.isFake')" prop="isReal">
        <el-select
            v-model="queryParams.isReal"
            :placeholder="$t('member.index.isFakePlaceholder')"
            clearable
            style="width: 240px"
        >
          <el-option
              v-for="dict in sys_yes_no"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('member.topup.type')" prop="isReal">
        <el-select
            v-model="queryParams.type"
            :placeholder="$t('member.topup.type')"
            clearable
            style="width: 240px"
        >
          <el-option
              v-for="dict in reTypes"
              :key="dict.value"
              :label="dict.name"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="$t('member.topup.rechargeTime')"
        style="width: 400px"
      >
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
        ></el-date-picker>
      </el-form-item>
      <el-form-item
        :label="$t('member.topup.auditTimeRange')"
        style="width: 400px"
      >
        <el-date-picker
          v-model="auditDateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
        ></el-date-picker>
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
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="topupList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        :label="$t('member.topup.id')"
        align="center"
        prop="id"
      />
      <el-table-column
        :label="$t('member.topup.code')"
        align="center"
        prop="code"
      />
      <el-table-column
        :label="$t('member.topup.username')"
        align="center"
        prop="username"
      />
      <el-table-column
          :label="$t('member.index.isFake')"
          align="center"
          prop="isReal"
      >
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.isReal" />
        </template>
        </el-table-column>
      <el-table-column
        :label="$t('member.topup.amount')"
        align="center"
        prop="amout"
      />
      <el-table-column
        :label="$t('member.topup.amountReal')"
        align="center"
        prop="realMoney"
      />
      <el-table-column
        :label="$t('member.topup.type')"
        align="center"
        prop="type"
      >
        <template #default="scope">
          <el-tag
            :type="scope.row.type == '0' ? 'primary' : 'success'"
            effect="light"
          >
            {{ scope.row.type == '1'  ? $t('member.topup.typeUser') : $t('member.topup.typeBackend')  }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('member.topup.payMethod')"
        align="center"
        prop="payMethod"
      />
      <el-table-column
        :label="$t('member.topup.address')"
        align="center"
        prop="address"
        width="180"
      />
      <el-table-column
        :label="$t('member.topup.status')"
        align="center"
        prop="status"
      >
        <template #default="scope">
          <el-tag v-if="scope.row.status == '0'" type="success" effect="light">{{ $t('member.topup.statusSuccess') }}</el-tag>
          <el-tag v-else-if="scope.row.status == '1'" type="warning" effect="light">{{ $t('member.topup.statusPending') }}</el-tag>
          <el-tag v-else-if="scope.row.status == '2'" type="danger" effect="light">{{ $t('member.topup.statusRejected') }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column
        :label="$t('member.topup.auditor')"
        align="center"
        prop="auditor"
      />
      <el-table-column
        :label="$t('member.topup.auditTime')"
        align="center"
        prop="auditTime"
        width="160"
      >
        <template #default="scope">
          <span v-if="scope.row.auditTime">{{
            parseTime(scope.row.auditTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('member.topup.createTime')"
        align="center"
        prop="createTime"
        width="160"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
        <el-table-column
        :label="$t('member.topup.remark')"
        align="center"
        prop="remark"
        width="120"
      />
      <el-table-column
        :label="$t('common.operation')"
        align="center"
        width="120"
        fixed="right"
      >
        <template #default="scope">
          <el-button
            v-if="scope.row.status == '1'"
            type="primary"
            link
            icon="Check"
            @click="handleAudit(scope.row)"
          >{{ $t('member.topup.audit') }}</el-button>
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
      <el-form ref="topupRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item :label="$t('member.topup.code')" prop="code">
          <el-input
            v-model="form.code"
            :placeholder="$t('member.topup.enterCode')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.topup.userId')" prop="userId">
          <el-input
            v-model="form.userId"
            :placeholder="$t('member.topup.enterUserId')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.topup.amount')" prop="amout">
          <el-input
            v-model="form.amout"
            :placeholder="$t('member.topup.enterAmount')"
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

    <!-- 审核对话框 -->
    <el-dialog :title="$t('member.topup.auditRecord')" v-model="auditOpen" width="500px" append-to-body>
      <el-form ref="auditRef" :model="auditForm" :rules="auditRules" label-width="80px">
        <el-form-item :label="$t('member.topup.amount')" prop="amout">
          <span>{{auditForm.amout}}</span>
        </el-form-item>
        <el-form-item :label="$t('member.topup.amountReal')" prop="realMoney">
          <el-input
            v-model="auditForm.realMoney"
            :placeholder="$t('member.topup.amountReal')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.topup.remark')" prop="remark">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="4"
            :placeholder="$t('member.topup.enterRemark')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="success" @click="submitAudit('0')">{{
            $t("member.topup.approve")
          }}</el-button>
          <el-button type="danger" @click="submitAudit('2')">{{
            $t("member.topup.reject")
          }}</el-button>
          <el-button @click="cancelAudit">{{ $t("common.cancel") }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Topup">
import {
  listTopup,
  getTopup,
  delTopup,
  addTopup,
  updateTopup,
} from "@/api/member/topup";
import { reactive, toRefs, ref, getCurrentInstance } from "vue";
// use global $t via proxy to avoid exposing `t` into template render context

const { proxy } = getCurrentInstance();

const { sys_yes_no } = proxy.useDict(
    "sys_yes_no"
);

const topupList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const auditDateRange = ref([]);
const auditOpen = ref(false);

const data = reactive({
  reTypes: [
    {name: '后台充值',value:0},
    {name: '用户充值',value:1}
  ],
  form: {},
  auditForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    code: null,
    username: null,
    agentUsername: null,
  },
  rules: {
    code: [
      {
        required: true,
        message: proxy.$t("member.topup.codeRequired"),
        trigger: "blur",
      },
    ],
    userId: [
      {
        required: true,
        message: proxy.$t("member.topup.userIdRequired"),
        trigger: "blur",
      },
    ],
    amout: [
      {
        required: true,
        message: proxy.$t("member.topup.amountRequired"),
        trigger: "blur",
      },
    ],
  },
  auditRules: {
    remark: [
      {
        required: true,
        message: proxy.$t("member.topup.remarkRequired"),
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules, auditForm, auditRules, reTypes } = toRefs(data);

/** 查询充值记录列表 */
function getList() {
  loading.value = true;
  let params = proxy.addDateRange(queryParams.value, dateRange.value);
  // 如果 addDateRange 创建了 beginTime/endTime 但没有实际选择日期，移除这些空参数
  if (params && params.params) {
    if (!params.params.beginTime) {
      delete params.params.beginTime;
      delete params.params.endTime;
    }
  }

  // 添加审核时间区间参数（只有在选择了区间时才传）
  if (auditDateRange.value && auditDateRange.value.length === 2 && auditDateRange.value[0] && auditDateRange.value[1]) {
    params.params = params.params || {};
    params.params.auditBeginTime = auditDateRange.value[0];
    params.params.auditEndTime = auditDateRange.value[1];
  } else if (params && params.params) {
    // 确保没有残留的 auditBeginTime/auditEndTime
    delete params.params.auditBeginTime;
    delete params.params.auditEndTime;
  }

  listTopup(params).then((response) => {
    topupList.value = response.rows;
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
    code: null,
    userId: null,
    amout: null,
    createTime: null,
  };
  proxy.resetForm("topupRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
  auditDateRange.value = [];
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
  title.value = proxy.$t("member.topup.addRechargeRecord");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getTopup(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = proxy.$t("member.topup.editRechargeRecord");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["topupRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateTopup(form.value).then((response) => {
          proxy.$modal.msgSuccess(proxy.$t("member.topup.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addTopup(form.value).then((response) => {
          proxy.$modal.msgSuccess(proxy.$t("member.topup.addSuccess"));
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
    .confirm(proxy.$t("member.topup.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delTopup(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(proxy.$t("member.topup.deleteSuccess"));
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/topup/export",
    {
      ...queryParams.value,
    },
    `${proxy.$t("member.topup.exportFileName")}_${new Date().getTime()}.xlsx`
  );
}

/** 审核按钮操作 */
function handleAudit(row) {
  auditForm.value = {
    id: row.id,
    userId: row.userId,
    amout: row.amout,
    remark: "",
  };
  auditOpen.value = true;
}

/** 取消审核 */
function cancelAudit() {
  auditOpen.value = false;
  auditForm.value = {};
}

/** 提交审核 */
function submitAudit(status) {
  proxy.$refs["auditRef"].validate((valid) => {
    if (valid) {
      const params = {
        id: auditForm.value.id,
        userId: auditForm.value.userId,
        status: status,
        amout: auditForm.value.amout,
        realMoney: auditForm.value.realMoney,
        remark: auditForm.value.remark,
      };
      updateTopup(params).then((response) => {
        proxy.$modal.msgSuccess(proxy.$t("member.topup.auditSuccess"));
        auditOpen.value = false;
        getList();
      });
    }
  });
}

getList();
</script>
