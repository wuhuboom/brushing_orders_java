<template>
  <div class="component-upload-media">
    <div ref="mediaGrid" class="media-grid">
      <div v-for="file in fileList" :key="file.uid || file.url || file.name" class="media-card">
        <video v-if="checkFileIsVideo(file)" class="media-thumb" :src="file.url"></video>
        <img v-else class="media-thumb" :src="file.url" alt="" />

        <div v-if="checkFileIsVideo(file)" class="video-play-indicator">
          <PlayCircleOutlined />
        </div>

        <div class="media-actions">
          <a-tooltip :title="checkFileIsVideo(file) ? '播放视频' : '查看图片'">
            <button class="media-action" type="button" @click="handlePreview(file)">
              <PlayCircleOutlined v-if="checkFileIsVideo(file)" />
              <EyeOutlined v-else />
            </button>
          </a-tooltip>
          <a-tooltip v-if="!disabled" title="删除">
            <button class="media-action danger" type="button" @click="handleRemove(file)">
              <DeleteOutlined />
            </button>
          </a-tooltip>
        </div>
      </div>

      <a-upload
        v-if="!disabled && fileList.length < limit"
        multiple
        :custom-request="uploadFile"
        :before-upload="handleBeforeUpload"
        :show-upload-list="false"
        :disabled="disabled"
        class="media-upload"
      >
        <div class="media-upload-card">
          <PlusOutlined />
          <span>上传</span>
        </div>
      </a-upload>
    </div>

    <div v-if="showTip && !disabled" class="upload-tip">
      请上传
      <template v-if="fileSize">
        大小不超过 <b>{{ fileSize }}MB</b>
      </template>
      <template v-if="fileType">
        格式为 <b>{{ fileType.join("/") }}</b>
      </template>
      的文件
    </div>

    <a-modal
      v-model:open="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :footer="null"
      destroy-on-close
    >
      <video
        v-if="dialogIsVideo"
        :src="dialogUrl"
        controls
        autoplay
        class="preview-video"
      ></video>
      <img v-else :src="dialogUrl" class="preview-image" alt="预览" />
    </a-modal>
  </div>
</template>

<script setup>
import axios from "axios"
import Sortable from "sortablejs"
import { DeleteOutlined, EyeOutlined, PlayCircleOutlined, PlusOutlined } from "@ant-design/icons-vue"
import { getToken } from "@/utils/auth"
import { isExternal } from "@/utils/validate"

const props = defineProps({
  modelValue: [String, Object, Array],
  action: {
    type: String,
    default: "/common/upload"
  },
  data: {
    type: Object
  },
  limit: {
    type: Number,
    default: 5
  },
  fileSize: {
    type: Number,
    default: 50
  },
  fileType: {
    type: Array,
    default: () => ["png", "jpg", "jpeg", "mp4", "mov", "webm"]
  },
  isShowTip: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  drag: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(["update:modelValue"])
const config = window.APP_CONFIG
const { proxy } = getCurrentInstance()
const mediaGrid = ref(null)
const number = ref(0)
const uploadList = ref([])
const dialogUrl = ref("")
const dialogVisible = ref(false)
const dialogIsVideo = ref(false)
const dialogTitle = ref("预览")
const baseUrl = config.baseApiUrl
const uploadUrl = ref(config.baseApiUrl + props.action)
const headers = ref({ Authorization: "Bearer " + getToken() })
const fileList = ref([])

const showTip = computed(() => props.isShowTip && (props.fileType || props.fileSize))

watch(
  () => props.modelValue,
  val => {
    if (val) {
      let temp = 1
      const list = Array.isArray(val) ? val : String(val).split(",")
      fileList.value = list.map(item => normalizeFileItem(item, temp++))
    } else {
      fileList.value = []
    }
  },
  { deep: true, immediate: true }
)

function normalizeFileItem(item, uidSeed) {
  if (typeof item === "string") {
    item = { name: item, url: item }
  }
  const url = item.url || item.name || ""
  return {
    ...item,
    name: item.name || url,
    url: getFullUrl(url),
    uid: item.uid || `${Date.now()}-${uidSeed}`
  }
}

function getFullUrl(url) {
  if (!url || isExternal(url) || url.indexOf(baseUrl) === 0 || url.indexOf("blob:") === 0) {
    return url
  }
  return baseUrl + url
}

function checkFileIsVideo(file) {
  if (file.type && file.type.indexOf("video") > -1) {
    return true
  }
  const checkStr = `${file.url || ""}${file.name || ""}`.toLowerCase()
  return /\.(mp4|mov|webm|ogg|mkv)$/.test(checkStr)
}

function isAllowedFile(file) {
  if (!props.fileType || !props.fileType.length) {
    return file.type.indexOf("image") > -1 || file.type.indexOf("video") > -1
  }
  const ext = file.name.lastIndexOf(".") > -1
    ? file.name.slice(file.name.lastIndexOf(".") + 1).toLowerCase()
    : ""
  return props.fileType.some(type => {
    const normalizedType = String(type).toLowerCase()
    if (file.type && file.type.toLowerCase().indexOf(normalizedType) > -1) {
      return true
    }
    return ext === normalizedType
  })
}

function handleBeforeUpload(file) {
  if (fileList.value.length + number.value >= props.limit) {
    proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`)
    return false
  }
  if (!isAllowedFile(file)) {
    proxy.$modal.msgError(`文件格式不正确，请上传${props.fileType.join("/")}格式文件!`)
    return false
  }
  if (file.name.includes(",")) {
    proxy.$modal.msgError("文件名不正确，不能包含英文逗号!")
    return false
  }
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      proxy.$modal.msgError(`上传文件大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }
  proxy.$modal.loading("正在上传文件，请稍候...")
  number.value++
  return true
}

function uploadFile({ file, onSuccess, onError }) {
  const formData = new FormData()
  formData.append("file", file)
  Object.entries(props.data || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      formData.append(key, value)
    }
  })

  axios
    .post(uploadUrl.value, formData, {
      headers: {
        ...headers.value,
        "Content-Type": "multipart/form-data"
      }
    })
    .then(res => {
      handleUploadSuccess(res.data)
      onSuccess?.(res.data, file)
    })
    .catch(error => {
      handleUploadError()
      onError?.(error)
    })
}

