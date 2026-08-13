<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item :label="$t('member.template.name')" prop="name">
          <el-input
              v-model="queryParams.name"
              :placeholder="$t('member.template.namePlaceholder')"
              clearable
              @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item :label="$t('member.template.description')" prop="description">
          <el-input
              v-model="queryParams.description"
              :placeholder="$t('member.template.descriptionPlaceholder')"
              clearable
              @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item :label="$t('member.template.status')" prop="status">
          <el-select v-model="queryParams.status" :placeholder="$t('member.template.statusPlaceholder')" clearable style="width:200px">
            <el-option
                v-for="dict in sys_normal_disable"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
            />
          </el-select>
        </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">{{ $t('member.template.search') }}</el-button>
        <el-button icon="Refresh" @click="resetQuery">{{ $t('member.template.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['member:template:add']"
        >{{ $t('member.template.add') }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['member:template:edit']"
        >{{ $t('member.template.edit') }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['member:template:remove']"
        >{{ $t('member.template.remove') }}</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange" :border="true">
      <el-table-column type="selection" width="55" align="center" />
              <el-table-column :label="$t('member.template.id')" align="center" prop="id" />
              <el-table-column :label="$t('member.template.name')" align="center" prop="name" />
              <el-table-column :label="$t('member.template.description')" align="center" prop="description" />
              <el-table-column :label="$t('member.template.taskCount')" align="center" prop="taskCount" />
              <el-table-column :label="$t('member.template.status')" align="center" prop="status">
                <template #default="scope">
                      <dict-tag :options="sys_normal_disable" :value="scope.row.status"/>
                </template>
              </el-table-column>
                    <el-table-column :label="$t('member.template.sort')" align="center" prop="sort" />
                  <el-table-column :label="$t('member.template.operation')" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
         <el-button style="width:80px"  type="success" size="small"  @click="openTaskDrawer(scope.row)" v-hasPermi="['member:template:edit']">{{ $t('member.template.taskManage') }}</el-button>
          <el-button style="width:60px"  type="primary" size="small"  @click="handleUpdate(scope.row)" v-hasPermi="['member:template:edit']">{{ $t('member.template.edit') }}</el-button>
          <el-button
            style="width:60px"
            :type="scope.row.status === '0' ? 'warning' : 'success'"
            size="small"
            @click="handleToggleStatus(scope.row)"
            v-hasPermi="['member:template:edit']"
          >
            {{ scope.row.status === '0' ? $t('member.template.disable') : $t('member.template.enable') }}
          </el-button>
          <el-button style="width:60px"  type="danger" size="small"  @click="handleDelete(scope.row)" v-hasPermi="['member:template:remove']">{{ $t('member.template.remove') }}</el-button>
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

    <!-- 添加或修改连单模板对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="templateRef" :model="form" :rules="rules" label-width="80px">
          <el-form-item :label="$t('member.template.name')" prop="name">
            <el-input v-model="form.name" :placeholder="$t('member.template.namePlaceholder')" />
          </el-form-item>
          <el-form-item :label="$t('member.template.description')" prop="description">
            <el-input v-model="form.description" :placeholder="$t('member.template.descriptionPlaceholder')" />
          </el-form-item>

          <el-form-item :label="$t('member.template.status')" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
              >{{dict.label}}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="$t('member.template.sort')" prop="sort">
            <el-input v-model="form.sort" :placeholder="$t('member.template.sortPlaceholder')" />
          </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ $t('common.confirm') }}</el-button>
          <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 任务管理抽屉 -->
    <el-drawer :title="$t('member.template.taskManageTitle')" v-model="taskDrawerOpen" direction="rtl" size="90%" append-to-body>
      <div style="padding: 12px;">
        <!-- reuse templateInfo list content -->
        <TemplateInfo :template-id="currentTemplateId" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="Template">
  import { listTemplate, getTemplate, delTemplate, addTemplate, updateTemplate } from "@/api/member/template";
  import TemplateInfo from '@/views/member/templateInfo/index.vue';

  const { proxy } = getCurrentInstance();
      const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

  const templateList = ref([]);
  const open = ref(false);
  const loading = ref(true);
  const showSearch = ref(true);
  const taskDrawerOpen = ref(false);
  const currentTemplateId = ref(null);
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
      name: null,
      description: null,
      taskCount: null,
      status: null,
      sort: null,
    },
    rules: {
          name: [
      { required: true, message: proxy.$t('member.template.nameRequired'), trigger: 'blur' }
    ],
          status: [
      { required: true, message: proxy.$t('member.template.statusRequired'), trigger: 'change' }
    ]
    }
  });

  const { queryParams, form, rules } = toRefs(data);

  /** 查询连单模板列表 */
  function getList() {
    loading.value = true;
    listTemplate(queryParams.value).then(response => {
            templateList.value = response.rows;
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
      name: null,
      description: null,
      taskCount: null,
      status: "0",
      sort: null,
      createTime: null,
      createBy: null
    };
    proxy.resetForm("templateRef");
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
    title.value = proxy.$t('member.template.addTitle');
  }

  /** 修改按钮操作 */
  function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value
    getTemplate(_id).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = proxy.$t('member.template.editTitle');
    });
  }

  // 切换状态（启用/禁用）
  function handleToggleStatus(row) {
    const id = row.id || row;
    const currentStatus = String(row.status ?? '0');
    // 按需求：当 status 为 '0' 时显示“禁用”，为 '1' 时显示“启用"
    const targetStatus = currentStatus === '0' ? '1' : '0';
    const actionText = currentStatus === '0' ? proxy.$t('member.template.disable') : proxy.$t('member.template.enable');
    proxy.$modal
      .confirm(proxy.$t('member.template.confirmToggle', { action: actionText, id }))
      .then(() => {
        return updateTemplate({ id: id, status: targetStatus });
      })
      .then(() => {
        proxy.$modal.msgSuccess(proxy.$t('member.template.toggleSuccess', { action: actionText }));
        getList();
      })
      .catch(() => {});
  }

  // 打开任务管理抽屉
  function openTaskDrawer(row) {
    try {
      console.log('openTaskDrawer called, row=', row);
    } catch (e) {}
    const id = row && (row.id ?? row) ? (row.id ?? row) : null;
    currentTemplateId.value = id;
    taskDrawerOpen.value = true;
  }

  /** 提交按钮 */
  function submitForm() {
    proxy.$refs["templateRef"].validate(valid => {
      if (valid) {
        if (form.value.id != null) {
          updateTemplate(form.value).then(response => {
            proxy.$modal.msgSuccess(proxy.$t('member.template.editSuccess'));
            open.value = false;
            getList();
          });
        } else {
          addTemplate(form.value).then(response => {
            proxy.$modal.msgSuccess(proxy.$t('member.template.addSuccess'));
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
    proxy.$modal.confirm(proxy.$t('member.template.deleteConfirm', { ids: _ids })).then(function() {
      return delTemplate(_ids);
    }).then(() => {
      getList();
      proxy.$modal.msgSuccess(proxy.$t('member.template.deleteSuccess'));
    }).catch(() => {});
  }

  /** 导出按钮操作 */
  function handleExport() {
    proxy.download('member/template/export', {
      ...queryParams.value
    }, `template_${new Date().getTime()}.xlsx`)
  }

  getList();
</script>
