import { createApp, defineAsyncComponent } from "vue";
import App from "./App";
import store from "./store";
import router from "./router";
import plugins from "./plugins";
import directive from "./directive";
import { download } from "@/utils/request";
import { useDict } from "@/utils/dict";
import { getConfigKey } from "@/api/system/config";
import { addDateRange, handleTree, parseTime, resetForm, selectDictLabel, selectDictLabels } from "@/utils/common";
import SvgIcon from "@/components/SvgIcon";
import "@fontsource-variable/inter";
import "virtual:svg-icons-register";
import "@/assets/styles/index.scss";
import "ant-design-vue/dist/reset.css";
import "./permission";

window.APP_CONFIG = Object.assign(
  { baseApiUrl: import.meta.env.VITE_APP_BASE_API },
  window.APP_CONFIG || {}
);

const app = createApp(App);

app.config.globalProperties.useDict = useDict;
app.config.globalProperties.download = download;
app.config.globalProperties.parseTime = parseTime;
app.config.globalProperties.resetForm = resetForm;
app.config.globalProperties.handleTree = handleTree;
app.config.globalProperties.addDateRange = addDateRange;
app.config.globalProperties.getConfigKey = getConfigKey;
app.config.globalProperties.selectDictLabel = selectDictLabel;
app.config.globalProperties.selectDictLabels = selectDictLabels;

const lazyComponent = (loader) => defineAsyncComponent(loader);
app.component("DictTag", lazyComponent(() => import("@/components/DictTag")));
app.component("Pagination", lazyComponent(() => import("@/components/Pagination")));
app.component("FileUpload", lazyComponent(() => import("@/components/FileUpload")));
app.component("ImageUpload", lazyComponent(() => import("@/components/ImageUpload")));
app.component("ImagePreview", lazyComponent(() => import("@/components/ImagePreview")));
app.component("RightToolbar", lazyComponent(() => import("@/components/RightToolbar")));
app.component("AntProTable", lazyComponent(() => import("@/components/AntProTable")));
app.component("Editor", lazyComponent(() => import("@/components/Editor")));
app.component("svg-icon", SvgIcon);

app.use(store);
app.use(router);
app.use(plugins);
directive(app);

app.mount("#app");
