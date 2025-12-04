<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="IP地址" prop="ip">
        <el-input
          v-model="queryParams.ip"
          placeholder="请输入IP地址"
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
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="memberloginlogList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="用户名" align="center" prop="username" />
      <el-table-column label="IP地址" align="center" prop="ip" />
      <el-table-column label="地址" align="center" prop="address" />
      <el-table-column label="参数" width="500">
        <!-- 使用 scoped slot 来自定义渲染 -->
        <template #default="{ row }">
          <el-button
            size="small"
            type="text"
            @click="toggleExpand(row)"
            style="margin-right: 8px"
          >
            {{ row.expanded ? "收缩" : "展开" }}
          </el-button>
          <div v-if="row.expanded">
            <!-- 展开时显示格式化 JSON，使用 pre 标签保持缩进 -->
            <pre
              style="
                white-space: pre-wrap;
                word-break: break-all;
                font-size: 12px;
                max-height: 200px;
                overflow-y: auto;
                background-color: #f5f5f5;
                padding: 8px;
                border-radius: 4px;
                margin: 0;
              "
            >
              {{ formatJson(row.loginParams) }}
            </pre>
          </div>
          <div v-else>
            <!-- 收缩时显示截断版本 -->
            <span style="font-size: 12px; color: #666">
              {{ truncateJson(row.loginParams) }}
            </span>
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

    <!-- 添加或修改登录日志对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form
        ref="memberloginlogRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="IP地址" prop="ip">
          <el-input v-model="form.ip" placeholder="请输入IP地址" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="参数" prop="loginParams">
          <el-input
            v-model="form.loginParams"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
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

const memberloginlogList = ref([]);
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

/** 查询登录日志列表 */
function getList() {
  loading.value = true;
  listMemberloginlog(queryParams.value).then((response) => {
    // 为每行数据添加 expanded 属性，默认 false
    memberloginlogList.value = response.rows.map((item) => ({
      ...item,
      expanded: false,
    }));
    total.value = response.total;
    loading.value = false;
  });
}

// 切换展开/收缩状态
function toggleExpand(row) {
  row.expanded = !row.expanded;
}

// 格式化 JSON 为可读字符串（带缩进）
function formatJson(val) {
  if (!val) return "";
  try {
    const parsed = JSON.parse(val);
    return JSON.stringify(parsed, null, 2);
  } catch (e) {
    // 如果不是有效 JSON，直接返回原值
    return val;
  }
}

// 截断 JSON 为简短显示
function truncateJson(val) {
  if (!val) return "";
  try {
    const parsed = JSON.parse(val);
    const str = JSON.stringify(parsed);
    return str.length > 100 ? str.slice(0, 100) + "..." : str;
  } catch (e) {
    // 如果不是有效 JSON，直接截断原值
    return val.length > 100 ? val.slice(0, 100) + "..." : val;
  }
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
    userId: null,
    ip: null,
    address: null,
    createTime: null,
    loginParams: null,
  };
  proxy.resetForm("memberloginlogRef");
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
  title.value = "添加登录日志";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getMemberloginlog(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改登录日志";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["memberloginlogRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateMemberloginlog(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addMemberloginlog(form.value).then((response) => {
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
    .confirm('是否确认删除登录日志编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delMemberloginlog(_ids);
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
    "member/memberloginlog/export",
    {
      ...queryParams.value,
    },
    `memberloginlog_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
