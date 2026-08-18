<template>
  <div ref="tablePageRef" class="ant-pro-page">
    <a-card v-if="$slots.search" class="ant-pro-search-card" :bordered="false">
      <slot name="search" />
    </a-card>

    <a-card class="ant-pro-table-card" :bordered="false">
      <div class="ant-pro-table-toolbar">
        <div class="ant-pro-table-title"><slot name="title">{{ title }}</slot></div>
        <div class="ant-pro-table-actions">
          <slot name="toolbar" />
          <a-space :size="8" class="ant-pro-table-tool-icons">
            <a-tooltip v-if="enabledTools.refresh" title="刷新">
              <span class="ant-pro-tool-action" :class="{ disabled: loading }" role="button" aria-label="刷新" :aria-disabled="loading" @click="!loading && reloadTable()">
                <ReloadOutlined :class="{ 'tool-icon-spinning': loading }" />
              </span>
            </a-tooltip>

            <a-dropdown v-if="enabledTools.density" placement="bottomRight" :trigger="['click']">
              <a-tooltip title="密度">
                <span class="ant-pro-tool-action" role="button" aria-label="密度"><ColumnHeightOutlined /></span>
              </a-tooltip>
              <template #overlay>
                <a-menu :selected-keys="[tableSize]" @click="handleDensityChange">
                  <a-menu-item key="large">宽松</a-menu-item>
                  <a-menu-item key="middle">中等</a-menu-item>
                  <a-menu-item key="small">紧凑</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>

            <a-popover
              v-if="enabledTools.columns"
              v-model:open="columnPopoverOpen"
              placement="bottomRight"
              trigger="click"
              overlay-class-name="ant-pro-column-popover"
              @open-change="handleColumnPopoverChange"
            >
              <a-tooltip title="列设置">
                <span class="ant-pro-tool-action" role="button" aria-label="列设置"><SettingOutlined /></span>
              </a-tooltip>
              <template #content>
                <div ref="columnPanelRef" class="column-setting-panel">
                  <div class="column-setting-header">
                    <a-checkbox
                      :checked="allColumnsVisible"
                      :indeterminate="someColumnsVisible"
                      @change="toggleAllColumns"
                    >列展示</a-checkbox>
                    <a-button type="link" size="small" @click="resetColumns">重置</a-button>
                  </div>

                  <section v-for="group in columnGroups" :key="group.key" class="column-setting-group">
                    <div class="column-setting-group-title">{{ group.title }}</div>
                    <div class="column-setting-list" :data-column-group="group.key">
                      <div
                        v-for="(item, index) in group.items"
                        :key="item.id"
                        class="column-setting-item"
                        :data-column-id="item.id"
                      >
                        <HolderOutlined class="drag-handle" />
                        <a-checkbox :checked="item.visible" @change="toggleColumn(item.id, $event.target.checked)" />
                        <span class="column-setting-label" :title="columnLabel(item)">{{ columnLabel(item) }}</span>
                        <a-space :size="2" class="column-setting-actions">
                          <a-tooltip title="上移">
                            <a-button type="text" size="small" :disabled="index === 0" @click="moveByOffset(item.id, -1)">
                              <ArrowUpOutlined />
                            </a-button>
                          </a-tooltip>
                          <a-tooltip title="下移">
                            <a-button type="text" size="small" :disabled="index === group.items.length - 1" @click="moveByOffset(item.id, 1)">
                              <ArrowDownOutlined />
                            </a-button>
                          </a-tooltip>
                          <a-dropdown placement="bottomRight" :trigger="['click']">
                            <a-tooltip title="固定位置">
                              <a-button type="text" size="small"><PushpinOutlined /></a-button>
                            </a-tooltip>
                            <template #overlay>
                              <a-menu @click="({ key }) => setColumnFixed(item.id, key)">
                                <a-menu-item key="left">固定在左侧</a-menu-item>
                                <a-menu-item key="none">不固定</a-menu-item>
                                <a-menu-item key="right">固定在右侧</a-menu-item>
                              </a-menu>
                            </template>
                          </a-dropdown>
                        </a-space>
                      </div>
                      <div v-if="group.items.length === 0" class="column-setting-empty">暂无列</div>
                    </div>
                  </section>
                </div>
              </template>
            </a-popover>

            <a-tooltip v-if="enabledTools.fullscreen" :title="fullscreenActive ? '退出全屏' : '全屏'">
              <span class="ant-pro-tool-action" role="button" aria-label="全屏" @click="toggleFullscreen">
                <FullscreenExitOutlined v-if="fullscreenActive" />
                <FullscreenOutlined v-else />
              </span>
            </a-tooltip>
          </a-space>
        </div>
      </div>

      <div v-if="selectedRowCount" class="ant-pro-selection-info">
        <span>已选择</span>
        <strong>{{ selectedRowCount }}</strong>
        <span>项</span>
        <a-button type="link" size="small" @click="clearSelection">取消选择</a-button>
      </div>

      <a-table
        v-bind="$attrs"
        class="ant-pro-table"
        :size="tableSize"
        :columns="visibleColumns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="false"
        :row-key="rowKey"
        :row-selection="rowSelection"
        :scroll="effectiveScroll"
        :locale="$slots.emptyText ? undefined : { emptyText: '暂无数据' }"
        @change="handleTableChange"
      >
        <template #bodyCell="slotProps">
          <slot name="bodyCell" v-bind="slotProps" />
        </template>
        <template v-if="$slots.emptyText" #emptyText>
          <slot name="emptyText" />
        </template>
      </a-table>

      <div v-if="pagination && pagination.total > 0" class="ant-pro-pagination">
        <a-pagination
          :current="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          :show-size-changer="pagination.showSizeChanger ?? pagination.total > 50"
          :show-quick-jumper="pagination.showQuickJumper ?? pagination.total > pagination.pageSize"
          :page-size-options="pagination.pageSizeOptions"
          :size="pagination.size"
          :show-total="(total, range) => `第 ${range[0]}-${range[1]} 条/总共 ${total} 条`"
          @change="handlePageChange"
          @showSizeChange="handlePageChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import Sortable from "sortablejs";
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  ColumnHeightOutlined,
  FullscreenExitOutlined,
  FullscreenOutlined,
  HolderOutlined,
  PushpinOutlined,
  ReloadOutlined,
  SettingOutlined,
} from "@ant-design/icons-vue";
import {
  createColumnStates,
  groupColumns,
  moveColumn,
  moveColumnByOffset,
  setAllColumnsVisible,
  setColumnVisible,
} from "@/utils/ant-pro-table";

