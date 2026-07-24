import router from "@/router";
import { Modal } from "ant-design-vue";
import { login, logout, getInfo, googleConfirm } from "@/api/login";
import { getToken, setToken, removeToken } from "@/utils/auth";
import { isHttp, isEmpty } from "@/utils/validate";
import defAva from "@/assets/images/profile.jpg";

const config = window.APP_CONFIG;

function confirmSecurity(content) {
  return new Promise((resolve, reject) => {
    Modal.confirm({
      title: "安全提示",
      content,
      okText: "确定",
      cancelText: "取消",
      onOk: () => resolve(),
      onCancel: () => reject(new Error("cancel")),
    });
  });
}

const useUserStore = defineStore("user", {
  state: () => ({
    token: getToken(),
    id: "",
    name: "",
    nickName: "",
    avatar: "",
    roles: [],
    permissions: [],
  }),
  actions: {
    login(userInfo) {
      const username = userInfo.username.trim();
      const password = userInfo.password;
      const code = userInfo.code;
      const uuid = userInfo.uuid;
      const googleCode = userInfo.googleCode;
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid, googleCode)
          .then((res) => {
            if (res.firstTimeGoogleSetup) {
              resolve({
                firstTimeGoogleSetup: true,
                qrCodeBase64: res.qrCodeBase64,
                otpAuthUrl: res.otpAuthUrl,
                username,
              });
              return;
            }

            setToken(res.token);
            this.token = res.token;
            resolve({ token: res.token });
          })
          .catch((error) => {
            reject(error);
          });
      });
    },
    confirmGoogleAuth(username, googleCode) {
      return new Promise((resolve, reject) => {
        googleConfirm(username, googleCode)
          .then((res) => {
            if (res.token) {
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
    getInfo() {
      return new Promise((resolve, reject) => {
        getInfo()
          .then((res) => {
            const user = res.user;
            let avatar = user.avatar || "";
            if (!isHttp(avatar)) {
              avatar = isEmpty(avatar) ? defAva : config.baseApiUrl + avatar;
            }
            if (res.roles && res.roles.length > 0) {
              this.roles = res.roles;
              this.permissions = res.permissions;
            } else {
              this.roles = ["ROLE_DEFAULT"];
            }
            this.id = user.userId;
            this.name = user.userName;
            this.nickName = user.nickName;
            this.avatar = avatar;

            if (res.isDefaultModifyPwd) {
              confirmSecurity("您的密码还是初始密码，请修改密码！")
                .then(() => {
                  router.push({ name: "Profile", params: { activeTab: "resetPwd" } });
                })
                .catch(() => {});
            }

            if (!res.isDefaultModifyPwd && res.isPasswordExpired) {
              confirmSecurity("您的密码已过期，请尽快修改密码！")
                .then(() => {
                  router.push({ name: "Profile", params: { activeTab: "resetPwd" } });
                })
                .catch(() => {});
            }

            resolve(res);
          })
          .catch((error) => {
            reject(error);
          });
      });
    },
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
  },
});

export default useUserStore;
