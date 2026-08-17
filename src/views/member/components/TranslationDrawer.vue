<template>
  <a-drawer
    v-model:open="visible"
    :title="title"
    width="70%"
    size="large"
    :destroy-on-close="true"
    :mask-closable="false"
    class="translation-drawer"
    @close="handleCancel"
  >
    <a-tabs v-model:activeKey="activeLanguage" class="translation-tabs">
      <a-tab-pane
        v-for="language in visibleLanguages"
        :key="language.field"
        :tab="`${language.flag} ${language.label}`"
        force-render
      >
        <a-form layout="vertical" class="translation-form">
          <template v-if="translationMode === 'rich'">
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item :label="richDefinition.label">
                  <editor
                    v-model="localModels[language.field].content"
                    :min-height="360"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </template>

          <template v-else-if="translationMode === 'customerService'">
            <a-form layout="vertical">
              <a-form-item label="客服名称">
                <a-input
                  v-model:value="localModels[language.field].name"
                  placeholder="客服名称"
                  allow-clear
                />
              </a-form-item>
            </a-form>
          </template>

          <template v-else-if="translationMode === 'level'">
            <a-form layout="vertical">
              <a-form-item label="VIP名称">
                <a-input
                  v-model:value="localModels[language.field].name"
                  placeholder="VIP名称"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="VIP描述">
                <editor
                  v-model="localModels[language.field].description"
                  :min-height="300"
                />
              </a-form-item>
            </a-form>
          </template>

          <template v-else-if="translationMode === 'noticeItem'">
            <a-form layout="vertical">
              <a-form-item label="公告标题">
                <a-input
                  v-model:value="localModels[language.field].title"
                  placeholder="公告标题"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="公告内容">
                <editor
                  v-model="localModels[language.field].content"
                  :min-height="300"
                />
              </a-form-item>
            </a-form>
          </template>

          <template v-else-if="translationMode === 'task'">
            <a-row :gutter="24">
              <a-col :xs="24" :xl="8">
                <a-form-item label="任务背景图">
                  <media-upload
                    v-model="localModels[language.field].backgroundImage"
                    :limit="1"
                    :file-size="50"
                    :file-type="taskBackgroundFileTypes"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :xl="8">
                <a-form-item label="连单图片">
                  <image-upload
                    v-model="localModels[language.field].evenPic"
                    :limit="1"
                    :file-size="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :xl="8">
                <a-form-item label="连单标题">
                  <a-input
                    v-model:value="localModels[language.field].evenTitle"
                    placeholder="连单标题"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item label="连单内容">
                  <a-textarea
                    v-model:value="localModels[language.field].evenContent"
                    :rows="8"
                    placeholder="连单内容"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </template>

          <template v-else-if="translationMode === 'notification'">
            <a-tabs
              v-model:activeKey="activeNoticeTabs[language.field]"
              type="card"
              class="notice-tabs"
            >
              <a-tab-pane
                v-for="notice in notificationKinds"
                :key="notice.key"
                :tab="notice.label"
                force-render
              >
                <a-row :gutter="24">
                  <a-col :span="24">
                    <a-form-item>
                      <template #label>
                        <a-space :size="4">
                          <span>标题</span>
                          <a-tooltip>
                            <template #title>
                              <template v-for="token in noticeTokens" :key="token">
                                {{ token }}<br />
                              </template>
                            </template>
                            <QuestionCircleOutlined class="field-help" />
                          </a-tooltip>
                        </a-space>
                      </template>
                      <a-input
                        v-model:value="localModels[language.field].notices[notice.key].title"
                        placeholder="标题"
                        allow-clear
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
                <a-row :gutter="24">
                  <a-col :span="24">
                    <a-form-item>
                      <template #label>
                        <a-space :size="4">
                          <span>内容</span>
                          <a-tooltip>
                            <template #title>
                              <template v-for="token in noticeTokens" :key="token">
                                {{ token }}<br />
                              </template>
                            </template>
                            <QuestionCircleOutlined class="field-help" />
                          </a-tooltip>
                        </a-space>
                      </template>
                      <editor
                        v-model="localModels[language.field].notices[notice.key].content"
                        :min-height="280"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-tab-pane>
            </a-tabs>
          </template>

          <template v-else-if="translationMode === 'email'">
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item label="标题">
                  <a-input
                    v-model:value="localModels[language.field].title"
                    placeholder="标题"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item>
                  <template #label>
                    <a-space :size="4">
                      <span>内容</span>
                      <a-tooltip title="{code}">
                        <QuestionCircleOutlined class="field-help" />
                      </a-tooltip>
                    </a-space>
                  </template>
                  <editor
                    v-model="localModels[language.field].content"
                    :min-height="360"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </template>

          <template v-else-if="translationMode === 'error'">
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item label="错误代码">
                  <a-table
                    :columns="errorColumns"
                    :data-source="localModels[language.field].codes"
                    :pagination="false"
                    row-key="_key"
                    size="middle"
                    bordered
                  >
                    <template #bodyCell="{ column, record, index }">
                      <template v-if="column.dataIndex === 'code'">
                        <a-input
                          v-model:value="record.code"
                          placeholder="代码"
                        />
                      </template>
                      <template v-else-if="column.dataIndex === 'message'">
                        <a-input
                          v-model:value="record.message"
                          placeholder="消息"
                        />
                      </template>
                      <template v-else-if="column.key === 'operation'">
                        <a-button
                          type="link"
                          danger
                          class="table-link"
                          @click="removeErrorCode(language.field, index)"
                        >
                          删除
                        </a-button>
                      </template>
                    </template>
                  </a-table>
                  <a-button
                    type="dashed"
                    block
                    class="add-code-button"
                    @click="addErrorCode(language.field)"
                  >
                    <PlusOutlined />
                    添加一行数据
                  </a-button>
                </a-form-item>
              </a-col>
            </a-row>
          </template>
        </a-form>
      </a-tab-pane>
    </a-tabs>

    <template #footer>
      <div class="translation-drawer-footer">
        <a-button @click="handleCancel">取 消</a-button>
        <a-button type="primary" @click="handleSubmit">确 定</a-button>
      </div>
    </template>
  </a-drawer>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { PlusOutlined, QuestionCircleOutlined } from "@ant-design/icons-vue";
