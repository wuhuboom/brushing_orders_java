<template>
  <nav class="top-nav-menu">
    <button
      v-for="item in topMenus"
      :key="item.path"
      type="button"
      class="top-nav-item"
      :class="{ active: item.path === activeMenu }"
      @click="handleMenuClick(item.path)"
    >
      <svg-icon
        v-if="item.meta && item.meta.icon && item.meta.icon !== '#'"
        :icon-class="item.meta.icon"
      />
      <span>{{ item.meta.title }}</span>
    </button>
  </nav>
</template>

<script setup>
import { isHttp } from "@/utils/validate";
import { getNormalPath } from "@/utils/ruoyi";
import useAppStore from "@/store/modules/app";
import usePermissionStore from "@/store/modules/permission";
import useSettingsStore from "@/store/modules/settings";

const appStore = useAppStore();
const permissionStore = usePermissionStore();
const settingsStore = useSettingsStore();
const route = useRoute();
const router = useRouter();

const activeMenu = ref("/index");
const currentIndex = ref(null);
const hiddenSidebarPaths = ["/index", "/user/profile"];
const homeMenu = { path: "/index", meta: { title: "首页", icon: "dashboard" } };

const routers = computed(() => permissionStore.topbarRouters || []);

const topMenus = computed(() => {
  const menus = [homeMenu];
  routers.value.forEach((menu) => {
    if (menu.hidden) {
      return;
    }
    if (menu.path === "/" && menu.children && menu.children[0]) {
      menus.push(normalizeTopMenu(menu.children[0], "/"));
      return;
    }
    menus.push(normalizeTopMenu(menu));
  });
  return menus;
});

const resolvedActiveMenu = computed(() => {
  const path = route.path;

  if (path === "/index") {
    return currentIndex.value || "/index";
  }

  const matched = topMenus.value.find((menu) => {
    if (menu.path === "/index") {
      return false;
    }
    return path === menu.path || path.startsWith(`${menu.path}/`);
  });

  if (matched && !hiddenSidebarPaths.includes(path)) {
    return matched.path;
  }

  return path;
});

watch(
  () => [
    resolvedActiveMenu.value,
    routers.value.length,
    settingsStore.layoutMode,
    settingsStore.splitMenus,
    settingsStore.menuVisible,
  ],
  ([key]) => {
    activeMenu.value = key;
    syncSidebarRoutes(key);
  },
  { immediate: true, flush: "post" }
);

function normalizeTopMenu(menu, basePath = "") {
  return {
    ...menu,
    path: resolvePath(basePath, menu.path),
  };
}

function resolvePath(basePath, routePath) {
  if (isHttp(routePath)) {
    return routePath;
  }
  if (isHttp(basePath)) {
    return basePath;
  }
  if (routePath && routePath.startsWith("/")) {
    return getNormalPath(routePath);
  }
  return getNormalPath(`${basePath}/${routePath || ""}`);
}

function findTopRoute(key) {
  return routers.value.find((item) => resolvePath("", item.path) === key);
}

function getSidebarRoutes(key) {
  const parent = findTopRoute(key);
  if (!parent || !parent.children) {
    return [];
  }

  return parent.children
    .filter((child) => !child.hidden)
    .map((child) => ({
      ...child,
      path: resolvePath(parent.path, child.path),
      parentPath: key,
    }));
}

function setSidebarRoutes(key) {
  if (!settingsStore.menuVisible || settingsStore.layoutMode === "top") {
    permissionStore.setSidebarRouters([]);
    appStore.toggleSideBarHide(true);
    return [];
  }

  if (!settingsStore.splitMenus) {
    const routes = permissionStore.defaultRoutes || [];
    permissionStore.setSidebarRouters(routes);
    appStore.toggleSideBarHide(routes.length === 0);
    if (routes.length > 0) appStore.openSideBar(false);
    return routes;
  }

  const routes = getSidebarRoutes(key);
  permissionStore.setSidebarRouters(routes);
  appStore.toggleSideBarHide(routes.length === 0);
  if (routes.length > 0) {
    appStore.toggleSideBarHide(false);
    appStore.openSideBar(false);
  }
  return routes;
}

function handleMenuClick(key) {
  currentIndex.value = key;
  activeMenu.value = key;

  if (isHttp(key)) {
    window.open(key, "_blank", "noopener");
    return;
  }

  const parent = findTopRoute(key);
  const targetRoutes = getSidebarRoutes(key);
  setSidebarRoutes(key);

  if (parent && targetRoutes.length > 0) {
    // The legacy shell only switches the sidebar group here. Content and the
    // current tab stay unchanged until the user chooses a concrete child page.
    return;
  }

  router.push({ path: key });
}

function syncSidebarRoutes(key) {
  if (shouldHideSidebar(key)) {
    permissionStore.setSidebarRouters([]);
    appStore.toggleSideBarHide(true);
    return;
  }

  setSidebarRoutes(key);
}

function shouldHideSidebar(key) {
  return hiddenSidebarPaths.some((path) => key === path || key.startsWith(`${path}/`));
}
</script>

<style lang="scss" scoped>
.top-nav-menu {
  display: flex;
  align-items: center;
  height: 44px;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.top-nav-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 44px;
  padding: 0 16px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--navbar-muted-text, var(--text-secondary));
  cursor: pointer;
  font-family: "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0;
  line-height: 44px;
  transition: background-color 0.2s ease, color 0.2s ease;

  &:hover {
    color: var(--navbar-text, var(--text-primary));
    background: var(--menu-hover);
  }

  &.active {
    color: var(--navbar-text, var(--text-primary));
    background: transparent;
  }

  .svg-icon {
    flex: 0 0 auto;
    font-size: 14px;
  }
}
</style>
