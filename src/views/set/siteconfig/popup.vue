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
            :label="$t('siteconfig.popupMessage')"
            prop="popupMessage"
          >
            <image-upload v-model="form.popupMessage" :limit="1" />
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
import { ElMessage } from "element-plus";

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
});

const rules = {
  siteName: [
    {
      required: true,
      message: t("siteconfig.siteNameRequired"),
      trigger: "blur",
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

// 提交表单
function submitForm() {
  proxy.$refs["siteconfigRef"].validate((valid) => {
    if (!valid) return;

    const submitApi = form.id ? updateSiteconfig : addSiteconfig;

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
