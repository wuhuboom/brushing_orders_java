<template>
  <a-form
    ref="websiteFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    size="large"
    class="config-form website-config-form"
  >
    <a-row :gutter="24">
      <a-col v-bind="quarterCol">
        <a-form-item label="网站名称" name="name">
          <a-input v-model:value="localForm.name" placeholder="网站名称" allow-clear />
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="货币单位" name="currencyUnit">
          <a-input v-model:value="localForm.currencyUnit" placeholder="货币单位" allow-clear />
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="版权" name="copyright">
          <a-input v-model:value="localForm.copyright" placeholder="版权" allow-clear />
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="网站logo" name="logo">
          <image-upload
            v-model="localForm.logo"
            :limit="1"
            :file-size="2"
            :is-show-tip="false"
          />
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="24">
      <a-col v-bind="quarterCol">
        <a-form-item label="开屏弹框次数限制（0-不限制）" name="popUpLimit">
          <a-input-number
            v-model:value="localForm.popUpLimit"
            :min="0"
            :precision="0"
            class="full-width"
            placeholder="开屏弹框次数限制（0-不限制）"
          />
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="前端开屏广告图片" name="popUpImage">
          <image-upload
            v-model="localForm.popUpImage"
            :limit="1"
            :file-size="2"
            :is-show-tip="false"
          />
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="网站背景" name="backgroundImage">
          <media-upload
            v-model="localForm.backgroundImage"
            :limit="1"
            :file-size="50"
            :file-type="backgroundFileTypes"
            :is-show-tip="false"
          />
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="H5网站背景" name="h5BackgroundImage">
          <media-upload
            v-model="localForm.h5BackgroundImage"
            :limit="1"
            :file-size="50"
            :file-type="backgroundFileTypes"
            :is-show-tip="false"
          />
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="24">
      <a-col v-bind="quarterCol">
        <a-form-item label="启用汇率转换" name="enableExchangeRateConversion">
          <a-radio-group v-model:value="localForm.enableExchangeRateConversion">
            <a-radio value="0">禁用</a-radio>
            <a-radio value="1">启用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item name="evenPriceKind">
          <template #label>
            <a-space :size="4">
              <span>默认连单价格类型</span>
              <a-tooltip>
                <template #title>
                  当价格类型为商品价格时，订单明细金额 = 价格，最终用户余额为当时用户余额 - 价格。<br />
                  当价格类型为交易后负余额时，订单明细金额 = 当时用户余额 + 价格，最终用户余额为价格的负数。
                </template>
                <QuestionCircleOutlined class="label-help" />
              </a-tooltip>
            </a-space>
          </template>
          <a-radio-group v-model:value="localForm.evenPriceKind">
            <a-radio value="1">商品价格</a-radio>
            <a-radio value="2">交易后负余额</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="启用商品自动更新" name="autoUpdateSpu">
          <a-radio-group v-model:value="localForm.autoUpdateSpu">
            <a-radio value="0">禁用</a-radio>
            <a-radio value="1">启用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="是否隔离客服" name="quarantineCustomerService">
          <a-radio-group v-model:value="localForm.quarantineCustomerService">
            <a-radio value="0">否</a-radio>
            <a-radio value="1">独立客服</a-radio>
            <a-radio value="2">组织客服</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="24">
      <a-col v-bind="quarterCol">
        <a-form-item label="注册会员默认是否允许邀请" name="allowInvitations">
          <a-radio-group v-model:value="localForm.allowInvitations">
            <a-radio value="0">禁用</a-radio>
            <a-radio value="1">启用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="注册会员默认交易状态" name="defaultTradingStatus">
          <a-radio-group v-model:value="localForm.defaultTradingStatus">
            <a-radio value="0">禁用</a-radio>
            <a-radio value="1">启用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="localForm.email" placeholder="邮箱" allow-clear />
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="手机号码" name="phone">
          <a-input v-model:value="localForm.phone" placeholder="手机号码" allow-clear />
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="24">
      <a-col v-bind="quarterCol">
        <a-form-item label="是否验证登录密码" name="validLoginPassword">
          <a-radio-group v-model:value="localForm.validLoginPassword">
            <a-radio value="0">否</a-radio>
            <a-radio value="1">是</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="是否显示Logo" name="showLogo">
          <a-radio-group v-model:value="localForm.showLogo">
            <a-radio value="0">否</a-radio>
            <a-radio value="1">是</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="启用Logo图片隐藏" name="hideImage">
          <a-radio-group v-model:value="localForm.hideImage">
            <a-radio value="0">禁用</a-radio>
            <a-radio value="1">启用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="Logo图片显示时间范围" name="imageShowTimeRange">
          <a-time-range-picker
            v-model:value="localForm.imageShowTimeRange"
            format="HH:mm"
            value-format="HH:mm:ss"
            class="full-width"
          />
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="24">
      <a-col v-bind="quarterCol">
        <a-form-item label="数据是否加密" name="dataEncryption">
          <a-radio-group v-model:value="localForm.dataEncryption">
            <a-radio value="0">否</a-radio>
            <a-radio value="1">是</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="启用Web3授权" name="enableWeb3Approve">
          <a-radio-group v-model:value="localForm.enableWeb3Approve">
            <a-radio value="0">否</a-radio>
            <a-radio value="1">是</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="启用等级奖励" name="enableLevelBonus">
          <a-radio-group v-model:value="localForm.enableLevelBonus">
            <a-radio value="0">否</a-radio>
            <a-radio value="1">是</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col v-bind="quarterCol">
        <a-form-item label="领取等级奖励需距离注册时间的间隔天数" name="levelBonusIntervalDay">
          <a-input-number
            v-model:value="localForm.levelBonusIntervalDay"
            :min="0"
            :precision="0"
            class="full-width"
            placeholder="领取等级奖励需距离注册时间的间隔天数"
          />
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="24">
      <a-col :span="24">
        <a-form-item label="重定向地址" name="redirectUrl">
          <a-input v-model:value="localForm.redirectUrl" placeholder="重定向地址" allow-clear />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="24">
      <a-col :span="24">
        <a-form-item label="域名列表" name="domains">
          <a-select
            v-model:value="localForm.domains"
            mode="tags"
            :token-separators="[',']"
            placeholder="域名列表"
            :open="false"
          />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="24">
      <a-col :span="24">
        <a-form-item label="客服脚本" name="customServiceScript">
          <a-input v-model:value="localForm.customServiceScript" placeholder="客服脚本" allow-clear />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="24">
      <a-col :span="24">
        <a-form-item label="客服样式" name="customServiceCss">
          <a-textarea v-model:value="localForm.customServiceCss" :rows="4" placeholder="客服样式" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="24">
      <a-col :span="24">
        <a-form-item name="userContract">
          <template #label>
            <a-space :size="4">
              <span>用户合同</span>
              <a-tooltip>
                <template #title>
                  {year}<br />{month}<br />{day}<br />{signature}
                </template>
                <QuestionCircleOutlined class="label-help" />
              </a-tooltip>
            </a-space>
          </template>
          <editor v-model="localForm.userContract" :min-height="260" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-row :gutter="24">
      <a-col :span="24">
        <a-form-item name="formalContract">
          <template #label>
            <a-space :size="4">
              <span>正式合同</span>
              <a-tooltip>
                <template #title>
                  {year}<br />{month}<br />{day}<br />{signature}
                </template>
                <QuestionCircleOutlined class="label-help" />
              </a-tooltip>
            </a-space>
          </template>
          <editor v-model="localForm.formalContract" :min-height="260" />
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { QuestionCircleOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import MediaUpload from "@/components/MediaUpload/index.vue";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);
const websiteFormRef = ref();
const sourceContent = ref({});
const quarterCol = { xs: 24, sm: 12, md: 12, lg: 12, xl: 6 };
const backgroundFileTypes = [
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

const defaultContent = {
  name: "",
  currencyUnit: "",
  copyright: "",
  logo: "",
  popUpLimit: 0,
  popUpImage: "",
  backgroundImage: "",
  h5BackgroundImage: "",
  enableExchangeRateConversion: "1",
  evenPriceKind: "1",
  autoUpdateSpu: "1",
  quarantineCustomerService: "0",
  allowInvitations: "1",
  defaultTradingStatus: "1",
  email: "",
  phone: "",
  validLoginPassword: "0",
  showLogo: "1",
  hideImage: "0",
  imageShowTimeRange: [],
  dataEncryption: "0",
  enableWeb3Approve: "0",
  enableLevelBonus: "0",
  levelBonusIntervalDay: 0,
  redirectUrl: "",
  domains: [],
  customServiceScript: "",
  customServiceCss: "",
  userContract: "",
  formalContract: "",
};

const localForm = reactive({ ...defaultContent });
const contentFieldKeys = Object.keys(defaultContent);
const legacyAliasKeys = [
  "siteName",
  "siteLogo",
  "splashAdImage",
  "siteBackground",
  "enableExchangeRate",
  "defaultOrderPriceType",
  "enableProductAutoUpdate",
  "enableRegisterInvitation",
  "enableImageHide",
  "isolateCustomerService",
  "imageDisplayTimeRange",
  "domainList",
  "customerServiceScript",
  "customerServiceStyle",
];

const required = (messageText) => [
  { required: true, message: messageText, trigger: "change" },
];
const localRules = {
  enableExchangeRateConversion: required("启用汇率转换为必填项"),
  evenPriceKind: required("默认连单价格类型为必填项"),
  autoUpdateSpu: required("启用商品自动更新为必填项"),
  quarantineCustomerService: required("是否隔离客服为必填项"),
  allowInvitations: required("注册会员默认是否允许邀请为必填项"),
  defaultTradingStatus: required("注册会员默认交易状态为必填项"),
  showLogo: required("是否显示Logo为必填项"),
  hideImage: required("启用Logo图片隐藏为必填项"),
  imageShowTimeRange: required("Logo图片显示时间范围为必填项"),
};

function toEnabled(exactValue, legacyValue, fallback) {
  if (exactValue !== undefined && exactValue !== null && exactValue !== "") {
    return String(exactValue);
  }
  if (legacyValue !== undefined && legacyValue !== null && legacyValue !== "") {
    return String(legacyValue) === "0" ? "1" : "0";
  }
  return fallback;
}

function normalizeDomains(value) {
  if (Array.isArray(value)) {
    return value.filter(Boolean);
  }
  if (!value) {
    return [];
  }
  return String(value)
    .split(",")
    .map((domain) => domain.trim())
    .filter(Boolean);
}

function normalizeTimeRange(value) {
  if (Array.isArray(value)) {
    return value.map((item) => {
      const text = String(item || "");
      return text.length === 5 ? `${text}:00` : text;
    });
  }
  if (typeof value === "string" && value) {
    return normalizeDomains(value);
  }
  return [];
}

function hydrateContent(content) {
  const parsed = content ? JSON.parse(content) : {};
  sourceContent.value = { ...parsed };
  Object.assign(localForm, defaultContent, {
    ...parsed,
    name: parsed.name ?? parsed.siteName ?? "",
    logo: parsed.logo ?? parsed.siteLogo ?? "",
    popUpImage: parsed.popUpImage ?? parsed.splashAdImage ?? "",
    backgroundImage: parsed.backgroundImage ?? parsed.siteBackground ?? "",
    enableExchangeRateConversion: toEnabled(
      parsed.enableExchangeRateConversion,
      parsed.enableExchangeRate,
      "1"
    ),
    evenPriceKind:
      parsed.evenPriceKind !== undefined
        ? String(parsed.evenPriceKind)
        : parsed.defaultOrderPriceType !== undefined
          ? String(Number(parsed.defaultOrderPriceType) + 1)
          : "1",
    autoUpdateSpu: toEnabled(parsed.autoUpdateSpu, parsed.enableProductAutoUpdate, "1"),
    allowInvitations: toEnabled(parsed.allowInvitations, parsed.enableRegisterInvitation, "1"),
    hideImage: toEnabled(parsed.hideImage, parsed.enableImageHide, "0"),
    quarantineCustomerService:
      parsed.quarantineCustomerService !== undefined
        ? String(parsed.quarantineCustomerService)
        : parsed.isolateCustomerService !== undefined
          ? String(Math.max(0, Number(parsed.isolateCustomerService) - 1))
          : "0",
    imageShowTimeRange: normalizeTimeRange(
      parsed.imageShowTimeRange ?? parsed.imageDisplayTimeRange
    ),
    domains: normalizeDomains(parsed.domains ?? parsed.domainList),
    customServiceScript: parsed.customServiceScript ?? parsed.customerServiceScript ?? "",
    customServiceCss: parsed.customServiceCss ?? parsed.customerServiceStyle ?? "",
  });
}

watch(
  () => props.form.content,
  (content) => {
    try {
      hydrateContent(content);
    } catch {
      Object.assign(localForm, defaultContent);
      message.error("解析网站设置失败");
    }
  },
  { immediate: true }
);

watch(
  localForm,
  () => {
    const toSave = { ...sourceContent.value };
    legacyAliasKeys.forEach((key) => delete toSave[key]);
    contentFieldKeys.forEach((key) => {
      toSave[key] = localForm[key];
    });
    emit("update:form", {
      ...props.form,
      content: JSON.stringify(toSave),
    });
  },
  { deep: true }
);

function handleSubmit() {
  websiteFormRef.value
    ?.validate?.()
    .then(() => emit("submit"))
    .catch(() => {});
}

function handleCancel() {
  emit("cancel");
}

defineExpose({ handleSubmit, handleCancel });
</script>

<style scoped>
.full-width {
  width: 100%;
}

.label-help {
  color: rgba(0, 0, 0, 0.45);
  cursor: help;
}

.website-config-form :deep(.ant-form-item) {
  margin-bottom: 24px;
}

.website-config-form :deep(.component-upload-image .ant-upload-list-picture-card .ant-upload-list-item-container),
.website-config-form :deep(.component-upload-image .ant-upload.ant-upload-select-picture-card) {
  width: 104px;
  height: 104px;
}
</style>
