<template>
  <div class="app-container">
   <!--  <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['member:field:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['member:field:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['member:field:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row> -->

    <el-table v-loading="loading" :data="fieldList" @selection-change="handleSelectionChange" :border="true">
      <el-table-column type="selection" width="55" align="center" />
              <el-table-column label="ID" align="center" prop="id" />
              <el-table-column label="表名" align="center" prop="tableName" />
              <el-table-column label="字段名" align="center" prop="fieldName" />
              <el-table-column label="类型" align="center" prop="type" />
              <el-table-column label="状态" align="center">
        <template #default="scope">
          <el-tag
            :type="scope.row.status == '0' ? 'success' : 'danger'"
            effect="plain"
            class="status-tag"
            @click="toggleStatus(scope.row)"
            style="cursor: pointer;"
          >
            {{ scope.row.status == '0' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
   <!--     <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button circle type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['member:field:edit']"></el-button>
          <el-button circle type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['member:field:remove']"></el-button>
        </template>
      </el-table-column> -->
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <!-- 添加或修改字段设置对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="fieldRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" placeholder="请输入表名" />
        </el-form-item>
        <el-form-item label="字段名" prop="fieldName">
          <el-input v-model="form.fieldName" placeholder="请输入字段名" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type"  style="width: 240px">
            <el-option
              key="user-phone"
              label="用户电话号码"
              value="user-phone"
            />
          </el-select>
         </el-form-item>
        <el-form-item label="状态" prop="fieldName">
           <el-switch
              v-model="form.status"
              class="ml-2"
               active-value="0"
              inactive-value="1"
              style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
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

<script setup name="Field">
  import { listField, getField, delField, addField, updateField } from "@/api/member/field";

  const { proxy } = getCurrentInstance();

  const fieldList = ref([]);
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
                    tableName: null,
                    fieldName: null,
                    type: null,
                    status: null,
    },
    rules: {
                    tableName: [
                { required: true, message: "表名不能为空", trigger: "blur" }
              ],
                    fieldName: [
                { required: true, message: "字段名不能为空", trigger: "blur" }
              ],
                    type: [
                { required: true, message: "类型不能为空", trigger: "change" }
              ],
              status: [
                { required: true, message: "状态不能为空", trigger: "change" }
              ],

    }
  });

  const { queryParams, form, rules } = toRefs(data);

  /** 查询字段设置列表 */
  function getList() {
    loading.value = true;
    listField(queryParams.value).then(response => {
            fieldList.value = response.rows;
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
                    tableName: null,
                    fieldName: null,
                    type: null,
                    status: null,
                    createTime: null
    };
    proxy.resetForm("fieldRef");
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
    title.value = "添加字段设置";
  }

  /** 修改按钮操作 */
  function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value
    getField(_id).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改字段设置";
    });
  }

  /** 提交按钮 */
  function submitForm() {
    proxy.$refs["fieldRef"].validate(valid => {
      if (valid) {
        if (form.value.id != null) {
          updateField(form.value).then(response => {
            proxy.$modal.msgSuccess("修改成功");
            open.value = false;
            getList();
          });
        } else {
          addField(form.value).then(response => {
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
    proxy.$modal.confirm('是否确认删除字段设置编号为"' + _ids + '"的数据项？').then(function() {
      return delField(_ids);
    }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    }).catch(() => {});
  }

  /** 导出按钮操作 */
  function handleExport() {
    proxy.download('member/field/export', {
      ...queryParams.value
    }, `field_${new Date().getTime()}.xlsx`)
  }

  /** 切换状态并确认 */
  function toggleStatus(row) {
    const targetStatus = row.status == '0' ? '1' : '0';
    proxy.$modal.confirm('是否确认将字段设置编号为"' + row.id + '"的状态修改为"' + (targetStatus == '0' ? '启用' : '禁用') + '"？').then(function() {
      return updateField({ id: row.id, status: targetStatus });
    }).then(() => {
      proxy.$modal.msgSuccess('状态修改成功');
      getList();
    }).catch(() => {});
  }

  getList();
</script>
