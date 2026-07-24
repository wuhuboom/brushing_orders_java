<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="列表" :columns="withdrawaltypeColumns" :data-source="withdrawaltypeList" :loading="loading" row-key="id" :row-selection="rowSelection" :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }" @page-change="handleAntPageChange" @refresh="getList">
      <template #search><a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle"><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="类型"><a-select v-model:value="queryParams.type" allow-clear placeholder="请选择类型"><a-select-option v-for="dict in order_zhlx" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option></a-select></a-form-item></a-col><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="名称"><a-input v-model:value="queryParams.name" allow-clear placeholder="请输入名称" @pressEnter="handleQuery" /></a-form-item></a-col><a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="handleQuery">查询</a-button></a-space></a-col></a-row></a-form></template>
      <template #toolbar><a-button type="primary" @click="handleAdd" v-hasPermi="['member:withdrawaltype:add']">创建</a-button><a-button :disabled="single" @click="handleUpdate" v-hasPermi="['member:withdrawaltype:edit']">修改</a-button><a-button :disabled="single" @click="handleCopy" v-hasPermi="['member:withdrawaltype:add']">复制</a-button><a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:withdrawaltype:remove']">删除</a-button></template>
      <template #bodyCell="{ column, record }"><template v-if="column.key === 'type'">{{ dictText(order_zhlx, record.type) }}</template><template v-else-if="column.key === 'icon'"><image-preview :src="record.icon" :width="50" :height="50" /></template><template v-else-if="column.key === 'operation'"><a-space><a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:withdrawaltype:edit']">修改</a-button><a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:withdrawaltype:add']">复制</a-button><a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['member:withdrawaltype:remove']">删除</a-button></a-space></template></template>
    </ant-pro-table>
    <a-modal v-model:open="open" :title="title" width="800px" destroy-on-close @ok="submitForm" @cancel="cancel">
      <a-form ref="withdrawaltypeRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="[20, 0]">
          <a-col :span="12">
            <a-form-item label="类型" name="type">
              <a-radio-group v-model:value="form.type">
                <a-radio v-for="dict in order_zhlx" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入名称" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="汇率" name="exchangeRate">
              <a-input v-model:value="form.exchangeRate" placeholder="请输入汇率" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="序号" name="sortOrder">
              <a-input v-model:value="form.sortOrder" placeholder="请输入序号" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="图标" name="icon">
              <image-upload v-model="form.icon" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row v-if="form.type === '0'" :gutter="[20, 0]">
          <a-col :span="12">
            <a-form-item label="银行名称" name="bankName">
              <a-input v-model:value="form.bankName" placeholder="请输入银行名称" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="存款种类" name="depositType">
              <a-input v-model:value="form.depositType" placeholder="请输入存款种类" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="支行代码" name="branchCode">
              <a-input v-model:value="form.branchCode" placeholder="请输入支行代码" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="支行名称" name="branchName">
              <a-input v-model:value="form.branchName" placeholder="请输入支行名称" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="银行账号" name="bankAccount">
              <a-input v-model:value="form.bankAccount" placeholder="请输入银行账号" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="账户持有人" name="accountHolder">
              <a-input v-model:value="form.accountHolder" placeholder="请输入账户持有人" allow-clear />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row v-if="form.type === '1'" :gutter="[20, 0]">
          <a-col :span="12">
            <a-form-item label="账户名称" name="accountName">
              <a-input v-model:value="form.accountName" placeholder="请输入账户名称" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="钱包名称" name="walletName">
              <a-input v-model:value="form.walletName" placeholder="请输入钱包名称" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="钱包地址" name="walletAddress">
              <a-input v-model:value="form.walletAddress" placeholder="请输入钱包地址" allow-clear />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="form.remarks" placeholder="请输入备注" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
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

const withdrawaltypeColumns = [
  { title: "ID", dataIndex: "id", width: 90 },
  { title: "类型", key: "type", dataIndex: "type", width: 120 },
  { title: "名称", dataIndex: "name", width: 160 },
  { title: "汇率", dataIndex: "exchangeRate", width: 120 },
  { title: "序号", dataIndex: "sortOrder", width: 100 },
  { title: "图标", key: "icon", dataIndex: "icon", width: 120 },
  { title: "创建时间", dataIndex: "createTime", width: 180 },
  { title: "备注", dataIndex: "remarks", width: 180 },
  { title: "操作", key: "operation", width: 130, fixed: "right" },
];
const rowSelection = computed(() => ({ selectedRowKeys: ids.value, onChange: (_keys, rows) => handleSelectionChange(rows) }));
function dictText(options, value) { return options.value?.find((item) => String(item.value) === String(value))?.label ?? value ?? "-"; }
function handleAntPageChange({ page, pageSize }) { queryParams.value.pageNum = page; queryParams.value.pageSize = pageSize; getList(); }

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

function handleCopy(row) {
  const _id = row?.id || ids.value[0];
  getWithdrawaltype(_id).then((response) => {
    const copied = { ...response.data };
    delete copied.id;
    delete copied.createTime;
    delete copied.updateTime;
    addWithdrawaltype(copied).then(() => {
      proxy.$modal.msgSuccess("复制成功");
      getList();
    });
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["withdrawaltypeRef"]?.validate?.().then(() => {
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
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row = {}) {
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
