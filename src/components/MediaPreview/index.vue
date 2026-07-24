<template>
  <div class="media-preview">
    <video
      v-if="isVideo"
      :src="realSrc"
      controls
      :style="{ width: realWidth, height: realHeight }"
    />
    <a-image-preview-group v-else :items="realSrcList">
      <a-image
        :src="realSrc"
        :style="{ width: realWidth, height: realHeight }"
        class="preview-image"
      />
    </a-image-preview-group>
  </div>
</template>

<script setup>
import { isExternal } from "@/utils/validate"

const props = defineProps({
  src: {
    type: String,
    default: ""
  },
  width: {
    type: [Number, String],
    default: ""
  },
  height: {
    type: [Number, String],
    default: ""
  }
})

const config = window.APP_CONFIG

const realSrc = computed(() => {
  if (!props.src) {
    return ""
  }
  const firstSrc = props.src.split(",")[0]
  if (isExternal(firstSrc)) {
    return firstSrc
  }
  return config.baseApiUrl + firstSrc
})

const realSrcList = computed(() => {
  if (!props.src) {
    return []
  }
  return props.src.split(",").map(item => (isExternal(item) ? item : config.baseApiUrl + item))
})

const realWidth = computed(() => (typeof props.width === "string" ? props.width : `${props.width}px`))
const realHeight = computed(() => (typeof props.height === "string" ? props.height : `${props.height}px`))

const isVideo = computed(() => {
  if (!props.src) {
    return false
  }
  const first = props.src.split(",")[0].toLowerCase()
  if (/\.(mp4|mov|webm|ogg|mkv)$/.test(first)) {
    return true
  }
  return first.indexOf("data:video") === 0
})
</script>

<style scoped lang="scss">
.media-preview {
  display: inline-flex;
  line-height: 1;
}

.preview-image {
  overflow: hidden;
  border-radius: 5px;
  background-color: #f5f5f5;
  box-shadow: 0 0 5px 1px rgba(0, 0, 0, 0.16);
  object-fit: cover;
  cursor: pointer;

  :deep(.ant-image-img) {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s;
  }

  &:hover :deep(.ant-image-img) {
    transform: scale(1.08);
  }
}

video {
  border-radius: 5px;
  background-color: #000;
  box-shadow: 0 0 5px 1px rgba(0, 0, 0, 0.16);
}
</style>
