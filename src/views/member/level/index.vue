<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="等级列表"
      :columns="levelColumns"
      :data-source="levelList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="名称">
                <a-input v-model:value="queryParams.name" allow-clear placeholder="请输入名称" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="7">
              <a-form-item label="级别">
                <a-input v-model:value="queryParams.level" allow-clear placeholder="请输入级别" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col flex="auto" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-button type="primary" :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:level:remove']">
          <DeleteOutlined />
          删除
        </a-button>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:level:add']">
          <PlusOutlined />
          创建
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'icon'">
          <image-preview :src="record.icon" :width="50" :height="50" />
        </template>
        <template v-else-if="column.key === 'productMatch'">
          <a
            v-if="record.productMatchEnabled == '0'"
            class="level-match-link level-match-link-danger"
            role="button"
            @click="openProductMatchDialog(record)"
          >禁用</a>
          <a v-else class="level-match-link" role="button" @click="openProductMatchDialog(record)">
            启用 : {{ record.productMatchMin }}% - {{ record.productMatchMax }}%
          </a>
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="8">
            <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:level:edit']">修改</a-button>
            <a-button type="link" @click="openTranslationDialog(record)" v-hasPermi="['member:level:edit']">国际化</a-button>
            <a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:level:add']">复制</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal v-model:open="open" :title="title" width="860px" destroy-on-close @ok="submitForm" @cancel="cancel">
      <a-form ref="levelRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="[20, 0]">
          <a-col v-for="item in levelFormItems" :key="item.prop" :span="item.span || 8">
            <a-form-item :label="item.label" :name="item.prop">
              <a-input
                v-if="item.type === 'input'"
                v-model:value="form[item.prop]"
                :placeholder="item.placeholder"
                allow-clear
              />
              <a-input-number
                v-else-if="item.type === 'number'"
                v-model:value="form[item.prop]"
                :min="item.min ?? 0"
                :max="item.max"
                :placeholder="item.placeholder"
                class="full-width"
              />
              <image-upload v-else-if="item.type === 'image'" v-model="form[item.prop]" :limit="1" />
              <editor v-else-if="item.type === 'editor'" v-model="form[item.prop]" :min-height="192" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="productMatchDialogVisible"
      :title="productMatchTitle"
      width="500px"
      destroy-on-close
      @ok="updateProductMatch"
      @cancel="closeProductMatchDialog"
    >
      <a-form ref="productMatchForRef" :model="productMatchForm" layout="vertical">
        <a-form-item label="产品匹配" name="productMatchEnabled">
          <a-radio-group v-model:value="productMatchForm.productMatchEnabled">
            <a-radio value="1">启用</a-radio>
            <a-radio value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="最小匹配值" name="productMatchMin">
          <a-input-number
            v-model:value="productMatchForm.productMatchMin"
            :min="0"
            :max="100"
            placeholder="请输入最小匹配值"
            class="full-width"
          />
        </a-form-item>
        <a-form-item label="最大匹配值" name="productMatchMax">
          <a-input-number
            v-model:value="productMatchForm.productMatchMax"
            :min="0"
            :max="100"
            placeholder="请输入最大匹配值"
            class="full-width"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <translation-dialog
      v-model="translationOpen"
      :title="translationTitle"
      :translations="translationForm"
      type="level"
      @submit="submitTranslations"
    />
  </div>
</template>

<script setup name="Level">
import {
  listLevel,
  getLevel,
  delLevel,
  addLevel,
  updateLevel,
} from "@/api/member/level";
import { DeleteOutlined, PlusOutlined } from "@ant-design/icons-vue";
import TranslationDialog from "@/views/member/components/TranslationDrawer.vue";
import { createEmptyTranslations } from "@/views/member/components/translationLanguages";

const { proxy } = getCurrentInstance();

const levelList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const productMatchDialogVisible = ref(false);
const productMatchTitle = ref("");
const translationOpen = ref(false);
const translationTitle = ref("");
const translationForm = ref(createEmptyTranslations());
const currentTranslationRow = ref(null);

