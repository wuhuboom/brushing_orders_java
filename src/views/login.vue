<template>
  <div class="login">
    <el-form
      ref="loginRef"
      :model="loginForm"
      :rules="loginRules"
      class="login-form"
    >
      <h3 class="title">{{ title }}</h3>

      <!-- 用户名输入框 -->
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          size="large"
          auto-complete="off"
          :placeholder="t('login.username')"
        >
          <template #prefix>
            <svg-icon icon-class="user" class="el-input__icon input-icon" />
          </template>
        </el-input>
      </el-form-item>

      <!-- 密码输入框 -->
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          size="large"
          auto-complete="off"
          :placeholder="t('login.password')"
          @keyup.enter="handleLogin"
        >
          <template #prefix>
            <svg-icon icon-class="password" class="el-input__icon input-icon" />
          </template>
        </el-input>
      </el-form-item>

      <!-- Google 验证器验证码输入框 -->
      <el-form-item prop="totpCode">
        <el-input
          v-model="loginForm.totpCode"
          size="large"
          auto-complete="off"
          :placeholder="t('login.totpPlaceholder')"
          maxlength="6"
          @keyup.enter="handleLogin"
        >
          <template #prefix>
            <svg-icon
              icon-class="validCode"
              class="el-input__icon input-icon"
            />
          </template>
        </el-input>
      </el-form-item>

      <!-- 记住密码选项 -->
      <el-checkbox v-model="loginForm.rememberMe">{{
        t("login.rememberMe")
      }}</el-checkbox>

      <!-- 登录按钮 -->
      <el-form-item style="width: 100%">
        <el-button
          :loading="loading"
          size="large"
          type="primary"
          style="width: 100%"
          @click.prevent="handleLogin"
        >
          <span v-if="!loading">{{ t("login.login") }}</span>
          <span v-else>{{ t("login.loggingIn") }}</span>
        </el-button>
      </el-form-item>
    </el-form>

    <!--  首次登录弹出框 -->
    <el-dialog
      :title="t('login.bindGoogleTitle')"
      v-model="isFirstLogin"
      width="500px"
      @close="closeDialog"
      class="qr-dialog"
    >
      <el-form class="qr-form" @submit.prevent="qrcodeLogin">
        <!-- 二维码容器 -->
        <el-form-item class="qr-image-container">
          <div class="qr-image-wrapper">
            <img :src="qrCodeUrl" :alt="t('login.scanAlt')" class="qr-img" />
          </div>
          <div class="qr-description">
            {{ t("login.scanDescription") }}
          </div>
        </el-form-item>

        <!-- Google 验证码输入框 -->
        <el-form-item class="totp-input">
          <el-input
            v-model="loginForm.totpCode"
            size="large"
            auto-complete="off"
            :placeholder="t('login.googleCode')"
            maxlength="6"
            @keyup.enter="qrcodeLogin"
          />
        </el-form-item>

        <!-- 按钮 -->
        <el-form-item>
          <div class="button-container">
            <el-button type="primary" @click.prevent="qrcodeLogin">
              {{ t("common.confirm") }}
            </el-button>
            <el-button @click="closeDialog">{{ t("common.cancel") }}</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-dialog>

    <div class="el-login-footer"></div>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { login, verifyMfa } from "@/api/login";
import Cookies from "js-cookie";
import useUserStore from "@/store/modules/user"; // 导入 userStore
const userStore = useUserStore(); // 使用 userStore
import QRCode from "qrcode";
import { encrypt, decrypt } from "@/utils/jsencrypt";

const { t } = useI18n();

const title = import.meta.env.VITE_APP_TITLE;
const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  totpCode: "", // Google 验证码
  uuid: "",
});

const loginRules = {
  username: [
    { required: true, trigger: "blur", message: t("login.usernameRequired") },
  ],
  password: [
    { required: true, trigger: "blur", message: t("login.passwordRequired") },
  ],
};

const isFirstLogin = ref(false); // 是否为首次登录
const qrCodeUrl = ref(""); // Google 验证器二维码链接
const loading = ref(false);
const redirect = ref(undefined);

watch(
  route,
  (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect;
  },
  { immediate: true }
);

