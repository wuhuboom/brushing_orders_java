<template>
  <div class="app-container">
    <el-card>
      <div class="center-container">
        <el-form
          ref="siteconfigRef"
          :model="form"
          :rules="rules"
          label-position="top"
          label-width="120px"
        >
          <el-form-item :label="$t('siteconfig.siteName')" prop="siteName">
            <el-input
              v-model="form.siteName"
              :placeholder="$t('siteconfig.enterSiteName')"
            />
          </el-form-item>

          <el-form-item
            :label="$t('siteconfig.customerServiceUrl')"
            prop="customerServiceUrl"
          >
            <el-input
              v-model="form.customerServiceUrl"
              :placeholder="$t('siteconfig.enterCustomerServiceUrl')"
            />
          </el-form-item>

          <!-- 新增：邮箱地址 + 复制按钮 -->
          <el-form-item
            :label="$t('siteconfig.emailAddress')"
            prop="emailAddress"
          >
            <el-input
              v-model="form.emailAddress"
              :placeholder="$t('siteconfig.enterEmailAddress')"
              clearable
            >
              <template #append>
                <el-button :icon="DocumentCopy" @click="copyEmail">{{
                  $t("common.copy")
                }}</el-button>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item
            :label="$t('siteconfig.copyrightInfo')"
            prop="copyrightInfo"
          >
            <el-input
              v-model="form.copyrightInfo"
              :placeholder="$t('siteconfig.enterCopyrightInfo')"
            />
          </el-form-item>

          <el-form-item
            :label="$t('siteconfig.registerBonusAmount')"
            prop="registerBonusAmount"
          >
            <el-input
              v-model="form.registerBonusAmount"
              :placeholder="$t('siteconfig.enterRegisterBonusAmount')"
            />
          </el-form-item>

          <el-form-item
            :label="$t('siteconfig.maintenanceImage')"
            prop="maintenanceImage"
          >
            <image-upload v-model="form.maintenanceImage" />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.siteLogo')" prop="siteLogo">
            <image-upload v-model="form.siteLogo" />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.favicon')" prop="favicon">
            <image-upload v-model="form.favicon" />
          </el-form-item>

          <el-form-item
            :label="$t('siteconfig.currencyType')"
            prop="currencyType"
          >
            <el-input
              v-model="form.currencyType"
              :placeholder="$t('siteconfig.enterCurrencyType')"
            />
          </el-form-item>

          <el-form-item
            :label="$t('siteconfig.maxRegisterPerIp')"
            prop="maxRegisterPerIp"
          >
            <el-input
              v-model="form.maxRegisterPerIp"
              :placeholder="$t('siteconfig.enterMaxRegisterPerIp')"
            />
          </el-form-item>

          <el-form-item
            :label="$t('siteconfig.ipBlacklist')"
            prop="ipBlacklist"
          >
            <el-input
              type="textarea"
              v-model="form.ipBlacklist"
              :placeholder="$t('siteconfig.enterIpBlacklist')"
            />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.levelStatus')">
            <el-switch
              v-model="form.levelStatus"
              active-value="0"
              inactive-value="1"
              @change="(val) => handleSwitchChange('levelStatus', val)"
            />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.autoVip')">
            <el-switch
              v-model="form.vipAutoShop"
              active-value="0"
              inactive-value="1"
              @change="(val) => handleSwitchChange('vipAutoShop', val)"
            />
          </el-form-item>

          <el-form-item
            :label="$t('siteconfig.seriesStatus')"
            prop="seriesStatus"
          >
            <el-switch
              v-model="form.seriesStatus"
              active-value="0"
              inactive-value="1"
              :active-text="$t('siteconfig.proportion')"
              :inactive-text="$t('siteconfig.multiplier')"
              @change="(val) => handleSwitchChange('seriesStatus', val)"
            />
          </el-form-item>

          <el-form-item
            :label="$t('siteconfig.registerEnabled')"
            prop="registerEnabled"
          >
            <el-switch
              v-model="form.registerEnabled"
              active-value="0"
              inactive-value="1"
              @change="(val) => handleSwitchChange('registerEnabled', val)"
            />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.autoReset')" prop="autoReset">
            <el-switch
              v-model="form.autoReset"
              active-value="0"
              inactive-value="1"
              @change="(val) => handleSwitchChange('autoReset', val)"
            />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.resetOrderCount')">
            <el-switch
              v-model="form.resetOrderCount"
              active-value="0"
              inactive-value="1"
              @change="(val) => handleSwitchChange('resetOrderCount', val)"
            />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.minBalance')">
            <el-switch
              v-model="form.minBalance"
              active-value="0"
              inactive-value="1"
              @change="(val) => handleSwitchChange('minBalance', val)"
            />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.enableFullOrder')">
            <el-switch
              v-model="form.enableFullOrder"
              active-value="0"
              inactive-value="1"
              @change="(val) => handleSwitchChange('enableFullOrder', val)"
            />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.emailVerificationEnabled')">
            <el-switch
              v-model="form.emailVerificationEnabled"
              active-value="0"
              inactive-value="1"
              @change="
                (val) => handleSwitchChange('emailVerificationEnabled', val)
              "
            />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.totpEnabled')">
            <el-switch
              v-model="form.totpEnabled"
              active-value="0"
              inactive-value="1"
              @change="(val) => handleSwitchChange('totpEnabled', val)"
            />
          </el-form-item>
        </el-form>

        <div class="dialog-footer" style="margin-top: 20px; text-align: right">
          <el-button type="primary" @click="submitForm">{{
            $t("common.save")
          }}</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup name="Siteconfig">
