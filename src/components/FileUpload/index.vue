<template>
  <div class="upload-file">
    <a-upload
      v-if="!disabled"
      multiple
      :custom-request="uploadFile"
      :before-upload="handleBeforeUpload"
      :show-upload-list="false"
      :disabled="disabled"
      class="upload-file-uploader"
    >
      <a-button type="primary">
        <UploadOutlined />
        选取文件
      </a-button>
    </a-upload>

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

    <transition-group
      ref="uploadFileList"
      class="upload-file-list"
      name="upload-list-fade"
      tag="ul"
    >
      <li v-for="(file, index) in fileList" :key="file.uid" class="upload-file-list-item">
        <a class="file-link" :href="getFileUrl(file.url)" target="_blank" rel="noopener noreferrer">
          <FileOutlined />
          <span>{{ getFileName(file.name) }}</span>
        </a>
        <a-button v-if="!disabled" type="link" danger size="small" @click="handleDelete(index)">
          删除
        </a-button>
      </li>
    </transition-group>
  </div>
</template>

<script setup>
import axios from "axios"
import Sortable from "sortablejs"
import { FileOutlined, UploadOutlined } from "@ant-design/icons-vue"
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
    default: 5
  },
  fileType: {
    type: Array,
    default: () => ["doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "pdf"]
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

const { proxy } = getCurrentInstance()
const config = window.APP_CONFIG
const emit = defineEmits(["update:modelValue"])
const uploadFileList = ref(null)
const number = ref(0)
const uploadList = ref([])
const baseUrl = config.baseApiUrl
const uploadFileUrl = ref(config.baseApiUrl + props.action)
const headers = ref({ Authorization: "Bearer " + getToken() })
const fileList = ref([])
const showTip = computed(() => props.isShowTip && (props.fileType || props.fileSize))

watch(
  () => props.modelValue,
  val => {
    if (val) {
      let temp = 1
      const list = Array.isArray(val) ? val : String(val).split(",")
      fileList.value = list.map(item => {
        if (typeof item === "string") {
          item = { name: item, url: item }
        }
        item.uid = item.uid || new Date().getTime() + temp++
        return item
      })
    } else {
      fileList.value = []
    }
  },
  { deep: true, immediate: true }
)

function handleBeforeUpload(file) {
  if (fileList.value.length + number.value >= props.limit) {
    proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`)
    return false
  }

  if (props.fileType.length) {
    const fileName = file.name.split(".")
    const fileExt = fileName[fileName.length - 1]
    const isTypeOk = props.fileType.indexOf(fileExt) >= 0
    if (!isTypeOk) {
      proxy.$modal.msgError(`文件格式不正确，请上传${props.fileType.join("/")}格式文件!`)
      return false
    }
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
    .post(uploadFileUrl.value, formData, {
      headers: {
        ...headers.value,
        "Content-Type": "multipart/form-data"
      }
    })
    .then(res => {
      handleUploadSuccess(res.data, file)
      onSuccess?.(res.data, file)
    })
    .catch(error => {
      handleUploadError(error)
      onError?.(error)
    })
}

function handleUploadError() {
  number.value = Math.max(number.value - 1, 0)
  proxy.$modal.msgError("上传文件失败")
  if (number.value === 0 || uploadList.value.length === number.value) {
    proxy.$modal.closeLoading()
  }
}

function handleUploadSuccess(res) {
  if (res.code === 200) {
    uploadList.value.push({ name: res.fileName, url: res.fileName })
    uploadedSuccessfully()
  } else {
    number.value = Math.max(number.value - 1, 0)
    proxy.$modal.closeLoading()
    proxy.$modal.msgError(res.msg)
    uploadedSuccessfully()
  }
}

function handleDelete(index) {
  fileList.value.splice(index, 1)
  emit("update:modelValue", listToString(fileList.value))
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

function getFileUrl(url) {
  if (!url || isExternal(url) || url.indexOf(baseUrl) === 0) {
    return url
  }
  return `${baseUrl}${url}`
}

function getFileName(name) {
  if (name.lastIndexOf("/") > -1) {
    return name.slice(name.lastIndexOf("/") + 1)
  }
  return name
}

function listToString(list, separator) {
  let strs = ""
  separator = separator || ","
  for (let i in list) {
    if (list[i].url) {
      strs += list[i].url + separator
    }
  }
  return strs !== "" ? strs.substr(0, strs.length - 1) : ""
}

onMounted(() => {
  if (props.drag && !props.disabled) {
    nextTick(() => {
      const element = uploadFileList.value?.$el || uploadFileList.value
      if (!element) {
        return
      }
      Sortable.create(element, {
        ghostClass: "file-upload-darg",
        onEnd: evt => {
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
.file-upload-darg {
  opacity: 0.5;
  background: #e6f4ff;
}

.upload-file-uploader {
  margin-bottom: 6px;
}

.upload-tip {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;

  b {
    color: #ff4d4f;
    font-weight: 500;
  }
}

.upload-file-list {
  padding: 0;
  margin: 8px 0 0;
  list-style: none;
}

.upload-file-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 8px 0 10px;
  margin-bottom: 8px;
  color: rgba(0, 0, 0, 0.88);
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
}

.file-link {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 6px;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.upload-list-fade-enter-active,
.upload-list-fade-leave-active {
  transition: opacity 0.2s;
}

.upload-list-fade-enter-from,
.upload-list-fade-leave-to {
  opacity: 0;
}
</style>
