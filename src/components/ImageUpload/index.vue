<template>
  <div ref="uploadRoot" class="component-upload-image">
    <a-upload
      v-model:file-list="fileList"
      :action="uploadImgUrl"
      :data="data"
      :headers="headers"
      :disabled="disabled"
      :max-count="limit"
      :multiple="limit > 1"
      list-type="picture-card"
      accept="image/*"
      @change="handleUploadChange"
      @preview="handlePictureCardPreview"
      :before-upload="handleBeforeUpload"
    >
      <div v-if="!disabled && fileList.length < limit" class="upload-card-trigger">
        <PlusOutlined />
        <div class="upload-card-text">上传</div>
      </div>
    </a-upload>

    <div class="upload-tip" v-if="showTip && !disabled">
      请上传
      <template v-if="fileSize">
        大小不超过 <b>{{ fileSize }}MB</b>
      </template>
      <template v-if="fileType">
        格式为 <b>{{ fileType.join("/") }}</b>
      </template>
      的文件
    </div>

    <a-modal v-model:open="dialogVisible" title="预览" :footer="null" width="800px">
      <img :src="dialogImageUrl" class="preview-image" />
    </a-modal>
  </div>
</template>

<script setup>
import { PlusOutlined } from "@ant-design/icons-vue";
import { getToken } from "@/utils/auth";
import { isExternal } from "@/utils/validate";
import Sortable from "sortablejs";

const props = defineProps({
  modelValue: [String, Object, Array],
  action: {
    type: String,
    default: "/common/upload",
  },
  data: {
    type: Object,
  },
  limit: {
    type: Number,
    default: 5,
  },
  fileSize: {
    type: Number,
    default: 5,
  },
  fileType: {
    type: Array,
    default: () => ["png", "jpg", "jpeg"],
  },
  isShowTip: {
    type: Boolean,
    default: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  drag: {
    type: Boolean,
    default: true,
  },
});

const { proxy } = getCurrentInstance();
const config = window.APP_CONFIG;
const emit = defineEmits(["update:modelValue"]);
const baseUrl = config.baseApiUrl;
const uploadRoot = ref();
const dialogImageUrl = ref("");
const dialogVisible = ref(false);
const uploadImgUrl = ref(config.baseApiUrl + props.action);
const headers = ref({ Authorization: "Bearer " + getToken() });
const fileList = ref([]);
const showTip = computed(() => props.isShowTip && (props.fileType || props.fileSize));
let sortableInstance = null;

watch(
  () => props.modelValue,
  (val) => {
    fileList.value = normalizeModelValue(val);
    nextTick(initSortable);
  },
  { deep: true, immediate: true }
);

function normalizeModelValue(value) {
  if (!value) return [];
  const list = Array.isArray(value) ? value : String(value).split(",");
  return list.filter(Boolean).map((item, index) => {
    const rawUrl = typeof item === "string" ? item : item.url || item.name || "";
    const url = normalizeUrl(rawUrl);
    return {
      uid: item.uid || `${Date.now()}-${index}`,
      name: item.name || rawUrl,
      status: "done",
      url,
    };
  });
}

function normalizeUrl(url) {
  if (!url) return "";
  if (isExternal(url) || url.startsWith("blob:") || url.startsWith("data:")) {
    return url;
  }
  return url.startsWith(baseUrl) ? url : baseUrl + url;
}

function handleBeforeUpload(file) {
  if (fileList.value.length >= props.limit) {
    proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个`);
    return false;
  }

  const extension = file.name.includes(".") ? file.name.split(".").pop().toLowerCase() : "";
  const mimeType = (file.type || "").toLowerCase();
  const isImg = props.fileType.length
    ? props.fileType.some((type) => {
        const normalizedType = String(type).toLowerCase();
        return mimeType.includes(normalizedType) || extension === normalizedType;
      })
    : mimeType.includes("image");

  if (!isImg) {
    proxy.$modal.msgError(`文件格式不正确，请上传 ${props.fileType.join("/")} 图片格式文件!`);
    return false;
  }

  if (file.name.includes(",")) {
    proxy.$modal.msgError("文件名不正确，不能包含英文逗号!");
    return false;
  }

  if (props.fileSize && file.size / 1024 / 1024 >= props.fileSize) {
    proxy.$modal.msgError(`上传图片大小不能超过 ${props.fileSize} MB!`);
    return false;
  }

  proxy.$modal.loading("正在上传图片，请稍候...");
  return true;
}

function handleUploadChange({ file, fileList: nextFileList }) {
  fileList.value = nextFileList.slice(0, props.limit);

  if (file.status === "done") {
    proxy.$modal.closeLoading();
    if (file.response?.code === 200) {
      file.url = normalizeUrl(file.response.fileName);
      file.name = file.response.fileName;
      syncModelValue();
      return;
    }
    fileList.value = fileList.value.filter((item) => item.uid !== file.uid);
    proxy.$modal.msgError(file.response?.msg || "上传图片失败");
    syncModelValue();
    return;
  }

  if (file.status === "error") {
    proxy.$modal.closeLoading();
    proxy.$modal.msgError("上传图片失败");
    fileList.value = fileList.value.filter((item) => item.uid !== file.uid);
    syncModelValue();
    return;
  }

  if (file.status === "removed") {
    syncModelValue();
  }
}

function handlePictureCardPreview(file) {
  dialogImageUrl.value = file.url || file.thumbUrl;
  dialogVisible.value = true;
}

function syncModelValue() {
  emit("update:modelValue", listToString(fileList.value));
  nextTick(initSortable);
}

function listToString(list, separator = ",") {
  return list
    .map((item) => {
      const url = item.response?.fileName || item.url || item.name || "";
      return url && !url.startsWith("blob:") ? url.replace(baseUrl, "") : "";
    })
    .filter(Boolean)
    .join(separator);
}

function initSortable() {
  if (!props.drag || props.disabled || sortableInstance || !uploadRoot.value) return;
  const element = uploadRoot.value.querySelector(".ant-upload-list");
  if (!element) return;
  sortableInstance = Sortable.create(element, {
    animation: 150,
    onEnd: (evt) => {
      if (evt.oldIndex === evt.newIndex) return;
      const movedItem = fileList.value.splice(evt.oldIndex, 1)[0];
      fileList.value.splice(evt.newIndex, 0, movedItem);
      syncModelValue();
    },
  });
}

onBeforeUnmount(() => {
  sortableInstance?.destroy();
  sortableInstance = null;
});
</script>

<style scoped lang="scss">
.component-upload-image {
  :deep(.ant-upload-list-picture-card) {
    gap: 8px;
  }

  :deep(.ant-upload-list-picture-card .ant-upload-list-item-container),
  :deep(.ant-upload.ant-upload-select-picture-card) {
    width: 86px;
    height: 86px;
  }

  :deep(.ant-upload-select),
  :deep(.ant-upload.ant-upload-select-picture-card),
  :deep(.ant-upload-list-picture-card .ant-upload-list-item) {
    background: var(--control-alt-bg) !important;
    border-color: var(--border-color) !important;
  }
}

.upload-card-trigger {
  color: var(--text-secondary);
  text-align: center;
}

.upload-card-text {
  margin-top: 8px;
  font-size: 12px;
}

.upload-tip {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;

  b {
    color: #ff4d4f;
    font-weight: 500;
  }
}

.preview-image {
  display: block;
  max-width: 100%;
  margin: 0 auto;
}
</style>
