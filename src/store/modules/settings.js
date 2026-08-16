import defaultSettings from '@/settings'
import { useDynamicTitle } from '@/utils/dynamicTitle'

const colorSchemeStorageKey = 'vueuse-color-scheme'

function getInitialDarkMode() {
  const stored = localStorage.getItem(colorSchemeStorageKey)
  if (stored === 'dark') return true
  return false
}

function applyDarkMode(value) {
  document.documentElement.classList.toggle('dark', value)
  localStorage.setItem(colorSchemeStorageKey, value ? 'dark' : 'light')
}

const initialDarkMode = getInitialDarkMode()
applyDarkMode(initialDarkMode)

const {
  layoutVersion,
  theme,
  sideTheme,
  showSettings,
  topNav,
  tagsView,
  tagsIcon,
  fixedHeader,
  fixedSidebar,
  sidebarLogo,
  dynamicTitle,
  footerVisible,
  footerContent,
  headerVisible,
  menuVisible,
  menuHeaderVisible,
  splitMenus,
  contentWidth,
  layoutMode,
  colorWeak
} = defaultSettings

function getStorageSetting() {
  try {
    const setting = JSON.parse(localStorage.getItem('layout-setting')) || {}
    return setting.layoutVersion === defaultSettings.layoutVersion ? setting : {}
  } catch (error) {
    return {}
  }
}

const storageSetting = getStorageSetting()

const useSettingsStore = defineStore(
  'settings',
  {
    state: () => ({
      title: '',
      layoutVersion,
      theme: storageSetting.theme && storageSetting.theme !== '#1890ff' ? storageSetting.theme : theme,
      sideTheme: storageSetting.sideTheme || sideTheme,
      showSettings: showSettings,
      layoutMode: storageSetting.layoutMode || layoutMode,
      topNav: storageSetting.topNav === undefined ? topNav : storageSetting.topNav,
      tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
      tagsIcon: storageSetting.tagsIcon === undefined ? tagsIcon : storageSetting.tagsIcon,
      fixedHeader: storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
      fixedSidebar: storageSetting.fixedSidebar === undefined ? fixedSidebar : storageSetting.fixedSidebar,
      sidebarLogo: storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
      dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle,
      footerVisible: storageSetting.footerVisible === undefined ? footerVisible : storageSetting.footerVisible,
      footerContent: footerContent,
      headerVisible: storageSetting.headerVisible === undefined ? headerVisible : storageSetting.headerVisible,
      menuVisible: storageSetting.menuVisible === undefined ? menuVisible : storageSetting.menuVisible,
      menuHeaderVisible: storageSetting.menuHeaderVisible === undefined ? menuHeaderVisible : storageSetting.menuHeaderVisible,
      splitMenus: storageSetting.splitMenus === undefined ? splitMenus : storageSetting.splitMenus,
      contentWidth: storageSetting.contentWidth || contentWidth,
      colorWeak: storageSetting.colorWeak === undefined ? colorWeak : storageSetting.colorWeak,
      isDark: initialDarkMode
    }),
    actions: {
      // 修改布局设置
      changeSetting(data) {
        const { key, value } = data
        if (this.hasOwnProperty(key)) {
          this[key] = value
        }
      },
      // 设置网页标题
      setTitle(title) {
        this.title = title
        useDynamicTitle()
      },
      // 切换暗黑模式
      toggleTheme() {
        this.setDark(!this.isDark)
      },
      // 设置整体暗黑模式
      setDark(value) {
        this.isDark = value
        applyDarkMode(value)
      }
    }
  })

export default useSettingsStore
