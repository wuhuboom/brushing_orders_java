<template>
  <div class="component-upload-media">
    <el-upload
      multiple
      :disabled="disabled"
      :action="uploadUrl"
      list-type="picture-card"
      :on-success="handleUploadSuccess"
      :before-upload="handleBeforeUpload"
      :data="data"
      :limit="limit"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      ref="mediaUpload"
      :before-remove="handleDelete"
      :show-file-list="true"
      :headers="headers"
      :file-list="fileList"
      :class="{ hide: fileList.length >= limit }"
    >
      <el-icon class="avatar-uploader-icon"><plus /></el-icon>

      <template #file="{ file }">
        <div style="width: 100%; height: 100%; position: relative">
          <template v-if="checkFileIsVideo(file)">
            <video
              class="el-upload-list__item-thumbnail"
              :src="file.url"
              style="
                object-fit: cover;
                width: 100%;
                height: 100%;
                display: block;
              "
            ></video>
            <div class="video-play-indicator">
              <el-icon :size="20" color="#fff"><VideoPlay /></el-icon>
            </div>
          </template>

          <img
            v-else
            class="el-upload-list__item-thumbnail"
            :src="file.url"
            alt=""
          />

          <span class="el-upload-list__item-actions">
            <span
              class="el-upload-list__item-preview"
              @click="handlePreview(file)"
              :title="checkFileIsVideo(file) ? '播放视频' : '查看图片'"
            >
              <el-icon v-if="checkFileIsVideo(file)"><VideoPlay /></el-icon>
              <el-icon v-else><ZoomIn /></el-icon>
            </span>

            <span
              v-if="!disabled"
              class="el-upload-list__item-delete"
              @click="handleRemove(file)"
            >
              <el-icon><Delete /></el-icon>
            </span>
          </span>
        </div>
      </template>
    </el-upload>

    <div class="el-upload__tip" v-if="showTip && !disabled">
      请上传
      <template v-if="fileSize">
        大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b>
      </template>
      <template v-if="fileType">
        格式为 <b style="color: #f56c6c">{{ fileType.join("/") }}</b>
      </template>
      的文件
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      append-to-body
      destroy-on-close
    >
      <template v-if="dialogIsVideo">
        <video
          :src="dialogUrl"
          controls
          autoplay
          style="display: block; max-width: 100%; margin: 0 auto"
        ></video>
      </template>
      <template v-else>
        <img
          :src="dialogUrl"
          style="display: block; max-width: 100%; margin: 0 auto"
        />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getToken } from "@/utils/auth";
import { isExternal } from "@/utils/validate";
import Sortable from "sortablejs";
// 引入 VideoPlay 图标
import { Plus, ZoomIn, Delete, VideoPlay } from "@element-plus/icons-vue";

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
    default: 50,
  },
  fileType: {
    type: Array,
    default: () => ["png", "jpg", "jpeg", "mp4", "mov", "webm"],
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

const emit = defineEmits(["update:modelValue"]);
const config = window.APP_CONFIG;
const { proxy } = getCurrentInstance();
const number = ref(0);
const uploadList = ref([]);
const dialogUrl = ref("");
const dialogVisible = ref(false);
const dialogIsVideo = ref(false);
const dialogTitle = ref("预览");
const baseUrl = config.baseApiUrl;
const uploadUrl = ref(config.baseApiUrl + props.action);
const headers = ref({ Authorization: "Bearer " + getToken() });
const fileList = ref([]);

const showTip = computed(
  () => props.isShowTip && (props.fileType || props.fileSize)
);

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      const list = Array.isArray(val) ? val : String(val).split(",");
      fileList.value = list.map((item) => {
        if (typeof item === "string") {
          if (item.indexOf(baseUrl) === -1 && !isExternal(item)) {
            item = { name: baseUrl + item, url: baseUrl + item };
          } else {
            item = { name: item, url: item };
          }
        }
        return item;
      });
    } else {
      fileList.value = [];
      return [];
    }
  },
  { deep: true, immediate: true }
);

function checkFileIsVideo(file) {
  if (file.type && file.type.indexOf("video") > -1) {
    return true;
  }
  const url = file.url || "";
  const name = file.name || "";
  const checkStr = (url + name).toLowerCase();
  return /\.(mp4|mov|webm|ogg|mkv)$/.test(checkStr);
}