const data = reactive({
  form: {},
  productMatchForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    level: null,
  },
  rules: {
    name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
    level: [{ required: true, message: "级别不能为空", trigger: "blur" }],
    icon: [{ required: true, message: "图标不能为空", trigger: "blur" }],
    price: [
      { required: true, message: "会员等级价格不能为空", trigger: "blur" },
    ],
    minBalance: [
      { required: true, message: "最低余额不能为空", trigger: "blur" },
    ],
    inviteCount: [
      {
        required: true,
        message: "自动升级所需邀请人数不能为空",
        trigger: "blur",
      },
    ],
    orderCountPerDay: [
      { required: true, message: "接单次数/天不能为空", trigger: "blur" },
    ],
    minCommissionRate: [
      { required: true, message: "最低返佣百分比不能为空", trigger: "blur" },
    ],
    maxCommissionRate: [
      { required: true, message: "最高返佣百分比不能为空", trigger: "blur" },
    ],
    minContinuousCommissionRate: [
      {
        required: true,
        message: "最低连单返佣百分比不能为空",
        trigger: "blur",
      },
    ],
    maxContinuousCommissionRate: [
      {
        required: true,
        message: "最高连单返佣百分比不能为空",
        trigger: "blur",
      },
    ],
    taskCountPerDay: [
      { required: true, message: "任务完成组数/天不能为空", trigger: "blur" },
    ],
    withdrawCountPerDay: [
      { required: true, message: "提现次数/天不能为空", trigger: "blur" },
    ],
    withdrawFeeRate: [
      { required: true, message: "提现手续费率不能为空", trigger: "blur" },
    ],
    minWithdrawAmount: [
      { required: true, message: "提现最低单数不能为空", trigger: "blur" },
    ],
    withdrawLimitPerDay: [
      { required: true, message: "提现限额/天不能为空", trigger: "blur" },
    ],
    minWithdraw: [
      { required: true, message: "最低提现金额不能为空", trigger: "blur" },
    ],
    maxWithdraw: [
      { required: true, message: "最高提现金额不能为空", trigger: "blur" },
    ],
    description: [{ required: true, message: "描述不能为空", trigger: "blur" }],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules, productMatchForm } = toRefs(data);

const levelColumns = [
  { title: "ID", dataIndex: "id", key: "id", width: 80 },
  { title: "名称", dataIndex: "name", key: "name" },
  { title: "级别", dataIndex: "level", key: "level", width: 120 },
  { title: "图标", dataIndex: "icon", key: "icon", width: 100 },
  { title: "价格", dataIndex: "price", key: "price", width: 120 },
  { title: "产品匹配", key: "productMatch", width: 220 },
  { title: "最低余额", dataIndex: "minBalance", key: "minBalance", width: 140 },
  { title: "自动升级所需邀请人数", dataIndex: "inviteCount", key: "inviteCount", width: 210 },
  { title: "操作", key: "operation", width: 220 },
];

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows),
}));

const levelFormItems = [
  { prop: "name", label: "名称", type: "input", placeholder: "请输入名称" },
  { prop: "level", label: "级别", type: "number", min: 1, placeholder: "请输入级别" },
  { prop: "icon", label: "图标", type: "image" },
  { prop: "price", label: "会员等级价格", type: "number", placeholder: "请输入会员等级价格" },
  { prop: "minBalance", label: "最低余额", type: "number", placeholder: "请输入最低余额" },
  { prop: "inviteCount", label: "自动升级所需邀请人数", type: "number", placeholder: "请输入自动升级所需邀请人数" },
  { prop: "orderCountPerDay", label: "接单次数/天", type: "number", placeholder: "请输入接单次数/天" },
  { prop: "minCommissionRate", label: "最低返佣百分比", type: "number", max: 100, placeholder: "请输入最低返佣百分比" },
  { prop: "maxCommissionRate", label: "最高返佣百分比", type: "number", max: 100, placeholder: "请输入最高返佣百分比" },
  { prop: "minContinuousCommissionRate", label: "最低连单返佣百分比", type: "number", max: 100, placeholder: "请输入最低连单返佣百分比" },
  { prop: "maxContinuousCommissionRate", label: "最高连单返佣百分比", type: "number", max: 100, placeholder: "请输入最高连单返佣百分比" },
  { prop: "taskCountPerDay", label: "任务完成组数/天", type: "number", placeholder: "请输入任务完成组数/天" },
  { prop: "withdrawCountPerDay", label: "提现次数/天", type: "number", placeholder: "请输入提现次数/天" },
  { prop: "withdrawFeeRate", label: "提现手续费率", type: "number", max: 100, placeholder: "请输入提现手续费率" },
  { prop: "minWithdrawAmount", label: "提现所需完成单数", type: "number", placeholder: "请输入提现所需完成单数" },
  { prop: "withdrawLimitPerDay", label: "提现限额/天", type: "number", placeholder: "请输入提现限额/天" },
  { prop: "minWithdraw", label: "最低提现金额", type: "number", placeholder: "请输入最低提现金额" },
  { prop: "maxWithdraw", label: "最高提现金额", type: "number", placeholder: "请输入最高提现金额" },
  { prop: "description", label: "描述", type: "editor", span: 24 }
];

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

