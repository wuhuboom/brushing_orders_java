import router from "./router";
import { ElMessage } from "element-plus";
import NProgress from "nprogress";
import "nprogress/nprogress.css";
import { getToken } from "@/utils/auth";
import { isHttp, isPathMatch } from "@/utils/validate";
import { isRelogin } from "@/utils/request";
import useUserStore from "@/store/modules/user";
import useSettingsStore from "@/store/modules/settings";
import usePermissionStore from "@/store/modules/permission";
import i18n from "@/locales";

NProgress.configure({ showSpinner: false });

const whiteList = ["/login", "/register"];

const resolveRouteTitle = (meta) => {
  if (!meta) return "";
  if (meta.titleKey) return i18n.global.t(meta.titleKey);
  return meta.title || "";
};

const applyRouteLocale = (targets) => {
  if (!targets) return;
  const records = Array.isArray(targets) ? targets : [targets];
  records.forEach((record) => {
    if (record && record.meta) {
      const translated = resolveRouteTitle(record.meta);
      if (translated) {
        record.meta.title = translated;
      }
    }
    if (record && record.children && record.children.length) {
      applyRouteLocale(record.children);
    }
  });
};

const isWhiteList = (path) => {
  return whiteList.some((pattern) => isPathMatch(pattern, path));
};

router.beforeEach((to, from, next) => {
  NProgress.start();
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title);
    /* has token*/
    if (to.path === "/login") {
      next({ path: "/" });
      NProgress.done();
    } else if (isWhiteList(to.path)) {
      next();
    } else {
      if (useUserStore().roles.length === 0) {
        isRelogin.show = true;
        // 判断当前用户是否已拉取完user_info信息
        useUserStore()
          .getInfo()
          .then(() => {
            isRelogin.show = false;
            usePermissionStore()
              .generateRoutes()
              .then((accessRoutes) => {
                // 根据roles权限生成可访问的路由表
                accessRoutes.forEach((route) => {
                  if (!isHttp(route.path)) {
                    router.addRoute(route); // 动态添加可访问路由表
                  }
                });
                next({ ...to, replace: true }); // hack方法 确保addRoutes已完成
              });
          })
          .catch((err) => {
            useUserStore()
              .logOut()
              .then(() => {
                ElMessage.error(err);
                next({ path: "/" });
              });
          });
      } else {
        next();
      }
    }
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      applyRouteLocale(to);
      applyRouteLocale(to.matched);
      const translatedTitle = resolveRouteTitle(to.meta);
      translatedTitle && useSettingsStore().setTitle(translatedTitle);
      // 在免登录白名单，直接进入
      next();
    } else {
      next(`/login?redirect=${to.fullPath}`); // 否则全部重定向到登录页
      NProgress.done();
    }
  }
});

router.afterEach(() => {
  NProgress.done();
});
