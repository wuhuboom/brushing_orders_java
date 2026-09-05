<template>
  <div class="login">
    <a-form
      ref="loginRef"
      :model="loginForm"
      :rules="loginRules"
      class="login-form"
    >
      <h3 class="title">{{ title }}</h3>

      <a-form-item name="username">
        <a-input
          v-model:value="loginForm.username"
          autocomplete="off"
          placeholder="账号"
          size="large"
        >
          <template #prefix
            ><svg-icon icon-class="user" class="input-icon"
          /></template>
        </a-input>
      </a-form-item>

      <a-form-item name="password">
        <a-input-password
          v-model:value="loginForm.password"
          autocomplete="off"
          placeholder="密码"
          size="large"
          @pressEnter="handleLogin"
        >
          <template #prefix
            ><svg-icon icon-class="lock" class="input-icon"
          /></template>
        </a-input-password>
      </a-form-item>

      <a-form-item v-if="showGoogleCodeInput" name="googleCode">
        <a-input
          v-model:value="loginForm.googleCode"
          autocomplete="off"
          :maxlength="6"
          placeholder="谷歌验证码（6位数字）"
          size="large"
          @pressEnter="handleLogin"
        >
          <template #prefix
            ><svg-icon icon-class="validCode" class="input-icon"
          /></template>
        </a-input>
      </a-form-item>

      <a-checkbox v-model:checked="loginForm.rememberMe" class="remember-check">
        记住密码
      </a-checkbox>

      <a-form-item class="login-action">
        <a-button
          :loading="loading"
          block
          size="large"
          type="primary"
          @click.prevent="handleLogin"
        >
          {{ loading ? "登录中..." : "登录" }}
        </a-button>
        <div v-if="register" class="register-link">
          <router-link class="link-type" to="/register">立即注册</router-link>
        </div>
      </a-form-item>
    </a-form>

    <a-modal
      v-model:open="googleBindDialog.visible"
      title="绑定谷歌验证器"
      width="420px"
      :mask-closable="false"
      :keyboard="false"
      @cancel="handleGoogleDialogClose"
    >
      <div class="google-bind-content">
        <div class="qr-section">
          <p class="tips">请使用 Google Authenticator 扫描下方二维码：</p>
          <div class="qr-code">
            <img
              :src="googleBindDialog.qrCodeBase64"
              alt="Google Authenticator QR Code"
            />
          </div>
        </div>

        <div class="manual-section">
          <p class="tips">或手动输入以下密钥：</p>
          <a-input :value="googleBindDialog.otpAuthUrl" readonly size="small">
            <template #addonAfter>
              <a-button type="link" size="small" @click="copyOtpUrl">
                <svg-icon icon-class="clipboard" />复制
              </a-button>
            </template>
          </a-input>
        </div>

        <div class="verify-section">
          <p class="tips">扫描后，请输入验证器显示的 6 位验证码：</p>
          <a-input
            v-model:value="googleBindDialog.verifyCode"
            :maxlength="6"
            placeholder="请输入 6 位验证码"
            size="large"
            @pressEnter="handleGoogleConfirm"
          />
        </div>

        <a-alert
          v-if="googleBindDialog.errorMsg"
          :message="googleBindDialog.errorMsg"
          type="error"
          show-icon
        />
      </div>

      <template #footer>
        <a-space>
          <a-button @click="handleGoogleDialogClose">取消</a-button>
          <a-button
            type="primary"
            :loading="googleBindDialog.loading"
            @click="handleGoogleConfirm"
          >
            确认绑定
          </a-button>
        </a-space>
      </template>
    </a-modal>

    <div class="login-footer">
      <span>Copyright © 2025 DataCenter</span>
    </div>
  </div>
</template>

<script setup>
import Cookies from "js-cookie";
import { message } from "ant-design-vue";
import { getCodeImg } from "@/api/login";
import { encrypt, decrypt } from "@/utils/jsencrypt";
import useUserStore from "@/store/modules/user";

const title = import.meta.env.VITE_APP_TITLE;
const userStore = useUserStore();
const route = useRoute();
const router = useRouter();
const loginRef = ref(null);

const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: "",
  googleCode: "",
});

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }],
};

const codeUrl = ref("");
const loading = ref(false);
const captchaEnabled = ref(true);
const register = ref(false);
const redirect = ref(undefined);
const showGoogleCodeInput = ref(true);

const googleBindDialog = ref({
  visible: false,
  qrCodeBase64: "",
  otpAuthUrl: "",
  verifyCode: "",
  username: "",
  loading: false,
  errorMsg: "",
});

watch(
  route,
  (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect;
  },
  { immediate: true },
);