defineOptions({ inheritAttrs: false });

const props = defineProps({
  title: { type: String, default: "列表" },
  columns: { type: Array, required: true },
  dataSource: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  rowKey: { type: [String, Function], default: "id" },
  rowSelection: { type: Object, default: undefined },
  scroll: { type: Object, default: undefined },
  pagination: { type: [Object, Boolean], default: undefined },
  defaultSize: {
    type: String,
    default: "middle",
    validator: (value) => ["large", "middle", "small"].includes(value),
  },
  toolOptions: { type: Object, default: () => ({}) },
});

const emit = defineEmits(["page-change", "refresh", "change"]);
const tablePageRef = ref();
const columnPanelRef = ref();
const tableSize = ref(props.defaultSize);
const columnStates = ref([]);
const columnPopoverOpen = ref(false);
const fullscreenActive = ref(false);
let sortableInstances = [];

const enabledTools = computed(() => ({
  refresh: true,
  density: true,
  columns: true,
  fullscreen: true,
  ...props.toolOptions,
}));

const visibleColumns = computed(() => columnStates.value
  .filter((item) => item.visible)
  .map((item) => ({ ...item.column, fixed: item.fixed || undefined })));

const groupedColumns = computed(() => groupColumns(columnStates.value));
const columnGroups = computed(() => [
  { key: "left", title: "固定在左侧", items: groupedColumns.value.left },
  { key: "none", title: "不固定", items: groupedColumns.value.none },
  { key: "right", title: "固定在右侧", items: groupedColumns.value.right },
]);

