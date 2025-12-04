<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="序号" prop="sort">
        <el-input
          v-model="queryParams.sort"
          placeholder="请输入序号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="配置名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入配置名称"
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

    <el-table
      v-loading="loading"
      :data="orderconfigList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="配置名称" align="center" prop="name" />
      <el-table-column label="序号" align="center" prop="sort" />
      <el-table-column label="创建时间" align="center" prop="createTime" />
      <el-table-column label="修改时间" align="center" prop="updateTime" />
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
            v-hasPermi="['member:orderconfig:edit']"
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

    <!-- 添加或修改网站设置对话框 -->
    <el-drawer :title="title" v-model="open" size="80%" append-to-body>
      <!-- 动态渲染子组件 -->
      <component
        :is="dialogComponent"
        v-if="open && currentType"
        :form="form"
        :loading="loading"
        @update:form="handleFormUpdate"
        @submit="handleSubmit"
        @cancel="cancel"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="loading">
            确 定
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="Orderconfig">
import {
  listOrderconfig,
  getOrderconfig,
  delOrderconfig,
  addOrderconfig,
  updateOrderconfig,
} from "@/api/member/orderconfig";
import WebsiteConfigDialog from "./components/website-config.vue"; // 导入子组件（动态导入见下方）

const { proxy } = getCurrentInstance();

const orderconfigList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const currentType = ref(""); // 当前类型，用于选择子组件
const dialogComponent = ref(null); // 动态组件

const data = reactive({
  form: {
    id: null,
    type: null,
    sort: null,
    name: null,
    content: null, // JSON 字符串
    createTime: null,
    updateTime: null,
  },
  queryParams: {
    pageNum: 1,
    pageSize: 30,
    type: null,
    sort: null,
    name: null,
    content: null,
  },
  rules: {
    name: [{ required: true, message: "配置名称不能为空", trigger: "blur" }], // 只保留通用规则
  },
});

const { queryParams, form, rules } = toRefs(data);

// 组件映射：根据 type 动态导入子组件
const componentMap = {
  website: WebsiteConfigDialog, // 静态导入，或用 defineAsyncComponent 动态
  // 其他类型如 'email': EmailConfigDialog, ...
};

// 使用动态导入（推荐，避免打包大）
const loadComponent = async (type) => {
  if (type === "website") {
    const { default: comp } = await import("./components/website-config.vue");
    dialogComponent.value = comp;
  } else if (type === "trade") {
    const { default: comp } = await import("./components/trade-config.vue");
    dialogComponent.value = comp;
  } else if (type === "signin") {
    // 新增 signin
    const { default: comp } = await import("./components/sign-in-config.vue");
    dialogComponent.value = comp;
  } else if (type === "register") {
    const { default: comp } = await import(
      "./components/register-protocol-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "about") {
    const { default: comp } = await import("./components/about-us-config.vue");
    dialogComponent.value = comp;
  } else if (type === "certificate") {
    const { default: comp } = await import(
      "./components/certificate-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "help") {
    // 新增 help-center
    const { default: comp } = await import(
      "./components/help-center-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "terms") {
    // 新增 terms
    const { default: comp } = await import("./components/terms-config.vue");
    dialogComponent.value = comp;
  } else if (type === "event") {
    // 新增 event
    const { default: comp } = await import("./components/event-config.vue");
    dialogComponent.value = comp;
  } else if (type === "transaction") {
    // 新增 transaction-description
    const { default: comp } = await import(
      "./components/transaction-description-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "order") {
    // 新增 order-description
    const { default: comp } = await import(
      "./components/order-description-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "usage") {
    // 新增 usage-description
    const { default: comp } = await import(
      "./components/usage-description-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "points") {
    // 新增 points
    const { default: comp } = await import("./components/points-config.vue");
    dialogComponent.value = comp;
  } else if (type === "task") {
    // 新增 task
    const { default: comp } = await import("./components/task-config.vue");
    dialogComponent.value = comp;
  } else if (type === "notification") {
    // 新增 task
    const { default: comp } = await import(
      "./components/notification-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "backend" || type === "front") {
    // 新增 task
    const { default: comp } = await import(
      "./components/backend-security-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "bonus") {
    // 新增 task
    const { default: comp } = await import("./components/bonus-config.vue");
    dialogComponent.value = comp;
  } else if (type === "balance") {
    // 新增 task
    const { default: comp } = await import(
      "./components/balance-treasure-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "telegram") {
    // 新增 task
    const { default: comp } = await import(
      "./components/telegram-bot-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "work") {
    // 新增 task
    const { default: comp } = await import(
      "./components/work-bonus-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "privacy") {
    // 新增 task
    const { default: comp } = await import(
      "./components/privacy-protocol-config.vue"
    );
    dialogComponent.value = comp;
  } else if (type === "email") {
    // 新增 task
    const { default: comp } = await import("./components/email-config.vue");
    dialogComponent.value = comp;
  } else if (type === "reward") {
    // 新增 task
    const { default: comp } = await import("./components/reward-config.vue");
    dialogComponent.value = comp;
  } else {
    return;
  }
};

/** 查询网站设置列表 */
function getList() {
  loading.value = true;
  listOrderconfig(queryParams.value).then((response) => {
    orderconfigList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  currentType.value = "";
  dialogComponent.value = null;
  reset();
}

// 表单重置
function reset() {
  Object.assign(form.value, {
    id: null,
    type: null,
    sort: null,
    name: null,
    content: null,
    createTime: null,
    updateTime: null,
  });
  proxy.resetForm("orderconfigRef");
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
  // 新增时需选择类型，暂用通用或弹类型选择；这里假设新增不支持动态，先保持原样
  open.value = true;
  title.value = "添加网站设置";
  // 对于新增，可扩展类型选择
}

/** 修改按钮操作 */
async function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  const response = await getOrderconfig(_id);
  Object.assign(form.value, response.data);
  currentType.value = row.type || "website"; // 根据 type 设置
  title.value = `${row.name}`;
  await loadComponent(currentType.value); // 动态加载子组件
  open.value = true;
}

/** 子组件更新 form 事件 */
function handleFormUpdate(updatedForm) {
  Object.assign(form.value, updatedForm);
}

/** 提交按钮（通用，子组件可 emit） */
async function handleSubmit() {
  // 校验由子组件处理，这里直接提交
  if (form.value.id != null) {
    await updateOrderconfig(form.value);
    proxy.$modal.msgSuccess("修改成功");
  } else {
    await addOrderconfig(form.value);
    proxy.$modal.msgSuccess("新增成功");
  }
  open.value = false;
  currentType.value = "";
  dialogComponent.value = null;
  getList();
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除网站设置编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delOrderconfig(_ids);
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
    "member/orderconfig/export",
    {
      ...queryParams.value,
    },
    `orderconfig_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>