function handleLogin() {
  loginRef.value
    ?.validate()
    .then(() => {
      loading.value = true;
      persistRememberedLogin();
      userStore
        .login(loginForm.value)
        .then((res) => {
          if (res && res.firstTimeGoogleSetup) {
            loading.value = false;
            googleBindDialog.value = {
              visible: true,
              qrCodeBase64: res.qrCodeBase64?.startsWith("data:")
                ? res.qrCodeBase64
                : `data:image/png;base64,${res.qrCodeBase64}`,
              otpAuthUrl: res.otpAuthUrl,
              verifyCode: "",
              username: res.username,
              loading: false,
              errorMsg: "",
            };
            return;
          }
          return navigateAfterLogin().finally(() => {
            loading.value = false;
          });
        })
        .catch(() => {
          loading.value = false;
          if (captchaEnabled.value) {
            getCode();
          }
        });
    })
    .catch(() => {});
}

function persistRememberedLogin() {
  if (loginForm.value.rememberMe) {
    Cookies.set("username", loginForm.value.username, { expires: 30 });
    Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 });
    Cookies.set("rememberMe", "true", { expires: 30 });
  } else {
    Cookies.remove("username");
    Cookies.remove("password");
    Cookies.remove("rememberMe");
  }
}

function navigateAfterLogin() {
  const query = route.query;
  const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
    if (cur !== "redirect") {
      acc[cur] = query[cur];
    }
    return acc;
  }, {});
  return router.push({ path: redirect.value || "/", query: otherQueryParams });
}

function handleGoogleConfirm() {
  const { verifyCode, username } = googleBindDialog.value;
  if (!verifyCode || verifyCode.length !== 6) {
    googleBindDialog.value.errorMsg = "请输入 6 位验证码";
    return;
  }

  googleBindDialog.value.loading = true;
  googleBindDialog.value.errorMsg = "";

  userStore
    .confirmGoogleAuth(username, verifyCode)
    .then(() => {
      googleBindDialog.value.loading = false;
      googleBindDialog.value.visible = false;
      message.success("谷歌验证绑定成功");
      navigateAfterLogin();
    })
    .catch((error) => {
      googleBindDialog.value.loading = false;
      googleBindDialog.value.errorMsg = getGoogleAuthErrorMessage(error);
    });
}

function getGoogleAuthErrorMessage(error) {
  const rawMessage = error?.message || "";
  if (rawMessage.includes("过期")) {
    return "二维码已过期，请重新登录获取新的二维码";
  }
  if (rawMessage.includes("验证失败") || rawMessage.includes("验证码")) {
    return "验证码错误，请检查后重试";
  }
  if (rawMessage.includes("时钟") || rawMessage.includes("时间")) {
    return "设备时间不同步，请校准手机时间后重试";
  }
  return rawMessage || "验证失败，请重试";
}

function copyOtpUrl() {
  navigator.clipboard
    .writeText(googleBindDialog.value.otpAuthUrl)
    .then(() => {
      message.success("已复制到剪贴板");
    })
    .catch(() => {
      message.error("复制失败，请手动复制");
    });
}

function handleGoogleDialogClose() {
  googleBindDialog.value.visible = false;
  googleBindDialog.value.verifyCode = "";
  googleBindDialog.value.errorMsg = "";
}

function getCode() {
  getCodeImg().then((res) => {
    captchaEnabled.value =
      res.captchaEnabled === undefined ? true : res.captchaEnabled;
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img;
      loginForm.value.uuid = res.uuid;
    }
  });
}

function getCookie() {
  const username = Cookies.get("username");
  const password = Cookies.get("password");
  const rememberMe = Cookies.get("rememberMe");
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password:
      password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === "true",
    code: "",
    uuid: "",
    googleCode: "",
  };
}

getCode();
getCookie();
</script>

<style lang="scss" scoped>
.login {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
  background-position: center;
}

.title {
  margin: 0 auto 32px auto;
  text-align: center;
  color: #1f3f77;
  font-size: 28px;
  font-weight: 700;
}

.login-form {
  margin: 6vw;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.82);
  width: 392px;
  padding: 38px 32px 18px;
  z-index: 1;
  box-shadow: 0 18px 45px rgba(24, 144, 255, 0.16);
  backdrop-filter: blur(10px);
}

.input-icon {
  color: #8c8c8c;
}

.remember-check {
  margin: 0 0 25px;
}

.login-action {
  width: 100%;
}

.register-link {
  margin-top: 12px;
  text-align: right;
}

.login-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  line-height: 22px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.92);
  font-family: var(--app-font-family);
  font-size: 14px;
  letter-spacing: normal;
}

.google-bind-content {
  .qr-section {
    text-align: center;
    margin-bottom: 20px;

    .qr-code {
      display: inline-block;
      padding: 10px;
      background: #fff;
      border: 1px solid #eee;
      border-radius: 4px;

      img {
        width: 200px;
        height: 200px;
      }
    }
  }

  .manual-section,
  .verify-section {
    margin-bottom: 20px;
  }

  .tips {
    font-size: 14px;
    color: #666;
    margin-bottom: 10px;
  }
}
</style>
