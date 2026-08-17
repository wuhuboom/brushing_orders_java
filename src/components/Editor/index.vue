<template>
  <div>
    <input
      v-if="type == 'url'"
      ref="uploadRef"
      type="file"
      accept="image/jpeg,image/jpg,image/png,image/svg+xml"
      class="editor-img-uploader"
      hidden
      @change="handleImageSelected"
    />
  </div>
  <div
    class="editor"
    :class="{ 'editor-readonly': readOnly }"
    :style="editorStyles"
  >
    <quill-editor
      ref="quillEditorRef"
      v-model:content="content"
      contentType="html"
      @textChange="emitContent"
      :options="options"
    />
  </div>
</template>

<script setup>
import axios from "axios";
import { Quill, QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import { getToken } from "@/utils/auth";
import { resolveApiResourceUrl } from "@/utils/apiResourceUrl";

const FONT_SIZES = [
  "12px",
  "14px",
  "16px",
  "18px",
  "20px",
  "24px",
  "28px",
  "30px",
  "32px",
  "36px",
  "40px",
  "48px",
  "56px",
  "64px",
  "72px",
  "96px",
  "120px",
  "144px",
];
const FONT_SIZE_OPTIONS = FONT_SIZES.map((size) =>
  size === "14px" ? false : size
);
const SizeStyle = Quill.import("attributors/style/size");
SizeStyle.whitelist = FONT_SIZES;
Quill.register(SizeStyle, true);

const { proxy } = getCurrentInstance();
const emit = defineEmits(["update:modelValue"]);

const config = window.APP_CONFIG;
const quillEditorRef = ref();
const uploadRef = ref();
const uploadUrl = ref(config.baseApiUrl + "/common/upload"); // 上传的图片服务器地址
const headers = ref({
  Authorization: "Bearer " + getToken(),
});

const props = defineProps({
  /* 编辑器的内容 */
  modelValue: {
    type: String,
  },
  /* 高度 */
  height: {
    type: Number,
    default: null,
  },
  /* 最小高度 */
  minHeight: {
    type: Number,
    default: null,
  },
  /* 只读 */
  readOnly: {
    type: Boolean,
    default: false,
  },
  /* 上传文件大小限制(MB) */
  fileSize: {
    type: Number,
    default: 5,
  },
  /* 类型（base64格式、url格式） */
  type: {
    type: String,
    default: "url",
  },
});

const options = ref({
  theme: "snow",
  bounds: document.body,
  debug: "warn",
  modules: {
    // 工具栏配置
    toolbar: [
      [{ font: [] }],
      ["bold", "italic", "underline", "strike"], // 加粗 斜体 下划线 删除线
      ["blockquote", "code-block"], // 引用  代码块
      [{ list: "ordered" }, { list: "bullet" }], // 有序、无序列表
      [{ indent: "-1" }, { indent: "+1" }], // 缩进
      [{ script: "sub" }, { script: "super" }],
      [{ size: FONT_SIZE_OPTIONS }], // 字体大小
      [{ header: [1, 2, 3, 4, 5, 6, false] }], // 标题
      [{ color: [] }, { background: [] }], // 字体颜色、字体背景颜色
      [{ align: [] }, { direction: "rtl" }], // 对齐方式与书写方向
      ["clean"], // 清除文本格式
      ["link", "image", "video"], // 链接、图片、视频
    ],
  },
  placeholder: "请输入内容",
  readOnly: props.readOnly,
});

const editorStyles = computed(() => {
  const style = {};
  if (props.minHeight) {
    style["--editor-min-height"] = `${props.minHeight}px`;
  }
  if (props.height) {
    style["--editor-height"] = `${props.height}px`;
  }
  return style;
});

const content = ref("");
watch(
  () => props.modelValue,
  (v) => {
    if (v !== content.value) {
      content.value = v == undefined ? "" : v;
    }
  },
  { immediate: true }
);

function emitContent() {
  const value = /^(<p><br><\/p>|<p><\/p>)$/.test(content.value || "")
    ? ""
    : content.value;
  emit("update:modelValue", value);
}

// 如果设置了上传地址则自定义图片上传事件
onMounted(() => {
  if (props.type == "url") {
    let quill = quillEditorRef.value.getQuill();
    let toolbar = quill.getModule("toolbar");
    toolbar.addHandler("image", (value) => {
      if (value) {
        uploadRef.value?.click();
      } else {
        quill.format("image", false);
      }
    });
    quill.root.addEventListener("paste", handlePasteCapture, true);
  }
});

// 上传前校检格式和大小
function handleBeforeUpload(file) {
  const type = ["image/jpeg", "image/jpg", "image/png", "image/svg"];
  const isJPG = type.includes(file.type);
  //检验文件格式
  if (!isJPG) {
    proxy.$modal.msgError(`图片格式错误!`);
    return false;
  }
  // 校检文件大小
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize;
    if (!isLt) {
      proxy.$modal.msgError(`上传文件大小不能超过 ${props.fileSize} MB!`);
      return false;
    }
  }
  return true;
}

