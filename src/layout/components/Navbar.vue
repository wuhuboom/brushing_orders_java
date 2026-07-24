<template>
  <div class="navbar">
    <hamburger
      v-if="!settingsStore.topNav"
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
      <div v-if="appStore.device !== 'mobile'" class="quick-actions">
        <span class="quick-badge">
          <a class="quick-brand">MOIDA</a>
        </span>
        <span class="quick-spacer" aria-hidden="true"></span>
        <span class="quick-badge">
          <router-link class="quick-link" to="/members/order/orderinfo">订单</router-link>
          <sup
            v-if="quickStats.totalOrders > 0"
            class="quick-count"
            :class="{ 'multiple-words': isMultipleCount(quickStats.totalOrders) }"
          >
            {{ formatCount(quickStats.totalOrders) }}
          </sup>
        </span>
        <span class="quick-spacer" aria-hidden="true"></span>
        <span class="quick-badge">
          <router-link class="quick-link" to="/members/transaction/withdrawal">提现</router-link>
          <sup
            v-if="quickStats.totalWithdrawals > 0"
            class="quick-count"
            :class="{ 'multiple-words': isMultipleCount(quickStats.totalWithdrawals) }"
          >
            {{ formatCount(quickStats.totalWithdrawals) }}
          </sup>
        </span>
        <span class="quick-spacer" aria-hidden="true"></span>
      </div>

      <div class="user-actions">
        <div class="account-menu" @click.stop>
          <button
            class="account-trigger"
            :class="{ open: userMenuOpen }"
            type="button"
            @click="toggleUserMenu"
          >
            <span class="account-avatar">
              <img :src="headerAvatar" alt="" />
            </span>
            <span class="account-name-wrap">
              <span class="account-name">{{ accountName }}</span>
            </span>
          </button>
          <div v-show="userMenuOpen" class="account-dropdown">
            <button class="account-dropdown-item" type="button" @click="openGoogleAuth">
              <span class="account-symbol google-symbol">G</span>
              <span>谷歌验证器</span>
            </button>
            <button class="account-dropdown-item" type="button" @click="openResetPwd">
              <svg-icon icon-class="password" />
              <span>修改密码</span>
            </button>
            <button class="account-dropdown-item" type="button" @click="logout">
              <span class="account-symbol logout-symbol"></span>
              <span>退出登录</span>
            </button>
          </div>
        </div>

        <div class="language-menu" @click.stop>
          <button
            class="language-trigger"
            :class="{ open: languageMenuOpen }"
            type="button"
            title="语言"
            aria-label="语言"
            @click="toggleLanguageMenu"
          >
            <svg class="language-icon" viewBox="0 0 24 24" focusable="false" aria-hidden="true">
              <path d="M0 0h24v24H0z" fill="none" />
              <path
                d="M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z"
              />
            </svg>
          </button>
          <div v-show="languageMenuOpen" class="language-dropdown">
            <button class="language-item active" type="button" @click="selectLanguage('zh-CN')">
              <span>简体中文</span><span>✓</span>
            </button>
            <button class="language-item" type="button" disabled title="桌面后台暂未配置英文语言包">
              English
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Modal } from "ant-design-vue";
import Breadcrumb from "@/components/Breadcrumb";
import TopNav from "@/components/TopNav";
import Hamburger from "@/components/Hamburger";
import useAppStore from "@/store/modules/app";
import useUserStore from "@/store/modules/user";
import useSettingsStore from "@/store/modules/settings";
import { getHeaderStats } from "@/api/index";

