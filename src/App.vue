<template>
  <a-config-provider :locale="zhCN" :theme="antdThemeConfig">
    <router-view />
  </a-config-provider>
</template>

<script setup>
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { theme as antdTheme } from 'ant-design-vue'
import useSettingsStore from '@/store/modules/settings'
import { handleThemeStyle } from '@/utils/theme'

const settingsStore = useSettingsStore()

const antdThemeConfig = computed(() => ({
  algorithm: settingsStore.isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
  token: {
    colorPrimary: settingsStore.theme,
    borderRadius: 6,
    controlHeight: 32,
    controlHeightLG: 40,
    controlHeightSM: 24,
    fontFamily: 'var(--app-font-family)',
    fontSize: 14,
    fontSizeLG: 14,
    lineHeight: 1.5714285714,
    lineHeightLG: 1.5714285714
  }
}))

onMounted(() => {
  nextTick(() => {
    // 初始化主题样式
    handleThemeStyle(settingsStore.theme)
  })
})
</script>
