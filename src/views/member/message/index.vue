<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否启用" prop="isEnabled">
        <el-select
          v-model="queryParams.isEnabled"
          placeholder="请选择是否启用"
          clearable
        >
          <el-option
            v-for="dict in user_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery"
          >搜索</el-button
        >
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['member:message:add']"
          >新增</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['member:message:edit']"
          >修改</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['member:message:remove']"
          >删除</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['member:message:export']"
          >导出</el-button
        >
      </el-col>
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="messageList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="标题" align="center" prop="title" />
      <el-table-column label="会员列表" align="center" prop="memberList">
        <template #default="scope">
          {{ scope.row.pushUsersDisplay }}
        </template>
      </el-table-column>
      <el-table-column label="是否启用" align="center" prop="isEnabled">
        <template #default="scope">
          <dict-tag :options="user_yes_no" :value="scope.row.isEnabled" />
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            circle
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['member:message:edit']"
          ></el-button>
          <el-button
            circle
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:message:remove']"
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

    <!-- 添加或修改站内信对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="messageRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="会员列表" prop="memberList">
          <el-select
            v-model="form.memberList"
            placeholder="请选择用户"
            filterable
            multiple
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="u in usersList"
              :key="u.id"
              :label="u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="是否启用" prop="isEnabled">
          <el-radio-group v-model="form.isEnabled">
            <el-radio
              v-for="dict in user_yes_no"
              :key="dict.value"
              :label="parseInt(dict.value)"
              >{{ dict.label }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <editor v-model="form.content" :min-height="192" />
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

<script setup name="Message">
import {
  listMessage,
  getMessage,
  delMessage,
  addMessage,
  updateMessage,
} from "@/api/member/message";
import { allUser } from "@/api/member/orderuser";

const { proxy } = getCurrentInstance();
const { user_yes_no } = proxy.useDict("user_yes_no");

const messageList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const usersList = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: null,
    memberList: null,
    isEnabled: null,
    content: null,
  },
  rules: {
    title: [{ required: true, message: "标题不能为空", trigger: "blur" }],
    memberList: [
      { required: true, message: "会员列表不能为空", trigger: "blur" },
    ],
    isEnabled: [
      { required: true, message: "是否启用不能为空", trigger: "change" },
    ],
    content: [{ required: true, message: "内容不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

function loadUsers() {
  allUser()
    .then((response) => {
      usersList.value = response.rows || response.data || response;
    })
    .catch(() => {
      usersList.value = [];
    });
}

/** 查询站内信列表 */
function getList() {
  loading.value = true;
  listMessage(queryParams.value)
    .then((response) => {
      messageList.value = (response.rows || []).map((row) => {
        const ids = row.memberList
          .split(",")
          .map((id) => parseInt(id.trim()))
          .filter((id) => id && !isNaN(id));
        const usernames = ids
          .map((id) => usersList.value.find((u) => u.id === id)?.username || id)
          .join(", ");
        row.pushUsersDisplay = usernames || "-";
        return row;
      });
      total.value = response.total;
      loading.value = false;
    })
    .catch(() => {});
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
    title: null,
    memberList: null,
    isEnabled: 0,
    createTime: null,
    content: null,
  };
  proxy.resetForm("messageRef");
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
  title.value = "添加站内信";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getMessage(_id).then((response) => {
    form.value = response.data;
    const userIds = form.value.memberList
      .split(",")
      .map((id) => parseInt(id.trim()))
      .filter((id) => id && !isNaN(id));
    form.value.memberList = userIds;
    open.value = true;
    title.value = "修改站内信";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["messageRef"].validate((valid) => {
    if (valid) {
      form.value.memberList = form.value.memberList.join(",");
      if (form.value.id != null) {
        updateMessage(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addMessage(form.value).then((response) => {
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
    .confirm('是否确认删除站内信编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delMessage(_ids);
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
    "member/message/export",
    {
      ...queryParams.value,
    },
    `message_${new Date().getTime()}.xlsx`
  );
}

getList();
loadUsers();
</script>
