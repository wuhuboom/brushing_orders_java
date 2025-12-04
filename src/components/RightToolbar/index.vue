<template>
  <div class="top-right-btn" :style="style">
    <el-row>
      <el-tooltip
        class="item"
        effect="dark"
        :content="showSearch ? '隐藏搜索' : '显示搜索'"
        placement="top"
        v-if="search"
      >
        <el-button circle icon="Search" @click="toggleSearch()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="刷新" placement="top">
        <el-button circle icon="Refresh" @click="refresh()" />
      </el-tooltip>
      <el-tooltip
        class="item"
        effect="dark"
        content="显隐列"
        placement="top"
        v-if="columns"
      >
        <el-button
          circle
          icon="Menu"
          @click="showColumn()"
          v-if="showColumnsType === 'transfer'"
        />
        <el-dropdown
          trigger="click"
          :hide-on-click="false"
          style="padding-left: 12px"
          v-if="showColumnsType === 'checkbox'"
          @visible-change="handleDropdownVisible"
        >
          <el-button circle icon="Menu" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>
                <el-checkbox
                  :indeterminate="isIndeterminate"
                  v-model="isChecked"
                  @change="toggleCheckAll"
                >
                  列展示
                </el-checkbox>
              </el-dropdown-item>
              <div class="check-line"></div>
              <draggable
                v-if="isDropdownVisible && draggableColumns.length"
                v-model="draggableColumns"
                item-key="key"
                tag="div"
                :component-data="{ tag: 'div', class: 'draggable-container' }"
                :animation="150"
                handle=".drag-handle"
                :group="{ name: 'columns', pull: true, put: true }"
                @start="handleDragStart"
                @end="handleDragEnd"
              >
                <template #item="{ element }">
                  <div class="drag-handle drag-item">
                    <i class="el-icon-rank drag-icon"></i>
                    <el-checkbox
                      v-model="element.visible"
                      @change="checkboxChange($event, element.label)"
                      :label="element.label"
                    />
                  </div>
                </template>
              </draggable>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-tooltip>
    </el-row>
    <el-dialog :title="title" v-model="open" append-to-body>
      <el-transfer
        :titles="['显示', '隐藏']"
        v-model="value"
        :data="columns"
        @change="dataChange"
      ></el-transfer>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from "vue";
import draggable from "vuedraggable";

const props = defineProps({
  showSearch: {
    type: Boolean,
    default: true,
  },
  columns: {
    type: Array,
    default: null,
  },
  search: {
    type: Boolean,
    default: true,
  },
  showColumnsType: {
    type: String,
    default: "checkbox",
  },
  gutter: {
    type: Number,
    default: 10,
  },
});

const emits = defineEmits([
  "update:showSearch",
  "queryTable",
  "update:columns",
]);

// 显隐数据
const value = ref([]);
// 弹出层标题
const title = ref("显示/隐藏");
// 是否显示弹出层
const open = ref(false);
// Draggable columns
const draggableColumns = ref([]);
// 下拉菜单是否可见
const isDropdownVisible = ref(false);

// 同步 props.columns 到 draggableColumns
watch(
  () => props.columns,
  (newColumns) => {
    if (Array.isArray(newColumns) && newColumns.length) {
      // 验证每项是否有 key 和 label，若无则提供默认值
      draggableColumns.value = newColumns.map((item, index) => ({
        key: item.key || `col-${index}`,
        label: item.label || `Column ${index + 1}`,
        visible: item.visible !== undefined ? item.visible : true,
        prop: item.prop || null,
        width: item.width || null,
        fixed: item.fixed || null,
      }));
    } else {
      draggableColumns.value = [];
      console.warn("No valid columns data received");
    }
  },
  { immediate: true, deep: true }
);

const style = computed(() => {
  const ret = {};
  if (props.gutter) {
    ret.marginRight = `${props.gutter / 2}px`;
  }
  return ret;
});

// 是否全选/半选 状态
const isChecked = computed({
  get: () =>
    draggableColumns.value.length &&
    draggableColumns.value.every((col) => col.visible),
  set: () => {},
});
const isIndeterminate = computed(
  () =>
    draggableColumns.value.length &&
    draggableColumns.value.some((col) => col.visible) &&
    !isChecked.value
);

// 下拉菜单可见性变化
function handleDropdownVisible(visible) {
  isDropdownVisible.value = visible;
  if (visible) {
    nextTick(() => {});
  } else {
  }
}

// 搜索
function toggleSearch() {
  emits("update:showSearch", !props.showSearch);
}

// 刷新
function refresh() {
  emits("queryTable");
}

// 右侧列表元素变化
function dataChange(data) {
  draggableColumns.value.forEach((item) => {
    item.visible = !data.includes(item.key);
  });
  emits("update:columns", [...draggableColumns.value]);
}

// 打开显隐列dialog
function showColumn() {
  open.value = true;
}

if (props.showColumnsType === "transfer") {
  value.value = props.columns
    .filter((item) => item.visible === false)
    .map((item) => item.key);
}

// 单勾选
function checkboxChange(event, label) {
  const column = draggableColumns.value.find((item) => item.label === label);
  if (column) {
    column.visible = event;
    emits("update:columns", [...draggableColumns.value]);
  }
}

// 切换全选/反选
function toggleCheckAll() {
  const newValue = !isChecked.value;
  draggableColumns.value.forEach((col) => (col.visible = newValue));
  emits("update:columns", [...draggableColumns.value]);
}

// 拖动开始
function handleDragStart(event) {
  event.originalEvent.stopPropagation();
}

// 拖动结束
function handleDragEnd(event) {
  emits("update:columns", [...draggableColumns.value]);
}
</script>

<style lang="scss" scoped>
:deep(.el-transfer__button) {
  border-radius: 50%;
  display: block;
  margin-left: 0px;
}
:deep(.el-transfer__button:first-child) {
  margin-bottom: 10px;
}
:deep(.el-dropdown-menu__item) {
  line-height: 30px;
  padding: 0 17px;
  user-select: none;
}
.draggable-container {
  padding: 0 17px;
  overflow-y: auto;
}
.drag-handle {
  cursor: move;
  display: flex;
  align-items: center;
}
.drag-item {
  display: flex;
  align-items: center;
  width: 100%;
}
.drag-icon {
  margin-right: 8px;
  cursor: move;
}
.check-line {
  width: 90%;
  height: 1px;
  background-color: #ccc;
  margin: 3px auto;
}
</style>
