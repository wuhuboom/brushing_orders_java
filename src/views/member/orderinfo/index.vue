<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="订单列表"
      :columns="orderinfoColumns"
      :data-source="orderinfoList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 2200 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams">
          <a-row :gutter="24" align="middle">
            <a-col :span="7">
              <a-form-item label="订单编号">
                <a-input
                  v-model:value="queryParams.orderNumber"
                  placeholder="请输入订单编号"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :span="7">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.username"
                  placeholder="请输入用户名"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :span="10" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'type'">
          <dict-tag :options="order_type" :value="record.type" />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <dict-tag :options="order_status" :value="record.status" />
        </template>
        <template v-else-if="column.dataIndex === 'expiryTime'">
          {{ parseTime(record.expiryTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'productImage'">
          <image-preview :src="record.productImage" :width="50" :height="50" />
        </template>
      </template>
    </ant-pro-table>
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

const orderinfoColumns = [
  { title: "订单编号", dataIndex: "orderNumber", align: "center", width: 200 },
  { title: "用户名", dataIndex: "username", align: "center", width: 160 },
  { title: "类型", dataIndex: "type", align: "center", width: 100 },
  { title: "单数", dataIndex: "orderCount", align: "center", width: 100 },
  { title: "金额", dataIndex: "amount", align: "center", width: 120 },
  { title: "返佣百分比", dataIndex: "rebatePercentage", align: "center", width: 130 },
  { title: "返佣", dataIndex: "rebate", align: "center", width: 100 },
  { title: "上级返佣百分比", dataIndex: "upperRebatePercentage", align: "center", width: 160 },
  { title: "上级返佣", dataIndex: "upperRebate", align: "center", width: 130 },
  { title: "状态", dataIndex: "status", align: "center", width: 120 },
  { title: "过期时间", dataIndex: "expiryTime", align: "center", width: 180 },
  { title: "商品图片", dataIndex: "productImage", align: "center", width: 120 },
  { title: "商品标题", dataIndex: "productTitle", align: "center", width: 320 },
  { title: "额外佣金", dataIndex: "extraCommissionId", align: "center", width: 120 },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "备注", dataIndex: "remarks", align: "center", width: 200 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_, selectedRows) => handleSelectionChange(selectedRows),
}));

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNumber: null,
    username: null,
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
    orderNumber: [{ required: true, message: "订单编号不能为空", trigger: "blur" }],
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    type: [{ required: true, message: "类型不能为空", trigger: "change" }],
    orderCount: [{ required: true, message: "单数不能为空", trigger: "blur" }],
    amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
    rebatePercentage: [{ required: true, message: "返佣百分比不能为空", trigger: "blur" }],
    rebate: [{ required: true, message: "返佣不能为空", trigger: "blur" }],
    upperRebatePercentage: [{ required: true, message: "上级返佣百分比不能为空", trigger: "blur" }],
    upperRebate: [{ required: true, message: "上级返佣不能为空", trigger: "blur" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }],
    expiryTime: [{ required: true, message: "过期时间不能为空", trigger: "blur" }],
    productId: [{ required: true, message: "商品ID不能为空", trigger: "blur" }],
    extraCommissionId: [{ required: true, message: "额外佣金ID不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 鏌ヨ璁㈠崟鍒楄〃 */
function getList() {
  loading.value = true;
  listOrderinfo(queryParams.value).then((response) => {
    orderinfoList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 鍙栨秷鎸夐挳
function cancel() {
  open.value = false;
  reset();
}

// 琛ㄥ崟閲嶇疆
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

/** 鎼滅储鎸夐挳鎿嶄綔 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 閲嶇疆鎸夐挳鎿嶄綔 */
function resetQuery() {
  queryParams.value.orderNumber = null;
  queryParams.value.username = null;
  handleQuery();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

// 澶氶€夋閫変腑鏁版嵁
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 鏂板鎸夐挳鎿嶄綔 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "修改订单";
}

/** 淇敼鎸夐挳鎿嶄綔 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getOrderinfo(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改订单";
  });
}

/** 鎻愪氦鎸夐挳 */
function submitForm() {
  proxy.$refs["orderinfoRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateOrderinfo(form.value).then((response) => {
          proxy.$modal.msgSuccess("操作成功");
          open.value = false;
          getList();
        });
      } else {
        addOrderinfo(form.value).then((response) => {
          proxy.$modal.msgSuccess("操作成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 鍒犻櫎鎸夐挳鎿嶄綔 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(`是否确认删除订单编号为 "${_ids}" 的数据项？`)
    .then(function () {
      return delOrderinfo(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("操作成功");
    })
    .catch(() => {});
}

/** 瀵煎嚭鎸夐挳鎿嶄綔 */
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


