import axios from "axios";
import { message, Modal, notification } from "ant-design-vue";
import { getToken } from "@/utils/auth";
import errorCode from "@/utils/errorCode";
import { tansParams, blobValidate } from "@/utils/common";
import cache from "@/plugins/cache";
import { saveAs } from "file-saver";
import useUserStore from "@/store/modules/user";
import { encodeApiPath } from "@/utils/apiPath";

let downloadLoadingInstance;
export let isRelogin = { show: false };
const config = window.APP_CONFIG;

axios.defaults.headers["Content-Type"] = "application/json;charset=utf-8";

const service = axios.create({
  baseURL: config.baseApiUrl,
  timeout: 10000,
});

function createLoading(content) {
  const key = `request-loading-${Date.now()}`;
  message.loading({ content, key, duration: 0 });
  return {
    close() {
      message.destroy(key);
    },
  };
}

function confirmRelogin() {
  return new Promise((resolve, reject) => {
    Modal.confirm({
      title: "系统提示",
      content: "登录状态已过期，您可以继续留在该页面，或者重新登录",
      okText: "重新登录",
      cancelText: "取消",
      onOk: () => resolve(),
      onCancel: () => reject(new Error("cancel")),
    });
  });
}

service.interceptors.request.use(
  (config) => {
    const isToken = (config.headers || {}).isToken === false;
    const isRepeatSubmit = (config.headers || {}).repeatSubmit === false;
    config.url = encodeApiPath(config.url);

    if (getToken() && !isToken) {
      config.headers.Authorization = `Bearer ${getToken()}`;
    }

    if (config.method === "get" && config.params) {
      let url = `${config.url}?${tansParams(config.params)}`;
      url = url.slice(0, -1);
      config.params = {};
      config.url = url;
    }

    if (!isRepeatSubmit && (config.method === "post" || config.method === "put")) {
      const requestObj = {
        url: config.url,
        data: typeof config.data === "object" ? JSON.stringify(config.data) : config.data,
        time: new Date().getTime(),
      };
      const requestSize = JSON.stringify(requestObj).length;
      const limitSize = 5 * 1024 * 1024;

      if (requestSize >= limitSize) {
        console.warn(`[${config.url}]: 请求数据大小超出允许的 5M 限制，无法进行防重复提交校验。`);
        return config;
      }

      const sessionObj = cache.session.getJSON("sessionObj");
      if (sessionObj === undefined || sessionObj === null || sessionObj === "") {
        cache.session.setJSON("sessionObj", requestObj);
      } else {
        const sUrl = sessionObj.url;
        const sData = sessionObj.data;
        const sTime = sessionObj.time;
        const interval = 1000;
        if (sData === requestObj.data && requestObj.time - sTime < interval && sUrl === requestObj.url) {
          const repeatMessage = "数据正在处理，请勿重复提交";
          console.warn(`[${sUrl}]: ${repeatMessage}`);
          return Promise.reject(new Error(repeatMessage));
        }
        cache.session.setJSON("sessionObj", requestObj);
      }
    }

    return config;
  },
  (error) => {
    console.log(error);
    return Promise.reject(error);
  }
);

service.interceptors.response.use(
  (res) => {
    const code = res.data.code || 200;
    const msg = errorCode[code] || res.data.msg || errorCode.default;

    if (res.request.responseType === "blob" || res.request.responseType === "arraybuffer") {
      return res.data;
    }

    if (code === 401) {
      const isAuthenticationPage = ["/login", "/register"].includes(window.location.pathname);
      if (!getToken() || isAuthenticationPage) {
        return Promise.reject(new Error(msg));
      }

      if (!isRelogin.show) {
        isRelogin.show = true;
        confirmRelogin()
          .then(() => {
            isRelogin.show = false;
            useUserStore()
              .logOut()
              .then(() => {
                location.href = "/index";
              });
          })
          .catch(() => {
            isRelogin.show = false;
          });
      }
      return Promise.reject("无效的会话，或者会话已过期，请重新登录。");
    }

    if (code === 500) {
      message.error(msg);
      return Promise.reject(new Error(msg));
    }

    if (code === 601) {
      message.warning(msg);
      return Promise.reject(new Error(msg));
    }

    if (code !== 200) {
      notification.error({ message: "系统提示", description: msg });
      return Promise.reject("error");
    }

    return Promise.resolve(res.data);
  },
  (error) => {
    console.log(`err${error}`);
    let { message: errorMessage } = error;
    if (errorMessage === "Network Error") {
      errorMessage = "后端接口连接异常";
    } else if (errorMessage.includes("timeout")) {
      errorMessage = "系统接口请求超时";
    } else if (errorMessage.includes("Request failed with status code")) {
      errorMessage = `系统接口${errorMessage.substr(errorMessage.length - 3)}异常`;
    }
    message.error({ content: errorMessage, duration: 5 });
    return Promise.reject(error);
  }
);

export function download(url, params, filename, config) {
  downloadLoadingInstance = createLoading("正在下载数据，请稍候");
  return service
    .post(url, params, {
      transformRequest: [
        (params) => {
          return tansParams(params);
        },
      ],
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      responseType: "blob",
      ...config,
    })
    .then(async (data) => {
      const isBlob = blobValidate(data);
      if (isBlob) {
        const blob = new Blob([data]);
        saveAs(blob, filename);
      } else {
        const resText = await data.text();
        const rspObj = JSON.parse(resText);
        const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode.default;
        message.error(errMsg);
      }
      downloadLoadingInstance?.close();
    })
    .catch((error) => {
      console.error(error);
      message.error("下载文件出现错误，请联系管理员！");
      downloadLoadingInstance?.close();
    });
}

export default service;
