<template>
  <!-- template 部分完全不变，与上次优化版相同 -->
  <div class="navbar">
    <hamburger
      id="hamburger-container"
      :is-active="appStore.sidebar.opened"
      class="hamburger-container"
      @toggleClick="toggleSideBar"
    />
    <breadcrumb
      v-if="!settingsStore.topNav"
      id="breadcrumb-container"
      class="breadcrumb-container"
    />
    <top-nav
      v-if="settingsStore.topNav"
      id="topmenu-container"
      class="topmenu-container"
    />

    <div class="right-menu">
      <template v-if="appStore.device !== 'mobile'">
        <el-tooltip
          :content="t('navbar.orders')"
          effect="dark"
          placement="bottom"
        >
          <el-badge
            :value="countdata.orderCount"
            class="badge"
            :offset="[10, 10]"
          >
            <el-button @click="handleOrderClick">{{
              t("navbar.orders")
            }}</el-button>
          </el-badge>
        </el-tooltip>

        <el-tooltip
          :content="t('navbar.withdrawals')"
          effect="dark"
          placement="bottom"
        >
          <el-badge
            :value="countdata.withdrawalCount"
            class="badge"
            :offset="[10, 10]"
          >
            <el-button @click="handleWithdrawClick">{{
              t("navbar.withdrawals")
            }}</el-button>
          </el-badge>
        </el-tooltip>

        <div class="right-menu-item tz-display">
          <span>{{ t("navbar.timezone") }}: {{ activeTz }} / {{ tzNow }}</span>
        </div>

        <screenfull id="screenfull" class="right-menu-item hover-effect" />

        <el-tooltip
          :content="t('navbar.theme')"
          effect="dark"
          placement="bottom"
        >
          <div
            class="right-menu-item hover-effect theme-switch-wrapper"
            @click="toggleTheme"
          >
            <svg-icon v-if="settingsStore.isDark" icon-class="sunny" />
            <svg-icon v-if="!settingsStore.isDark" icon-class="moon" />
          </div>
        </el-tooltip>

        <el-tooltip
          :content="t('navbar.size')"
          effect="dark"
          placement="bottom"
        >
          <size-select id="size-select" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-dropdown
          :key="currentLocale"
          class="right-menu-item hover-effect language-switch"
          trigger="click"
          @command="handleLanguageChange"
        >
          <div class="language-trigger">
            <span>{{ currentLanguageShortLabel }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="option in languageOptions"
                :key="option.value"
                :command="option.value"
                :disabled="option.value === currentLocale"
              >
                {{ option.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>

      <el-dropdown
        @command="handleCommand"
        class="avatar-container right-menu-item hover-effect"
        trigger="hover"
      >
        <div class="avatar-wrapper">
          <img :src="userStore.avatar" class="user-avatar" />
          <span class="user-nickname">{{ userStore.nickName }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/user/profile">
              <el-dropdown-item>{{ t("navbar.profile") }}</el-dropdown-item>
            </router-link>
            <el-dropdown-item divided command="logout">
              <span>{{ t("navbar.logout") }}</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <div
        v-if="settingsStore.showSettings"
        class="right-menu-item hover-effect setting"
        @click="setLayout"
      >
        <svg-icon icon-class="more-up" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus";
import { nextTick } from "vue";
import Breadcrumb from "@/components/Breadcrumb";
import TopNav from "@/components/TopNav";
import Hamburger from "@/components/Hamburger";
import Screenfull from "@/components/Screenfull";
import SizeSelect from "@/components/SizeSelect";
import useAppStore from "@/store/modules/app";
import useUserStore from "@/store/modules/user";
import useSettingsStore from "@/store/modules/settings";
import useTagsViewStore from "@/store/modules/tagsView";
import { countStatus } from "@/api/member/orderInfo";
import { useRouter, useRoute } from "vue-router";
import { computed, ref, onMounted, onBeforeUnmount } from "vue";
import { useI18n } from "vue-i18n";
import { localeOptions, changeLocale } from "@/locales";
import {
  refreshActiveTimeZone,
  getActiveTimeZone,
} from "@/utils/timezone-helper";
import { resolveMenuTitle } from "@/utils/locale-title";
import { setLanguage } from "@/api/system/zone";

const router = useRouter();
const route = useRoute();
const emits = defineEmits(["setLayout"]);

const { t, locale } = useI18n();
const languageOptions = localeOptions;
const currentLocale = computed(() => locale.value);
const currentLanguageShortLabel = computed(() => {
  return (
    languageOptions.find((item) => item.value === currentLocale.value)
      ?.shortLabel ?? currentLocale.value
  );
});

const activeTz = ref(getActiveTimeZone());
const tzNow = ref("--:--:--");
const countdata = ref({});

let timer = null;
let countInterval = null;

// 时钟 tick 函数（支持 i18n locale）
function tick() {
  const tz = getActiveTimeZone();
  activeTz.value = tz;

  const fmt = new Intl.DateTimeFormat(
    locale.value === "en" ? "en-US" : "zh-CN",
    {
      timeZone: tz,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }
  );
  tzNow.value = fmt.format(new Date());
}

// 计数器初始化（加错误处理）
async function init() {
  try {
    const res = await countStatus();
    countdata.value = res.data;
  } catch (error) {
    console.warn("Failed to fetch count status:", error);
  }
}

// 生命周期
onMounted(async () => {
  await refreshActiveTimeZone();
  tick();
  timer = setInterval(tick, 1000);
  countInterval = setInterval(init, 5000);
  await init(); // 初始加载
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
  if (countInterval) clearInterval(countInterval);
});

// UI 交互函数
function toggleSideBar() {
  appStore.toggleSideBar();
}

function handleOrderClick() {
  router.push("/money/orderInfo");
}

function handleWithdrawClick() {
  router.push("/money/withdrawal");
}

function handleLanguageChange(value) {
  if (value !== locale.value) {
    const nextLocale = changeLocale(value);
    locale.value = nextLocale;
    nextTick(() => {
      tagsViewStore.refreshLocaleTitles();
      const localizedTitle = resolveMenuTitle(route.meta, t, locale);
      if (localizedTitle) {
        settingsStore.setTitle(localizedTitle);
      }
    });
    console.log(value);
    setLanguage(value).then((res) => {});
  }
}

function toggleTheme() {
  settingsStore.toggleTheme();
}

function setLayout() {
  emits("setLayout");
}

function handleCommand(command) {
  switch (command) {
    case "setLayout":
      setLayout();
      break;
    case "logout":
      logout();
      break;
    default:
      break;
  }
}

function logout() {
  ElMessageBox.confirm(t("navbar.logoutConfirm"), t("navbar.logout"), {
    confirmButtonText: t("common.confirm"),
    cancelButtonText: t("common.cancel"),
    type: "warning",
  })
    .then(() => {
      userStore.logOut().then(() => {
        location.href = "/index";
      });
    })
    .catch(() => {});
}

// 实例化 store（移到最后，避免依赖问题）
const appStore = useAppStore();
const userStore = useUserStore();
const settingsStore = useSettingsStore();
const tagsViewStore = useTagsViewStore();
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: var(--navbar-bg);
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background 0.3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;
    display: flex;
    align-items: center;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: flex; /* 改：统一用 flex，确保所有子项居中 */
      align-items: center; /* 新增：垂直居中 */
      justify-content: center; /* 新增：水平居中（可选，防偏移） */
      height: 100%; /* 新增：占满高度 */
      padding: 0 8px;
      font-size: 18px;
      color: #5a5e66;

      &.hover-effect {
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: rgba(0, 0, 0, 0.025);
        }
      }

      &.theme-switch-wrapper {
        svg {
          transition: transform 0.3s;

          &:hover {
            transform: scale(1.15);
          }
        }
      }
    }

    .language-switch {
      .language-trigger {
        display: flex;
        align-items: center;
        height: 100%; /* 新增：占满 item 高度 */

        span {
          /* 新增：针对 span 文本 */
          line-height: 50px; /* 新增：匹配 navbar 高度，确保基线居中 */
          font-weight: 600;
          font-size: 15px;
        }
      }
    }

    .avatar-container {
      margin-right: 0px;
      padding-right: 0px;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        justify-content: center; /* 新增：水平居中 */
        height: 100%;
        position: relative;

        .user-avatar {
          cursor: pointer;
          width: 30px;
          height: 30px;
          border-radius: 50%;
          flex-shrink: 0;
        }

        .user-nickname {
          margin-left: 5px;
          font-size: 14px;
          font-weight: bold;
        }

        i {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 25px;
          font-size: 12px;
        }
      }
    }

    .tz-display {
      /* 新增：时区显示统一规则 */
      display: flex;
      align-items: center;
      height: 100%;
      font-weight: 600;
      font-size: 15px;
      white-space: nowrap;

      span {
        line-height: 50px; /* 新增：文本基线居中 */
      }
    }
  }
}
</style>
<style scoped>
.badge {
  margin-right: 30px;
}
</style>
