<template>
  <a-drawer
    v-model:open="showSettings"
    :closable="false"
    :width="300"
    :body-style="{ padding: '24px' }"
    :mask-closable="true"
    :z-index="4000"
    placement="right"
    root-class-name="layout-setting-drawer"
  >
    <div class="setting-panel" :style="{ '--setting-theme': themeColor }">
      <section class="setting-section setting-section-plain">
        <h3 class="setting-title">整体风格设置</h3>
        <div class="preview-list">
          <button
            v-for="item in overallStyleOptions"
            :key="item.value"
            class="preview-card style-card"
            :class="[item.value, { active: currentOverallStyle === item.value }]"
            type="button"
            :aria-label="item.label"
            @click="setOverallStyle(item.value)"
          >
            <span class="style-sider"></span>
            <span class="style-header"></span>
            <span class="style-content"></span>
            <span v-if="currentOverallStyle === item.value" class="preview-check" aria-hidden="true">✓</span>
          </button>
        </div>
      </section>

      <section class="setting-section">
        <h3 class="setting-title">主题色</h3>
        <div class="color-list">
          <button
            v-for="color in themeColors"
            :key="color"
            class="color-swatch"
            :class="{ active: themeColor === color }"
            :style="{ backgroundColor: color }"
            type="button"
            :aria-label="`主题色 ${color}`"
            @click="setThemeColor(color)"
          >
            <span v-if="themeColor === color" aria-hidden="true">✓</span>
          </button>
        </div>
      </section>

      <section class="setting-section">
        <h3 class="setting-title">导航模式</h3>
        <div class="preview-list">
          <button
            v-for="item in navigationOptions"
            :key="item.value"
            class="preview-card nav-card"
            :class="[item.value, { active: currentNavMode === item.value }]"
            type="button"
            :aria-label="item.label"
            @click="setNavigationMode(item.value)"
          >
            <span class="nav-sider"></span>
            <span class="nav-header"></span>
            <span class="nav-content"></span>
            <span v-if="currentNavMode === item.value" class="preview-check" aria-hidden="true">✓</span>
          </button>
        </div>

        <h3 class="setting-title sub-title">侧边菜单类型</h3>
        <div class="preview-list">
          <button
            v-for="item in sideMenuOptions"
            :key="item.value"
            class="preview-card menu-card"
            :class="[item.value, { active: settingsStore.sideTheme === item.value }]"
            type="button"
            :aria-label="item.label"
            @click="setSideTheme(item.value)"
          >
            <span class="menu-sider"></span>
            <span class="menu-content"></span>
            <span class="menu-line line-1"></span>
            <span class="menu-line line-2"></span>
            <span v-if="settingsStore.sideTheme === item.value" class="preview-check" aria-hidden="true">✓</span>
          </button>
        </div>

        <div class="setting-row">
          <span>内容区域宽度</span>
          <a-select
            v-model:value="settingsStore.contentWidth"
            class="content-width-select"
            size="small"
            :options="contentWidthOptions"
          />
        </div>
        <div class="setting-row">
          <span>固定 Header</span>
          <a-switch v-model:checked="settingsStore.fixedHeader" size="small" />
        </div>
        <div class="setting-row">
          <span>固定侧边菜单</span>
          <a-switch v-model:checked="settingsStore.fixedSidebar" size="small" />
        </div>
        <div class="setting-row">
          <span>自动分割菜单</span>
          <a-switch
            v-model:checked="settingsStore.splitMenus"
            size="small"
            :disabled="settingsStore.layoutMode !== 'mix'"
            @change="handleSplitMenusChange"
          />
        </div>
      </section>

      <section class="setting-section">
        <h3 class="setting-title">内容区域</h3>
        <div class="setting-row">
          <span>顶栏</span>
          <a-switch v-model:checked="settingsStore.headerVisible" size="small" />
        </div>
        <div class="setting-row">
          <span>页脚</span>
          <a-switch v-model:checked="settingsStore.footerVisible" size="small" />
        </div>
        <div class="setting-row">
          <span>菜单</span>
          <a-switch v-model:checked="settingsStore.menuVisible" size="small" @change="handleMenuVisibleChange" />
        </div>
        <div class="setting-row">
          <span>菜单头</span>
          <a-switch v-model:checked="settingsStore.menuHeaderVisible" size="small" />
        </div>
      </section>

      <section class="setting-section">
        <h3 class="setting-title">其他设置</h3>
        <div class="setting-row">
          <span>色弱模式</span>
          <a-switch v-model:checked="settingsStore.colorWeak" size="small" @change="handleColorWeakChange" />
        </div>
      </section>

      <a-button block class="copy-button" @click="copySetting">
        <template #icon>
          <svg-icon icon-class="clipboard" />
        </template>
        拷贝设置
      </a-button>
    </div>
  </a-drawer>
</template>

<script setup>
import { message } from 'ant-design-vue'
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import { handleThemeStyle } from '@/utils/theme'

const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()
const showSettings = ref(false)

