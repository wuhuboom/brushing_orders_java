<template>
  <div class="app-container">
    <el-card>
      <div class="center-container">
        <el-form
          ref="globalconfigRef"
          :model="form"
          :rules="rules"
          label-width="150px"
          class="space-y-4"
          label-position="top"
        >
          <el-form-item
            :label="$t('globalconfig.latestEventEn')"
            prop="latestEventEn"
          >
            <editor
              v-model="form.latestEventEn"
              :min-height="192"
              class="editor-wide"
            />
          </el-form-item>
          <el-form-item
            :label="$t('globalconfig.latestEventZh')"
            prop="latestEventLocal"
          >
            <editor
              v-model="form.latestEventLocal"
              :min-height="192"
              class="editor-wide"
            />
          </el-form-item>
          <el-form-item
            :label="$t('globalconfig.latestEventJa')"
            prop="latestEventsJa"
          >
            <editor
              v-model="form.latestEventsJa"
              :min-height="192"
              class="editor-wide"
            />
          </el-form-item>
          <el-form-item
            :label="$t('globalconfig.latestEventTh')"
            prop="latestEventsTh"
          >
            <editor
              v-model="form.latestEventsTh"
              :min-height="192"
              class="editor-wide"
            />
          </el-form-item>
          <el-form-item
            :label="$t('globalconfig.latestEventKo')"
            prop="latestEventsKo"
          >
            <editor
              v-model="form.latestEventsKo"
              :min-height="192"
              class="editor-wide"
            />
          </el-form-item>
          <el-form-item
            :label="$t('globalconfig.latestEventZhTw')"
            prop="latestEventsZhTw"
          >
            <editor
              v-model="form.latestEventsZhTw"
              :min-height="192"
              class="editor-wide"
            />
          </el-form-item>
          <el-form-item
            :label="$t('globalconfig.latestEventPor')"
            prop="latestEventsPor"
          >
            <editor
              v-model="form.latestEventsPor"
              :min-height="192"
              class="editor-wide"
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

<script setup>
import { ref, reactive, toRefs, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import {
  getGlobalconfig,
  addGlobalconfig,
  updateGlobalconfig,
} from "@/api/set/globalconfig";
import { getCurrentInstance } from "vue";

const { t } = useI18n();

const { proxy } = getCurrentInstance();

const open = ref(true);
const title = ref(t("globalconfig.latestEventTitle"));
const loading = ref(false);

const data = reactive({
  form: {
    id: null,
    registerProtocolEn: null,
    registerProtocolLocal: null,
    aboutUsEn: null,
    aboutUsLocal: null,
    certificateEn: null,
    certificateLocal: null,
    faqEn: null,
    faqLocal: null,
    latestEventEn: null,
    latestEventLocal: null,
    termsEn: null,
    termsLocal: null,
    incomeGuideEn: null,
    incomeGuideLocal: null,
  },
  rules: {},
});

const { form, rules } = toRefs(data);

// 页面加载时获取 ID=1 的配置
onMounted(() => {
  loading.value = true;
  getGlobalconfig(1)
    .then((response) => {
      if (response.data) {
        form.value = response.data;
      }
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
});

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    registerProtocolEn: null,
    registerProtocolLocal: null,
    aboutUsEn: null,
    aboutUsLocal: null,
    certificateEn: null,
    certificateLocal: null,
    faqEn: null,
    faqLocal: null,
    latestEventEn: null,
    latestEventLocal: null,
    termsEn: null,
    termsLocal: null,
    incomeGuideEn: null,
    incomeGuideLocal: null,
  };
  proxy.resetForm("globalconfigRef");
}

// 提交表单
function submitForm() {
  proxy.$refs["globalconfigRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateGlobalconfig(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("globalconfig.updateSuccess"));
          open.value = false;
        });
      } else {
        addGlobalconfig(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("globalconfig.addSuccess"));
          open.value = false;
        });
      }
    }
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
