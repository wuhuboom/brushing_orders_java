<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="列表" :columns="noticeColumns" :data-source="noticeList" :loading="loading" row-key="noticeId" :row-selection="rowSelection" :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }" @page-change="handleAntPageChange" @refresh="getList">
      <template #search><a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form"><a-row :gutter="[24,16]" align="middle"><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="公告标题"><a-input v-model:value="queryParams.noticeTitle" allow-clear placeholder="请输入公告标题" @pressEnter="handleQuery" /></a-form-item></a-col><a-col :xs="24" :sm="12" :md="8" :lg="7"><a-form-item label="操作人员"><a-input v-model:value="queryParams.createBy" allow-clear placeholder="请输入操作人员" @pressEnter="handleQuery" /></a-form-item></a-col><a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="handleQuery">查询</a-button></a-space></a-col></a-row></a-form></template>
      <template #toolbar><a-button type="primary" @click="handleAdd" v-hasPermi="['system:notice:add']">新增</a-button><a-button :disabled="single" @click="handleUpdate" v-hasPermi="['system:notice:edit']">修改</a-button><a-button :disabled="single" @click="openTranslationDialog(selectedRow)" v-hasPermi="['system:notice:edit']">国际化</a-button><a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['system:notice:remove']">删除</a-button></template>
      <template #bodyCell="{ column, record }"><template v-if="column.key==='status'">{{ dictText(sys_notice_status, record.status) }}</template><template v-else-if="column.key==='createTime'">{{ parseTime(record.createTime) }}</template><template v-else-if="column.key==='operation'"><a-space><a-button type="link" @click="handleUpdate(record)" v-hasPermi="['system:notice:edit']">修改</a-button><a-button type="link" @click="openTranslationDialog(record)" v-hasPermi="['system:notice:edit']">国际化</a-button><a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['system:notice:remove']">删除</a-button></a-space></template></template>
    </ant-pro-table>
    <a-modal v-model:open="open" :title="title" width="780px" destroy-on-close @cancel="cancel">
      <a-form ref="noticeRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }" :wrapper-col="{ flex: 1 }">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="公告标题" name="noticeTitle">
              <a-input v-model:value="form.noticeTitle" placeholder="请输入公告标题" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio v-for="dict in sys_notice_status" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="内容" name="noticeContent">
              <editor v-model="form.noticeContent" :min-height="192" />
            </a-form-item>
          </a-col>
        </a-row>
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
    <translation-drawer
      v-model="translationOpen"
      :title="translationTitle"
      :translations="translationForm"
      type="noticeItem"
      @submit="submitTranslations"
    />
  </div>
</template>



<script setup name="Notice">
import {
  listNotice,
  getNotice,
  delNotice,
  addNotice,
  updateNotice,
} from "@/api/system/notice";
import TranslationDrawer from "@/views/member/components/TranslationDrawer.vue";
import { createEmptyTranslations } from "@/views/member/components/translationLanguages";

const { proxy } = getCurrentInstance();
const { sys_notice_status, sys_notice_type } = proxy.useDict(
  "sys_notice_status",
  "sys_notice_type"
);

const noticeList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const noticeRef = ref(null);
const translationOpen = ref(false);
const translationTitle = ref("");
const translationForm = ref(createEmptyTranslations());
const currentTranslationRow = ref(null);
const selectedRow = computed(() => noticeList.value.find((item) => item.noticeId === ids.value[0]));

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    noticeTitle: undefined,
    createBy: undefined,
    status: undefined,
  },
  rules: {
    noticeTitle: [
      { required: true, message: "公告标题不能为空", trigger: "blur" },
    ],
    noticeType: [
      { required: true, message: "公告类型不能为空", trigger: "change" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

const noticeColumns=[{title:"序号",dataIndex:"noticeId",width:100},{title:"公告标题",dataIndex:"noticeTitle",width:260},{title:"状态",key:"status",dataIndex:"status",width:100},{title:"创建者",dataIndex:"createBy",width:120},{title:"创建时间",key:"createTime",dataIndex:"createTime",width:180},{title:"操作",key:"operation",width:210,fixed:"right"}];
const rowSelection=computed(()=>({selectedRowKeys:ids.value,onChange:(_keys,rows)=>handleSelectionChange(rows)}));
function dictText(options,value){return options.value?.find((item)=>String(item.value)===String(value))?.label??value??"-";}
function handleAntPageChange({page,pageSize}){queryParams.value.pageNum=page;queryParams.value.pageSize=pageSize;getList();}

/** 鏌ヨ鍏憡鍒楄〃 */
function getList() {
  loading.value = true;
  listNotice(queryParams.value).then((response) => {
    noticeList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** 鍙栨秷鎸夐挳 */
function cancel() {
  open.value = false;
  reset();
}

/** 琛ㄥ崟閲嶇疆 */
function reset() {
  form.value = {
    noticeId: undefined,
    noticeTitle: undefined,
    noticeType: undefined,
    noticeContent: undefined,
    status: "0",
  };
  noticeRef.value?.clearValidate?.();
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

/** 澶氶€夋閫変腑鏁版嵁 */
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.noticeId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 鏂板鎸夐挳鎿嶄綔 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加公告";
}

/**淇敼鎸夐挳鎿嶄綔 */
function handleUpdate(row) {
  reset();
  const noticeId = row.noticeId || ids.value;
  getNotice(noticeId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改公告";
  });
}

/** 鎻愪氦鎸夐挳 */
function submitForm() {
  noticeRef.value?.validate().then(() => {
    if (form.value.noticeId != undefined) {
      updateNotice(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addNotice(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

async function openTranslationDialog(row) {
  if (!row?.noticeId) return;
  const response = await getNotice(row.noticeId);
  currentTranslationRow.value = response.data;
  translationForm.value = {
    ...createEmptyTranslations(),
    ...(response.data.translations || {}),
  };
  translationTitle.value = `${response.data.noticeTitle || "公告"} - 国际化`;
  translationOpen.value = true;
}

async function submitTranslations(translations) {
  const row = currentTranslationRow.value;
  if (!row) return;
  const translationsId = translations.id || row.translationsId;
  await updateNotice({
    ...row,
    translationsId,
    translations: { ...translations, id: translationsId },
  });
  proxy.$modal.msgSuccess("修改成功");
  translationOpen.value = false;
  currentTranslationRow.value = null;
  getList();
}

/** 鍒犻櫎鎸夐挳鎿嶄綔 */
function handleDelete(row = {}) {
  const noticeIds = row.noticeId || ids.value;
  proxy.$modal
    .confirm(`是否确认删除公告编号为 "${noticeIds}" 的数据项？`)
    .then(function () {
      return delNotice(noticeIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

getList();
</script>


