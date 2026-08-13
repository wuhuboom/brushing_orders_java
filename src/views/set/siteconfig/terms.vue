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
          <el-tab-pane :label="$t('globalconfig.termsEn')" name="en">
            <el-form-item prop="termsEn">
              <editor
                v-model="form.termsEn"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.termsZh')" name="zh">
            <el-form-item prop="termsLocal">
              <editor
                v-model="form.termsLocal"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.termsJa')" name="ja">
            <el-form-item prop="termsConditionsJa">
              <editor
                v-model="form.termsConditionsJa"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.termsTh')" name="th">
            <el-form-item prop="termsConditionsTh">
              <editor
                v-model="form.termsConditionsTh"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.termsKo')" name="ko">
            <el-form-item prop="termsConditionsKo">
              <editor
                v-model="form.termsConditionsKo"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.termsZhTw')" name="zhTw">
            <el-form-item prop="termsConditionsZhTw">
              <editor
                v-model="form.termsConditionsZhTw"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.termsPor')" name="por">
            <el-form-item prop="termsConditionsPor">
              <editor
                v-model="form.termsConditionsPor"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.termsEs')" name="es">
            <el-form-item prop="termsConditionsEs">
              <editor
                v-model="form.termsConditionsEs"
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
    termsEn: null,
    termsLocal: null,
    termsConditionsJa: null,
    termsConditionsTh: null,
    termsConditionsKo: null,
    termsConditionsZhTw: null,
    termsConditionsPor: null,
    termsConditionsEs: null,
  },
  rules: {},
});

const { form, rules } = toRefs(data);

// 初始化
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
