
<template>
  <div class="app-container">
    <el-card>
      <el-form
        ref="globalconfigRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="space-y-4"
      >
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane
            :label="$t('globalconfig.registerProtocolEn')"
            name="en"
          >
            <el-form-item prop="registerProtocolEn">
              <editor
                v-model="form.registerProtocolEn"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane
            :label="$t('globalconfig.registerProtocolZh')"
            name="zh"
          >
            <el-form-item prop="registerProtocolLocal">
              <editor
                v-model="form.registerProtocolLocal"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane
            :label="$t('globalconfig.registerProtocolJa')"
            name="ja"
          >
            <el-form-item prop="registrationAgreementJa">
              <editor
                v-model="form.registrationAgreementJa"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane
            :label="$t('globalconfig.registerProtocolTh')"
            name="th"
          >
            <el-form-item prop="registrationAgreementTh">
              <editor
                v-model="form.registrationAgreementTh"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane
            :label="$t('globalconfig.registerProtocolKo')"
            name="ko"
          >
            <el-form-item prop="registrationAgreementKo">
              <editor
                v-model="form.registrationAgreementKo"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane
            :label="$t('globalconfig.registerProtocolZhTw')"
            name="zhTw"
          >
            <el-form-item prop="registrationAgreementZhTw">
              <editor
                v-model="form.registrationAgreementZhTw"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane
            :label="$t('globalconfig.registerProtocolPor')"
            name="por"
          >
            <el-form-item prop="registrationAgreementPor">
              <editor
                v-model="form.registrationAgreementPor"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane
            :label="$t('globalconfig.registerProtocolEs')"
            name="es"
          >
            <el-form-item prop="registrationAgreementEs">
              <editor
                v-model="form.registrationAgreementEs"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>
        </el-tabs>

        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">
            {{ $t("common.save") }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, toRefs, onMounted, getCurrentInstance } from "vue";
import { useI18n } from "vue-i18n";
import {
  getGlobalconfig,
  addGlobalconfig,
  updateGlobalconfig,
} from "@/api/set/globalconfig";

const { t } = useI18n();
const { proxy } = getCurrentInstance();

const activeTab = ref("en");
const loading = ref(false);

const data = reactive({
  form: {
    id: null,
    registerProtocolEn: null,
    registerProtocolLocal: null,
    registrationAgreementJa: null,
    registrationAgreementTh: null,
    registrationAgreementKo: null,
    registrationAgreementZhTw: null,
    registrationAgreementPor: null,
    registrationAgreementEs: null,
  },
  rules: {},
});

const { form, rules } = toRefs(data);

// 初始化加载
onMounted(() => {
  loading.value = true;
  getGlobalconfig(1)
    .then((res) => {
      if (res.data) {
        form.value = res.data;
      }
    })
    .finally(() => {
      loading.value = false;
    });
});

// 提交
function submitForm() {
  proxy.$refs.globalconfigRef.validate((valid) => {
    if (!valid) return;

    const api = form.value.id
      ? updateGlobalconfig(form.value)
      : addGlobalconfig(form.value);

    api.then(() => {
      proxy.$modal.msgSuccess(
        form.value.id
          ? t("globalconfig.updateSuccess")
          : t("globalconfig.addSuccess")
      );
    });
  });
}
</script>
<style scoped>
.editor-wide {
  width: 100%;
}

.dialog-footer {
  margin-top: 20px;
  text-align: right;
}
</style>
