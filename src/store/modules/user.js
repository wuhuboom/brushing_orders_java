import { login, logout, getInfo, verifyMfa, qrcodeLogin } from "@/api/login"; // 修改为包含 mfa 验证的接口
import { setToken, removeToken } from "@/utils/auth";
import { ElMessageBox } from "element-plus";
import router from "@/router";
import { getToken } from "@/utils/auth";

const useUserStore = defineStore("user", {
  state: () => ({
    token: getToken(),
    id: "",
    name: "",
    nickName: "",
    avatar: "",
    roles: [],
    permissions: [],
    isFirstLogin: false, // 判断是否为首次登录
    mfaRequired: false, // 是否需要 Google 验证码
    qrCodeUrl: "", // 存储二维码链接
  }),
  actions: {
    // 登录
    login(userInfo) {
      const username = userInfo.username.trim();
      const password = userInfo.password;
      const totpCode = userInfo.totpCode;
      const uuid = userInfo.uuid;

      return new Promise((resolve, reject) => {
        login(username, password, totpCode, uuid)
          .then((res) => {
            if (res.isFirstLogin) {
              this.isFirstLogin = true; // 标记首次登录
              this.mfaRequired = true; // 设置为需要 Google 验证
              this.qrCodeUrl = res.otpauthUri; // 存储二维码链接
            } else {
              this.mfaRequired = false; // 不需要 Google 验证
              setToken(res.token);
              this.token = res.token;
            }
            resolve(res);
          })
          .catch((error) => {
            reject(error);
          });
      });
    },
    firstLogin(userInfo) {
      const username = userInfo.username.trim();
      const password = userInfo.password;
      const totpCode = userInfo.totpCode;
      const uuid = userInfo.uuid;

      return new Promise((resolve, reject) => {
        qrcodeLogin(username, password, totpCode, uuid)
          .then((res) => {
            if (res.isFirstLogin) {
              this.isFirstLogin = true; // 标记首次登录
              this.qrCodeUrl = res.otpauthUri; // 存储二维码链接
            } else {
              setToken(res.token);
              this.token = res.token;
            }
            resolve(res);
          })
          .catch((error) => {
            reject(error);
          });
      });
    },

    // 获取用户信息
    getInfo() {
      return new Promise((resolve, reject) => {
        getInfo()
          .then((res) => {
            const user = res.user;
            this.roles = res.roles;
            this.permissions = res.permissions;
            this.id = user.userId;
            this.name = user.userName;
            this.nickName = user.nickName;
            this.avatar = user.avatar || "";
            resolve(res);
          })
          .catch((error) => {
            reject(error);
          });
      });
    },

    // 退出系统
    logOut() {
      return new Promise((resolve, reject) => {
        logout(this.token)
          .then(() => {
            this.token = "";
            this.roles = [];
            this.permissions = [];
            removeToken();
            resolve();
          })
          .catch((error) => {
            reject(error);
          });
      });
    },

    // 验证 Google 验证码
    verifyMfa(mfaCode) {
      return new Promise((resolve, reject) => {
        verifyMfa({
          loginToken: this.qrCodeUrl, // 用于标识当前登录
          code: mfaCode,
        })
          .then((res) => {
            if (res.code === 200) {
              setToken(res.token);
              this.token = res.token;
              resolve();
            } else {
              reject("Google验证码错误");
            }
          })
          .catch((error) => {
            reject(error);
          });
      });
    },
  },
});

export default useUserStore;
