<template>
  <div class="app-container">
    <el-row :gutter="20">
      <splitpanes
        :horizontal="appStore.device === 'mobile'"
        class="default-theme"
      >
        <!--用户数据-->
        <pane size="84">
          <el-col>
            <el-form
              :model="queryParams"
              ref="queryRef"
              :inline="true"
              v-show="showSearch"
              label-width="120px"
            >
              <el-form-item :label="t('user.userName')" prop="userName">
                <el-input
                  v-model="queryParams.userName"
                  :placeholder="t('user.enterUserName')"
                  clearable
                  style="width: 240px"
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
              <el-form-item :label="t('user.phoneNumber')" prop="phonenumber">
                <el-input
                  v-model="queryParams.phonenumber"
                  :placeholder="t('user.enterPhoneNumber')"
                  clearable
                  style="width: 240px"
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
              <el-form-item :label="t('user.status')" prop="status">
                <el-select
                  v-model="queryParams.status"
                  :placeholder="t('user.userStatus')"
                  clearable
                  style="width: 240px"
                >
                  <el-option
                    v-for="dict in sys_normal_disable"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item :label="t('user.createTime')" style="width: 308px">
                <el-date-picker
                  v-model="dateRange"
                  value-format="YYYY-MM-DD"
                  type="daterange"
                  range-separator="-"
                  :start-placeholder="t('user.startDate')"
                  :end-placeholder="t('user.endDate')"
                ></el-date-picker>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="handleQuery">
                  {{ t("user.search") }}
                </el-button>
                <el-button icon="Refresh" @click="resetQuery">
                  {{ t("user.reset") }}
                </el-button>
              </el-form-item>
            </el-form>

            <el-row :gutter="10" class="mb8">
              <el-col :span="1.5">
                <el-button
                  type="primary"
                  plain
                  icon="Plus"
                  @click="handleAdd"
                  v-hasPermi="['system:user:add']"
                >
                  {{ t("user.add") }}
                </el-button>
              </el-col>
              <el-col :span="1.5">
                <el-button
                  type="success"
                  plain
                  icon="Edit"
                  :disabled="single"
                  @click="handleUpdate"
                  v-hasPermi="['system:user:edit']"
                >
                  {{ t("user.edit") }}
                </el-button>
              </el-col>
              <el-col :span="1.5">
                <el-button
                  type="danger"
                  plain
                  icon="Delete"
                  :disabled="multiple"
                  @click="handleDelete"
                  v-hasPermi="['system:user:remove']"
                >
                  {{ t("user.delete") }}
                </el-button>
              </el-col>
              <right-toolbar
                v-model:showSearch="showSearch"
                @queryTable="getList"
                :columns="columns"
                @update:columns="handleColumnsUpdate"
              ></right-toolbar>
            </el-row>
            <el-table
              v-loading="loading"
              :data="userList"
              border
              @selection-change="handleSelectionChange"
              :key="tableKey"
            >
              <el-table-column type="selection" width="50" align="center" />
              <el-table-column
                v-for="column in filteredColumns"
                :key="column.key"
                :label="column.label"
                :prop="column.prop"
                :width="column.width"
                :fixed="column.fixed"
                align="center"
                :show-overflow-tooltip="column.showOverflowTooltip"
                :class-name="column.className"
              >
                <template v-if="column.key === '4'" #default="scope">
                  <div>{{ scope.row.loginIp }}</div>
                  <div>{{ scope.row.loginLocation }}</div>
                </template>
                <template v-if="column.key === '5'" #default="scope">
                  <span>{{ parseTime(scope.row.loginDate) }}</span>
                </template>
                <template v-if="column.key === '6'" #default="scope">
                  <el-switch
                    v-model="scope.row.status"
                    active-value="0"
                    inactive-value="1"
                    @change="handleStatusChange(scope.row)"
                  ></el-switch>
                </template>
                <template v-if="column.key === '7'" #default="scope">
                  <el-tooltip
                    :content="t('user.edit')"
                    placement="top"
                    v-if="scope.row.userId !== 1"
                  >
                    <el-button
                      link
                      type="primary"
                      icon="Edit"
                      @click="handleUpdate(scope.row)"
                      v-hasPermi="['system:user:edit']"
                    >
                      {{ t("user.edit") }}
                    </el-button>
                  </el-tooltip>
                  <el-tooltip
                    :content="t('user.delete')"
                    placement="top"
                    v-if="scope.row.userId !== 1"
                  >
                    <el-button
                      link
                      type="primary"
                      icon="Delete"
                      @click="handleDelete(scope.row)"
                      v-hasPermi="['system:user:remove']"
                    >
                      {{ t("user.delete") }}
                    </el-button>
                  </el-tooltip>
                  <el-tooltip
                    :content="t('user.resetPassword')"
                    placement="top"
                    v-if="scope.row.userId !== 1"
                  >
                    <el-button
                      link
                      type="primary"
                      icon="Key"
                      @click="handleResetPwd(scope.row)"
                      v-hasPermi="['system:user:resetPwd']"
                    >
                      {{ t("user.resetPassword") }}
                    </el-button>
                  </el-tooltip>
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
          </el-col>
        </pane>
      </splitpanes>
    </el-row>

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="userRef" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item
              v-if="form.userId == undefined"
              :label="t('user.userName')"
              prop="userName"
            >
              <el-input
                v-model="form.userName"
                :placeholder="t('user.enterUserName')"
                maxlength="30"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              v-if="form.userId == undefined"
              :label="t('user.password')"
              prop="password"
            >
              <el-input
                v-model="form.password"
                :placeholder="t('user.enterPassword')"
                type="password"
                maxlength="20"
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="t('user.phoneNumber')" prop="phonenumber">
              <el-input
                v-model="form.phonenumber"
                :placeholder="t('user.enterPhoneNumber')"
                maxlength="11"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('user.email')" prop="email">
              <el-input
                v-model="form.email"
                :placeholder="t('user.enterEmail')"
                maxlength="50"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="t('user.role')">
              <el-select
                v-model="form.roleIds"
                multiple
                :placeholder="t('user.selectRole')"
              >
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                  :disabled="item.status == 1"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('user.status')">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :value="dict.value"
                >
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="t('user.bindAgent')" prop="agentUser">
              <el-input
                v-model="form.agentUser"
                :placeholder="t('user.enterBindAgent')"
                maxlength="30"
              /> </el-form-item
          ></el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="t('user.remark')">
              <el-input
                v-model="form.remark"
                type="textarea"
                :placeholder="t('user.enterRemark')"
              ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">
            {{ t("common.confirm") }}
          </el-button>
          <el-button @click="cancel">{{ t("common.cancel") }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <el-dialog
      :title="upload.title"
      v-model="upload.open"
      width="400px"
      append-to-body
    >
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          {{ t("user.dragOrClickUpload") }}
        </div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport">
                {{ t("user.updateExisting") }}
              </el-checkbox>
            </div>
            <span>{{ t("user.fileFormat") }}</span>
            <el-link
              type="primary"
              :underline="false"
              style="font-size: 12px; vertical-align: baseline"
              @click="importTemplate"
            >
              {{ t("user.downloadTemplate") }}
            </el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm">
            {{ t("common.confirm") }}
          </el-button>
          <el-button @click="upload.open = false">
            {{ t("common.cancel") }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="User">
import { getToken } from "@/utils/auth";
import useAppStore from "@/store/modules/app";
import {
  changeUserStatus,
  listUser,
  resetUserPwd,
  delUser,
  getUser,
  updateUser,
  addUser,
  deptTreeSelect,
} from "@/api/system/user";
import { Splitpanes, Pane } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import { ref, reactive, toRefs, onMounted, computed, watch } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";

const router = useRouter();
const { t } = useI18n();
const appStore = useAppStore();
const { proxy } = getCurrentInstance();
const { sys_normal_disable, sys_user_sex } = proxy.useDict(
  "sys_normal_disable",
  "sys_user_sex"
);

const userList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const deptName = ref("");
const deptOptions = ref(undefined);
const enabledDeptOptions = ref(undefined);
const initPassword = ref(undefined);
const postOptions = ref([]);
const roleOptions = ref([]);
const tableKey = ref(Date.now());

const config = window.APP_CONFIG;

/*** 用户导入参数 */
const upload = reactive({
  open: false,
  title: t("user.importTitle"),
  isUploading: false,
  updateSupport: 0,
  headers: { Authorization: "Bearer " + getToken() },
  url: config.baseApiUrl + "/system/user/importData",
});

const columns = ref([
  { key: "0", label: t("user.userId"), visible: true, prop: "userId" },
  {
    key: "1",
    label: t("user.userName"),
    visible: true,
    prop: "userName",
    showOverflowTooltip: true,
  },
  {
    key: "2",
    label: t("user.agentName"),
    visible: true,
    prop: "agentUser",
    width: "180",
  },
  {
    key: "3",
    label: t("user.roleName"),
    visible: true,
    prop: "roleName",
    width: "200",
  },
  { key: "4", label: t("user.ip"), visible: true, prop: null },
  { key: "5", label: t("user.loginTime"), visible: true, prop: null },
  {
    key: "6",
    label: t("user.status"),
    visible: true,
    prop: "status",
    width: "100",
  },
  {
    key: "7",
    label: t("user.action"),
    visible: true,
    prop: null,
    width: "300",
    fixed: "right",
    className: "small-padding fixed-width",
  },
  {
    key: "8",
    label: t("user.createTime"),
    visible: true,
    prop: "createTime",
    width: "160",
  },
]);

const filteredColumns = computed(() => {
  return columns.value.filter((column) => column.visible);
});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userName: undefined,
    phonenumber: undefined,
    status: undefined,
    deptId: undefined,
  },
  rules: {
    userName: [
      { required: true, message: t("user.usernameRequired"), trigger: "blur" },
      {
        min: 2,
        max: 20,
        message: t("user.usernameLength"),
        trigger: "blur",
      },
    ],
    nickName: [
      { required: true, message: t("user.nicknameRequired"), trigger: "blur" },
    ],
    password: [
      { required: true, message: t("user.passwordRequired"), trigger: "blur" },
      {
        min: 5,
        max: 20,
        message: t("user.passwordLength"),
        trigger: "blur",
      },
      {
        pattern: /^[^<>"'|\\]+$/,
        message: t("user.invalidPasswordChars"),
        trigger: "blur",
      },
    ],
    email: [
      {
        type: "email",
        message: t("user.invalidEmail"),
        trigger: ["blur", "change"],
      },
    ],
    phonenumber: [
      {
        pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
        message: t("user.invalidPhone"),
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

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

/** 通过条件过滤节点 */
const filterNode = (value, data) => {
  if (!value) return true;
  return data.label.indexOf(value) !== -1;
};

/** 根据名称筛选部门树 */
watch(deptName, (val) => {
  proxy.$refs["deptTreeRef"].filter(val);
});

/** 查询用户列表 */
function getList() {
  loading.value = true;
  listUser(proxy.addDateRange(queryParams.value, dateRange.value)).then(
    (res) => {
      loading.value = false;
      userList.value = res.rows;
      console.log(res);
      total.value = res.total;
    }
  );
}

/** 查询部门下拉树结构 */
function getDeptTree() {
  deptTreeSelect().then((response) => {
    deptOptions.value = response.data;
    enabledDeptOptions.value = filterDisabledDept(
      JSON.parse(JSON.stringify(response.data))
    );
  });
}

/** 过滤禁用的部门 */
function filterDisabledDept(deptList) {
  return deptList.filter((dept) => {
    if (dept.disabled) {
      return false;
    }
    if (dept.children && dept.children.length) {
      dept.children = filterDisabledDept(dept.children);
    }
    return true;
  });
}

/** 节点单击事件 */
function handleNodeClick(data) {
  queryParams.value.deptId = data.id;
  handleQuery();
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
  queryParams.value.deptId = undefined;
  proxy.$refs.deptTreeRef.setCurrentKey(null);
  handleQuery();
}

/** 删除按钮操作 */
function handleDelete(row) {
  const userIds = row.userId || ids.value;
  proxy.$modal
    .confirm(t("user.deleteConfirm", { userIds: userIds }))
    .then(function () {
      return delUser(userIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("user.deleteSuccess"));
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "system/user/export",
    {
      ...queryParams.value,
    },
    `user_${new Date().getTime()}.xlsx`
  );
}

/** 用户状态修改 */
function handleStatusChange(row) {
  let text = row.status === "0" ? t("user.enable") : t("user.disable");
  proxy.$modal
    .confirm(t("user.statusConfirm", { action: text, userName: row.userName }))
    .then(function () {
      return changeUserStatus(row.userId, row.status);
    })
    .then(() => {
      proxy.$modal.msgSuccess(t("user.statusSuccess", { action: text }));
    })
    .catch(function () {
      row.status = row.status === "0" ? "1" : "0";
    });
}

/** 跳转角色分配 */
function handleAuthRole(row) {
  const userId = row.userId;
  router.push("/system/user-auth/role/" + userId);
}

/** 重置密码按钮操作 */
function handleResetPwd(row) {
  proxy
    .$prompt(
      t("user.resetPwdPrompt", { userName: row.userName }),
      t("user.prompt"),
      {
        confirmButtonText: t("common.confirm"),
        cancelButtonText: t("common.cancel"),
        closeOnClickModal: false,
        inputPattern: /^.{5,20}$/,
        inputErrorMessage: t("user.passwordLength"),
        inputValidator: (value) => {
          if (/<|>|"|'|\||\\/.test(value)) {
            return t("user.invalidPasswordChars");
          }
        },
      }
    )
    .then(({ value }) => {
      resetUserPwd(row.userId, value).then((response) => {
        proxy.$modal.msgSuccess(t("user.resetPwdSuccess", { password: value }));
      });
    })
    .catch(() => {});
}

/** 选择条数 */
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.userId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 导入按钮操作 */
function handleImport() {
  upload.title = t("user.importTitle");
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    "system/user/importTemplate",
    {},
    `user_template_${new Date().getTime()}.xlsx`
  );
}

/** 文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true;
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].handleRemove(file);
  proxy.$alert(
    "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
      response.msg +
      "</div>",
    t("user.importResult"),
    { dangerouslyUseHTMLString: true }
  );
  getList();
};

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
}

/** 重置操作表单 */
function reset() {
  form.value = {
    userId: undefined,
    deptId: undefined,
    userName: undefined,
    nickName: undefined,
    password: undefined,
    phonenumber: undefined,
    email: undefined,
    sex: undefined,
    status: "0",
    remark: undefined,
    postIds: [],
    roleIds: [],
    agentUser: null,
    agentSwitch: "0",
  };
  proxy.resetForm("userRef");
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  getUser().then((response) => {
    postOptions.value = response.posts;
    roleOptions.value = response.roles;
    open.value = true;
    title.value = t("user.addUser");
    form.value.password = initPassword.value;
  });
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const userId = row.userId || ids.value;
  getUser(userId).then((response) => {
    form.value = response.data;
    postOptions.value = response.posts;
    roleOptions.value = response.roles;
    form.value.postIds = response.postIds;
    form.value.roleIds = response.roleIds;
    open.value = true;
    title.value = t("user.editUser");
    form.value.password = "";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["userRef"].validate((valid) => {
    if (valid) {
      if (form.value.userId != undefined) {
        updateUser(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("user.editSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addUser(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("user.addSuccess"));
          open.value = false;
          getList();
        });
      }
    }
  });
}

onMounted(() => {
  getDeptTree();
  getList();
  proxy.getConfigKey("sys.user.initPassword").then((response) => {
    initPassword.value = response.msg;
  });
});
</script>
