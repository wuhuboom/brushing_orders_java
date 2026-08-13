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
          <el-tab-pane :label="$t('globalconfig.faqEn')" name="en">
            <el-form-item prop="faqEn">
              <editor
                v-model="form.faqEn"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.faqZh')" name="zh">
            <el-form-item prop="faqLocal">
              <editor
                v-model="form.faqLocal"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.faqJa')" name="ja">
            <el-form-item prop="faqJa">
              <editor
                v-model="form.faqJa"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.faqTh')" name="th">
            <el-form-item prop="faqTh">
              <editor
                v-model="form.faqTh"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.faqKo')" name="ko">
            <el-form-item prop="faqKo">
              <editor
                v-model="form.faqKo"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.faqZhTw')" name="zhTw">
            <el-form-item prop="faqZhTw">
              <editor
                v-model="form.faqZhTw"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.faqPor')" name="por">
            <el-form-item prop="faqPor">
              <editor
                v-model="form.faqPor"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.faqEs')" name="es">
            <el-form-item prop="faqEs">
              <editor
                v-model="form.faqEs"
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
    faqEn: null,
    faqLocal: null,
    faqJa: null,
    faqTh: null,
    faqKo: null,
    faqZhTw: null,
    faqPor: null,
    faqEs: null,
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