async function handleLogin() {
  console.log(123);
  proxy.$refs.loginRef.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      // 记住密码逻辑
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 });
        Cookies.set("password", encrypt(loginForm.value.password), {
          expires: 30,
        });
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 });
      } else {
        // 否则移除
        Cookies.remove("username");
        Cookies.remove("password");
        Cookies.remove("rememberMe");
      }

      loading.value = false;
      // 调用登录方法
      const res = await userStore.login(loginForm.value);

      console.log(res);
      if (res.isFirstLogin) {
        // 如果是首次登录，显示二维码
        loginForm.value.totpCode = null;
        isFirstLogin.value = true;

        // 使用 qrcode 库生成二维码
        QRCode.toDataURL(res.otpauthUri)
          .then((url) => {
            qrCodeUrl.value = url; // 将二维码 URL 设置为 img 的 src
          })
          .catch((err) => {
            console.error("生成二维码失败", err);
          });
      } else {
        // 普通登录，存储 token 并跳转
        localStorage.setItem("token", res.token);
        const query = route.query;
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur];
          }
          return acc;
        }, {});
        router.push({ path: redirect.value || "/", query: otherQueryParams });
      }
    }
  });
}

async function qrcodeLogin() {
  if (loginForm.value.totpCode == null) {
    proxy.$modal.msgError(t("login.enterCode"));
    return;
  }
  const res = await userStore.firstLogin(loginForm.value);
  if (res.code === 200) {
    localStorage.setItem("token", res.token);
    const query = route.query;
    const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
      if (cur !== "redirect") {
        acc[cur] = query[cur];
      }
      return acc;
    }, {});
    router.push({ path: redirect.value || "/", query: otherQueryParams });
  }
}

function closeDialog() {
  loginForm.value.totpCode = null;
  isFirstLogin.value = false;
}

function getCookie() {
  const username = Cookies.get("username");
  const password = Cookies.get("password");
  const rememberMe = Cookies.get("rememberMe");
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password:
      password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
  };
}
getCookie();
</script>

<style lang="scss" scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(-45deg, #4facfe, #00f2fe, #a8edea, #fed6e3);
  background-size: 400% 400%;
  animation: gradient 15s ease infinite;
  position: relative;
  overflow: hidden;
}

@keyframes gradient {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #333;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 1px;
}

.login-form {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  width: 420px;
  padding: 30px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  z-index: 1;
  transition: transform 0.3s ease;
}

.login-form:hover {
  transform: translateY(-5px);
}

.el-input {
  height: 44px;
  input {
    height: 44px;
    border-radius: 8px;
    border: 1px solid #dcdcdc;
    transition: border-color 0.3s ease;
  }
  input:focus {
    border-color: #4facfe;
    box-shadow: 0 0 5px rgba(79, 172, 254, 0.5);
  }
}

.input-icon {
  height: 39px;
  width: 14px;
  margin-left: 8px;
  color: #666;
}

.login-code {
  width: 33%;
  height: 44px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 8px;
  }
}

.login-code-img {
  height: 44px;
  padding-left: 12px;
}

.el-checkbox {
  color: #333;
}

.el-button {
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  transition: background-color 0.3s ease;
}

.el-button:hover {
  background-color: #3b9ce6;
}

.link-type {
  color: #4facfe;
  text-decoration: none;
  font-size: 14px;
}

.link-type:hover {
  text-decoration: underline;
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial, sans-serif;
  font-size: 12px;
  letter-spacing: 1px;
  background: rgba(0, 0, 0, 0.3);
}
.qr-dialog .el-dialog {
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-width: 500px;
  margin: 0 auto;
}

.qr-dialog .el-dialog__header {
  width: 100%;
  text-align: center;
  padding: 20px;
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.qr-form {
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 0 20px;
  box-sizing: border-box;
}

.qr-image-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.qr-image-wrapper {
  width: 200px;
  height: 200px;
  padding: 10px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  margin: auto;
}

.qr-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-description {
  width: 100%;
  margin-top: 10px;
  font-size: 14px;
  color: #666;
  text-align: center;
}

.totp-input {
  width: 100%;
  margin-bottom: 20px;
}

.totp-input .el-input__inner {
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  padding: 0 15px;
  font-size: 14px;
}

.button-container {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.button-container .el-button {
  padding: 10px 20px;
  font-size: 14px;
  border-radius: 4px;
}

.button-container .el-button--primary {
  background-color: #409eff;
  border-color: #409eff;
  transition: background-color 0.3s;
}

.button-container .el-button--primary:hover {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

.button-container .el-button:not(.el-button--primary) {
  background-color: #f5f7fa;
  border-color: #dcdfe6;
  color: #606266;
}

.button-container .el-button:not(.el-button--primary):hover {
  background-color: #e6e9f0;
  border-color: #c6c9d0;
}
</style>