import { reactive, ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import {
  getSiteconfig,
  addSiteconfig,
  updateSiteconfig,
} from "@/api/set/siteconfig";
import { ElMessage, ElMessageBox } from "element-plus";
import { DocumentCopy } from "@element-plus/icons-vue";

const { t } = useI18n();

const { proxy } = getCurrentInstance();

const title = ref(t("siteconfig.title"));

const form = reactive({
  id: null,
  siteName: null,
  customerServiceUrl: null,
  popupMessage: null,
  copyrightInfo: null,
  registerBonusAmount: null,
  maintenanceImage: null,
  siteLogo: null,
  favicon: null,
  currencyType: null,
  maxRegisterPerIp: null,
  ipBlacklist: null,
  registerEnabled: "1",
  emailVerificationEnabled: "1",
  totpEnabled: "1",
  // 新增字段：邮箱地址
  autoReset: "1",
  emailAddress: null,
  levelStatus: "1",
  vipAutoShop: "1",
  seriesStatus: "1",
  resetOrderCount: "1",
  minBalance: "1",
  enableFullOrder:"0" ,
});

const rules = {
  siteName: [
    {
      required: true,
      message: t("siteconfig.siteNameRequired"),
      trigger: "blur",
    },
  ],
  // 可选的邮箱格式校验（不强制）
  emailAddress: [
    {
      type: "email",
      message: t("siteconfig.emailAddressEmail"),
      trigger: ["blur", "change"],
    },
  ],
};

// 页面加载时查询 ID=1 的配置
onMounted(() => {
  getSiteconfig(1)
    .then((res) => {
      if (res && res.data) {
        Object.assign(form, res.data);
        title.value = t("siteconfig.editTitle");
      } else {
        title.value = t("siteconfig.addTitle");
      }
    })
    .catch(() => {
      title.value = t("siteconfig.addTitle");
    });
});

// 复制邮箱到剪贴板
async function copyEmail() {
  const text = form.emailAddress?.trim();
  if (!text) {
    ElMessage.warning(t("siteconfig.copyWarning"));
    return;
  }
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
    } else {
      // 兼容旧浏览器
      const el = document.createElement("textarea");
      el.value = text;
      el.setAttribute("readonly", "");
      el.style.position = "absolute";
      el.style.left = "-9999px";
      document.body.appendChild(el);
      el.select();
      document.execCommand("copy");
      document.body.removeChild(el);
    }
    ElMessage.success(t("siteconfig.copySuccess"));
  } catch (e) {
    ElMessage.error(t("siteconfig.copyFailed"));
  }
}

// 处理开关切换
function handleSwitchChange(key, newVal) {
  // 计算旧值（因为是 toggle，所以旧值是 newVal 的反转）
  const oldVal = newVal === "0" ? "1" : "0";

  // 互斥检查：levelStatus 和 autoVip 只能有一个为 "0" (active)
  if ((key === "levelStatus" || key === "vipAutoShop") && newVal === "0") {
    const otherKey = key === "levelStatus" ? "vipAutoShop" : "levelStatus";
    if (form[otherKey] === "0") {
      ElMessage.warning(
        "通过余额更新VIP和订单数自动升级VIP，二者只能有一个设置为开启状态。"
      );
      // 回滚
      form[key] = oldVal;
      return;
    }
  }

  // 获取功能标签
  let label = "";
  switch (key) {
    case "levelStatus":
      label = "等级状态";
      break;
    case "autoVip":
      label = "自动VIP";
      break;
    case "seriesStatus":
      label = "系列状态";
      break;
    case "registerEnabled":
      label = "注册启用";
      break;
    case "autoReset":
      label = "自动重置";
      break;
    case "resetOrderCount":
      label = "重置订单计数";
      break;
    case "minBalance":
      label = "最低余额";
      break;
    case "emailVerificationEnabled":
      label = "邮箱验证启用";
      break;
    case "totpEnabled":
      label = "TOTP 启用";
      break;
    case "enableFullOrder":
      label = "满单提示";
      break;
    default:
      label = key;
  }

  const action = newVal === "0" ? "开启" : "关闭";
  const message = `确认${action}${label}功能吗？`;

  ElMessageBox.confirm(message, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      // 确认，保持新值
    })
    .catch(() => {
      // 取消，回滚到旧值
      form[key] = oldVal;
    });
}

// 提交表单
function submitForm() {
  proxy.$refs["siteconfigRef"].validate((valid) => {
    if (!valid) return;

    const submitApi = form.id ? updateSiteconfig : addSiteconfig;
    console.log("Submitting form:", form);

    submitApi(form).then(() => {
      ElMessage.success(
        form.id ? t("siteconfig.updateSuccess") : t("siteconfig.addSuccess")
      );
    });
  });
}
</script>

<style scoped>
.center-container {
  width: 60%;
  margin: 0 auto;
  padding-top: 20px;
}
</style>
