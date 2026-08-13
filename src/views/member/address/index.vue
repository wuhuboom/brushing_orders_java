<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item :label="$t('member.address.name')" prop="name">
        <el-input
            v-model="queryParams.name"
            :placeholder="$t('member.address.enterName')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.address.url')" prop="url">
        <el-input
            v-model="queryParams.url"
            :placeholder="$t('member.address.enterUrl')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">{{ $t('common.search') }}</el-button>
        <el-button icon="Refresh" @click="resetQuery">{{ $t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['member:address:add']"
        >{{ $t('common.add') }}
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['member:address:edit']"
        >{{ $t('common.edit') }}
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['member:address:remove']"
        >{{ $t('common.delete') }}
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="addressList" @selection-change="handleSelectionChange" :border="true">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column :label="$t('member.address.id')" align="center" prop="id"/>
      <el-table-column :label="$t('member.address.name')" align="center" prop="name"/>
      <el-table-column :label="$t('member.address.url')" align="center" prop="url"/>
      <el-table-column :label="$t('member.address.status')" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_notice_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column :label="$t('member.address.sort')" align="center" prop="sort"/>
      <el-table-column :label="$t('member.address.operation')" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['member:address:edit']">
            {{ $t('common.edit') }}
          </el-button>
          <el-button size="mini" type="warning" @click="toggleStatus(scope.row)" v-hasPermi="['member:address:edit']">
            {{ scope.row.status == '0' ? $t('member.address.disable') : $t('member.address.enable') }}
          </el-button>
          <el-button type="danger" @click="handleDelete(scope.row)" v-hasPermi="['member:address:remove']">
            {{ $t('common.delete') }}
          </el-button>
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

    <!-- 添加或修改充值地址对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="addressRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item :label="$t('member.address.name')" prop="name">
          <el-input v-model="form.name" :placeholder="$t('member.address.enterName')"/>
        </el-form-item>
        <el-form-item :label="$t('member.address.url')" prop="url">
          <el-input v-model="form.url" :placeholder="$t('member.address.enterUrl')"/>
        </el-form-item>
        <el-form-item :label="$t('member.address.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
                v-for="dict in sys_notice_status"
                :key="dict.value"
                :label="dict.value"
            >{{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('member.address.sort')" prop="sort">
          <el-input v-model="form.sort" :placeholder="$t('member.address.enterSort')"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ $t('common.confirm') }}</el-button>
          <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Address">
import {
  listAddress,
  getAddress,
  delAddress,
  addAddress,
  updateAddress,
  updateAddressStatus
} from "@/api/member/address";

const {proxy} = getCurrentInstance();
const {sys_notice_status} = proxy.useDict('sys_notice_status');

const addressList = ref([]);
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
    name: null,
    url: null,
    status: null,
    sort: null,
  },
  rules: {
    name: [
      {required: true, message: proxy.$t('member.address.nameRequired'), trigger: "blur"}
    ],
    url: [
      {required: true, message: proxy.$t('member.address.urlRequired'), trigger: "blur"}
    ],
    status: [
      {required: true, message: proxy.$t('member.address.statusRequired'), trigger: "change"}
    ],
    sort: [
      {required: true, message: proxy.$t('member.address.sortRequired'), trigger: "blur"}
    ],
    createTime: [
      {required: true, message: proxy.$t('member.address.createTimeRequired'), trigger: "blur"}
    ]
  }
});

const {queryParams, form, rules} = toRefs(data);

/** 查询充值地址列表 */
function getList() {
  loading.value = true;
  listAddress(queryParams.value).then(response => {
    addressList.value = response.rows;
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
    url: null,
    status: null,
    sort: null,
    createTime: null
  };
  proxy.resetForm("addressRef");
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
  title.value = proxy.$t('member.address.addAddress');
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getAddress(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = proxy.$t('member.address.editAddress');
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["addressRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAddress(form.value).then(response => {
          proxy.$modal.msgSuccess(proxy.$t('member.address.updateSuccess'));
          open.value = false;
          getList();
        });
      } else {
        addAddress(form.value).then(response => {
          proxy.$modal.msgSuccess(proxy.$t('member.address.addSuccess'));
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
  proxy.$modal.confirm(proxy.$t('member.address.deleteConfirm', {ids: _ids})).then(function () {
    return delAddress(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess(proxy.$t('member.address.deleteSuccess'));
  }).catch(() => {
  });
}

/** 启用/禁用按钮操作 */
function toggleStatus(row) {
  const id = row.id || ids.value;
  const isZero = String(row.status) === '0';
  const actionText = isZero ? proxy.$t('member.address.disable') : proxy.$t('member.address.enable');
  const newStatus = isZero ? 1 : 0;
  proxy.$modal.confirm(proxy.$t('member.address.confirmToggle', {action: actionText, id})).then(function () {
    return updateAddress({id: id, status: newStatus});
  }).then(() => {
    proxy.$modal.msgSuccess(proxy.$t('member.address.toggleSuccess', {action: actionText}));
    getList();
  }).catch(() => {
  });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('member/address/export', {
    ...queryParams.value
  }, `address_${new Date().getTime()}.xlsx`)
}

getList();
</script>
