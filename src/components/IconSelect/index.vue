<template>
  <div class="icon-body">
    <a-input
      v-model:value="iconName"
      class="icon-search"
      allow-clear
      placeholder="请输入图标名称"
      @input="filterIcons"
    >
      <template #prefix><SearchOutlined /></template>
    </a-input>
    <div class="icon-list">
      <div class="list-container">
        <div v-for="(item, index) in iconList" :key="index" class="icon-item-wrapper" @click="selectedIcon(item)">
          <div :class="['icon-item', { active: activeIcon === item }]">
            <svg-icon :icon-class="item" class-name="icon" style="height: 25px;width: 16px;" />
            <span>{{ item }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { SearchOutlined } from "@ant-design/icons-vue"
import icons from "./requireIcons"

defineProps({
  activeIcon: {
    type: String
  }
})

const iconName = ref("")
const iconList = ref(icons)
const emit = defineEmits(["selected"])

function filterIcons() {
  iconList.value = iconName.value
    ? icons.filter(item => item.includes(iconName.value))
    : icons
}

function selectedIcon(name) {
  emit("selected", name)
}

function reset() {
  iconName.value = ""
  iconList.value = icons
}

defineExpose({
  reset
})
</script>

<style lang="scss" scoped>
.icon-body {
  width: 100%;
  padding: 10px;

  .icon-search {
    position: relative;
    margin-bottom: 8px;
  }

  .icon-list {
    height: 220px;
    overflow: auto;

    .list-container {
      display: flex;
      flex-wrap: wrap;
      gap: 4px 0;

      .icon-item-wrapper {
        width: calc(100% / 3);
        height: 28px;
        line-height: 28px;
        cursor: pointer;
        display: flex;

        .icon-item {
          display: flex;
          align-items: center;
          max-width: 100%;
          height: 100%;
          padding: 0 6px;
          border-radius: 4px;

          &:hover,
          &.active {
            background: #f0f5ff;
            color: #1677ff;
          }

          .icon {
            flex-shrink: 0;
          }

          span {
            display: inline-block;
            padding-left: 4px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
    }
  }
}
</style>