const themeColors = [
  '#1677ff',
  '#1890ff',
  '#f5222d',
  '#fa541c',
  '#faad14',
  '#13c2c2',
  '#52c41a',
  '#2f54eb',
  '#722ed1'
]

const overallStyleOptions = [
  { value: 'light', label: '亮色整体风格' },
  { value: 'dark', label: '暗色整体风格' }
]

const navigationOptions = [
  { value: 'side', label: '侧边菜单布局' },
  { value: 'top', label: '顶部菜单布局' },
  { value: 'mix', label: '混合菜单布局' }
]

const sideMenuOptions = [
  { value: 'theme-dark', label: '暗色侧边菜单' },
  { value: 'theme-light', label: '亮色侧边菜单' }
]

const contentWidthOptions = [
  { label: '流式', value: 'Fluid' },
  { label: '定宽', value: 'Fixed' }
]

const themeColor = computed(() => settingsStore.theme)
const currentOverallStyle = computed(() => (settingsStore.isDark ? 'dark' : 'light'))
const currentNavMode = computed(() => settingsStore.layoutMode || (settingsStore.topNav ? 'mix' : 'side'))

watch(
  () => ({
    theme: settingsStore.theme,
    layoutVersion: settingsStore.layoutVersion,
    sideTheme: settingsStore.sideTheme,
    topNav: settingsStore.topNav,
    tagsView: settingsStore.tagsView,
    tagsIcon: settingsStore.tagsIcon,
    fixedHeader: settingsStore.fixedHeader,
    fixedSidebar: settingsStore.fixedSidebar,
    sidebarLogo: settingsStore.sidebarLogo,
    dynamicTitle: settingsStore.dynamicTitle,
    footerVisible: settingsStore.footerVisible,
    headerVisible: settingsStore.headerVisible,
    menuVisible: settingsStore.menuVisible,
    menuHeaderVisible: settingsStore.menuHeaderVisible,
    splitMenus: settingsStore.splitMenus,
    contentWidth: settingsStore.contentWidth,
    layoutMode: settingsStore.layoutMode,
    colorWeak: settingsStore.colorWeak,
    isDark: settingsStore.isDark
  }),
  persistSetting,
  { deep: true }
)

onMounted(() => {
  handleColorWeakChange(settingsStore.colorWeak)
})

function setOverallStyle(value) {
  settingsStore.setDark(value === 'dark')
}

function setSideTheme(value) {
  settingsStore.sideTheme = value
}

function setThemeColor(color) {
  settingsStore.theme = color
  handleThemeStyle(color)
}

function setNavigationMode(value) {
  settingsStore.layoutMode = value

  if (value === 'side') {
    settingsStore.topNav = false
    permissionStore.setSidebarRouters(permissionStore.defaultRoutes)
    appStore.toggleSideBarHide(!settingsStore.menuVisible)
    if (settingsStore.menuVisible) appStore.openSideBar(false)
    return
  }

  settingsStore.topNav = true
  if (value === 'top') {
    permissionStore.setSidebarRouters([])
    appStore.toggleSideBarHide(true)
    return
  }

  if (!settingsStore.splitMenus) {
    permissionStore.setSidebarRouters(permissionStore.defaultRoutes)
    appStore.toggleSideBarHide(!settingsStore.menuVisible)
    if (settingsStore.menuVisible) appStore.openSideBar(false)
  }
}

function handleSplitMenusChange(value) {
  if (settingsStore.layoutMode !== 'mix') return
  if (!value) {
    permissionStore.setSidebarRouters(permissionStore.defaultRoutes)
    appStore.toggleSideBarHide(!settingsStore.menuVisible)
    if (settingsStore.menuVisible) appStore.openSideBar(false)
  }
}

function handleMenuVisibleChange(value) {
  if (!value) {
    appStore.toggleSideBarHide(true)
    return
  }
  if (settingsStore.layoutMode === 'top') {
    appStore.toggleSideBarHide(true)
    return
  }
  if (settingsStore.layoutMode === 'side' || !settingsStore.splitMenus) {
    permissionStore.setSidebarRouters(permissionStore.defaultRoutes)
  }
  appStore.toggleSideBarHide(false)
  appStore.openSideBar(false)
}

function handleColorWeakChange(value) {
  document.body.classList.toggle('color-weak', value)
}

function buildLayoutSetting() {
  return {
    layoutVersion: settingsStore.layoutVersion,
    theme: settingsStore.theme,
    sideTheme: settingsStore.sideTheme,
    topNav: settingsStore.topNav,
    tagsView: settingsStore.tagsView,
    tagsIcon: settingsStore.tagsIcon,
    fixedHeader: settingsStore.fixedHeader,
    fixedSidebar: settingsStore.fixedSidebar,
    sidebarLogo: settingsStore.sidebarLogo,
    dynamicTitle: settingsStore.dynamicTitle,
    footerVisible: settingsStore.footerVisible,
    headerVisible: settingsStore.headerVisible,
    menuVisible: settingsStore.menuVisible,
    menuHeaderVisible: settingsStore.menuHeaderVisible,
    splitMenus: settingsStore.splitMenus,
    contentWidth: settingsStore.contentWidth,
    layoutMode: settingsStore.layoutMode,
    colorWeak: settingsStore.colorWeak,
    isDark: settingsStore.isDark
  }
}

