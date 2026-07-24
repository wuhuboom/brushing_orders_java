<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="登录日志"
      :columns="memberloginlogColumns"
      :data-source="memberloginlogList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 1100 }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams">
          <a-row :gutter="24" align="middle">
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
            <a-col :span="7">
              <a-form-item label="IP地址">
                <a-input
                  v-model:value="queryParams.ip"
                  placeholder="请输入IP地址"
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

      <template #toolbar>
        <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:memberloginlog:remove']">删除</a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'success'">
          <dict-tag :options="user_yes_no" :value="record.success" />
        </template>
        <template v-else-if="column.key === 'loginParams'">
          <a-button type="link" size="small" @click="toggleExpand(record)">
            {{ record.expanded ? "收缩" : "展开" }}
          </a-button>
          <div v-if="record.expanded">
            <pre class="login-params-pre">{{ formatJson(record.loginParams) }}</pre>
          </div>
          <span v-else class="login-params-preview">
            {{ truncateJson(record.loginParams) }}
          </span>
        </template>
        <template v-else-if="column.key === 'requestHeaders'">
          <span class="login-params-preview">{{ truncateJson(record.requestHeaders) }}</span>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      :title="title"
      v-model:open="open"
      width="500px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="submitting"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form
        ref="memberloginlogRef"
        :model="form"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="用户ID" name="userId">
          <a-input v-model:value="form.userId" placeholder="请输入用户ID" />
        </a-form-item>
        <a-form-item label="IP地址" name="ip">
          <a-input v-model:value="form.ip" placeholder="请输入IP地址" />
        </a-form-item>
        <a-form-item label="地址" name="address">
          <a-input v-model:value="form.address" placeholder="请输入地址" />
        </a-form-item>
        <a-form-item label="参数" name="loginParams">
          <a-textarea
            v-model:value="form.loginParams"
            placeholder="请输入内容"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="Memberloginlog">
import {
  listMemberloginlog,
  getMemberloginlog,
  delMemberloginlog,
  addMemberloginlog,
  updateMemberloginlog,
} from "@/api/member/memberloginlog";

const { proxy } = getCurrentInstance();
const { user_yes_no } = proxy.useDict("user_yes_no");

const memberloginlogList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const submitting = ref(false);
const memberloginlogRef = ref();

const memberloginlogColumns = [
  { title: "ID", dataIndex: "id", align: "center", width: 100 },
  { title: "用户名", dataIndex: "username", align: "center", width: 160 },
  { title: "IP地址", dataIndex: "ip", align: "center", width: 160 },
  { title: "地址", dataIndex: "address", align: "center", width: 180 },
  { title: "是否成功", dataIndex: "success", align: "center", width: 120 },
  { title: "登录时间", dataIndex: "createTime", align: "center", width: 180 },
  { title: "请求头", key: "requestHeaders", dataIndex: "requestHeaders", width: 300 },
  { title: "参数", key: "loginParams", dataIndex: "loginParams", width: 500 },
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
    username: null,
    userId: null,
    ip: null,
    address: null,
    loginParams: null,
  },
  rules: {
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    ip: [{ required: true, message: "IP地址不能为空", trigger: "blur" }],
    address: [{ required: true, message: "地址不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 鏌ヨ鐧诲綍鏃ュ織鍒楄〃 */
function getList() {
  loading.value = true;
  listMemberloginlog(queryParams.value).then((response) => {
    // 涓烘瘡琛屾暟鎹坊鍔?expanded 灞炴€э紝榛樿 false
    memberloginlogList.value = response.rows.map((item) => ({
      ...item,
      expanded: false,
    }));
    total.value = response.total;
    loading.value = false;
  });
}

// 鍒囨崲灞曞紑/鏀剁缉鐘舵€?
function toggleExpand(row) {
  row.expanded = !row.expanded;
}

// 鏍煎紡鍖?JSON 涓哄彲璇诲瓧绗︿覆锛堝甫缂╄繘锛?
function formatJson(val) {
  if (!val) return "";
  try {
    const parsed = JSON.parse(val);
    return JSON.stringify(parsed, null, 2);
  } catch (e) {
    // 濡傛灉涓嶆槸鏈夋晥 JSON锛岀洿鎺ヨ繑鍥炲師鍊?
    return val;
  }
}

// 鎴柇 JSON 涓虹畝鐭樉绀?
function truncateJson(val) {
  if (!val) return "";
  try {
    const parsed = JSON.parse(val);
    const str = JSON.stringify(parsed);
    return str.length > 100 ? str.slice(0, 100) + "..." : str;
  } catch (e) {
    // 濡傛灉涓嶆槸鏈夋晥 JSON锛岀洿鎺ユ埅鏂師鍊?
    return val.length > 100 ? val.slice(0, 100) + "..." : val;
  }
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
    userId: null,
    ip: null,
    address: null,
    createTime: null,
    loginParams: null,
  };
  nextTick(() => memberloginlogRef.value?.clearValidate?.());
}

/** 鎼滅储鎸夐挳鎿嶄綔 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 閲嶇疆鎸夐挳鎿嶄綔 */
function resetQuery() {
  queryParams.value.username = null;
  queryParams.value.ip = null;
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
  title.value = "修改登录日志";
}

/** 淇敼鎸夐挳鎿嶄綔 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getMemberloginlog(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改登录日志";
  });
}

/** 鎻愪氦鎸夐挳 */
async function submitForm() {
  try {
    await memberloginlogRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (form.value.id != null) {
      await updateMemberloginlog(form.value);
    } else {
      await addMemberloginlog(form.value);
    }
    proxy.$modal.msgSuccess("操作成功");
    open.value = false;
    getList();
  } finally {
    submitting.value = false;
  }
}

/** 鍒犻櫎鎸夐挳鎿嶄綔 */
function handleDelete(row = {}) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(`是否确认删除登录日志编号为 "${_ids}" 的数据项？`)
    .then(function () {
      return delMemberloginlog(_ids);
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
    "member/memberloginlog/export",
    {
      ...queryParams.value,
    },
    `memberloginlog_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>

<style scoped lang="scss">
.login-params-pre {
  max-height: 200px;
  margin: 8px 0 0;
  padding: 8px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
}

.login-params-preview {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>