// 上传成功处理
function handleUploadSuccess(res, file) {
  // 如果上传成功
  if (res.code == 200) {
    // 获取富文本实例
    let quill = toRaw(quillEditorRef.value).getQuill();
    // 获取光标位置
    let length = quill.selection.savedRange.index;
    // 使用运行时 config.js 的 API 地址生成绝对路径，避免把 /dev-api 之类的相对代理路径存入富文本
    const imageUrl = resolveApiResourceUrl(
      res.fileName,
      config.baseApiUrl,
      window.location.origin
    );
    quill.insertEmbed(length, "image", imageUrl);
    // 调整光标到最后
    quill.setSelection(length + 1);
  } else {
    proxy.$modal.msgError("图片插入失败");
  }
}

// 上传失败处理
function handleUploadError() {
  proxy.$modal.msgError("图片插入失败");
}

function handleImageSelected(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file || handleBeforeUpload(file) === false) {
    return;
  }
  insertImage(file);
}

// 复制粘贴图片处理
function handlePasteCapture(e) {
  const clipboard = e.clipboardData || window.clipboardData;
  if (clipboard && clipboard.items) {
    for (let i = 0; i < clipboard.items.length; i++) {
      const item = clipboard.items[i];
      if (item.type.indexOf("image") !== -1) {
        e.preventDefault();
        const file = item.getAsFile();
        insertImage(file);
      }
    }
  }
}

function insertImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  axios
    .post(uploadUrl.value, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
        Authorization: headers.value.Authorization,
      },
    })
    .then((res) => {
      handleUploadSuccess(res.data);
    })
    .catch(() => {
      handleUploadError();
    });
}
</script>
<style scoped>
.editor-container {
  width: 100%;
}

.editor {
  width: 100%;
  overflow: visible;
  border: 1px solid #d9d9d9;
  background: #fff;
  transition: border-color 0.2s;
}

.editor:focus-within {
  border-color: #4096ff;
}

.editor-readonly {
  background: #f5f5f5;
}

.editor-img-uploader {
  display: none;
}

/* 全局样式需要移到没有scoped的style标签中，或者添加:deep() */
:deep(.editor),
:deep(.ql-toolbar) {
  white-space: pre-wrap !important;
  line-height: normal !important;
  width: 100%;
}

:deep(.ql-container) {
  width: 100%;
  min-height: var(--editor-min-height, 240px);
  height: var(--editor-height, auto);
  border: 0 !important;
  font-family: inherit;
}

:deep(.ql-editor) {
  width: 100%;
  min-height: var(--editor-min-height, 240px);
  padding: 16px;
  font-size: 14px;
}

:deep(.ql-toolbar.ql-snow) {
  position: relative;
  z-index: 2;
  border: 0;
  border-bottom: 1px solid #e8e8e8;
  background: #fff;
}

/* 其他原有样式保持不变 */
:deep(.quill-img) {
  display: none;
}

:deep(.ql-snow .ql-tooltip[data-mode="link"]::before) {
  content: "请输入链接地址:";
}

/* ... 其他所有原有的ql-snow样式都需要加上:deep() */
</style>
<style>
.editor-img-uploader {
  display: none;
}
.editor,
.ql-toolbar {
  white-space: pre-wrap !important;
  line-height: normal !important;
}
.quill-img {
  display: none;
}
.ql-snow .ql-tooltip[data-mode="link"]::before {
  content: "请输入链接地址:";
}
.ql-snow .ql-tooltip.ql-editing a.ql-action::after {
  border-right: 0px;
  content: "保存";
  padding-right: 0px;
}
.ql-snow .ql-tooltip[data-mode="video"]::before {
  content: "请输入视频地址:";
}
.ql-snow .ql-picker.ql-size {
  width: 68px;
}
.ql-snow .ql-picker.ql-size .ql-picker-label::before {
  content: "14px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item::before {
  content: attr(data-value);
}
.ql-snow .ql-picker.ql-size .ql-picker-item:not([data-value])::before {
  content: "14px";
}
.ql-snow .ql-picker.ql-size .ql-picker-options {
  min-width: 76px;
  max-height: 320px;
  overflow-y: auto;
  z-index: 10;
}
.ql-snow .ql-picker.ql-size .ql-picker-item::before {
  font-size: 14px !important;
  line-height: 24px;
}
.ql-snow .ql-picker.ql-header .ql-picker-label::before,
.ql-snow .ql-picker.ql-header .ql-picker-item::before {
  content: "文本";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="1"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="1"]::before {
  content: "标题1";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="2"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="2"]::before {
  content: "标题2";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="3"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="3"]::before {
  content: "标题3";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="4"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="4"]::before {
  content: "标题4";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="5"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="5"]::before {
  content: "标题5";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="6"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="6"]::before {
  content: "标题6";
}
.ql-snow .ql-picker.ql-font .ql-picker-label::before,
.ql-snow .ql-picker.ql-font .ql-picker-item::before {
  content: "标准字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="serif"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="serif"]::before {
  content: "衬线字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="monospace"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="monospace"]::before {
  content: "等宽字体";
}
</style>