function persistSetting() {
  localStorage.setItem('layout-setting', JSON.stringify(buildLayoutSetting()))
}

async function copySetting() {
  const text = `export default ${JSON.stringify(buildLayoutSetting(), null, 2)}`
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      copyWithTextarea(text)
    }
    message.success('配置已复制')
  } catch (error) {
    copyWithTextarea(text)
    message.success('配置已复制')
  }
}

function copyWithTextarea(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

function openSetting() {
  showSettings.value = true
}

defineExpose({
  openSetting
})
</script>

<style lang="scss" scoped>
.setting-panel {
  color: var(--text-primary);
  font-family: "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  font-size: 14px;
  letter-spacing: 0;
}

.setting-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}

.setting-section-plain {
  margin-bottom: 10px;
  padding-bottom: 0;
  border-bottom: 0;
}

.setting-title {
  margin: 8px 0;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 22px;
}

.sub-title {
  margin-top: 18px;
}

.preview-list,
.color-list {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-list {
  flex-wrap: wrap;
}

.preview-card {
  position: relative;
  width: 44px;
  height: 36px;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 2px;
  background: #fff;
  box-shadow: 0 1px 2.5px rgba(0, 0, 0, 0.18);
  cursor: pointer;
}

.preview-card.active {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
}

.preview-check {
  position: absolute;
  right: 8px;
  bottom: 7px;
  color: var(--setting-theme);
  font-size: 14px;
}

.style-card {
  .style-sider,
  .style-header,
  .style-content {
    position: absolute;
    display: block;
  }

  .style-sider {
    top: 0;
    left: 0;
    width: 16px;
    height: 100%;
    background: #fff;
  }

  .style-header {
    top: 0;
    left: 16px;
    right: 0;
    height: 10px;
    background: #f5f5f5;
  }

  .style-content {
    top: 10px;
    left: 16px;
    right: 0;
    bottom: 0;
    background: #fff;
  }

  &.dark {
    background: #001529;

    .style-sider {
      background: #001529;
    }

    .style-header {
      background: #172336;
    }

    .style-content {
      background: #1f2d3d;
    }
  }
}

.color-list {
  gap: 8px;
}

.color-swatch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: 0;
  border-radius: 2px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.nav-card,
.menu-card {
  .nav-sider,
  .nav-header,
  .nav-content,
  .menu-sider,
  .menu-content,
  .menu-line {
    position: absolute;
    display: block;
  }
}

.nav-card {
  .nav-content {
    inset: 0;
    background: #fff;
    z-index: 0;
  }

  &.side .nav-sider,
  &.mix .nav-sider {
    top: 0;
    left: 0;
    width: 12px;
    height: 100%;
    background: #001529;
    z-index: 1;
  }

  &.top .nav-header,
  &.mix .nav-header {
    top: 0;
    left: 0;
    right: 0;
    height: 10px;
    background: #001529;
    z-index: 1;
  }

  &.side .nav-content {
    left: 12px;
  }

  &.top .nav-content {
    top: 10px;
  }

  &.mix .nav-content {
    top: 10px;
    left: 12px;
  }

  &.mix .nav-sider {
    top: 10px;
    height: calc(100% - 10px);
  }
}

.menu-card {
  .menu-sider {
    inset: 0 auto 0 0;
    width: 14px;
    background: #001529;
  }

  .menu-content {
    inset: 0 0 0 14px;
    background: #f5f7fa;
  }

  .menu-line {
    left: 4px;
    width: 6px;
    height: 2px;
    border-radius: 2px;
    background: rgba(255, 255, 255, 0.5);
  }

  .line-1 {
    top: 10px;
  }

  .line-2 {
    top: 16px;
  }

  &.theme-light {
    .menu-sider {
      background: #fff;
      border-right: 1px solid #f0f0f0;
    }

    .menu-line {
      background: #d9d9d9;
    }
  }
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  color: var(--text-primary);
  line-height: 22px;
}

.content-width-select {
  width: 80px;
}

.copy-button {
  height: 32px;
  border-radius: 4px;
  color: var(--text-primary);
  background: var(--card-bg);
  border-color: var(--border-color);
}

:global(.layout-setting-drawer .ant-drawer-body) {
  overflow-x: hidden;
  background: var(--card-bg);
}

:global(.layout-setting-drawer) {
  z-index: 4000;
}

:global(.layout-setting-drawer .ant-drawer-mask) {
  inset: 0;
}

:global(.layout-setting-drawer .ant-switch.ant-switch-checked) {
  background: var(--primary-color) !important;
}

:global(.layout-setting-drawer .ant-select-selector) {
  border-radius: 4px !important;
}

:global(body.color-weak) {
  filter: invert(80%);
}
</style>
