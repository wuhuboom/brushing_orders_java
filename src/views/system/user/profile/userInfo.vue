<template>
  <el-form
    ref="userRef"
    :model="form"
    :rules="rules"
    label-width="80px"
    label-position="top"
  >
    <el-form-item :label="t('profile.userinfo.nickName')" prop="nickName">
      <el-input v-model="form.nickName" maxlength="30" />
    </el-form-item>
    <el-form-item :label="t('profile.userinfo.phoneNumber')" prop="phonenumber">
      <el-input v-model="form.phonenumber" maxlength="11" />
    </el-form-item>
    <el-form-item :label="t('profile.userinfo.email')" prop="email">
      <el-input v-model="form.email" maxlength="50" />
    </el-form-item>
    <el-form-item :label="t('profile.userinfo.gender')">
      <el-radio-group v-model="form.sex">
        <el-radio :value="0">{{ t("profile.userinfo.male") }}</el-radio>
        <el-radio :value="1">{{ t("profile.userinfo.female") }}</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit">{{
        t("profile.userinfo.save")
      }}</el-button>
      <el-button type="danger" @click="close">{{
        t("profile.userinfo.close")
      }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { getCurrentInstance } from "vue";
import { useI18n } from "vue-i18n";
import { updateUserProfile } from "@/api/system/user";

const props = defineProps({
  user: {
    type: Object,
  },
});

const { proxy } = getCurrentInstance();
const { t } = useI18n();

const form = ref({});
const rules = ref({
  nickName: [
    {
      required: true,
      message: t("profile.userinfo.nickNameRequired"),
      trigger: "blur",
    },
  ],
  email: [
    {
      required: true,
      message: t("profile.userinfo.emailRequired"),
      trigger: "blur",
    },
    {
      type: "email",
      message: t("profile.userinfo.invalidEmail"),
      trigger: ["blur", "change"],
    },
  ],
  phonenumber: [
    {
      required: true,
      message: t("profile.userinfo.phoneRequired"),
      trigger: "blur",
    },
    {
      pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
      message: t("profile.userinfo.invalidPhone"),
      trigger: "blur",
    },
  ],
});

/** 提交按钮 */
function submit() {
  proxy.$refs.userRef.validate((valid) => {
    if (valid) {
      updateUserProfile(form.value).then((response) => {
        proxy.$modal.msgSuccess(t("profile.userinfo.updateSuccess"));
        props.user.phonenumber = form.value.phonenumber;
        props.user.email = form.value.email;
      });
    }
  });
}

/** 关闭按钮 */
function close() {
  proxy.$tab.closePage();
}

// 回显当前登录用户信息
watch(
  () => props.user,
  (user) => {
    if (user) {
      form.value = {
        nickName: user.nickName,
        phonenumber: user.phonenumber,
        email: user.email,
        sex: user.sex,
      };
    }
  },
  { immediate: true }
);
</script>
