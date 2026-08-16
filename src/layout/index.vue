<template>
  <div :class="classObj" class="app-wrapper" :style="{ '--current-color': theme }">
    <div v-if="device === 'mobile' && sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-show="settingsStore.menuVisible && !sidebar.hide" class="sidebar-container" />
    <button
      v-if="settingsStore.topNav && settingsStore.menuVisible && !sidebar.hide"
      class="sider-trigger"
      type="button"
      @click="toggleSideBar"
    >
      <left-outlined v-if="sidebar.opened" />
      <right-outlined v-else />
    </button>
    <div
      :class="{
        hasTagsView: needTagsView,
        sidebarHide: sidebar.hide || !settingsStore.menuVisible,
        noHeader: !settingsStore.headerVisible
      }"
      class="main-container"
    >
      <div v-show="settingsStore.headerVisible" class="layout-header" :class="{ 'fixed-header': fixedHeader }">
        <navbar @setLayout="setLayout" />
        <tags-view v-if="needTagsView" />
      </div>
      <app-main />
      <settings ref="settingRef" />
      <button
        v-if="settingsStore.showSettings"
        class="layout-setting-trigger"
        type="button"
        title="整体风格设置"
        @click="setLayout"
      >
        <svg-icon icon-class="system" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import Sidebar from './components/Sidebar/index.vue'
import { AppMain, Navbar, Settings, TagsView } from './components'
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'

const settingsStore = useSettingsStore()
const theme = computed(() => settingsStore.theme)
const sideTheme = computed(() => settingsStore.sideTheme)
const sidebar = computed(() => useAppStore().sidebar)
const device = computed(() => useAppStore().device)
const needTagsView = computed(() => settingsStore.headerVisible && settingsStore.tagsView)
const fixedHeader = computed(() => settingsStore.fixedHeader)

const classObj = computed(() => ({
  hideSidebar: !sidebar.value.opened,
  openSidebar: sidebar.value.opened,
  sidebarHide: sidebar.value.hide,
  menuHidden: !settingsStore.menuVisible,
  fixedSidebar: settingsStore.fixedSidebar,
  contentFixed: settingsStore.contentWidth === 'Fixed',
  topNavMode: settingsStore.topNav,
  withoutAnimation: sidebar.value.withoutAnimation,
  mobile: device.value === 'mobile'
}))

const width = ref(window.innerWidth)
const WIDTH = 992 // refer to Bootstrap's responsive design

function updateViewportWidth() {
  width.value = window.innerWidth
}

watch(() => device.value, () => {
  if (device.value === 'mobile' && sidebar.value.opened) {
    useAppStore().closeSideBar({ withoutAnimation: false })
  }
})

watch(width, () => {
  if (width.value - 1 < WIDTH) {
    useAppStore().toggleDevice('mobile')
    useAppStore().closeSideBar({ withoutAnimation: true })
  } else {
    useAppStore().toggleDevice('desktop')
    if (settingsStore.topNav && settingsStore.menuVisible && !sidebar.value.hide) {
      useAppStore().openSideBar(true)
    }
  }
}, { immediate: true })

onMounted(() => window.addEventListener('resize', updateViewportWidth))
onBeforeUnmount(() => window.removeEventListener('resize', updateViewportWidth))

function handleClickOutside() {
  useAppStore().closeSideBar({ withoutAnimation: false })
}

function toggleSideBar() {
  useAppStore().toggleSideBar(false)
}

const settingRef = ref(null)
function setLayout() {
  settingRef.value.openSetting()
}
</script>

<style lang="scss" scoped>
@use "@/assets/styles/mixin.scss" as mix;
@use "@/assets/styles/variables.module.scss" as vars;

.app-wrapper {
  @include mix.clearfix;
  position: relative;
  height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - #{vars.$base-sidebar-width});
  transition: width 0.28s;
}

.sider-trigger {
  position: fixed;
  top: 74px;
  left: #{vars.$base-sidebar-width - 11px};
  z-index: 830;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  border-radius: 40px;
  background: #fff;
  color: rgba(0, 0, 0, 0.25);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  line-height: 16.1px;
  font-size: 14px;
}

.layout-setting-trigger {
  position: fixed;
  top: 246px;
  right: 0;
  z-index: 900;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 0;
  border-radius: 6px 0 0 6px;
  background: #1890ff;
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.35);

  .svg-icon {
    margin-right: 0;
    color: #ffffff;
    font-size: 20px;
  }
}

.hideSidebar .sider-trigger {
  left: 42px;
}

.hideSidebar .fixed-header {
  width: calc(100% - 54px);
}

.sidebarHide .fixed-header {
  width: 100%;
}

.mobile .fixed-header {
  width: 100%;
}

.noHeader {
  :deep(.app-main) {
    min-height: 100vh;
  }
}

.contentFixed {
  :deep(.app-main) {
    max-width: 1200px;
    margin-right: auto;
    margin-left: auto;
  }
}

.app-wrapper:not(.fixedSidebar) {
  .sidebar-container {
    position: absolute;
  }
}
</style>
