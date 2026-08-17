<template>
  <div :class="{ 'has-logo': showLogo }" class="sidebar-container">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <div class="sidebar-scrollbar">
      <nav class="legacy-side-menu" :class="{ collapsed: isCollapse }">
        <template v-for="node in menuNodes" :key="node.key">
          <button
            type="button"
            class="side-menu-item"
            :class="{
              active: isNodeActive(node),
              parent: node.children.length,
              opened: isOpen(node.key),
            }"
            @click="handleNodeClick(node)"
          >
            <component :is="resolveMenuIcon(node)" v-if="resolveMenuIcon(node)" class="side-menu-icon" />
            <svg-icon v-else-if="node.icon && node.icon !== '#'" :icon-class="node.icon" class="side-menu-icon" />
            <span v-if="!isCollapse" class="side-title">{{ node.title }}</span>
            <right-outlined v-if="node.children.length && !isCollapse" class="side-arrow" />
          </button>

          <div
            v-if="node.children.length && !isCollapse"
            v-show="isOpen(node.key)"
            class="side-submenu"
          >
            <template v-for="child in node.children" :key="child.key">
              <button
                type="button"
                class="side-menu-item side-menu-child"
                :class="{
                  active: isNodeActive(child),
                  parent: child.children.length,
                  opened: isOpen(child.key),
                }"
                @click="handleNodeClick(child)"
              >
                <component :is="resolveMenuIcon(child)" v-if="resolveMenuIcon(child)" class="side-menu-icon" />
                <svg-icon v-else-if="child.icon && child.icon !== '#'" :icon-class="child.icon" class="side-menu-icon" />
                <span class="side-title">{{ child.title }}</span>
                <right-outlined v-if="child.children.length" class="side-arrow" />
              </button>

              <div
                v-if="child.children.length"
                v-show="isOpen(child.key)"
                class="side-submenu side-submenu-nested"
              >
                <button
                  v-for="grandchild in child.children"
                  :key="grandchild.key"
                  type="button"
                  class="side-menu-item side-menu-grandchild"
                  :class="{ active: isNodeActive(grandchild) }"
                  @click="handleNodeClick(grandchild)"
                >
                  <svg-icon v-if="grandchild.icon && grandchild.icon !== '#'" :icon-class="grandchild.icon" class="side-menu-icon" />
                  <span class="side-title">{{ grandchild.title }}</span>
                </button>
              </div>
            </template>
          </div>
        </template>
      </nav>
    </div>
  </div>
</template>

<script setup>
import {
  AccountBookOutlined,
  FireOutlined,
  FolderOutlined,
  GiftOutlined,
  GlobalOutlined,
  IdcardOutlined,
  MoneyCollectOutlined,
  OrderedListOutlined,
  RightOutlined,
  ShopOutlined,
  UnorderedListOutlined,
  UserOutlined,
} from "@ant-design/icons-vue";
import Logo from "./Logo";
import variables from "@/assets/styles/variables.module.scss";
import { isExternal } from "@/utils/validate";
import { getNormalPath } from "@/utils/common";
import useAppStore from "@/store/modules/app";
import useSettingsStore from "@/store/modules/settings";
import usePermissionStore from "@/store/modules/permission";

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();

const sidebarRouters = computed(() => permissionStore.sidebarRouters || []);
const showLogo = computed(() => settingsStore.sidebarLogo && settingsStore.menuHeaderVisible);
const sideTheme = computed(() => settingsStore.sideTheme);
const theme = computed(() => settingsStore.theme);
const isCollapse = computed(() => !appStore.sidebar.opened);
const openKeys = ref([]);

const menuIcons = {
  用户管理: UserOutlined,
  权限管理: IdcardOutlined,
  文件管理: FolderOutlined,
  操作日志: UnorderedListOutlined,
  商品管理: ShopOutlined,
  会员管理: UserOutlined,
  订单管理: OrderedListOutlined,
  资金管理: MoneyCollectOutlined,
  余额管理: AccountBookOutlined,
  官网管理: GlobalOutlined,
  礼品管理: GiftOutlined,
  活动管理: FireOutlined,
};

