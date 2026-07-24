<template>
  <div class="register">
    <a-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
      <h3 class="title">{{ title }}</h3>

      <a-form-item name="username">
        <a-input
          v-model:value="registerForm.username"
          autocomplete="off"
          placeholder="账号"
          size="large"
        >
          <template #prefix><svg-icon icon-class="user" class="input-icon" /></template>
        </a-input>
      </a-form-item>

      <a-form-item name="password">
        <a-input-password
          v-model:value="registerForm.password"
          autocomplete="off"
          placeholder="密码"
          size="large"
          @pressEnter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="lock" class="input-icon" /></template>
        </a-input-password>
      </a-form-item>

      <a-form-item name="confirmPassword">
        <a-input-password
          v-model:value="registerForm.confirmPassword"
          autocomplete="off"
          placeholder="确认密码"
          size="large"
          @pressEnter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="lock" class="input-icon" /></template>
        </a-input-password>
      </a-form-item>

      <a-form-item v-if="captchaEnabled" name="code">
        <div class="captcha-row">
          <a-input
            v-model:value="registerForm.code"
            autocomplete="off"
            placeholder="验证码"
            size="large"
            @pressEnter="handleRegister"
          >
            <template #prefix><svg-icon icon-class="validCode" class="input-icon" /></template>
          </a-input>
          <button class="captcha-button" type="button" @click="getCode">
            <img :src="codeUrl" alt="验证码" />
          </button>
        </div>
      </a-form-item>

      <a-form-item class="register-action">
        <a-button :loading="loading" block size="large" type="primary" @click.prevent="handleRegister">
          {{ loading ? "注册中..." : "注册" }}
        </a-button>
        <div class="login-link">
          <router-link class="link-type" to="/login">使用已有账户登录</router-link>
        </div>
      </a-form-item>
    </a-form>

    <div class="register-footer">
      <span>Copyright © 2025 DataCenter</span>
    </div>
  </div>
</template>

<script setup>
import { h } from "vue"
import { Modal } from "ant-design-vue"
import { getCodeImg, register } from "@/api/login"

const title = import.meta.env.VITE_APP_TITLE
const router = useRouter()
const registerRef = ref(null)

const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: "",
  code: "",
  uuid: ""
})

const equalToPassword = (_rule, value) => {
  if (!value) {
    return Promise.resolve()
  }
  if (registerForm.value.password !== value) {
    return Promise.reject(new Error("两次输入的密码不一致"))
  }
  return Promise.resolve()
}

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    { min: 2, max: 20, message: "用户账号长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  password: [
    { required: true, trigger: "blur", message: "请输入您的密码" },
    { min: 5, max: 20, message: "用户密码长度必须介于 5 和 20 之间", trigger: "blur" },
    { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\ |", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { validator: equalToPassword, trigger: "blur" }
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)

function handleRegister() {
  registerRef.value?.validate().then(() => {
    loading.value = true
    register(registerForm.value)
      .then(() => {
        const username = registerForm.value.username
        loading.value = false
        Modal.success({
          title: "系统提示",
          content: h("span", [
            "恭喜你，您的账号 ",
            h("span", { style: { color: "#ff4d4f", fontWeight: 600 } }, username),
            " 注册成功！"
          ]),
          onOk: () => router.push("/login")
        })
      })
      .catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
  }).catch(() => {})
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      registerForm.value.uuid = res.uuid
    }
  })
}

getCode()
</script>

<style lang="scss" scoped>
.register {
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

.register-form {
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

.captcha-row {
  display: flex;
  gap: 12px;
}

.captcha-button {
  flex: 0 0 112px;
  height: 40px;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;

  img {
    display: block;
    width: 100%;
    height: 100%;
  }
}

.register-action {
  width: 100%;
}

.login-link {
  margin-top: 12px;
  text-align: right;
}

.register-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.92);
  font-family: Arial, sans-serif;
  font-size: 12px;
  letter-spacing: 1px;
}
</style>