function isAllowedFile(file) {
  if (!props.fileType || !props.fileType.length) {
    return file.type.indexOf("image") > -1 || file.type.indexOf("video") > -1;
  }
  let ext = "";
  if (file.name.lastIndexOf(".") > -1) {
    ext = file.name.slice(file.name.lastIndexOf(".") + 1).toLowerCase();
  }
  const allowed = props.fileType.some((type) => {
    if (file.type && file.type.indexOf(type) > -1) return true;
    if (ext && ext.indexOf(type) > -1) return true;
    return false;
  });
  return allowed;
}

function handleBeforeUpload(file) {
  if (!isAllowedFile(file)) {
    proxy.$modal.msgError(
      `文件格式不正确，请上传${props.fileType.join("/")}格式文件!`
    );
    return false;
  }
  if (file.name.includes(",")) {
    proxy.$modal.msgError("文件名不正确，不能包含英文逗号!");
    return false;
  }
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize;
    if (!isLt) {
      proxy.$modal.msgError(`上传文件大小不能超过 ${props.fileSize} MB!`);
      return false;
    }
  }
  proxy.$modal.loading("正在上传文件，请稍候...");
  number.value++;
  return true;
}

function handleExceed() {
  proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`);
}

function handleUploadSuccess(res, file) {
  if (res.code === 200) {
    uploadList.value.push({ name: res.fileName, url: res.fileName });
    uploadedSuccessfully();
  } else {
    number.value--;
    proxy.$modal.closeLoading();
    proxy.$modal.msgError(res.msg);
    proxy.$refs.mediaUpload.handleRemove(file);
    uploadedSuccessfully();
  }
}

function handleRemove(file) {
  const findex = fileList.value.findIndex(
    (f) => f.name === file.name || f.url === file.url
  );
  if (findex > -1) {
    fileList.value.splice(findex, 1);
    emit("update:modelValue", listToString(fileList.value));
  }
  if (proxy.$refs.mediaUpload) {
    proxy.$refs.mediaUpload.handleRemove(file);
  }
}

function handleDelete(file) {
  const findex = fileList.value.map((f) => f.name).indexOf(file.name);
  if (findex > -1 && uploadList.value.length === number.value) {
    fileList.value.splice(findex, 1);
    emit("update:modelValue", listToString(fileList.value));
    return false;
  }
}

function uploadedSuccessfully() {
  if (number.value > 0 && uploadList.value.length === number.value) {
    fileList.value = fileList.value
      .filter((f) => f.url !== undefined)
      .concat(uploadList.value);
    uploadList.value = [];
    number.value = 0;
    emit("update:modelValue", listToString(fileList.value));
    proxy.$modal.closeLoading();
  }
}

function handleUploadError() {
  proxy.$modal.msgError("上传失败");
  proxy.$modal.closeLoading();
}

function handlePreview(file) {
  const url = file.url || file.response?.fileName;
  const fullUrl = url
    ? isExternal(url) || url.indexOf(baseUrl) === 0
      ? url
      : baseUrl + url
    : "";
  dialogUrl.value = fullUrl;

  const isVideo = checkFileIsVideo(file) || checkFileIsVideo({ url: fullUrl });

  dialogIsVideo.value = isVideo;
  dialogTitle.value = isVideo ? "视频预览" : "预览";
  dialogVisible.value = true;
}

function listToString(list, separator) {
  let strs = "";
  separator = separator || ",";
  for (let i in list) {
    if (undefined !== list[i].url && list[i].url.indexOf("blob:") !== 0) {
      strs += list[i].url.replace(baseUrl, "") + separator;
    }
  }
  return strs != "" ? strs.substr(0, strs.length - 1) : "";
}

onMounted(() => {
  if (props.drag && !props.disabled) {
    nextTick(() => {
      const element =
        proxy.$refs.mediaUpload?.$el?.querySelector(".el-upload-list");
      if (element) {
        Sortable.create(element, {
          onEnd: (evt) => {
            const movedItem = fileList.value.splice(evt.oldIndex, 1)[0];
            fileList.value.splice(evt.newIndex, 0, movedItem);
            emit("update:modelValue", listToString(fileList.value));
          },
        });
      }
    });
  }
});
</script>

<style scoped lang="scss">
/* 保持原样式 */
:deep(.hide .el-upload--picture-card) {
  display: none;
}

:deep(.el-upload.el-upload--picture-card.is-disabled) {
  display: none !important;
}

/* 新增样式：视频中间的播放指示器 */
.video-play-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.4);
  border-radius: 50%;
  width: 30px;
  height: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  pointer-events: none; /* 让鼠标事件穿透到下层，不然可能影响hover显示遮罩 */
}
</style>
