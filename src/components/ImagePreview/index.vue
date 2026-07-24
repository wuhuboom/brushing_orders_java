<template>
  <div class="image-preview" :style="{ width: realWidth, height: realHeight }">
    <a-image-preview-group v-if="realSrcList.length" :items="realSrcList">
      <a-image :src="realSrc" :width="realWidth" :height="realHeight" :preview="true">
        <template #placeholder>
          <div class="image-slot">
            <PictureOutlined />
          </div>
        </template>
      </a-image>
    </a-image-preview-group>
    <div v-else class="image-slot">
      <PictureOutlined />
    </div>
  </div>
</template>

<script setup>
import { PictureOutlined } from "@ant-design/icons-vue";
import { isExternal } from "@/utils/validate";

const props = defineProps({
  src: {
    type: String,
    default: "",
  },
  width: {
    type: [Number, String],
    default: "",
  },
  height: {
    type: [Number, String],
    default: "",
  },
});

const config = window.APP_CONFIG;

const realSrc = computed(() => {
  if (!props.src) return "";
  const firstSrc = props.src.split(",")[0];
  return normalizeSrc(firstSrc);
});

const realSrcList = computed(() => {
  if (!props.src) return [];
  return props.src.split(",").filter(Boolean).map(normalizeSrc);
});

const realWidth = computed(() => (typeof props.width === "string" ? props.width : `${props.width}px`));
const realHeight = computed(() => (typeof props.height === "string" ? props.height : `${props.height}px`));

function normalizeSrc(src) {
  if (isExternal(src)) return src;
  return config.baseApiUrl + src;
}
</script>

<style lang="scss" scoped>
.image-preview {
  display: inline-flex;
  overflow: hidden;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: #f5f7fa;
  box-shadow: 0 0 0 1px #e5e7eb;

  :deep(.ant-image),
  :deep(.ant-image-img) {
    width: 100% !important;
    height: 100% !important;
  }

  :deep(.ant-image-img) {
    object-fit: cover;
    transition: transform 0.2s ease;
    cursor: pointer;
  }

  :deep(.ant-image-img:hover) {
    transform: scale(1.08);
  }
}

.image-slot {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 22px;
}
</style>
