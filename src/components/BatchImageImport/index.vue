<template>
  <a-modal
    :open="open"
    title="批量导入图片"
    :width="820"
    :closable="!importing"
    :mask-closable="false"
    :keyboard="!importing"
    @cancel="handleClose"
  >
    <a-alert type="info" show-icon class="import-help">
      <template #message>将图片按以下规则命名后，打包为一个 ZIP 文件</template>
      <template #description>
        <div>v01、v02… 分别对应 VIP 等级 1、2…，更新对应等级的图片。</div>
        <div>s01、s02… 按数字顺序导入同一个事件；z01、z02… 按数字顺序导入同一个证书。</div>
        <div>支持 JPG、JPEG、PNG、WebP、GIF、BMP，名称及扩展名不区分大小写。例如 v01.png、s02.webp、z01.jpeg。</div>
        <div>ZIP 不超过 10 MB，解压后总大小不超过 100 MB，最多包含 300 项。</div>
      </template>
    </a-alert>

    <a-form layout="vertical" class="import-form">
      <a-form-item label="ZIP 压缩包">
        <input ref="fileInput" type="file" accept=".zip,application/zip" hidden :disabled="importing" @change="handleFileChange" />
        <a-space wrap>
          <a-button :disabled="importing" @click="fileInput?.click()"><UploadOutlined />选择 ZIP 文件</a-button>
          <span class="selected-file">{{ selectedFile?.name || '尚未选择文件' }}</span>
        </a-space>
      </a-form-item>
      <a-form-item label="事件和证书的导入方式">
        <a-radio-group :value="richTextMode" :disabled="importing" @change="handleModeChange">
          <a-radio value="append">追加到原内容末尾</a-radio>
          <a-radio value="replace">替换原内容</a-radio>
        </a-radio-group>
        <div class="mode-description">
          {{ richTextMode === 'replace'
            ? 'ZIP 中包含事件或证书图片时，将替换对应的整块富文本内容，包括原文字和图片；未包含的分类不修改。'
            : '事件和证书保留原内容，新图片按序追加；未包含的分类不修改。' }}
          VIP 图片始终更新为 ZIP 中对应等级的图片。
        </div>
      </a-form-item>
      <a-button :loading="previewing" :disabled="!selectedFile || importing" @click="handlePreview">校验并预览</a-button>
    </a-form>

    <a-alert v-if="requestError" type="error" show-icon :message="requestError" class="import-result" />
    <template v-if="preview">
      <a-alert
        :type="canImport ? 'success' : 'error'"
        show-icon
        :message="canImport ? '校验通过，请核对下方匹配结果后确认导入' : '校验未通过，请修正压缩包后重新预览'"
        class="import-result"
      />
      <a-space wrap class="import-summary">
        <a-tag>VIP：{{ preview.summary?.vip || 0 }} 张</a-tag>
        <a-tag>事件：{{ preview.summary?.event || 0 }} 张</a-tag>
        <a-tag>证书：{{ preview.summary?.certificate || 0 }} 张</a-tag>
      </a-space>
      <ul v-if="preview.errors?.length" class="import-errors">
        <li v-for="(error, index) in preview.errors" :key="index">{{ error }}</li>
      </ul>
      <a-table
        :columns="columns"
        :data-source="preview.entries || []"
        :pagination="false"
        :scroll="{ y: 260, x: 560 }"
        :row-key="entry => `${entry.group}-${entry.sequence}-${entry.fileName}`"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'group'">{{ groupLabels[record.group] || record.group }}</template>
        </template>
      </a-table>
    </template>

    <template #footer>
      <a-space>
        <a-button :disabled="importing" @click="handleClose">取消</a-button>
        <a-button type="primary" :disabled="!canImport || previewing" :loading="importing" @click="handleImport">确认导入</a-button>
      </a-space>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, shallowRef, watch } from "vue";
import { message } from "ant-design-vue";
import { UploadOutlined } from "@ant-design/icons-vue";
import { importConfigurationImages, previewConfigurationImages } from "@/api/member/orderconfig";

