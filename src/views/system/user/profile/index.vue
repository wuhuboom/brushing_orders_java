<template>
  <div class="app-container profile-page">
    <a-row :gutter="20">
      <a-col :xs="24" :lg="6">
        <a-card title="个人信息" class="profile-card">
          <div class="avatar-wrap">
            <userAvatar />
          </div>
          <ul class="profile-list">
            <li>
              <span><svg-icon icon-class="user" /> 用户名称</span>
              <strong>{{ state.user.userName || "-" }}</strong>
            </li>
            <li>
              <span><svg-icon icon-class="phone" /> 手机号码</span>
              <strong>{{ state.user.phonenumber || "-" }}</strong>
            </li>
            <li>
              <span><svg-icon icon-class="email" /> 用户邮箱</span>
              <strong>{{ state.user.email || "-" }}</strong>
            </li>
            <li>
              <span><svg-icon icon-class="tree" /> 所属组织</span>
              <strong v-if="state.user.dept">{{ state.user.dept.deptName }} / {{ state.postGroup }}</strong>
              <strong v-else>-</strong>
            </li>
            <li>
              <span><svg-icon icon-class="peoples" /> 所属角色</span>
              <strong>{{ state.roleGroup || "-" }}</strong>
            </li>
            <li>
              <span><svg-icon icon-class="date" /> 创建日期</span>
              <strong>{{ state.user.createTime || "-" }}</strong>
            </li>
          </ul>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="18">
        <a-card title="基本资料" class="profile-card">
          <a-tabs v-model:activeKey="selectedTab">
            <a-tab-pane key="userinfo" tab="基本资料">
              <userInfo :user="state.user" />
            </a-tab-pane>
            <a-tab-pane key="resetPwd" tab="修改密码">
              <resetPwd />
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup name="Profile">
import userAvatar from "./userAvatar";
import userInfo from "./userInfo";
import resetPwd from "./resetPwd";
import { getUserProfile } from "@/api/system/user";

const route = useRoute();
const selectedTab = ref("userinfo");
const state = reactive({
  user: {},
  roleGroup: "",
  postGroup: "",
});

function getUser() {
  getUserProfile().then((response) => {
    state.user = response.data;
    state.roleGroup = response.roleGroup;
    state.postGroup = response.postGroup;
  });
}

onMounted(() => {
  getUser();
});

watch(
  () => route.params?.activeTab,
  (activeTab) => {
    selectedTab.value = activeTab === "resetPwd" ? "resetPwd" : "userinfo";
  },
  { immediate: true }
);
</script>

<style scoped lang="scss">
.profile-page {
  .profile-card {
    height: 100%;
  }

  :deep(.ant-card-head-title) {
    font-weight: 600;
  }
}

.avatar-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.profile-list {
  padding: 0;
  margin: 0;
  list-style: none;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 42px;
    border-bottom: 1px solid #f0f0f0;
    color: rgba(0, 0, 0, 0.65);
    gap: 12px;
  }

  span {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    white-space: nowrap;
  }

  strong {
    color: rgba(0, 0, 0, 0.85);
    font-weight: 500;
    text-align: right;
    word-break: break-all;
  }
}
</style>
