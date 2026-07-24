<template>
  <div>
    <a-dropdown :trigger="['click']">
      <button class="size-icon--style" type="button">
        <svg-icon class-name="size-icon" icon-class="size" />
      </button>
      <template #overlay>
        <a-menu @click="({ key }) => handleSetSize(key)">
          <a-menu-item v-for="item of sizeOptions" :key="item.value" :disabled="size === item.value">
            {{ item.label }}
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<script setup>
import useAppStore from "@/store/modules/app"

const appStore = useAppStore()
const size = computed(() => appStore.size)
const { proxy } = getCurrentInstance()
const sizeOptions = ref([
  { label: "较大", value: "large" },
  { label: "默认", value: "default" },
  { label: "稍小", value: "small" },
])

function handleSetSize(size) {
  proxy.$modal.loading("正在设置布局大小，请稍候...")
  appStore.setSize(size)
  setTimeout("window.location.reload()", 1000)
}
</script>

<style lang="scss" scoped>
.size-icon--style {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 50px;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 18px;
}
</style>