function openProductMatchDialog(row) {
  resetMatch();
  const _id = row.id || ids.value;
  getLevel(_id).then((response) => {
    productMatchForm.value = response.data;
    productMatchTitle.value = "产品匹配";
    productMatchDialogVisible.value = true;
  });
}

function closeProductMatchDialog() {
  productMatchDialogVisible.value = false;
}

function updateProductMatch() {
  if (
    productMatchForm.value.productMatchMin === null ||
    productMatchForm.value.productMatchMax === null
  ) {
    return;
  }

  const params = {
    id: productMatchForm.value.id,
    productMatchMin: productMatchForm.value.productMatchMin,
    productMatchMax: productMatchForm.value.productMatchMax,
    productMatchEnabled: productMatchForm.value.productMatchEnabled,
  };

  updateLevel(params).then((response) => {
    productMatchDialogVisible.value = false;
    getList(); // 更新列表
    proxy.$modal.msgSuccess("修改成功");
  });
}

async function openTranslationDialog(row) {
  const response = await getLevel(row.id);
  currentTranslationRow.value = response.data;
  translationForm.value = {
    ...createEmptyTranslations(),
    ...(response.data.translations || {}),
  };
  translationTitle.value = `${response.data.name || "等级"} - 国际化`;
  translationOpen.value = true;
}

async function submitTranslations(translations) {
  const row = currentTranslationRow.value;
  if (!row) {
    return;
  }
  const translationsId = translations.id || row.translationsId;
  await updateLevel({
    id: row.id,
    translationsId,
    translations: {
      ...translations,
      id: translationsId,
    },
  });
  proxy.$modal.msgSuccess("修改成功");
  translationOpen.value = false;
  currentTranslationRow.value = null;
  getList();
}

/** 查询等级列表 */
function getList() {
  loading.value = true;
  listLevel(queryParams.value).then((response) => {
    levelList.value = response.rows;
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
    level: null,
    icon: null,
    price: null,
    minBalance: null,
    inviteCount: null,
    orderCountPerDay: null,
    minCommissionRate: null,
    maxCommissionRate: null,
    minContinuousCommissionRate: null,
    maxContinuousCommissionRate: null,
    taskCountPerDay: null,
    withdrawCountPerDay: null,
    withdrawFeeRate: null,
    minWithdrawAmount: null,
    withdrawLimitPerDay: null,
    minWithdraw: null,
    maxWithdraw: null,
    description: null,
    createTime: null,
  };
  proxy.resetForm("levelRef");
}

function resetMatch() {
  productMatchForm.value = {
    id: null,
    productMatchMin: null,
    productMatchMax: null,
    productMatchEnabled: null,
  };
  proxy.resetForm("productMatchForRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.name = null;
  queryParams.value.level = null;
  handleQuery();
}

function handleToolbarTranslation() {
  const row = levelList.value.find((item) => item.id === ids.value[0]);
  if (row) {
    openTranslationDialog(row);
  }
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
  title.value = "添加等级";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getLevel(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改等级";
  });
}

function handleCopy(row) {
  const _id = row?.id || ids.value[0];
  getLevel(_id).then((response) => {
    const copied = { ...response.data };
    delete copied.id;
    delete copied.createTime;
    delete copied.updateTime;
    addLevel(copied).then(() => {
      proxy.$modal.msgSuccess("复制成功");
      getList();
    });
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["levelRef"]?.validate?.().then(() => {
      if (form.value.id != null) {
        updateLevel(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addLevel(form.value).then((response) => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row = {}) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除等级编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delLevel(_ids);
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
    "member/level/export",
    {
      ...queryParams.value,
    },
    `level_${new Date().getTime()}.xlsx`,
  );
}

getList();
</script>

<style scoped>
.full-width {
  width: 100%;
}

.level-match-link {
  color: #1677ff;
  font-size: 15px;
  line-height: 24px;
}

.level-match-link-danger {
  color: #ff4d4f;
}
</style>