const menuIconNames = {
  user: UserOutlined,
  validCode: IdcardOutlined,
  folder: FolderOutlined,
  log: UnorderedListOutlined,
  goods: ShopOutlined,
  shopping: ShopOutlined,
  order: OrderedListOutlined,
  money: MoneyCollectOutlined,
  account: AccountBookOutlined,
  international: GlobalOutlined,
  gift: GiftOutlined,
  fire: FireOutlined,
};

const getMenuBackground = computed(() => {
  if (settingsStore.isDark) {
    return "var(--sidebar-bg)";
  }
  return sideTheme.value === "theme-dark" ? variables.menuBg : variables.menuLightBg;
});

const getMenuTextColor = computed(() => {
  if (settingsStore.isDark) {
    return "var(--sidebar-text)";
  }
  return sideTheme.value === "theme-dark" ? variables.menuText : variables.menuLightText;
});

const getMenuMutedColor = computed(() => {
  if (settingsStore.isDark || sideTheme.value === "theme-dark") {
    return "rgba(255, 255, 255, 0.68)";
  }
  return "rgba(0, 0, 0, 0.65)";
});

const getMenuHoverBackground = computed(() => {
  if (settingsStore.isDark || sideTheme.value === "theme-dark") {
    return "rgba(255, 255, 255, 0.08)";
  }
  return "rgba(0, 0, 0, 0.03)";
});

const getMenuActiveBackground = computed(() => {
  if (settingsStore.isDark || sideTheme.value === "theme-dark") {
    return theme.value;
  }
  return "rgba(0, 0, 0, 0.04)";
});

const getMenuActiveTextColor = computed(() => {
  if (settingsStore.isDark || sideTheme.value === "theme-dark") {
    return "#ffffff";
  }
  return "rgba(0, 0, 0, 0.95)";
});

const activeMenu = computed(() => {
  const { meta, path } = route;
  return meta.activeMenu || path;
});

const menuState = computed(() => {
  const map = new Map();
  const items = buildMenuNodes(sidebarRouters.value, "", [], map);
  return { items, map };
});

const menuNodes = computed(() => menuState.value.items);

watch(
  () => [activeMenu.value, menuNodes.value, isCollapse.value],
  () => {
    if (isCollapse.value) {
      return;
    }
    const activeNode = findActiveNode(menuNodes.value);
    if (!activeNode) {
      return;
    }
    openKeys.value = Array.from(new Set([...openKeys.value, ...activeNode.parentKeys]));
  },
  { immediate: true, flush: "post" }
);

function buildMenuNodes(routes = [], basePath = "", parentKeys = [], map = new Map()) {
  return routes
    .filter((item) => !item.hidden)
    .map((item) => buildMenuNode(item, basePath, parentKeys, map))
    .filter(Boolean);
}

function buildMenuNode(item, basePath, parentKeys, map) {
  const children = (item.children || []).filter((child) => !child.hidden);
  const routePath = resolvePath(basePath, item.path);
  const onlyChild = children.length === 1 ? children[0] : null;

  if (onlyChild && !item.alwaysShow) {
    return buildMenuNode(
      {
        ...onlyChild,
        path: resolvePath(routePath, onlyChild.path),
        meta: {
          ...(onlyChild.meta || {}),
          icon: (onlyChild.meta && onlyChild.meta.icon) || (item.meta && item.meta.icon),
        },
      },
      "",
      parentKeys,
      map
    );
  }

  if (!item.meta) {
    return null;
  }

  const node = {
    key: routePath,
    title: item.meta.title || "",
    icon: item.meta.icon,
    query: item.query,
    parentKeys,
    children: buildMenuNodes(children, routePath, [...parentKeys, routePath], map),
  };

  map.set(node.key, node);
  return node;
}

function resolveMenuIcon(node) {
  if (node.parentKeys.length) {
    return null;
  }
  return menuIcons[node.title] || menuIconNames[node.icon] || null;
}

