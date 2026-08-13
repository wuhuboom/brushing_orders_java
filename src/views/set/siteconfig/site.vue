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
          <el-form-item
            :label="$t('siteconfig.splashAdImage')"
            prop="splashAdImage"
          >
            <image-upload v-model="form.splashAdImage" :limit="1" />
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
            label="选择商品数据"
            prop="goodsTableType"
          >
            <el-switch
              v-model="form.goodsTableType"
              active-value="1"
              inactive-value="2"
              active-text = "商品"
              inactive-text = "酒店"
             :before-change="beforeGoodsTableTypeChange"
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

          <el-form-item :label="$t('siteconfig.newUserCanTask')" prop="newUserCanTask">
            <el-switch
              v-model="form.newUserCanTask"
              active-value="0"
              inactive-value="1"
              :active-text="$t('siteconfig.newUserCanTaskAllow')"
              :inactive-text="$t('siteconfig.newUserCanTaskDeny')"
              @change="(val) => handleSwitchChange('newUserCanTask', val)"
            />
          </el-form-item>

          <el-form-item :label="$t('siteconfig.needPhone')" prop="needPhone">
            <el-switch
              v-model="form.needPhone"
              active-value="0"
              inactive-value="1"
              :active-text="$t('siteconfig.needPhoneRequired')"
              :inactive-text="$t('siteconfig.needPhoneNotRequired')"
              @change="(val) => handleSwitchChange('needPhone', val)"
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
  splashAdImage: null,
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
  enableFullOrder: "0",
  // 新字段：新用户是否能任务 0=能, 1=不能
  newUserCanTask: "0",
  // 新字段：是否需要手机 0=需要, 1=不需要
  needPhone: "1",
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

function beforeGoodsTableTypeChange() {
  const newVal = form.goodsTableType === "1" ? "2" : "1";
  const label = newVal === "1" ? "商品" : "酒店";

  return new Promise((resolve) => {
    ElMessageBox.confirm(
      `确认切换为【${label}】数据源吗？`,
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    )
      .then(() => resolve(true))   // 允许切换
      .catch(() => resolve(false)); // 阻止切换
  });
}


// 处理开关切换
function handleSwitchChange(key, newVal) {
  // 计算旧值（因为是 toggle，所以旧值是 newVal 的反转）
  const oldVal = newVal === "0" ? "1" : "0";

  // 互斥检查：levelStatus 和 autoVip 只能有一个为 "0" (active)
  if ((key === "levelStatus" || key === "vipAutoShop") && newVal === "0") {
    const otherKey = key === "levelStatus" ? "vipAutoShop" : "levelStatus";
    if (form[otherKey] === "0") {
      ElMessage.warning(t('siteconfig.levelVipMutualExclusive') || "通过余额更新VIP和订单数自动升级VIP，二者只能有一个设置为开启状态。");
      // 回滚
      form[key] = oldVal;
      return;
    }
  }

  // 获取功能标签（从 i18n 中读取，回退到 key）
  const label = t(`siteconfig.${key}`) || key;

  const action = newVal === "0" ? t('common.confirmEnable') || "开启" : t('common.confirmDisable') || "关闭";
  const message = t('siteconfig.confirmToggle', { action, label }) || `确认${action}${label}功能吗？`;

  ElMessageBox.confirm(message, t('common.prompt') || "提示", {
    confirmButtonText: t('common.confirm') || "确定",
    cancelButtonText: t('common.cancel') || "取消",
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
