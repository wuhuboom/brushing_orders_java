import router, { notFoundRoute } from "./router";
import { message } from "ant-design-vue";
import NProgress from "nprogress";
import "nprogress/nprogress.css";
import { getToken } from "@/utils/auth";
import { isHttp, isPathMatch } from "@/utils/validate";
import { isRelogin } from "@/utils/request";
import useUserStore from "@/store/modules/user";
import useSettingsStore from "@/store/modules/settings";
import usePermissionStore from "@/store/modules/permission";
import { refreshActiveTimeZone } from "@/utils/timezone-helper";

NProgress.configure({ showSpinner: false });

const whiteList = ["/login", "/register"];

const isWhiteList = (path) => whiteList.some((pattern) => isPathMatch(pattern, path));

router.beforeEach((to, from, next) => {
  NProgress.start();
  if (getToken()) {
    if (to.meta.title) {
      useSettingsStore().setTitle(to.meta.title);
    }

    if (to.path === "/login") {
      next({ path: "/" });
      NProgress.done();
      return;
    }

    if (isWhiteList(to.path)) {
      next();
      return;
    }

    if (useUserStore().roles.length === 0) {
      isRelogin.show = true;
      useUserStore()
        .getInfo()
        .then(async () => {
          isRelogin.show = false;
          await refreshActiveTimeZone();
          usePermissionStore().generateRoutes().then((accessRoutes) => {
            accessRoutes.forEach((route) => {
              if (!isHttp(route.path)) {
                router.addRoute(route);
              }
            });
            if (!router.hasRoute("NotFound")) {
              router.addRoute(notFoundRoute);
            }
            // 按原始地址重新解析，避免启动阶段命中的 NotFound 路由名称被一并重放。
            next({ path: to.path, query: to.query, hash: to.hash, replace: true });
          });
        })
        .catch((error) => {
          useUserStore().logOut().then(() => {
            message.error(error);
            next({ path: "/" });
          });
        });
      return;
    }

    next();
    return;
  }

  if (isWhiteList(to.path)) {
    next();
  } else {
    next(`/login?redirect=${to.fullPath}`);
    NProgress.done();
  }
});

router.afterEach(() => {
  NProgress.done();
});
