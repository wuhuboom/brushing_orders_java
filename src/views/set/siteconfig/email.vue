<template>
  <div class="app-container">
    <el-card>
      <div class="center-container">
        <el-form
          ref="emialconfigRef"
          :model="form"
          :rules="rules"
          label-width="150px"
          class="space-y-4"
          label-position="top"
        >
          <el-form-item :label="$t('emailconfig.host')" prop="host">
            <el-input
              v-model="form.host"
              :placeholder="$t('emailconfig.enterHost')"
              class="input-wide"
            />
          </el-form-item>
          <el-form-item :label="$t('emailconfig.username')" prop="username">
            <el-input
              v-model="form.username"
              :placeholder="$t('emailconfig.enterUsername')"
              class="input-wide"
            />
          </el-form-item>
          <el-form-item :label="$t('emailconfig.password')" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              :placeholder="$t('emailconfig.enterPassword')"
              class="input-wide"
            />
          </el-form-item>
          <el-form-item
            :label="$t('emailconfig.fromAddress')"
            prop="fromAddress"
          >
            <el-input
              v-model="form.fromAddress"
              :placeholder="$t('emailconfig.enterFromAddress')"
              class="input-wide"
            />
          </el-form-item>
          <el-form-item :label="$t('emailconfig.fromName')" prop="fromName">
            <el-input
              v-model="form.fromName"
              :placeholder="$t('emailconfig.enterFromName')"
              class="input-wide"
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
  getEmialconfig,
  addEmialconfig,
  updateEmialconfig,
} from "@/api/set/emialconfig";
import { getCurrentInstance } from "vue";

const { t } = useI18n();

const { proxy } = getCurrentInstance();

const open = ref(true);
const title = ref(t("emailconfig.titleAdd"));
const loading = ref(false);

const data = reactive({
  form: {
    id: null,
    host: null,
    username: null,
    password: null,
    fromAddress: null,
    fromName: null,
  },
  rules: {
    host: [
      {
        required: true,
        message: t("emailconfig.hostRequired"),
        trigger: "blur",
      },
      {
        pattern: /^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
        message: t("emailconfig.hostPattern"),
        trigger: "blur",
      },
    ],
    username: [
      {
        required: true,
        message: t("emailconfig.usernameRequired"),
        trigger: "blur",
      },
    ],
    password: [
      {
        required: true,
        message: t("emailconfig.passwordRequired"),
        trigger: "blur",
      },
    ],
    fromAddress: [
      {
        required: true,
        message: t("emailconfig.fromAddressRequired"),
        trigger: "blur",
      },
      {
        type: "email",
        message: t("emailconfig.fromAddressEmail"),
        trigger: "blur",
      },
    ],
    fromName: [
      {
        required: false,
        message: t("emailconfig.fromNameRequired"),
        trigger: "blur",
      },
    ],
  },
});

const { form, rules } = toRefs(data);

// 页面加载时获取 ID=1 的配置
onMounted(() => {
  loading.value = true;
  getEmialconfig(1)
    .then((response) => {
      if (response.data) {
        form.value = response.data;
        title.value = t("emailconfig.titleEdit");
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
    host: null,
    username: null,
    password: null,
    fromAddress: null,
    fromName: null,
  };
  proxy.resetForm("emialconfigRef");
}

// 提交表单
function submitForm() {
  proxy.$refs["emialconfigRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateEmialconfig(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("emailconfig.updateSuccess"));
          open.value = false;
        });
      } else {
        addEmialconfig(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("emailconfig.addSuccess"));
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
