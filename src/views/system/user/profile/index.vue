<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6" :xs="24">
        <el-card class="box-card">
          <template v-slot:header>
            <div class="clearfix">
              <span>{{ t("profile.personalInfo") }}</span>
            </div>
          </template>
          <div>
            <div class="text-center">
              <userAvatar />
            </div>
            <ul class="list-group list-group-striped">
              <li class="list-group-item">
                <svg-icon icon-class="user" />
                {{ t("profile.userName") }}
                <div class="pull-right">{{ state.user.userName }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="phone" />
                {{ t("profile.phoneNumber") }}
                <div class="pull-right">{{ state.user.phonenumber }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="email" />
                {{ t("profile.email") }}
                <div class="pull-right">{{ state.user.email }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="peoples" />
                {{ t("profile.role") }}
                <div class="pull-right">{{ state.roleGroup }}</div>
              </li>
            </ul>
          </div>
        </el-card>
      </el-col>
      <el-col :span="18" :xs="24">
        <el-card>
          <template v-slot:header>
            <div class="clearfix">
              <span>{{ t("profile.basicInfo") }}</span>
            </div>
          </template>
          <el-tabs v-model="selectedTab">
            <el-tab-pane :label="t('profile.basicData')" name="userinfo">
              <userInfo :user="state.user" />
            </el-tab-pane>
            <el-tab-pane :label="t('profile.changePassword')" name="resetPwd">
              <resetPwd />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Profile">
import { ref, reactive, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import userAvatar from "./userAvatar";
import userInfo from "./userInfo";
import resetPwd from "./resetPwd";
import { getUserProfile } from "@/api/system/user";

const route = useRoute();
const { t } = useI18n();
const selectedTab = ref("userinfo");
const state = reactive({
  user: {},
  roleGroup: {},
  postGroup: {},
});

function getUser() {
  getUserProfile().then((response) => {
    state.user = response.data;
    state.roleGroup = response.roleGroup;
    state.postGroup = response.postGroup;
  });
}

onMounted(() => {
  const activeTab = route.params && route.params.activeTab;
  if (activeTab) {
    selectedTab.value = activeTab;
  }
  getUser();
});
</script>