const allColumnsVisible = computed(() => columnStates.value.length > 0 && columnStates.value.every((item) => item.visible));
const someColumnsVisible = computed(() => {
  const count = columnStates.value.filter((item) => item.visible).length;
  return count > 0 && count < columnStates.value.length;
});
const selectedRowCount = computed(() => {
  const selectedKeys = props.rowSelection?.selectedRowKeys;
  return Array.isArray(selectedKeys) ? selectedKeys.length : 0;
});

const effectiveScroll = computed(() => {
  if (!props.scroll) return undefined;
  if (typeof props.scroll.x !== "number") return props.scroll;
  const configuredSelectionWidth = Number(props.rowSelection?.columnWidth);
  const selectionWidth = props.rowSelection
    ? (Number.isFinite(configuredSelectionWidth) && configuredSelectionWidth > 0 ? configuredSelectionWidth : 48)
    : 0;
  const visibleWidth = visibleColumns.value.reduce((total, column) => {
    const width = Number(column.width);
    return total + (Number.isFinite(width) ? width : 120);
  }, selectionWidth);
  return { ...props.scroll, x: Math.max(visibleWidth, 600) };
});

watch(
  () => props.columns,
  (columns) => {
    columnStates.value = createColumnStates(columns, columnStates.value);
    if (columnPopoverOpen.value) nextTick(initSortables);
  },
  { immediate: true, deep: true }
);

watch(() => props.defaultSize, (value) => { tableSize.value = value; });

function columnLabel(item) {
  return typeof item.column.title === "string"
    ? item.column.title
    : String(item.column.dataIndex ?? item.column.key ?? item.id);
}

function handlePageChange(page, pageSize) {
  emit("page-change", { page, pageSize });
}

function handleTableChange(pagination, filters, sorter, extra) {
  emit("change", pagination, filters, sorter, extra);
}

function reloadTable() {
  emit("refresh");
}

function clearSelection() {
  props.rowSelection?.onChange?.([], []);
}

function handleDensityChange({ key }) {
  tableSize.value = key;
}

function toggleColumn(id, visible) {
  columnStates.value = setColumnVisible(columnStates.value, id, visible);
}

function toggleAllColumns(event) {
  const visible = event.target.checked;
  columnStates.value = setAllColumnsVisible(columnStates.value, visible);
}

function resetColumns() {
  columnStates.value = createColumnStates(props.columns);
  nextTick(initSortables);
}

function moveByOffset(id, offset) {
  columnStates.value = moveColumnByOffset(columnStates.value, id, offset);
  nextTick(initSortables);
}

function setColumnFixed(id, group) {
  const items = groupColumns(columnStates.value)[group] || [];
  columnStates.value = moveColumn(columnStates.value, id, group, items.length);
  nextTick(initSortables);
}

function destroySortables() {
  sortableInstances.forEach((instance) => instance.destroy());
  sortableInstances = [];
}

function initSortables() {
  destroySortables();
  if (!columnPanelRef.value || !columnPopoverOpen.value) return;
  columnPanelRef.value.querySelectorAll(".column-setting-list").forEach((element) => {
    sortableInstances.push(Sortable.create(element, {
      group: "ant-pro-table-columns",
      animation: 150,
      handle: ".drag-handle",
      draggable: ".column-setting-item",
      onEnd(event) {
        const id = event.item?.dataset.columnId;
        const group = event.to?.dataset.columnGroup || "none";
        if (id) columnStates.value = moveColumn(columnStates.value, id, group, event.newIndex ?? 0);
        nextTick(initSortables);
      },
    }));
  });
}

function handleColumnPopoverChange(open) {
  if (open) nextTick(initSortables);
  else destroySortables();
}

function handleFullscreenChange() {
  fullscreenActive.value = document.fullscreenElement === tablePageRef.value;
}

function toggleFullscreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen();
    return;
  }
  tablePageRef.value?.requestFullscreen();
}

onMounted(() => document.addEventListener("fullscreenchange", handleFullscreenChange));
onBeforeUnmount(() => {
  destroySortables();
  document.removeEventListener("fullscreenchange", handleFullscreenChange);
});
</script>