function handleUploadSuccess(res) {
  if (res.code === 200) {
    uploadList.value.push({ name: res.fileName, url: getFullUrl(res.fileName) })
    uploadedSuccessfully()
  } else {
    number.value = Math.max(number.value - 1, 0)
    proxy.$modal.closeLoading()
    proxy.$modal.msgError(res.msg)
    uploadedSuccessfully()
  }
}

function handleRemove(file) {
  const findex = fileList.value.findIndex(f => f.name === file.name || f.url === file.url)
  if (findex > -1) {
    fileList.value.splice(findex, 1)
    emit("update:modelValue", listToString(fileList.value))
  }
}

function uploadedSuccessfully() {
  if (number.value > 0 && uploadList.value.length === number.value) {
    fileList.value = fileList.value.filter(f => f.url !== undefined).concat(uploadList.value)
    uploadList.value = []
    number.value = 0
    emit("update:modelValue", listToString(fileList.value))
    proxy.$modal.closeLoading()
  }
}

function handleUploadError() {
  number.value = Math.max(number.value - 1, 0)
  proxy.$modal.msgError("上传失败")
  if (number.value === 0 || uploadList.value.length === number.value) {
    proxy.$modal.closeLoading()
  }
}

function handlePreview(file) {
  const fullUrl = getFullUrl(file.url || file.response?.fileName || "")
  dialogUrl.value = fullUrl
  dialogIsVideo.value = checkFileIsVideo(file) || checkFileIsVideo({ url: fullUrl })
  dialogTitle.value = dialogIsVideo.value ? "视频预览" : "预览"
  dialogVisible.value = true
}

function listToString(list, separator) {
  let strs = ""
  separator = separator || ","
  for (let i in list) {
    if (undefined !== list[i].url && list[i].url.indexOf("blob:") !== 0) {
      strs += list[i].url.replace(baseUrl, "") + separator
    }
  }
  return strs !== "" ? strs.substr(0, strs.length - 1) : ""
}

onMounted(() => {
  if (props.drag && !props.disabled) {
    nextTick(() => {
      const element = mediaGrid.value
      if (!element) {
        return
      }
      Sortable.create(element, {
        draggable: ".media-card",
        onEnd: evt => {
          if (evt.oldIndex === undefined || evt.newIndex === undefined) {
            return
          }
          const movedItem = fileList.value.splice(evt.oldIndex, 1)[0]
          fileList.value.splice(evt.newIndex, 0, movedItem)
          emit("update:modelValue", listToString(fileList.value))
        }
      })
    })
  }
})
</script>

<style scoped lang="scss">
.media-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.media-card,
.media-upload-card {
  position: relative;
  width: 104px;
  height: 104px;
  overflow: hidden;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fafafa;
}

.media-card {
  cursor: grab;

  &:hover .media-actions {
    opacity: 1;
  }
}

.media-thumb {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-actions {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s;
}

.media-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: #fff;
  cursor: pointer;
  background: transparent;
  border: 0;

  &:hover {
    color: #91caff;
  }

  &.danger:hover {
    color: #ff7875;
  }
}

.media-upload-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #8c8c8c;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;

  &:hover {
    color: #1677ff;
    border-color: #1677ff;
  }
}

.video-play-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #fff;
  pointer-events: none;
  background: rgba(0, 0, 0, 0.46);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.upload-tip {
  margin-top: 8px;
  color: #8c8c8c;
  font-size: 12px;

  b {
    color: #ff4d4f;
    font-weight: 500;
  }
}

.preview-video,
.preview-image {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  margin: 0 auto;
}
</style>
