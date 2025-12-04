<template>
  <el-form
    ref="pwdRef"
    :model="user"
    label-position="top"
    :rules="rules"
    :label-width="t('profile.formLabelWidth')"
  >
    <el-form-item :label="t('profile.resetPwd.oldPassword')" prop="oldPassword">
      <el-input
        v-model="user.oldPassword"
        :placeholder="t('profile.resetPwd.enterOldPassword')"
        type="password"
        show-password
      />
    </el-form-item>
    <el-form-item :label="t('profile.resetPwd.newPassword')" prop="newPassword">
      <el-input
        v-model="user.newPassword"
        :placeholder="t('profile.resetPwd.enterNewPassword')"
        type="password"
        show-password
      />
    </el-form-item>
    <el-form-item
      :label="t('profile.resetPwd.confirmPassword')"
      prop="confirmPassword"
    >
      <el-input
        v-model="user.confirmPassword"
        :placeholder="t('profile.resetPwd.confirmNewPassword')"
        type="password"
        show-password
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit">{{
        t("profile.resetPwd.save")
      }}</el-button>
      <el-button type="danger" @click="close">{{
        t("profile.resetPwd.close")
      }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive } from "vue";
import { getCurrentInstance } from "vue";
import { useI18n } from "vue-i18n";
import { updateUserPwd } from "@/api/system/user";

const { proxy } = getCurrentInstance();
const { t } = useI18n();

const user = reactive({
  oldPassword: undefined,
  newPassword: undefined,
  confirmPassword: undefined,
});

const equalToPassword = (rule, value, callback) => {
  if (user.newPassword !== value) {
    callback(new Error(t("profile.resetPwd.passwordMismatch")));
  } else {
    callback();
  }
};

const rules = ref({
  oldPassword: [
    {
      required: true,
      message: t("profile.resetPwd.oldPasswordRequired"),
      trigger: "blur",
    },
  ],
  newPassword: [
    {
      required: true,
      message: t("profile.resetPwd.newPasswordRequired"),
      trigger: "blur",
    },
    {
      min: 6,
      max: 20,
      message: t("profile.resetPwd.passwordLength"),
      trigger: "blur",
    },
    {
      pattern: /^[^<>"'|\\]+$/,
      message: t("profile.resetPwd.invalidPasswordChars"),
      trigger: "blur",
    },
  ],
  confirmPassword: [
    {
      required: true,
      message: t("profile.resetPwd.confirmPasswordRequired"),
      trigger: "blur",
    },
    { required: true, validator: equalToPassword, trigger: "blur" },
  ],
});

/** 提交按钮 */
function submit() {
  proxy.$refs.pwdRef.validate((valid) => {
    if (valid) {
      updateUserPwd(user.oldPassword, user.newPassword).then((response) => {
        proxy.$modal.msgSuccess(t("profile.resetPwd.updateSuccess"));
      });
    }
  });
}

/** 关闭按钮 */
function close() {
  proxy.$tab.closePage();
}
</script>