import MediaUpload from "@/components/MediaUpload/index.vue";
import {
  createEmptyTranslations,
  translationLanguages,
} from "./translationLanguages";
import { notificationKinds } from "../orderconfig/notificationKinds";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: "国际化",
  },
  translations: {
    type: Object,
    default: () => ({}),
  },
  type: {
    type: String,
    default: "",
  },
  settingId: [String, Number],
  languageFields: {
    type: Array,
    default: null,
  },
});

const emit = defineEmits(["update:modelValue", "submit"]);

const richDefinitions = {
  register: { field: "registrationAgreement", label: "注册协议" },
  about: { field: "aboutUs", label: "关于我们" },
  certificate: { field: "certificate", label: "证书" },
  help: { field: "helpMe", label: "帮助中心" },
  terms: { field: "clause", label: "条款" },
  event: { field: "event", label: "事件" },
  transaction: { field: "tradingDescription", label: "交易说明" },
  order: { field: "orderDescription", label: "订单说明" },
  usage: { field: "useDescription", label: "使用说明" },
  bonus: { field: "awardDescription", label: "彩金规则" },
  balance: { field: "rule", label: "余额宝规则" },
  privacy: { field: "privacyPolicy", label: "隐私协议" },
};

const noticeTokens = [
  "{username}",
  "{phone}",
  "{amount}",
  "{beforeBalance}",
  "{afterBalance}",
  "{currencyUnit}",
];
const taskBackgroundFileTypes = [
  "png",
  "jpg",
  "jpeg",
  "gif",
  "webp",
  "bmp",
  "svg",
  "tiff",
  "psd",
  "mp4",
  "avi",
  "mov",
  "mpeg",
  "quicktime",
];
const errorColumns = [
  { title: "代码", dataIndex: "code", width: 180 },
  { title: "消息", dataIndex: "message" },
  { title: "操作", key: "operation", width: 100, align: "center" },
];

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});
const visibleLanguages = computed(() => {
  if (!Array.isArray(props.languageFields) || !props.languageFields.length) {
    return translationLanguages;
  }
  const languagesByField = new Map(
    translationLanguages.map((language) => [language.field, language])
  );
  return props.languageFields
    .map((field) => languagesByField.get(field))
    .filter(Boolean);
});
const translationMode = computed(() => {
  if (props.type === "customerService") return "customerService";
  if (props.type === "level") return "level";
  if (props.type === "noticeItem") return "noticeItem";
  if (richDefinitions[props.type]) return "rich";
  if (props.type === "task") return "task";
  if (props.type === "notification") return "notification";
  if (props.type === "email") return "email";
  if (props.type === "error") return "error";
  return "rich";
});
const richDefinition = computed(
  () => richDefinitions[props.type] || { field: "content", label: "内容" }
);
const localModels = ref({});
const originalTranslations = ref({});
const activeLanguage = ref(translationLanguages[0].field);
const activeNoticeTabs = ref({});

function emptyNotices() {
  return Object.fromEntries(
    notificationKinds.map(({ key }) => [key, { title: "", content: "" }])
  );
}

function parseStoredValue(raw) {
  if (raw && typeof raw === "object") {
    return raw.value && typeof raw.value === "object" ? raw.value : raw;
  }
  if (typeof raw !== "string" || !raw.trim()) {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    return parsed?.value && typeof parsed.value === "object" ? parsed.value : parsed;
  } catch {
    return raw;
  }
}

