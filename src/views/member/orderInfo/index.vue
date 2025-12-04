<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('member.orderinfo.orderNo')" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          :placeholder="$t('member.orderinfo.enterOrderNo')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.orderinfo.username')" prop="username">
        <el-input
          v-model="queryParams.username"
          :placeholder="$t('member.orderinfo.enterUsername')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.orderinfo.phone')" prop="phone">
        <el-input
          v-model="queryParams.phone"
          :placeholder="$t('member.orderinfo.enterPhone')"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('member.orderinfo.orderTime')"
        style="width: 400px"
      >
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          :start-placeholder="$t('common.startDate')"
          :end-placeholder="$t('common.endDate')"
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
        :columns="columns"
        @queryTable="getList"
        @update:columns="handleColumnsUpdate"
      ></right-toolbar>
    </el-row>
    <el-table
      v-loading="loading"
      :data="orderInfoList"
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
        align="center"
      >
        <template v-if="column.key === '5'" #default="scope">
          <div>{{ scope.row.product.name }}</div>
        </template>
        <template v-if="column.key === '6'" #default="scope">
          <div>{{ scope.row.price }}</div>
        </template>
        <template v-if="column.key === '9'" #default="scope">
          <dict-tag
            :options="commission_status"
            :value="scope.row.commissionStatus"
          />
        </template>
        <template
          v-if="column.key === '10' || column.key === '11'"
          #default="scope"
        >
          <span>{{
            parseTime(scope.row[column.prop], "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
        <template v-if="column.key === '12'" #default="scope">
          <dict-tag :options="order_type" :value="scope.row.orderType" />
        </template>
        <template v-if="column.key === '13'" #default="scope">
          <dict-tag :options="order_status" :value="scope.row.status" />
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
        ref="orderInfoRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item :label="$t('member.orderinfo.orderNo')" prop="orderNo">
          <el-input
            v-model="form.orderNo"
            :placeholder="$t('member.orderinfo.enterOrderNo')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.orderinfo.userId')" prop="userId">
          <el-input
            v-model="form.userId"
            :placeholder="$t('member.orderinfo.enterUserId')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('member.orderinfo.productId')"
          prop="productId"
        >
          <el-input
            v-model="form.productId"
            :placeholder="$t('member.orderinfo.enterProductId')"
          />
        </el-form-item>
        <el-form-item :label="$t('member.orderinfo.quantity')" prop="quantity">
          <el-input
            v-model="form.quantity"
            :placeholder="$t('member.orderinfo.enterQuantity')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('member.orderinfo.commission')"
          prop="commission"
        >
          <el-input
            v-model="form.commission"
            :placeholder="$t('member.orderinfo.enterCommission')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('member.orderinfo.commissionRate')"
          prop="commissionRate"
        >
          <el-input
            v-model="form.commissionRate"
            :placeholder="$t('member.orderinfo.enterCommissionRate')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('member.orderinfo.commissionStatus')"
          prop="commissionStatus"
        >
          <el-radio-group v-model="form.commissionStatus">
            <el-radio
              v-for="dict in commission_status"
              :key="dict.value"
              :label="dict.value"
              >{{ dict.label }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item
          :label="$t('member.orderinfo.orderTime')"
          prop="orderTime"
        >
          <el-date-picker
            clearable
            v-model="form.orderTime"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="$t('member.orderinfo.selectOrderTime')"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item
          :label="$t('member.orderinfo.submitTime')"
          prop="submitTime"
        >
          <el-date-picker
            clearable
            v-model="form.submitTime"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="$t('member.orderinfo.selectSubmitTime')"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item
          :label="$t('member.orderinfo.orderType')"
          prop="orderType"
        >
          <el-select
            v-model="form.orderType"
            :placeholder="$t('member.orderinfo.selectOrderType')"
          >
            <el-option
              v-for="dict in order_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('member.orderinfo.status')" prop="status">
          <el-select
            v-model="form.status"
            :placeholder="$t('member.orderinfo.selectStatus')"
          >
            <el-option
              v-for="dict in order_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
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

<script setup name="OrderInfo">
import {
  listOrderInfo,
  getOrderInfo,
  delOrderInfo,
  addOrderInfo,
  updateOrderInfo,
} from "@/api/member/orderInfo";
import { ref, reactive, toRefs, computed } from "vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

const { proxy } = getCurrentInstance();
const { commission_status, order_status, order_type } = proxy.useDict(
  "commission_status",
  "order_status",
  "order_type"
);

const orderInfoList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const tableKey = ref(Date.now());

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: null,
    username: null,
    phone: null,
  },
  rules: {
    orderNo: [
      {
        required: true,
        message: t("member.orderinfo.orderNoRequired"),
        trigger: "blur",
      },
    ],
    userId: [
      {
        required: true,
        message: t("member.orderinfo.userIdRequired"),
        trigger: "blur",
      },
    ],
    productId: [
      {
        required: true,
        message: t("member.orderinfo.productIdRequired"),
        trigger: "blur",
      },
    ],
  },
});

const columns = ref([
  {
    key: "0",
    label: t("member.orderinfo.primaryKeyId"),
    visible: true,
    prop: "id",
    width: "80",
  },
  {
    key: "1",
    label: t("member.orderinfo.orderNo"),
    visible: true,
    prop: "orderNo",
  },
  {
    key: "2",
    label: t("member.orderinfo.username"),
    visible: true,
    prop: "username",
    width: "120",
  },
  {
    key: "3",
    label: t("member.orderinfo.phone"),
    visible: true,
    prop: "phone",
    width: "180",
  },
  {
    key: "4",
    label: t("member.orderinfo.quantity"),
    visible: true,
    prop: "quantity",
    width: "80",
  },
  {
    key: "5",
    label: t("member.orderinfo.productName"),
    visible: true,
    prop: null,
  },
  {
    key: "6",
    label: t("member.orderinfo.productPrice"),
    visible: true,
    prop: null,
    width: "120",
  },
  {
    key: "7",
    label: t("member.orderinfo.commissionAmount"),
    visible: true,
    prop: "commission",
    width: "120",
  },
  {
    key: "8",
    label: t("member.orderinfo.commissionRate"),
    visible: true,
    prop: "commissionRate",
    width: "80",
    formatter: (row) =>
      row.commissionRate != null ? row.commissionRate + "%" : "",
  },
  {
    key: "9",
    label: t("member.orderinfo.commissionStatus"),
    visible: true,
    prop: "commissionStatus",
    width: "120",
  },
  {
    key: "10",
    label: t("member.orderinfo.orderTime"),
    visible: true,
    prop: "orderTime",
    width: "160",
  },
  {
    key: "11",
    label: t("member.orderinfo.submitTime"),
    visible: true,
    prop: "submitTime",
    width: "160",
  },
  {
    key: "12",
    label: t("member.orderinfo.orderType"),
    visible: true,
    prop: "orderType",
    width: "100",
  },
  {
    key: "13",
    label: t("member.orderinfo.status"),
    visible: true,
    prop: "status",
    width: "100",
    fixed: "right",
  },
]);

const { queryParams, form, rules } = toRefs(data);

const filteredColumns = computed(() => {
  return columns.value.filter((column) => column.visible);
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

/** 查询订单列表 */
function getList() {
  loading.value = true;
  listOrderInfo(proxy.addDateRange(queryParams.value, dateRange.value)).then(
    (response) => {
      orderInfoList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    }
  );
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
    orderNo: null,
    userId: null,
    productId: null,
    quantity: null,
    commission: null,
    commissionRate: null,
    commissionStatus: null,
    orderTime: null,
    submitTime: null,
    orderType: null,
    status: null,
    createTime: null,
    updateTime: null,
  };
  proxy.resetForm("orderInfoRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
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
  title.value = t("member.orderinfo.addOrderInfo");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getOrderInfo(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("member.orderinfo.editOrderInfo");
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["orderInfoRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateOrderInfo(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.orderinfo.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addOrderInfo(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.orderinfo.addSuccess"));
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
    .confirm(t("member.orderinfo.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delOrderInfo(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("member.orderinfo.deleteSuccess"));
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/orderInfo/export",
    {
      ...queryParams.value,
    },
    `${t("member.orderinfo.exportFileName")}_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
