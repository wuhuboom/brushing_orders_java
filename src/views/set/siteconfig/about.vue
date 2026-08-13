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
          <el-tab-pane :label="$t('globalconfig.aboutUsEn')" name="en">
            <el-form-item prop="aboutUsEn">
              <editor
                v-model="form.aboutUsEn"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.aboutUsZh')" name="zh">
            <el-form-item prop="aboutUsLocal">
              <editor
                v-model="form.aboutUsLocal"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.aboutUsJa')" name="ja">
            <el-form-item prop="aboutUsJa">
              <editor
                v-model="form.aboutUsJa"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.aboutUsTh')" name="th">
            <el-form-item prop="aboutUsTh">
              <editor
                v-model="form.aboutUsTh"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.aboutUsKo')" name="ko">
            <el-form-item prop="aboutUsKo">
              <editor
                v-model="form.aboutUsKo"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.aboutUsZhTw')" name="zhTw">
            <el-form-item prop="aboutUsZhTw">
              <editor
                v-model="form.aboutUsZhTw"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.aboutUsPor')" name="por">
            <el-form-item prop="aboutUsPor">
              <editor
                v-model="form.aboutUsPor"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.aboutUsEs')" name="es">
            <el-form-item prop="aboutUsEs">
              <editor
                v-model="form.aboutUsEs"
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
    aboutUsEn: null,
    aboutUsLocal: null,
    aboutUsJa: null,
    aboutUsTh: null,
    aboutUsKo: null,
    aboutUsZhTw: null,
    aboutUsPor: null,
    aboutUsEs: null,
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
