import { h, ref } from "vue";
import { Input, message, Modal, notification } from "ant-design-vue";

let loadingKey = null;

function modalPromise(options) {
  return new Promise((resolve, reject) => {
    Modal.confirm({
      title: "系统提示",
      okText: "确定",
      cancelText: "取消",
      ...options,
      onOk: () => resolve(),
      onCancel: () => reject(new Error("cancel")),
    });
  });
}

function alert(type, content) {
  const method = Modal[type] || Modal.info;
  return method({
    title: "系统提示",
    content,
    okText: "确定",
  });
}

function notify(type, content) {
  const payload = typeof content === "object" && content !== null
    ? content
    : { message: "系统提示", description: content };
  return notification[type](payload);
}

function getPromptError(value, options) {
  if (options.inputPattern && !options.inputPattern.test(value)) {
    return options.inputErrorMessage || "输入内容格式不正确";
  }
  if (typeof options.inputValidator === "function") {
    const result = options.inputValidator(value);
    if (result) return result;
  }
  return "";
}

export default {
  msg(content) {
    message.info(content);
  },
  msgError(content) {
    message.error(content);
  },
  msgSuccess(content) {
    message.success(content);
  },
  msgWarning(content) {
    message.warning(content);
  },
  alert(content) {
    return alert("info", content);
  },
  alertError(content) {
    return alert("error", content);
  },
  alertSuccess(content) {
    return alert("success", content);
  },
  alertWarning(content) {
    return alert("warning", content);
  },
  notify(content) {
    return notify("info", content);
  },
  notifyError(content) {
    return notify("error", content);
  },
  notifySuccess(content) {
    return notify("success", content);
  },
  notifyWarning(content) {
    return notify("warning", content);
  },
  confirm(content) {
    return modalPromise({
      content,
      title: "系统提示",
      okText: "确定",
      cancelText: "取消",
    });
  },
  prompt(content, title = "系统提示", options = {}) {
    const value = ref(options.inputValue || "");
    return new Promise((resolve, reject) => {
      Modal.confirm({
        title,
        okText: options.confirmButtonText || "确定",
        cancelText: options.cancelButtonText || "取消",
        content: () => h("div", [
          h("div", { style: "margin-bottom: 12px;" }, content),
          h(Input, {
            value: value.value,
            autofocus: true,
            "onUpdate:value": (nextValue) => {
              value.value = nextValue;
            },
            onPressEnter: () => {},
          }),
        ]),
        onOk: () => {
          const error = getPromptError(value.value, options);
          if (error) {
            message.warning(error);
            return Promise.reject(error);
          }
          resolve({ value: value.value });
          return undefined;
        },
        onCancel: () => reject(new Error("cancel")),
      });
    });
  },
  loading(content) {
    loadingKey = `global-loading-${Date.now()}`;
    message.loading({ content, key: loadingKey, duration: 0 });
  },
  closeLoading() {
    if (loadingKey) {
      message.destroy(loadingKey);
      loadingKey = null;
    }
  },
};