const appStore = useAppStore();
const userStore = useUserStore();
const settingsStore = useSettingsStore();
const router = useRouter();
const route = useRoute();
const userMenuOpen = ref(false);
const languageMenuOpen = ref(false);
const quickStats = reactive({ totalOrders: 0, totalWithdrawals: 0 });
const headerAvatar = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAhsSURBVHhe7Vp9jFxVFR8LBoiBmBjRYAHdeffcmUWpsAqoaE27++557+0HaCvSRJqIUk3QGP8R/3FjFBDUBP4wBoOSSPygSdvtvnvfx8zuTrfbL/nQRhIjGP+iEUQjgSBgt9tnzn0z03l33szOpjWs+n7JyZudeefcc3/345x77pZKBQoUKFCgQIECBQoUKFCgQIEC/yOwcL4MTnQLOPF3wQl/BRgsMFRPAqrjINTvQMijIEJVunosfpuF9WuYG3+QubIt1ckD1/DRmctMw+sZIyMPvRXc+Hbu1A6CE56sThxMqpOHEnpWxg8klfGFpOItpM/xRlKZWExKgMGN9CV34oQ7UVuGp44mFqr7zUbWKwBnt4Jb+3118rDuLPUBUK0qpHgj9+YTIAUnbEt16kjCbHmf2dB6BBPyq3pkJw50dXA1KYGjPsbdegIYZH6gqcOE/J7Z2HqDZftfJl+5GyeAsquDWaE+Bgk4zed/OwHcUR/ibv00d2s5nU2FlkJ1YjGpTi7pdU99Jal48+keQBtePgGHEyb8b5uNrh8kb2FCPk4bXK+Rp46DE77OMZgBob5u2WqclnxZhB8BR22xMNhG62dXymCWAGKHCf9RC/ffQAok3J3/qIXBsOmKiYotR7g3/3nA4BsM5X1MqB8xoR4GVI8ABj9hKB9gIvwmc8NbmTe/qbS5cb5pYzVw4U9WJ5a6On2m8wdpYw+5CLmpm0G6droNkOiI4Nba06Y6eYSWxZJpwwRDFb7/08fTEKRlKZWJ5rP1PTlJ7bvxM9yp3Ts0uvsK01YvMJRROvqm31LbBQwiUycX5shnpblhNCWdFWrBtGEChNyb71yOOKFej8NTRxLuzv0DxOznTHsm+GjtMhDqDQrdpj0ilLvxy8OufLeplwvTQD+h+MqEnDdtmABUewYmoC0yqXhzevQslNtNm53gGG7vZV9v3rb/Y1OnJ3TczzGkxQkyyRFNYSbkAdOGiZSAJT266TKiUaGllCZb/WYdxXOG6m9XTu17u2m3BUB1P3XU1CW7tNMDBltNnZ4AIVe6DbVEnmYol0GoUyQVr7ECtl83bZgAVPWrPvWUzi4ZqpNMqFdAqJdAyJe1nfFGa53mtNmMQI66zbTbAhNyVu/whh4RzFC++r4t+99l6vQEOOqWvCigHRT+Q2V7v1W2Iy1Vr8aY57/HtGECHOVUxxs3lUV4Ldsqh8ihyta97xga23Mp2QEndgDDX9DaN9tNCdA5yE9Nuy0wlE9RPm/q0RICVM+Wpqc3mDo9UfHkSK88wEL1HfP9cwnA4Ad5a7m518yZ7xM2b26cD0L9udlZQ69BM+6wqdMXb2YmyEX4XnDCZW7sQ7ojws/tCJ1eGaq/pD4bBFCUQjVY+GvhzSSgVJreAEK+aKayKQHykPk2wcLgkjwd7bPeF6Rv6vTFf4oAPnnoYubJIeZFm5jtX8fsyBD/Oi78TzJUfzeTsX4E8MmZiwHVX3sSIKQydfriXBJQ8eIR7tbu4RgeAgyfBwxOnskm86Q7kVmNgI3bHrsIhDqRuwQGzFMyOBcE0C7P3dqvuUfpMhUk6NRVS2O+ri9kM8qMrJGA0nSygQn5TBpBTD2dQzxpqvTF2RJgYfBOcKI/UgWpb1K1BulLQJpnHEsTnqweDQAI/zkLgwtMnZ44WwKY7f98+KZjuUdSGqUzB6GDuWK2OyABual2swy2TLmHqdMTZ0NA2Y4uZ6iW8w4laaISHGcof8gcdSfDYCdDf2f6TD9zIXdRdmjW71YnQN6dnwqnCRwT8lZTpyfOhgDLVjtodE0n0tpc8MtBMjJwKKYPHgYJDIOb81LhtG3KBWYHzwX6E6DuNd/vBNjyW10jQQcor56Uxey15vsmLHzwAkC5ZgLoqAuoXsuPIoFeejC6b7ADUUVXeupdO7ImAOUD5vudAKG+bxKgp7PwT1OWZ75vwsK9Ze7E/1zrEiBQvM/bB7S+rnLHLwwyCCVmR5va4SrjRO98vAVAeY9JgB4BCkdjM58w3+/E0OjsFeDGv8091AxAAMPA7UWAri2QXSd6DTC6m5KxjduOXGTa0LAw2MiE/Jc5CmktoLYCGNxFp0C6JaLn0Fh8aUuX2erObgKaI4DhE2URXpVpbPvu8ypOHcCN7uJu/CKRnBs9BiCAwIR/NK0Ldtug72iJ6D2KBlfI5wDVbyhRooEl+yDUHzpOV92JhS5XkZNCrjBUr1YmFlcs9L/WdsCV1+cfaam6s5CACE4xVE+DUIu6QVTPAgbL5JS57jtlYAJo9rr1Uzr+59jp7Actc7KbXpGRNPRApYaEephqcvlMNoujTqzrdnQL0/Zg++7zQMine+3IpKfr7+0G5zJXVvRb+ne23UEJIDDbv43a5/p4nO9/P9FGuKM+oFPXVYzQyDGUX+l0wMLATh2gUeitmxEnSIanKHlSe0CoY2m7Z35fCwEEy57dAe7cK5SGm3vZatI2AmL2C9ybX9G5fGuktDGa3pS3h3oGdC6BTt2KN/8G6eZVl1IJ0jVJN7VEmBM9SjPIQvXg8M2Pt0vvJNUpupSRT5jt9AMXM5x79d3cjU+3L0jJl3YfTH9SnzJGAGc/DG79Z+DEfwIMXtdTn8jQhMTk+DJDeUdGqQnYsv9q8OqPgBM/T+/rVLcjDU5DU/QSuDUf3AhbelThrUwsnqATXksq440TTMh92RYGA+0L4NamwYkagMELaV6wkF6Pte8lmj6NL2QJaIE2xvJUdDn9zwAlSnSdRLc9dL7feMNj+eGkCQuPXcLc+Hpww88wDO5gItxlOdEO5s19vDOCdOLKzY0LM7KzceGaDjQ9QMUTusmiKjFD/7OAwe2WUF9ijvoiZbGWrcZMnQIFChQoUKBAgQIFChQo8P+AfwPyhEEAe4u/9QAAAABJRU5ErkJggg==";
const accountName = computed(() => userStore.name || "-");

