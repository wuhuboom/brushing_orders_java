<template>
  <div class="app-container">
    <el-tabs
      v-model="activeTab"
      type="border-card"
      @tab-click="handleTabChange"
    >
      <el-tab-pane :label="$t('siteconfig.siteSetting')" name="site" />
      <el-tab-pane :label="$t('siteconfig.tradeControl')" name="trade" />
      <el-tab-pane :label="$t('siteconfig.emailConfig')" name="email" />
      <el-tab-pane :label="$t('siteconfig.registerProtocol')" name="register" />
      <el-tab-pane :label="$t('siteconfig.aboutUs')" name="about" />
      <el-tab-pane :label="$t('siteconfig.certificate')" name="certificate" />
      <el-tab-pane :label="$t('siteconfig.faq')" name="faq" />
      <el-tab-pane :label="$t('siteconfig.latestEvent')" name="event" />
      <el-tab-pane :label="$t('siteconfig.terms')" name="terms" />
      <el-tab-pane :label="$t('siteconfig.incomeGuide')" name="income" />
      <el-tab-pane :label="$t('siteconfig.popup')" name="popup" />
      <component :is="activeTabComponent" />
    </el-tabs>
  </div>
</template>

<script setup name="Siteconfig">
import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";

// 引入各个子页面组件（按需创建）
import SiteSetting from "./site.vue";
import TradeControl from "./trade.vue";
import EmailConfig from "./email.vue";
import RegisterProtocol from "./register.vue";
import AboutUs from "./about.vue";
import Certificate from "./certificate.vue";
import FAQ from "./faq.vue";
import LatestEvent from "./event.vue";
import Terms from "./terms.vue";
import IncomeGuide from "./income.vue";
import PopupContent from "./popup.vue";

const { t } = useI18n();

// 当前激活的标签
const activeTab = ref("site");

// Tab 对应的组件映射表
const tabMap = {
  site: SiteSetting,
  trade: TradeControl,
  email: EmailConfig,
  register: RegisterProtocol,
  about: AboutUs,
  certificate: Certificate,
  faq: FAQ,
  event: LatestEvent,
  terms: Terms,
  income: IncomeGuide,
  popup: PopupContent,
};

// 根据当前 activeTab 动态计算组件
const activeTabComponent = computed(() => tabMap[activeTab.value]);

const handleTabChange = (tab) => {
  activeTab.value = tab.paneName;
};
</script>