function resolvePath(basePath, routePath) {
  if (isExternal(routePath)) {
    return routePath;
  }
  if (isExternal(basePath)) {
    return basePath;
  }
  if (routePath && routePath.startsWith("/")) {
    return getNormalPath(routePath);
  }
  return getNormalPath(`${basePath}/${routePath || ""}`);
}

function isOpen(key) {
  return openKeys.value.includes(key);
}

function isNodeActive(node) {
  return activeMenu.value === node.key || activeMenu.value.startsWith(`${node.key}/`);
}

function toggleNode(node) {
  if (isOpen(node.key)) {
    const closedKeys = new Set([node.key, ...collectChildKeys(node)]);
    openKeys.value = openKeys.value.filter((key) => !closedKeys.has(key));
    return;
  }

  openKeys.value = Array.from(new Set([...openKeys.value, ...node.parentKeys, node.key]));
}

function collectChildKeys(node) {
  return node.children.flatMap((child) => [child.key, ...collectChildKeys(child)]);
}

function findActiveNode(nodes = []) {
  for (const node of nodes) {
    if (isNodeActive(node)) {
      const child = findActiveNode(node.children);
      return child || node;
    }
  }
  return null;
}

function handleNodeClick(node) {
  if (isCollapse.value) {
    appStore.openSideBar(false);
  }

  if (node.children.length) {
    toggleNode(node);
    return;
  }

  navigateNode(node);
}

function navigateNode(node) {
  if (isExternal(node.key)) {
    window.open(node.key, "_blank", "noopener");
    return;
  }

  if (node.query) {
    try {
      router.push({ path: node.key, query: JSON.parse(node.query) });
    } catch (error) {
      router.push({ path: node.key });
    }
    return;
  }

  router.push({ path: node.key });
}
</script>

<style lang="scss" scoped>
.sidebar-container {
  background-color: v-bind(getMenuBackground);
}

.sidebar-scrollbar {
  width: 100%;
  height: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: none;
  background-color: v-bind(getMenuBackground);

  &::-webkit-scrollbar {
    display: none;
  }
}

.legacy-side-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  min-height: 100%;
  padding: 4px 0;
  background: v-bind(getMenuBackground);
  color: v-bind(getMenuTextColor);
  font-family: var(--app-font-family);

  &.collapsed {
    .side-menu-item {
      justify-content: center;
      padding: 0;
      margin: 0 8px;
      width: calc(100% - 16px);
    }

    .side-menu-icon {
      margin-right: 0;
    }
  }
}

.side-menu-item {
  position: relative;
  display: flex;
  align-items: center;
  width: calc(100% - 24px);
  height: 40px;
  margin: 0 12px;
  padding: 0 34px 0 16px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: v-bind(getMenuMutedColor);
  cursor: pointer;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0;
  line-height: 40px;
  text-align: left;
  transition: background-color 0.2s ease, color 0.2s ease;

  &:hover {
    color: v-bind(getMenuActiveTextColor);
    background: v-bind(getMenuHoverBackground);
  }

  &.active {
    color: v-bind(getMenuActiveTextColor);
    background: v-bind(getMenuActiveBackground);
  }

  &.parent.active {
    background: transparent;
  }

  &.opened .side-arrow {
    transform: rotate(-90deg);
  }

  .side-menu-icon {
    flex: 0 0 auto;
    margin-right: 8px;
    font-size: 14px;
    color: inherit;
    line-height: 1;
  }
}

.side-title {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-arrow {
  position: absolute;
  top: 15px;
  right: 16px;
  flex: 0 0 auto;
  width: 10px;
  height: 10px;
  color: inherit;
  font-size: 10px;
  line-height: 1;
  transform: rotate(0deg);
  transition: transform 0.2s ease;
}

.side-submenu {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: transparent;
}

.side-menu-child {
  width: calc(100% - 34px);
  margin-right: 12px;
  margin-left: 22px;
  padding: 0 16px 0 32px;
}

.side-menu-grandchild {
  padding-left: 52px;
}
</style>