<style scoped lang="scss">
.ant-pro-page { color: var(--text-primary); }
.ant-pro-search-card { margin-bottom: 16px; border-radius: 6px; }
.ant-pro-search-card :deep(.ant-card-body) { padding: 24px; }
.ant-pro-search-card :deep(.ant-form-item) { margin-bottom: 0; }
.ant-pro-search-card :deep(.ant-pro-query-actions) { display: flex; justify-content: flex-end; }
.ant-pro-table-card { border-radius: 6px; }
.ant-pro-table-card :deep(.ant-card-body) { padding: 0 24px 16px; }
.ant-pro-table-toolbar { display: flex; align-items: center; justify-content: space-between; min-height: 64px; }
.ant-pro-table-title { color: var(--text-primary); font-size: 16px; font-weight: 500; line-height: 16px; }
.ant-pro-table-actions { display: flex; align-items: center; gap: 8px; }
.ant-pro-table-actions > :deep(.ant-btn), .ant-pro-table-actions > :deep(.ant-upload-wrapper .ant-btn) { height: 32px; padding-inline: 15px; }
.ant-pro-selection-info { display: flex; align-items: center; gap: 6px; min-height: 40px; margin-bottom: 12px; padding: 8px 16px; color: rgba(0, 0, 0, 0.65); background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 2px; }
.ant-pro-selection-info strong { color: #1890ff; font-weight: 500; }
.ant-pro-selection-info :deep(.ant-btn-link) { height: auto; padding: 0 4px; }
.ant-pro-table-tool-icons { margin-left: 4px; }
.ant-pro-tool-action { display: inline-flex; align-items: center; justify-content: center; padding: 4px; color: rgba(0, 0, 0, 0.65); font-size: 16px; line-height: 1; cursor: pointer; border-radius: 4px; transition: color .2s, background-color .2s; }
.ant-pro-tool-action:hover { color: #1890ff; background: rgba(0, 0, 0, 0.025); }
.ant-pro-tool-action.disabled { color: rgba(0, 0, 0, 0.25); cursor: not-allowed; }
.ant-pro-table :deep(.ant-table) { border-radius: 8px 8px 0 0; }
.ant-pro-table :deep(.ant-table-thead > tr > th) { padding: 12px 8px; background: var(--table-header-bg); color: var(--text-primary); font-size: 14px; font-weight: 600; line-height: 22px; }
.ant-pro-table :deep(.ant-table-tbody > tr > td) { padding: 12px 8px; color: var(--text-primary); font-size: 14px; line-height: 22px; vertical-align: middle; }
.ant-pro-table :deep(.ant-btn-link) { padding: 0; }
.ant-pro-table :deep(.ant-btn-sm) { height: 24px; padding: 0 7px; border-radius: 4px; font-size: 14px; line-height: 22px; }
.ant-pro-pagination { display: flex; justify-content: flex-end; padding-top: 16px; }
.ant-pro-query-form :deep(.ant-btn) { height: 32px; padding-inline: 15px; }
.tool-icon-spinning { animation: ant-pro-spin 0.8s linear infinite; }

.column-setting-panel { width: 320px; max-height: 520px; overflow-y: auto; }
.column-setting-header { display: flex; align-items: center; justify-content: space-between; padding: 0 4px 8px; border-bottom: 1px solid var(--border-color); }
.column-setting-group { padding-top: 10px; }
.column-setting-group-title { padding: 0 4px 6px; color: var(--text-secondary); font-size: 12px; }
.column-setting-list { min-height: 8px; }
.column-setting-item { display: flex; align-items: center; min-height: 36px; padding: 2px 4px; border-radius: 4px; }
.column-setting-item:hover { background: var(--control-alt-bg, rgba(0, 0, 0, 0.04)); }
.drag-handle { margin-right: 8px; color: var(--text-secondary); cursor: grab; }
.column-setting-label { flex: 1; min-width: 0; margin-left: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.column-setting-actions { visibility: hidden; }
.column-setting-item:hover .column-setting-actions { visibility: visible; }
.column-setting-empty { padding: 6px 28px; color: var(--text-secondary); font-size: 12px; }

@keyframes ant-pro-spin { to { transform: rotate(360deg); } }
</style>
