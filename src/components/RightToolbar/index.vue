<template>
  <div ref="toolbarRef" class="top-right-btn" :style="style">
    <a-space :size="8">
      <a-tooltip title="刷新">
        <a-button shape="circle" size="small" aria-label="刷新" @click="refresh">
          <ReloadOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="密度">
        <a-button shape="circle" size="small" aria-label="密度" @click="toggleDensity">
          <ColumnHeightOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="列设置">
        <a-button
          v-if="showColumnsType === 'transfer'"
          shape="circle"
          size="small"
          aria-label="列设置"
          @click="showColumn"
        >
          <SettingOutlined />
        </a-button>
        <a-dropdown v-else-if="columns" :trigger="['click']">
          <a-button shape="circle" size="small" aria-label="列设置">
            <MenuOutlined />
          </a-button>
          <template #overlay>
            <a-menu class="column-dropdown-menu">
              <a-menu-item>
                <a-checkbox :indeterminate="isIndeterminate" :checked="isChecked" @change="toggleCheckAll">
                  列展示
                </a-checkbox>
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item v-for="item in columns" :key="item.key || item.label">
                <a-checkbox :checked="item.visible !== false" @change="checkboxChange($event, item.label)">
                  {{ item.label }}
                </a-checkbox>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button v-else shape="circle" size="small" aria-label="列设置">
          <SettingOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="全屏">
        <a-button shape="circle" size="small" aria-label="全屏" @click="toggleFullscreen">
          <FullscreenOutlined />
        </a-button>
      </a-tooltip>
    </a-space>

    <a-modal v-model:open="open" :title="title" width="640px" @ok="open = false">
      <a-transfer
        v-model:target-keys="value"
        :data-source="transferColumns"
        :titles="['显示', '隐藏']"
        :render="(item) => item.title"
        @change="dataChange"
      />
    </a-modal>
  </div>
</template>

<script setup>
import {
  ColumnHeightOutlined,
  FullscreenOutlined,
  MenuOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'

const props = defineProps({
  showSearch: {
    type: Boolean,
    default: true
  },
  columns: {
    type: Array,
    default: undefined
  },
  search: {
    type: Boolean,
    default: true
  },
  showColumnsType: {
    type: String,
    default: 'checkbox'
  },
  gutter: {
    type: Number,
    default: 10
  },
})

const emits = defineEmits(['update:showSearch', 'queryTable'])
const toolbarRef = ref()
const value = ref([])
const title = ref('显示/隐藏')
const open = ref(false)

const safeColumns = computed(() => props.columns || [])

const transferColumns = computed(() =>
  safeColumns.value.map((item, index) => ({
    ...item,
    key: String(item.key ?? index),
    title: item.label || item.title || String(item.key ?? index)
  }))
)

const style = computed(() => {
  const ret = {}
  if (props.gutter) {
    ret.marginRight = `${props.gutter / 2}px`
  }
  return ret
})

const isChecked = computed(() => safeColumns.value.length > 0 && safeColumns.value.every(col => col.visible !== false))
const isIndeterminate = computed(() => safeColumns.value.some(col => col.visible !== false) && !isChecked.value)

function refresh() {
  emits('queryTable')
}

function getPageContainer() {
  return toolbarRef.value?.closest('.app-container')
}

function toggleDensity() {
  getPageContainer()?.classList.toggle('ant-pro-compact-table')
}

function toggleFullscreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen()
    return
  }
  getPageContainer()?.requestFullscreen()
}

function dataChange(targetKeys) {
  safeColumns.value.forEach((item, index) => {
    const key = String(item.key ?? index)
    item.visible = !targetKeys.includes(key)
  })
}

function showColumn() {
  value.value = transferColumns.value
    .filter((item) => item.visible === false)
    .map((item) => item.key)
  open.value = true
}

function checkboxChange(event, label) {
  const column = safeColumns.value.find(item => item.label === label)
  if (column) {
    column.visible = event.target.checked
  }
}

function toggleCheckAll() {
  const newValue = !isChecked.value
  safeColumns.value.forEach((col) => (col.visible = newValue))
}
</script>

<style lang="scss" scoped>
.top-right-btn {
  display: inline-flex;
  align-items: center;
}

.column-dropdown-menu {
  min-width: 148px;
}
</style>