function toggleSideBar() {
  appStore.toggleSideBar();
}

function toggleUserMenu() {
  languageMenuOpen.value = false;
  userMenuOpen.value = !userMenuOpen.value;
}

function toggleLanguageMenu() {
  userMenuOpen.value = false;
  languageMenuOpen.value = !languageMenuOpen.value;
}

function closeUserMenu() {
  userMenuOpen.value = false;
  languageMenuOpen.value = false;
}

function handleBodyClick() {
  closeUserMenu();
}

function openGoogleAuth() {
  closeUserMenu();
  router.push({ name: "Profile", params: { activeTab: "userinfo" } });
}

function openResetPwd() {
  closeUserMenu();
  router.push({ name: "Profile", params: { activeTab: "resetPwd" } });
}

function selectLanguage() {
  languageMenuOpen.value = false;
}

function formatCount(value) {
  const count = Number(value || 0);
  return count > 9999 ? "9999+" : String(count);
}

function isMultipleCount(value) {
  return formatCount(value).length > 2;
}

async function refreshQuickStats() {
  try {
    const response = await getHeaderStats();
    quickStats.totalOrders = Number(response.data?.totalOrders || 0);
    quickStats.totalWithdrawals = Number(response.data?.totalWithdrawals || 0);
  } catch (_) {}
}

function logout() {
  closeUserMenu();
  Modal.confirm({
    title: "提示",
    content: "确定注销并退出系统吗？",
    okText: "确定",
    cancelText: "取消",
    onOk() {
      return userStore.logOut().then(() => {
        location.href = "/index";
      });
    },
  });
}

onMounted(() => {
  document.addEventListener("click", handleBodyClick);
  window.addEventListener("focus", refreshQuickStats);
});

watch(() => route.fullPath, refreshQuickStats, { immediate: true });

onBeforeUnmount(() => {
  document.removeEventListener("click", handleBodyClick);
  window.removeEventListener("focus", refreshQuickStats);
});
</script>

<style lang="scss" scoped>
.navbar {
  height: 56px;
  overflow: visible;
  position: relative;
  z-index: 810;
  background: var(--navbar-bg);
  border-bottom: 1px solid var(--border-color);
  box-shadow: none;

  .hamburger-container {
    line-height: 52px;
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
    left: 140px;
    top: 6px;
    right: 360px;
  }

  .right-menu {
    float: right;
    height: 100%;
    display: flex;
    align-items: center;
    padding-right: 0;
    color: rgba(0, 0, 0, 0.88);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
    font-size: 14px;
    line-height: 56px;

    &:focus {
      outline: none;
    }
  }
}

.quick-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  padding: 0 2px;
  border-radius: 6px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 16px;
  line-height: 56px;
}

.quick-badge {
  position: relative;
  display: block;
  box-sizing: border-box;
  height: 26px;
  padding: 6px;
  border-radius: 6px;
  color: rgba(0, 0, 0, 0.88);
  font-size: 14px;
  line-height: 14px;
}

