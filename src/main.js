function initApp(Vue, App) {
  const app = Vue.createApp(App);

  // 延迟导入模块，确保 config 加载之后再初始化
  Promise.all([
    import("js-cookie"),
    import("element-plus"),
    import("./store"),
    import("./router"),
    import("./plugins"),
    import("./directive"),
    import("@/utils/request"),
    import("@/utils/dict"),
    import("@/api/system/config"),
    import("@/utils/ruoyi"),
    import("@/components/SvgIcon"),
    import("@/components/SvgIcon/svgicon"),
    import("virtual:svg-icons-register"),
    import("@/components/Pagination"),
    import("@/components/RightToolbar"),
    import("@/components/Editor"),
    import("@/components/FileUpload"),
    import("@/components/ImageUpload"),
    import("@/components/ImagePreview"),
    import("@/components/DictTag"),
    import("@/assets/styles/index.scss"),
    import("element-plus/dist/index.css"),
    import("element-plus/theme-chalk/dark/css-vars.css"),
    import("./permission"),
  ]).then(
    ([
      Cookies,
      ElementPlus,
      store,
      router,
      plugins,
      directive,
      request,
      dict,
      sysConfig,
      ruoyi,
      SvgIcon,
      elementIcons,
      ,
      Pagination,
      RightToolbar,
      Editor,
      FileUpload,
      ImageUpload,
      ImagePreview,
      DictTag,
    ]) => {
      // 全局挂载
      app.config.globalProperties.useDict = dict.useDict;
      app.config.globalProperties.download = request.download;
      app.config.globalProperties.parseTime = ruoyi.parseTime;
      app.config.globalProperties.resetForm = ruoyi.resetForm;
      app.config.globalProperties.handleTree = ruoyi.handleTree;
      app.config.globalProperties.addDateRange = ruoyi.addDateRange;
      app.config.globalProperties.getConfigKey = sysConfig.getConfigKey;
      app.config.globalProperties.selectDictLabel = ruoyi.selectDictLabel;
      app.config.globalProperties.selectDictLabels = ruoyi.selectDictLabels;

      // 全局组件
      app.component("DictTag", DictTag.default);
      app.component("Pagination", Pagination.default);
      app.component("FileUpload", FileUpload.default);
      app.component("ImageUpload", ImageUpload.default);
      app.component("ImagePreview", ImagePreview.default);
      app.component("RightToolbar", RightToolbar.default);
      app.component("Editor", Editor.default);
      app.component("svg-icon", SvgIcon.default);

      // 插件/路由/状态
      app.use(store.default);
      app.use(router.default);
      app.use(plugins.default);
      app.use(elementIcons.default);
      directive.default(app);

      // Element Plus
      app.use(ElementPlus.default, {
        size: Cookies.default.get("size") || "default",
      });

      app.mount("#app");
    }
  );
}

// 等待 config.js 加载完
(function waitForConfig() {
  if (window.APP_CONFIG) {
    Promise.all([import("vue"), import("./App")]).then(([Vue, App]) => {
      initApp(Vue, App.default);
    });
  } else {
    setTimeout(waitForConfig, 50);
  }
})();