function createLanguageModel(raw) {
  const parsed = parseStoredValue(raw);
  if (translationMode.value === "customerService") {
    return {
      name: typeof parsed === "string" ? parsed : parsed?.name ?? "",
    };
  }
  if (translationMode.value === "level") {
    return {
      name: typeof parsed === "object" && parsed ? parsed.name ?? "" : "",
      description:
        typeof parsed === "string" ? parsed : parsed?.description ?? "",
    };
  }
  if (translationMode.value === "noticeItem") {
    return {
      title: typeof parsed === "object" && parsed ? parsed.title ?? "" : "",
      content:
        typeof parsed === "string" ? parsed : parsed?.content ?? "",
    };
  }
  if (translationMode.value === "rich") {
    const field = richDefinition.value.field;
    return {
      content:
        typeof parsed === "string"
          ? parsed
          : parsed?.[field] ?? parsed?.content ?? "",
    };
  }
  if (translationMode.value === "task") {
    const value = typeof parsed === "object" && parsed ? parsed : {};
    return {
      backgroundImage: value.backgroundImage ?? value.taskBackground ?? "",
      evenPic: value.evenPic ?? value.continuousOrderImage ?? "",
      evenTitle: value.evenTitle ?? value.continuousOrderTitle ?? "",
      evenContent: value.evenContent ?? value.continuousOrderContent ?? "",
    };
  }
  if (translationMode.value === "notification") {
    const value = typeof parsed === "object" && parsed ? parsed : {};
    const source = value.notices && typeof value.notices === "object" ? value.notices : value;
    const notices = emptyNotices();
    notificationKinds.forEach(({ key }) => {
      Object.assign(notices[key], source[key] || {});
    });
    return { notices };
  }
  if (translationMode.value === "email") {
    const value = typeof parsed === "object" && parsed ? parsed : {};
    const template = value.template || value;
    return {
      title: template.title || "",
      content: template.content ?? template.contents ?? "",
    };
  }
  if (translationMode.value === "error") {
    const value = typeof parsed === "object" && parsed ? parsed : {};
    return {
      codes: (Array.isArray(value.codes) ? value.codes : []).map((item, index) => ({
        ...item,
        _key: `${Date.now()}-${index}`,
      })),
    };
  }
  return { content: typeof parsed === "string" ? parsed : "" };
}

function hydrateTranslations(value) {
  const source = {
    ...createEmptyTranslations(),
    ...(value || {}),
  };
  originalTranslations.value = { ...(value || {}) };
  const models = {};
  const noticeTabs = {};
  visibleLanguages.value.forEach((language) => {
    models[language.field] = createLanguageModel(source[language.field]);
    noticeTabs[language.field] = notificationKinds[0].key;
  });
  localModels.value = models;
  activeNoticeTabs.value = noticeTabs;
}

function encodeLanguageModel(model) {
  if (translationMode.value === "customerService") {
    return JSON.stringify({ name: model.name || "" });
  }
  if (translationMode.value === "level") {
    return JSON.stringify({
      name: model.name || "",
      description: model.description || "",
    });
  }
  if (translationMode.value === "noticeItem") {
    return JSON.stringify({
      title: model.title || "",
      content: model.content || "",
    });
  }
  if (translationMode.value === "rich") {
    return model.content || "";
  }
  if (translationMode.value === "task") {
    return JSON.stringify({
      backgroundImage: model.backgroundImage,
      evenPic: model.evenPic,
      evenTitle: model.evenTitle,
      evenContent: model.evenContent,
    });
  }
  if (translationMode.value === "notification") {
    return JSON.stringify(model.notices);
  }
  if (translationMode.value === "email") {
    return JSON.stringify({
      template: {
        title: model.title,
        content: model.content,
      },
    });
  }
  if (translationMode.value === "error") {
    return JSON.stringify({
      codes: model.codes.map(({ _key, ...code }) => code),
    });
  }
  return model.content || "";
}

watch(
  [() => props.translations, () => props.type, () => props.languageFields],
  ([value]) => hydrateTranslations(value),
  { immediate: true, deep: true }
);

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      activeLanguage.value = visibleLanguages.value[0]?.field || translationLanguages[0].field;
      hydrateTranslations(props.translations);
    }
  }
);

function addErrorCode(languageField) {
  const codes = localModels.value[languageField].codes;
  codes.push({
    _key: `${Date.now()}-${codes.length}`,
    code: String(codes.length + 1),
    message: "",
  });
}

function removeErrorCode(languageField, index) {
  localModels.value[languageField].codes.splice(index, 1);
}

function handleCancel() {
  visible.value = false;
}

function handleSubmit() {
  const result = { ...originalTranslations.value };
  visibleLanguages.value.forEach((language) => {
    result[language.field] = encodeLanguageModel(localModels.value[language.field]);
  });
  emit("submit", result);
}
</script>

<style scoped>
.translation-drawer :deep(.ant-drawer-body) {
  padding: 24px;
  overflow: auto;
}

.translation-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 24px;
}

.translation-form :deep(.ant-form-item) {
  margin-bottom: 24px;
}

.translation-form :deep(.ant-form-item-label > label) {
  color: rgba(0, 0, 0, 0.88);
  font-size: 14px;
}

.notice-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 20px;
}

.field-help {
  color: rgba(0, 0, 0, 0.45);
  cursor: help;
}

.table-link {
  height: auto;
  padding: 0;
}

.add-code-button {
  margin-top: 12px;
}

.translation-drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