.quick-brand {
  color: #1677ff;
  font-size: 14px;
  font-weight: 700;
  line-height: 14px;
}

.quick-link {
  color: #1677ff;
  font-size: 14px;
  font-weight: 400;
  line-height: 14px;

  &:hover {
    color: #0958d9;
  }
}

.quick-spacer {
  display: flex;
  align-items: center;
  flex: 0 0 12px;
  box-sizing: border-box;
  width: 12px;
  height: 12px;
  margin: 0 12px;
  padding: 6px;
  border-radius: 6px;
  gap: 8px;
}

.quick-count {
  position: absolute;
  top: 0;
  right: 0;
  display: block;
  min-width: 20px;
  height: 20px;
  padding: 0;
  border-radius: 10px;
  background: #ff4d4f;
  color: #ffffff;
  font-size: 12px;
  font-weight: 400;
  line-height: 20px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  transform: translate(50%, -50%);
  transform-origin: 100% 0;
  box-shadow: 0 0 0 1px #ffffff;

  &.multiple-words {
    padding: 0 8px;
  }
}

.user-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  padding: 0 16px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
  line-height: 56px;
}

.account-menu {
  position: relative;
  display: flex;
  align-items: center;
  height: 44px;
}

.language-menu {
  position: relative;
  display: flex;
  align-items: center;
  width: 26px;
  height: 26px;
}

.language-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 4px;
  border: 0;
  outline: none;
  background: transparent;
  color: rgba(0, 0, 0, 0.45);
  cursor: pointer;
  font-size: 18px;
  line-height: 56px;
  box-shadow: none;

  &:hover,
  &.open {
    color: var(--primary-color);
    background: var(--menu-hover);
  }

}

.language-icon {
  display: inline-block;
  width: 1em;
  height: 1em;
  overflow: hidden;
  fill: currentColor;
  vertical-align: -0.125em;
}

.language-dropdown {
  position: absolute;
  top: 44px;
  right: 8px;
  z-index: 850;
  width: 136px;
  padding: 6px 0;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--card-bg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.language-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 36px;
  padding: 0 14px;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;

  &:hover:not(:disabled) {
    background: var(--menu-hover);
  }

  &.active {
    color: var(--primary-color);
  }

  &:disabled {
    color: var(--text-secondary);
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.account-trigger {
  display: inline-flex;
  align-items: center;
  height: 44px;
  padding: 8px;
  border: 0;
  border-radius: 6px;
  outline: none;
  background: transparent;
  color: rgba(0, 0, 0, 0.45);
  cursor: pointer;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
  font-size: 14px;
  line-height: 44px;
  box-shadow: none;

  &:hover,
  &.open {
    background: var(--menu-hover);
  }

}

.account-avatar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 28px;
  box-sizing: border-box;
  width: 28px;
  height: 28px;
  overflow: hidden;
  border: 1px solid transparent;
  border-radius: 50%;
  color: #ffffff;
  font-size: 18px;
  line-height: 28.2857px;

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.account-name-wrap {
  display: block;
  height: 44px;
  max-width: 86px;
  overflow: hidden;
  margin-left: 8px;
  line-height: 44px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-name {
  display: inline-flex;
  align-items: center;
  overflow: visible;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
  font-weight: 400;
  line-height: 0;
  vertical-align: -0.125em;
}

.account-dropdown {
  position: absolute;
  top: 44px;
  right: 0;
  z-index: 850;
  width: 152px;
  padding: 8px 0;
  border: 1px solid var(--border-color);
  border-radius: 2px;
  background: var(--card-bg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.account-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 40px;
  padding: 0 16px;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  font-size: 14px;
  line-height: 40px;
  text-align: left;

  &:hover {
    background: var(--menu-hover);
  }

  .svg-icon {
    flex: 0 0 auto;
    margin-right: 0;
    color: var(--text-secondary);
    font-size: 14px;
  }
}

.account-symbol {
  position: relative;
  flex: 0 0 14px;
  width: 14px;
  height: 14px;
  color: var(--text-primary);
}

.google-symbol {
  font-family: Arial, sans-serif;
  font-size: 14px;
  font-weight: 700;
  line-height: 14px;
  text-align: center;
}

.logout-symbol {
  border: 1.5px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;

  &::after {
    content: "";
    position: absolute;
    right: -1px;
    top: 0;
    width: 5px;
    height: 5px;
    border-top: 1.5px solid currentColor;
    border-right: 1.5px solid currentColor;
    transform: rotate(45deg);
  }
}
</style>
