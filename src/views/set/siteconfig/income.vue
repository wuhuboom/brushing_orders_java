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
          <el-tab-pane :label="$t('globalconfig.incomeGuideEn')" name="en">
            <el-form-item prop="incomeGuideEn">
              <editor
                v-model="form.incomeGuideEn"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.incomeGuideZh')" name="zh">
            <el-form-item prop="incomeGuideLocal">
              <editor
                v-model="form.incomeGuideLocal"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.incomeGuideJa')" name="ja">
            <el-form-item prop="incomeGuideJa">
              <editor
                v-model="form.incomeGuideJa"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.incomeGuideTh')" name="th">
            <el-form-item prop="incomeGuideTh">
              <editor
                v-model="form.incomeGuideTh"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.incomeGuideKo')" name="ko">
            <el-form-item prop="incomeGuideKo">
              <editor
                v-model="form.incomeGuideKo"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.incomeGuideZhTw')" name="zhTw">
            <el-form-item prop="incomeGuideZhTw">
              <editor
                v-model="form.incomeGuideZhTw"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.incomeGuidePor')" name="por">
            <el-form-item prop="incomeGuidePor">
              <editor
                v-model="form.incomeGuidePor"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.incomeGuideEs')" name="es">
            <el-form-item prop="incomeGuideEs">
              <editor
                v-model="form.incomeGuideEs"
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
    incomeGuideEn: null,
    incomeGuideLocal: null,
    incomeGuideJa: null,
    incomeGuideTh: null,
    incomeGuideKo: null,
    incomeGuideZhTw: null,
    incomeGuidePor: null,
    incomeGuideEs: null,
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
