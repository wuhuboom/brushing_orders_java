<template>
  <div class="app-container">
    <a-form v-show="showSearch" ref="queryRef" :model="queryParams" layout="inline" class="ant-pro-search-form">
      <a-form-item label="菜单名称" name="menuName">
        <a-input v-model:value="queryParams.menuName" placeholder="请输入菜单名称" allow-clear @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="状态" name="status">
        <a-select v-model:value="queryParams.status" placeholder="请选择状态" allow-clear>
          <a-select-option v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
            {{ dict.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item class="ant-pro-search-actions">
        <a-button @click="resetQuery">重置</a-button>
        <a-button type="primary" @click="handleQuery">查询</a-button>
      </a-form-item>
    </a-form>

    <ant-pro-table
      v-if="refreshTable"
      row-key="menuId"
      title="菜单列表"
      :columns="menuColumns"
      :data-source="menuList"
      :loading="loading"
      :pagination="false"
      :default-expand-all-rows="isExpandAll"
      :children-column-name="'children'"
      @refresh="getList"
    >
      <template #toolbar>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['system:menu:add']">新增</a-button>
        <a-button @click="toggleExpandAll">展开/折叠</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'icon'">
          <svg-icon :icon-class="record.icon" />
        </template>
        <template v-else-if="column.key === 'status'">
          <dict-tag :options="sys_normal_disable" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['system:menu:edit']">修改</a-button>
            <a-button type="link" size="small" @click="handleAdd(record)" v-hasPermi="['system:menu:add']">新增</a-button>
            <a-button type="link" size="small" danger @click="handleDelete(record)" v-hasPermi="['system:menu:remove']">删除</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal v-model:open="open" :title="title" width="760px" destroy-on-close @cancel="cancel">
      <a-form ref="menuRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }" :wrapper-col="{ flex: 1 }">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="上级菜单" name="parentId">
              <a-tree-select
                v-model:value="form.parentId"
                :tree-data="menuOptions"
                :field-names="treeSelectFieldNames"
                :dropdown-style="{ maxHeight: '360px', overflow: 'auto' }"
                placeholder="选择上级菜单"
                tree-default-expand-all
                tree-node-filter-prop="menuName"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="菜单类型" name="menuType">
              <a-radio-group v-model:value="form.menuType">
                <a-radio value="M">目录</a-radio>
                <a-radio value="C">菜单</a-radio>
                <a-radio value="F">按钮</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col v-if="form.menuType != 'F'" :span="12">
            <a-form-item label="菜单图标" name="icon">
              <a-popover v-model:open="iconPopoverOpen" placement="bottomLeft" trigger="click" overlay-class-name="menu-icon-popover">
                <template #content>
                  <icon-select ref="iconSelectRef" :active-icon="form.icon" @selected="selected" />
                </template>
                <a-input v-model:value="form.icon" placeholder="点击选择图标" readonly @click="showSelectIcon">
                  <template #prefix>
                    <svg-icon v-if="form.icon" :icon-class="form.icon" class="menu-input-icon" />
                    <SearchOutlined v-else />
                  </template>
                </a-input>
              </a-popover>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="显示排序" name="orderNum">
              <a-input-number v-model:value="form.orderNum" :min="0" class="menu-full-control" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单名称" name="menuName">
              <a-input v-model:value="form.menuName" placeholder="请输入菜单名称" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.menuType == 'C'" :span="12">
            <a-form-item name="routeName" label="路由名称">
              <a-input v-model:value="form.routeName" placeholder="请输入路由名称" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.menuType != 'F'" :span="12">
            <a-form-item label="是否外链" name="isFrame">
              <a-radio-group v-model:value="form.isFrame">
                <a-radio value="0">是</a-radio>
                <a-radio value="1">否</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col v-if="form.menuType != 'F'" :span="12">
            <a-form-item name="path" label="路由地址">
              <a-input v-model:value="form.path" placeholder="请输入路由地址" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.menuType == 'C'" :span="12">
            <a-form-item name="component" label="组件路径">
              <a-input v-model:value="form.component" placeholder="请输入组件路径" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.menuType != 'M'" :span="12">
            <a-form-item label="权限字符" name="perms">
              <a-input v-model:value="form.perms" placeholder="请输入权限标识" maxlength="100" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.menuType == 'C'" :span="12">
            <a-form-item label="路由参数" name="query">
              <a-input v-model:value="form.query" placeholder="请输入路由参数" maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.menuType == 'C'" :span="12">
            <a-form-item label="是否缓存" name="isCache">
              <a-radio-group v-model:value="form.isCache">
                <a-radio value="0">缓存</a-radio>
                <a-radio value="1">不缓存</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col v-if="form.menuType != 'F'" :span="12">
            <a-form-item label="显示状态" name="visible">
              <a-radio-group v-model:value="form.visible">
                <a-radio v-for="dict in sys_show_hide" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="modal-footer-actions">
          <a-space>
            <a-button type="primary" @click="submitForm">确定</a-button>
            <a-button @click="cancel">取消</a-button>
          </a-space>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="Menu">
import { addMenu, delMenu, getMenu, listMenu, updateMenu } from "@/api/system/menu"
import SvgIcon from "@/components/SvgIcon"
import IconSelect from "@/components/IconSelect"
import { SearchOutlined } from "@ant-design/icons-vue"

const { proxy } = getCurrentInstance()
const { sys_show_hide, sys_normal_disable } = proxy.useDict("sys_show_hide", "sys_normal_disable")

const menuList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const title = ref("")
const menuOptions = ref([])
const isExpandAll = ref(false)
const refreshTable = ref(true)
const iconSelectRef = ref(null)
const iconPopoverOpen = ref(false)
const menuRef = ref(null)

const treeSelectFieldNames = {
  label: "menuName",
  value: "menuId",
  children: "children"
}

const menuColumns = [
  { title: "菜单名称", dataIndex: "menuName", width: 180 },
  { title: "图标", dataIndex: "icon", key: "icon", width: 100 },
  { title: "排序", dataIndex: "orderNum", width: 80 },
  { title: "权限标识", dataIndex: "perms", width: 180 },
  { title: "组件路径", dataIndex: "component", width: 200 },
  { title: "状态", dataIndex: "status", key: "status", width: 100 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "operation", width: 220, fixed: "right" }
]

const data = reactive({
  form: {},
  queryParams: {
    menuName: undefined,
    visible: undefined,
    status: undefined
  },
  rules: {
    menuName: [{ required: true, message: "菜单名称不能为空", trigger: "blur" }],
    orderNum: [{ required: true, message: "菜单顺序不能为空", trigger: "blur" }],
    path: [{ required: true, message: "路由地址不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listMenu(queryParams.value).then(response => {
    menuList.value = proxy.handleTree(response.data, "menuId")
    loading.value = false
  })
}

function getTreeselect() {
  menuOptions.value = []
  return listMenu().then(response => {
    const menu = { menuId: 0, menuName: "主类目", children: [] }
    menu.children = proxy.handleTree(response.data, "menuId")
    menuOptions.value.push(menu)
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  iconPopoverOpen.value = false
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
    status: "0"
  }
  menuRef.value?.clearValidate?.()
}

function showSelectIcon() {
  iconSelectRef.value?.reset?.()
}

function selected(name) {
  form.value.icon = name
  iconPopoverOpen.value = false
}

function handleQuery() {
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleAdd(row) {
  reset()
  getTreeselect()
  if (row != null && row.menuId) {
    form.value.parentId = row.menuId
  } else {
    form.value.parentId = 0
  }
  open.value = true
  title.value = "添加菜单"
}

function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => {
    refreshTable.value = true
  })
}

async function handleUpdate(row) {
  reset()
  await getTreeselect()
  getMenu(row.menuId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改菜单"
  })
}

function submitForm() {
  menuRef.value?.validate().then(() => {
    if (form.value.menuId != undefined) {
      updateMenu(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addMenu(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  }).catch(() => {})
}

function handleDelete(row) {
  proxy.$modal.confirm(`是否确认删除名称为 "${row.menuName}" 的数据项？`).then(function() {
    return delMenu(row.menuId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.menu-full-control {
  width: 100%;
}

.menu-input-icon {
  width: 16px;
  height: 16px;
}
</style>

<style>
.menu-icon-popover {
  width: 540px;
}
</style>
