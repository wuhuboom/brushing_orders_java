<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="列表" :columns="zoneColumns" :data-source="zoneList" :loading="loading" row-key="id" :row-selection="rowSelection" :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }" @page-change="handleAntPageChange" @refresh="getList">
      <template #search><a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form"><a-row :gutter="[24,16]" align="middle"><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="时区"><a-input v-model:value="queryParams.name" allow-clear placeholder="请输入时区" @pressEnter="handleQuery" /></a-form-item></a-col><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="时区ID"><a-input v-model:value="queryParams.tzName" allow-clear placeholder="请输入 IANA 时区名，如 Asia/Shanghai" @pressEnter="handleQuery" /></a-form-item></a-col><a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="handleQuery">查询</a-button></a-space></a-col></a-row></a-form></template>
      <template #toolbar><a-button type="primary" @click="handleAdd" v-hasPermi="['system:zone:add']">新增</a-button><a-button :disabled="single" @click="handleUpdate" v-hasPermi="['system:zone:edit']">修改</a-button><a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['system:zone:remove']">删除</a-button></template>
      <template #bodyCell="{ column, record }"><template v-if="column.key==='status'"><span v-if="record.status == '0'">使用中</span><span v-else>--</span></template><template v-else-if="column.key==='operation'"><a-space><a-button v-if="record.status !== 0" type="link" @click="handleSetActive(record)" v-hasPermi="['system:zone:active']">启用</a-button><a-button type="link" @click="handleUpdate(record)" v-hasPermi="['system:zone:edit']">修改</a-button><a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['system:zone:remove']">删除</a-button></a-space></template></template>
    </ant-pro-table>
    <a-modal v-model:open="open" :title="title" width="500px" destroy-on-close @cancel="cancel">
      <a-form ref="zoneRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }" :wrapper-col="{ flex: 1 }">
        <a-form-item label="时区" name="name">
          <a-input v-model:value="form.name" placeholder="请输入时区" />
        </a-form-item>
        <a-form-item label="时区ID" name="tzName">
          <a-input v-model:value="form.tzName" placeholder="请输入 IANA 时区名，如 Asia/Shanghai" />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="modal-footer-actions">
          <a-space>
            <a-button type="primary" @click="submitForm">确 定</a-button>
            <a-button @click="cancel">取 消</a-button>
          </a-space>
        </div>
      </template>
    </a-modal>
  </div>
</template>



<script setup name="Zone">
import {
  listZone,
  getZone,
  delZone,
  addZone,
  updateZone,
  setActiveZone,
  getZoneActive,
} from "@/api/system/zone";
import { refreshActiveTimeZone } from "@/utils/timezone-helper";

const { proxy } = getCurrentInstance();

const zoneList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const zoneRef = ref(null);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    tzName: null,
    status: null,
  },
  rules: {
    name: [{ required: true, message: "时区不能为空", trigger: "blur" }],
    tzName: [
      {
        required: true,
        message: "时区ID不能为空",
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

const zoneColumns=[{title:"时区",dataIndex:"name",width:180},{title:"时区ID",dataIndex:"tzName",width:240},{title:"状态",key:"status",dataIndex:"status",width:120},{title:"操作",key:"operation",width:180,fixed:"right"}];
const rowSelection=computed(()=>({selectedRowKeys:ids.value,onChange:(_keys,rows)=>handleSelectionChange(rows)}));
function handleAntPageChange({page,pageSize}){queryParams.value.pageNum=page;queryParams.value.pageSize=pageSize;getList();}

function handleSetActive(row) {
  proxy.$modal.confirm(`确认将【${row.name || row.tzName}】设置为“在用”吗？此操作会将当前在用时区停用。`)
    .then(() => setActiveZone(row.id))
    .then(() => {
      proxy.$modal.msgSuccess("已切换为在用时区");
      refreshActiveTimeZone();
      return getList();
    })
    .catch(() => {});
}

/** 鏌ヨ鏃跺尯绠＄悊鍒楄〃 */
function getList() {
  loading.value = true;
  listZone(queryParams.value).then((response) => {
    zoneList.value = response.rows;
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
    name: null,
    tzName: null,
    status: null,
    createTime: null,
  };
  zoneRef.value?.clearValidate?.();
}

/** 鎼滅储鎸夐挳鎿嶄綔 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 閲嶇疆鎸夐挳鎿嶄綔 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
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
  title.value = "添加时区";
}

/** 淇敼鎸夐挳鎿嶄綔 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getZone(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改时区";
  });
}

/** 鎻愪氦鎸夐挳 */
function submitForm() {
  zoneRef.value?.validate().then(() => {
    if (form.value.id != null) {
      updateZone(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addZone(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        refreshActiveTimeZone();
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

/** 鍒犻櫎鎸夐挳鎿嶄綔 */
function handleDelete(row = {}) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(`是否确认删除时区编号为 "${_ids}" 的数据项？`)
    .then(function () {
      return delZone(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** 瀵煎嚭鎸夐挳鎿嶄綔 */
function handleExport() {
  proxy.download(
    "system/zone/export",
    {
      ...queryParams.value,
    },
    `zone_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>


