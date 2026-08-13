<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
    >
      <el-form-item :label="t('menu.menuName')" prop="menuName">
        <el-input
          v-model="queryParams.menuName"
          :placeholder="t('menu.enterMenuName')"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('menu.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="t('menu.menuStatus')"
          clearable
          style="width: 200px"
        >
          <el-option
            v-for="dict in sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">
          {{ t("menu.search") }}
        </el-button>
        <el-button icon="Refresh" @click="resetQuery">
          {{ t("menu.reset") }}
        </el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="info" plain icon="Sort" @click="toggleExpandAll">
          {{ t("menu.toggleExpand") }}
        </el-button>
      </el-col>
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="menuList"
      border
      row-key="menuId"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column
        prop="menuName"
        :label="t('menu.menuName')"
        :show-overflow-tooltip="true"
        width="160"
      ></el-table-column>
      <el-table-column
        prop="icon"
        :label="t('menu.icon')"
        align="center"
        width="100"
      >
        <template #default="scope">
          <svg-icon :icon-class="scope.row.icon" />
        </template>
      </el-table-column>
      <el-table-column
        prop="orderNum"
        :label="t('menu.orderNum')"
        width="120"
      ></el-table-column>
      <el-table-column
        prop="perms"
        :label="t('menu.perms')"
        :show-overflow-tooltip="true"
      ></el-table-column>
      <el-table-column
        prop="component"
        :label="t('menu.component')"
        :show-overflow-tooltip="true"
      ></el-table-column>
      <el-table-column prop="status" :label="t('menu.status')" width="80">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('menu.createTime')"
        align="center"
        width="160"
        prop="createTime"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('menu.action')"
        align="center"
        width="210"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:menu:edit']"
            >{{ t("menu.edit") }}</el-button
          >
          <el-button
            link
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:menu:remove']"
            >{{ t("menu.delete") }}</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改菜单对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="menuRef" :model="form" :rules="rules" label-width="150px">
        <el-row>
          <el-col :span="24">
            <el-form-item :label="t('menu.parentMenu')">
              <el-tree-select
                v-model="form.parentId"
                :data="menuOptions"
                :props="{
                  value: 'menuId',
                  label: 'menuName',
                  children: 'children',
                }"
                value-key="menuId"
                :placeholder="t('menu.selectParentMenu')"
                check-strictly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('menu.menuName')" prop="menuName">
              <el-input
                v-model="form.menuName"
                :placeholder="t('menu.enterMenuName')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('menu.enName')" prop="enName">
              <el-input v-model="form.enName" :placeholder="t('menu.enName')" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{
            t("common.confirm")
          }}</el-button>
          <el-button @click="cancel">{{ t("common.cancel") }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Menu">
import {
  delMenu,
  getMenu,
  listMenu,
  updateMenu,
} from "@/api/system/menu";
import SvgIcon from "@/components/SvgIcon";
import IconSelect from "@/components/IconSelect";
import { ref, reactive, toRefs, onMounted, nextTick } from "vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();
const { proxy } = getCurrentInstance();
const { sys_show_hide, sys_normal_disable } = proxy.useDict(
  "sys_show_hide",
  "sys_normal_disable"
);

const menuList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const menuOptions = ref([]);
const isExpandAll = ref(false);
const refreshTable = ref(true);
const iconSelectRef = ref(null);
const isEditMode = ref(false);

const data = reactive({
  form: {},
  queryParams: {
    menuName: undefined,
    visible: undefined,
  },
  rules: {
    menuName: [
      { required: true, message: t("menu.menuNameRequired"), trigger: "blur" },
    ],
    orderNum: [
      { required: true, message: t("menu.orderNumRequired"), trigger: "blur" },
    ],
    path: [
      { required: true, message: t("menu.pathRequired"), trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询菜单列表 */
function getList() {
  loading.value = true;
  listMenu(queryParams.value).then((response) => {
    menuList.value = proxy.handleTree(response.data, "menuId");
    loading.value = false;
  });
}

/** 查询菜单下拉树结构 */
function getTreeselect() {
  menuOptions.value = [];
  listMenu().then((response) => {
    const menu = { menuId: 0, menuName: t("menu.root"), children: [] };
    menu.children = proxy.handleTree(response.data, "menuId");
    menuOptions.value.push(menu);
  });
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 表单重置 */
function reset() {
  form.value = {
    menuId: undefined,
    parentId: 0,
    menuName: undefined,
    icon: undefined,
    menuType: "M",
    orderNum: undefined,
    isFrame: "1",
    isCache: "0",
    visible: "0",
    status: "0",
  };
  isEditMode.value = false;
  proxy.resetForm("menuRef");
}

/** 展示下拉图标 */
function showSelectIcon() {
  iconSelectRef.value.reset();
}

/** 选择图标 */
function selected(name) {
  form.value.icon = name;
}

/** 搜索按钮操作 */
function handleQuery() {
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// Add action removed: adding menus is disabled in this view.

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}

/** 修改按钮操作 */
async function handleUpdate(row) {
  reset();
  await getTreeselect();
  getMenu(row.menuId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = t("menu.editMenu");
    isEditMode.value = true;
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["menuRef"].validate((valid) => {
    if (valid) {
      if (form.value.menuId != undefined) {
        updateMenu(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("menu.editSuccess"));
          open.value = false;
          getList();
        });
      } else {
        proxy.$modal.msgWarning("不允许新增菜单，请选择一条已有菜单进行编辑");
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal
    .confirm(t("menu.deleteConfirm", { menuName: row.menuName }))
    .then(function () {
      return delMenu(row.menuId);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("menu.deleteSuccess"));
    })
    .catch(() => {});
}

getList();
</script>
