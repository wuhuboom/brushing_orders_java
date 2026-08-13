<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="用户名" prop="username">
          <el-input
              v-model="queryParams.username"
              placeholder="请输入用户名"
              clearable
              @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="IP" prop="ip">
          <el-input
              v-model="queryParams.ip"
              placeholder="请输入IP"
              clearable
              @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="地点" prop="location">
          <el-input
              v-model="queryParams.location"
              placeholder="请输入地点"
              clearable
              @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="登录状态" prop="status">
          <el-select style="width:200px" v-model="queryParams.status" placeholder="请选择登录状态" clearable>
            <el-option
                v-for="dict in sys_common_status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
            />
          </el-select>
        </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>



    <el-table v-loading="loading" :data="memberlogList" @selection-change="handleSelectionChange" :border="true">
      <el-table-column type="selection" width="55" align="center" />
              <el-table-column label="ID" align="center" prop="id" />
              <el-table-column label="用户名" align="center" prop="username" />
              <el-table-column label="IP" align="center" prop="ip" />
              <el-table-column label="地点" align="center" prop="location" />
              <el-table-column label="登录域名" align="center" prop="loginDomain" />
              <el-table-column label="浏览器" align="center" prop="browser" />
              <el-table-column label="操作系统" align="center" prop="os" />
              <el-table-column label="登录状态" align="center" prop="status">
                <template #default="scope">
                      <dict-tag :options="sys_common_status" :value="scope.row.status"/>
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
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <!-- 添加或修改会员登录日志对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="memberlogRef" :model="form" :rules="rules" label-width="80px">
                        <el-form-item label="用户id" prop="userId">
                          <el-input v-model="form.userId" placeholder="请输入用户id" />
                        </el-form-item>
                        <el-form-item label="IP" prop="ip">
                          <el-input v-model="form.ip" placeholder="请输入IP" />
                        </el-form-item>
                        <el-form-item label="地点" prop="location">
                          <el-input v-model="form.location" placeholder="请输入地点" />
                        </el-form-item>
                        <el-form-item label="登录域名" prop="loginDomain">
                          <el-input v-model="form.loginDomain" placeholder="请输入登录域名" />
                        </el-form-item>
                        <el-form-item label="浏览器" prop="browser">
                          <el-input v-model="form.browser" placeholder="请输入浏览器" />
                        </el-form-item>
                        <el-form-item label="操作系统" prop="os">
                          <el-input v-model="form.os" placeholder="请输入操作系统" />
                        </el-form-item>
                        <el-form-item label="登录状态" prop="status">
                          <el-radio-group v-model="form.status">
                            <el-radio
                                v-for="dict in sys_common_status"
                                :key="dict.value"
                                :label="dict.value"
                            >{{dict.label}}</el-radio>
                          </el-radio-group>
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

<script setup name="Memberlog">
  import { listMemberlog, getMemberlog, delMemberlog, addMemberlog, updateMemberlog } from "@/api/member/memberlog";

  const { proxy } = getCurrentInstance();
      const { sys_common_status } = proxy.useDict('sys_common_status');

  const memberlogList = ref([]);
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
      username: null,
      ip: null,
      location: null,
      loginDomain: null,
      browser: null,
      os: null,
      status: null,
    },
    rules: {
                    userId: [
                { required: true, message: "用户id不能为空", trigger: "blur" }
              ],
                    ip: [
                { required: true, message: "IP不能为空", trigger: "blur" }
              ],
                    status: [
                { required: true, message: "登录状态不能为空", trigger: "change" }
              ],
                    createTime: [
                { required: true, message: "登录时间不能为空", trigger: "blur" }
              ]
    }
  });

  const { queryParams, form, rules } = toRefs(data);

  /** 查询会员登录日志列表 */
  function getList() {
    loading.value = true;
    listMemberlog(queryParams.value).then(response => {
            memberlogList.value = response.rows;
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
                    userId: null,
                    ip: null,
                    location: null,
                    loginDomain: null,
                    browser: null,
                    os: null,
                    status: null,
                    createTime: null
    };
    proxy.resetForm("memberlogRef");
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
    ids.value = selection.map(item => item.id);
    single.value = selection.length != 1;
    multiple.value = !selection.length;
  }

  /** 新增按钮操作 */
  function handleAdd() {
    reset();
    open.value = true;
    title.value = "添加会员登录日志";
  }

  /** 修改按钮操作 */
  function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value
    getMemberlog(_id).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改会员登录日志";
    });
  }

  /** 提交按钮 */
  function submitForm() {
    proxy.$refs["memberlogRef"].validate(valid => {
      if (valid) {
        if (form.value.id != null) {
          updateMemberlog(form.value).then(response => {
            proxy.$modal.msgSuccess("修改成功");
            open.value = false;
            getList();
          });
        } else {
          addMemberlog(form.value).then(response => {
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
    proxy.$modal.confirm('是否确认删除会员登录日志编号为"' + _ids + '"的数据项？').then(function() {
      return delMemberlog(_ids);
    }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    }).catch(() => {});
  }

  /** 导出按钮操作 */
  function handleExport() {
    proxy.download('member/memberlog/export', {
      ...queryParams.value
    }, `memberlog_${new Date().getTime()}.xlsx`)
  }

  getList();
</script>
