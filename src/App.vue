<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted, nextTick } from "vue";
import useSettingsStore from "@/store/modules/settings";
import { handleThemeStyle } from "@/utils/theme";
import { resolveElementLocale } from "@/locales";
import { useI18n } from "vue-i18n";

const { locale } = useI18n();
const elementLocale = computed(() => resolveElementLocale(locale.value));

onMounted(() => {
  nextTick(() => {
    handleThemeStyle(useSettingsStore().theme);
  });
});
</script>
