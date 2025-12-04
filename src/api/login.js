import request from "@/utils/request";

// 登录方法
export function login(username, password, totpCode, uuid) {
  const data = {
    username,
    password,
    totpCode,
    uuid,
  };
  return request({
    url: "/login",
    headers: {
      isToken: false,
      repeatSubmit: false,
    },
    method: "post",
    data: data,
  });
}

export function qrcodeLogin(username, password, totpCode, uuid) {
  const data = {
    username,
    password,
    totpCode,
    uuid,
  };
  return request({
    url: "/firstLogin",
    headers: {
      isToken: false,
      repeatSubmit: false,
    },
    method: "post",
    data: data,
  });
}

// 注册方法
export function register(data) {
  return request({
    url: "/register",
    headers: {
      isToken: false,
    },
    method: "post",
    data: data,
  });
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: "/getInfo",
    method: "get",
  });
}

// 退出方法
export function logout() {
  return request({
    url: "/logout",
    method: "post",
  });
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: "/captchaImage",
    headers: {
      isToken: false,
    },
    method: "get",
    timeout: 20000,
  });
}

export function verifyMfa(data) {
  return request({
    url: "/mfa/verify",
    method: "post",
    data: data,
  });
}