const props = defineProps({ open: Boolean });
const emit = defineEmits(["update:open", "success"]);
const fileInput = ref();
const selectedFile = shallowRef(null);
const richTextMode = ref("append");
const preview = ref(null);
const previewing = ref(false);
const importing = ref(false);
const requestError = ref("");
let revision = 0;

const groupLabels = { vip: "VIP", event: "事件", certificate: "证书" };
const columns = [
  { title: "文件名", dataIndex: "fileName", key: "fileName", ellipsis: true },
  { title: "分类", key: "group", width: 80 },
  { title: "对应位置", dataIndex: "targetLabel", key: "targetLabel" },
  { title: "序号", dataIndex: "sequence", key: "sequence", width: 70 },
];
const canImport = computed(() => Boolean(
  selectedFile.value && preview.value?.valid === true &&
  !preview.value.errors?.length && preview.value.entries?.length
));

function invalidatePreview() {
  revision += 1;
  preview.value = null;
  previewing.value = false;
  requestError.value = "";
}

function reset() {
  invalidatePreview();
  selectedFile.value = null;
  richTextMode.value = "append";
  if (fileInput.value) fileInput.value.value = "";
}

watch(() => props.open, value => {
  if (!value) reset();
});
onBeforeUnmount(() => { revision += 1; });

function handleFileChange(event) {
  if (importing.value) return;
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) return;
  invalidatePreview();
  selectedFile.value = null;
  if (!/\.zip$/i.test(file.name)) {
    requestError.value = "请选择 ZIP 格式的压缩包";
    return;
  }
  if (!file.size) {
    requestError.value = "压缩包不能为空";
    return;
  }
  if (file.size > 10 * 1024 * 1024) {
    requestError.value = "ZIP 压缩包不能超过 10 MB";
    return;
  }
  selectedFile.value = file;
}

function handleModeChange(event) {
  if (importing.value) return;
  const mode = event.target.value;
  if (!["append", "replace"].includes(mode) || mode === richTextMode.value) return;
  richTextMode.value = mode;
  invalidatePreview();
}

async function handlePreview() {
  if (!selectedFile.value || importing.value || previewing.value) return;
  invalidatePreview();
  const requestRevision = revision;
  previewing.value = true;
  try {
    const response = await previewConfigurationImages(selectedFile.value, richTextMode.value);
    if (requestRevision !== revision || !props.open) return;
    preview.value = response.data;
    if (!response.data || !Array.isArray(response.data.entries)) {
      preview.value = null;
      requestError.value = "未能读取匹配结果，请重新预览";
    }
  } catch (error) {
    if (requestRevision === revision && props.open) requestError.value = error.message || "校验失败，请重试";
  } finally {
    if (requestRevision === revision) previewing.value = false;
  }
}

async function handleImport() {
  if (!canImport.value || importing.value || previewing.value) return;
  const requestRevision = revision;
  importing.value = true;
  requestError.value = "";
  try {
    const response = await importConfigurationImages(selectedFile.value, richTextMode.value);
    if (requestRevision !== revision || !props.open) return;
    if (response.data?.valid !== true || response.data.errors?.length) {
      preview.value = response.data || null;
      requestError.value = "导入未完成，请检查匹配结果并重新预览";
      return;
    }
    message.success("图片批量导入成功");
    emit("success", response.data);
    emit("update:open", false);
    reset();
  } catch (error) {
    if (requestRevision === revision && props.open) {
      preview.value = null;
      requestError.value = error.message || "导入失败，请重新预览后重试";
    }
  } finally {
    importing.value = false;
  }
}

function handleClose() {
  if (importing.value) return;
  reset();
  emit("update:open", false);
}
</script>

<style scoped>
.import-help { margin-bottom: 20px; }
.import-help :deep(.ant-alert-description) { line-height: 1.8; }
.import-form { margin-bottom: 20px; }
.selected-file { overflow-wrap: anywhere; }
.mode-description { margin-top: 8px; color: #666; line-height: 1.7; }
.import-result { margin-bottom: 12px; }
.import-summary { margin-bottom: 12px; }
.import-errors { max-height: 160px; overflow: auto; padding-left: 22px; color: #cf1322; }
</style>
