<template>
  <a-form ref="pwdRef" :model="user" :rules="rules" layout="vertical" class="password-form">
    <a-form-item label="旧密码" name="oldPassword">
      <a-input-password v-model:value="user.oldPassword" placeholder="请输入旧密码" />
    </a-form-item>
    <a-form-item label="新密码" name="newPassword">
      <a-input-password v-model:value="user.newPassword" placeholder="请输入新密码" />
    </a-form-item>
    <a-form-item label="确认密码" name="confirmPassword">
      <a-input-password v-model:value="user.confirmPassword" placeholder="请确认新密码" />
    </a-form-item>
    <a-space>
      <a-button type="primary" @click="submit">保存</a-button>
      <a-button danger @click="close">关闭</a-button>
    </a-space>
  </a-form>
</template>

<script setup>
import { updateUserPwd } from "@/api/system/user";

const { proxy } = getCurrentInstance();
const pwdRef = ref();

const user = reactive({
  oldPassword: undefined,
  newPassword: undefined,
  confirmPassword: undefined,
});

const equalToPassword = async (_rule, value) => {
  if (user.newPassword !== value) {
    return Promise.reject(new Error("两次输入的密码不一致"));
  }
  return Promise.resolve();
};

const rules = {
  oldPassword: [{ required: true, message: "旧密码不能为空", trigger: "blur" }],
  newPassword: [
    { required: true, message: "新密码不能为空", trigger: "blur" },
    { min: 6, max: 20, message: "长度在 6 到 20 个字符", trigger: "blur" },
    { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\ |", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "确认密码不能为空", trigger: "blur" },
    { validator: equalToPassword, trigger: "blur" },
  ],
};

function submit() {
  pwdRef.value?.validate().then(() => {
    updateUserPwd(user.oldPassword, user.newPassword).then(() => {
      proxy.$modal.msgSuccess("修改成功");
      user.oldPassword = undefined;
      user.newPassword = undefined;
      user.confirmPassword = undefined;
      pwdRef.value?.clearValidate?.();
    });
  }).catch(() => {});
}

function close() {
  proxy.$tab.closePage();
}
</script>

<style scoped>
.password-form {
  max-width: 520px;
}
</style>
