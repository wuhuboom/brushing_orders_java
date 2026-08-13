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
          <el-tab-pane :label="$t('globalconfig.latestEventEn')" name="en">
            <el-form-item prop="latestEventEn">
              <editor
                v-model="form.latestEventEn"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.latestEventZh')" name="zh">
            <el-form-item prop="latestEventLocal">
              <editor
                v-model="form.latestEventLocal"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.latestEventJa')" name="ja">
            <el-form-item prop="latestEventsJa">
              <editor
                v-model="form.latestEventsJa"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.latestEventTh')" name="th">
            <el-form-item prop="latestEventsTh">
              <editor
                v-model="form.latestEventsTh"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.latestEventKo')" name="ko">
            <el-form-item prop="latestEventsKo">
              <editor
                v-model="form.latestEventsKo"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.latestEventZhTw')" name="zhTw">
            <el-form-item prop="latestEventsZhTw">
              <editor
                v-model="form.latestEventsZhTw"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.latestEventPor')" name="por">
            <el-form-item prop="latestEventsPor">
              <editor
                v-model="form.latestEventsPor"
                :min-height="260"
                class="editor-wide"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane :label="$t('globalconfig.latestEventEs')" name="es">
            <el-form-item prop="latestEventsEs">
              <editor
                v-model="form.latestEventsEs"
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
    latestEventEn: null,
    latestEventLocal: null,
    latestEventsJa: null,
    latestEventsTh: null,
    latestEventsKo: null,
    latestEventsZhTw: null,
    latestEventsPor: null,
    latestEventsEs: null,
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
