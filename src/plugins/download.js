import axios from "axios";
import { message } from "ant-design-vue";
import { saveAs } from "file-saver";
import { getToken } from "@/utils/auth";
import errorCode from "@/utils/errorCode";
import { blobValidate } from "@/utils/common";

const config = window.APP_CONFIG;
const baseURL = config.baseApiUrl;
let downloadLoadingInstance;

function createLoading(content) {
  const key = `download-loading-${Date.now()}`;
  message.loading({ content, key, duration: 0 });
  return {
    close() {
      message.destroy(key);
    },
  };
}

export default {
  name(name, isDelete = true) {
    const url = `${baseURL}/common/download?fileName=${encodeURIComponent(name)}&delete=${isDelete}`;
    axios({
      method: "get",
      url,
      responseType: "blob",
      headers: { Authorization: `Bearer ${getToken()}` },
    }).then((res) => {
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data]);
        this.saveAs(blob, decodeURIComponent(res.headers["download-filename"]));
      } else {
        this.printErrMsg(res.data);
      }
    });
  },
  resource(resource) {
    const url = `${baseURL}/common/download/resource?resource=${encodeURIComponent(resource)}`;
    axios({
      method: "get",
      url,
      responseType: "blob",
      headers: { Authorization: `Bearer ${getToken()}` },
    }).then((res) => {
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data]);
        this.saveAs(blob, decodeURIComponent(res.headers["download-filename"]));
      } else {
        this.printErrMsg(res.data);
      }
    });
  },
  zip(url, name) {
    const downloadUrl = baseURL + url;
    downloadLoadingInstance = createLoading("正在下载数据，请稍候");
    axios({
      method: "get",
      url: downloadUrl,
      responseType: "blob",
      headers: { Authorization: `Bearer ${getToken()}` },
    })
      .then((res) => {
        const isBlob = blobValidate(res.data);
        if (isBlob) {
          const blob = new Blob([res.data], { type: "application/zip" });
          this.saveAs(blob, name);
        } else {
          this.printErrMsg(res.data);
        }
        downloadLoadingInstance?.close();
      })
      .catch((error) => {
        console.error(error);
        message.error("下载文件出现错误，请联系管理员！");
        downloadLoadingInstance?.close();
      });
  },
  saveAs(text, name, opts) {
    saveAs(text, name, opts);
  },
  async printErrMsg(data) {
    const resText = await data.text();
    const rspObj = JSON.parse(resText);
    const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode.default;
    message.error(errMsg);
  },
};
