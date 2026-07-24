<template>
  <div class="profile-form">
    <a-form ref="userRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="20">
        <a-col :xs="24" :md="12">
          <a-form-item label="用户昵称" name="nickName">
            <a-input v-model:value="form.nickName" :maxlength="30" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-form-item label="手机号码" name="phonenumber">
            <a-input v-model:value="form.phonenumber" :maxlength="11" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="form.email" :maxlength="50" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-form-item label="性别" name="sex">
            <a-radio-group v-model:value="form.sex">
              <a-radio value="0">男</a-radio>
              <a-radio value="1">女</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>
      <a-space>
        <a-button type="primary" @click="submit">保存</a-button>
        <a-button danger @click="close">关闭</a-button>
      </a-space>
    </a-form>

    <a-divider orientation="left">谷歌验证设置</a-divider>
    <a-form layout="vertical">
      <a-row :gutter="20">
        <a-col :xs="24" :md="12">
          <a-form-item label="谷歌验证">
            <a-switch
              v-model:checked="googleEnabled"
              checked-value="0"
              un-checked-value="1"
              checked-children="已启用"
              un-checked-children="已停用"
              @change="handleToggleGoogleAuth"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-form-item label="重置绑定">
            <a-space>
              <a-button danger @click="handleResetGoogleAuth">重置谷歌验证</a-button>
              <span class="form-help">重置后需要重新绑定谷歌验证器</span>
            </a-space>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script setup>
import { updateUserProfile, getUserProfile, toggleGoogleAuth, resetGoogleAuth } from "@/api/system/user";

const props = defineProps({
  user: {
    type: Object,
  },
});

const { proxy } = getCurrentInstance();
const userRef = ref();
const form = reactive({
  nickName: "",
  phonenumber: "",
  email: "",
  sex: "0",
});
const googleEnabled = ref("1");
const currentUserId = ref(null);

const rules = {
  nickName: [{ required: true, message: "用户昵称不能为空", trigger: "blur" }],
  email: [
    { required: true, message: "邮箱地址不能为空", trigger: "blur" },
    { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] },
  ],
  phonenumber: [
    { required: true, message: "手机号码不能为空", trigger: "blur" },
    { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" },
  ],
};

function assignUser(user) {
  form.nickName = user.nickName || "";
  form.phonenumber = user.phonenumber || "";
  form.email = user.email || "";
  form.sex = user.sex || "0";
  currentUserId.value = user.userId;
  googleEnabled.value = user.googleEnabled !== undefined ? String(user.googleEnabled) : "1";
}

function fetchUserProfile() {
  getUserProfile().then((response) => {
    assignUser(response.data || {});
  });
}

function submit() {
  userRef.value?.validate().then(() => {
    updateUserProfile(form).then(() => {
      proxy.$modal.msgSuccess("修改成功");
      if (props.user) {
        props.user.nickName = form.nickName;
        props.user.phonenumber = form.phonenumber;
        props.user.email = form.email;
        props.user.sex = form.sex;
      }
    });
  }).catch(() => {});
}

function close() {
  proxy.$tab.closePage();
}

function handleToggleGoogleAuth(val) {
  const text = val === "0" ? "启用" : "停用";
  const currentStatus = val === "0" ? "1" : "0";
  proxy.$modal
    .confirm(`确认要${text}谷歌验证吗？`)
    .then(() => toggleGoogleAuth(currentUserId.value, currentStatus))
    .then(() => {
      proxy.$modal.msgSuccess(`${text}成功`);
    })
    .catch(() => {
      googleEnabled.value = val === "0" ? "1" : "0";
    });
}

function handleResetGoogleAuth() {
  proxy.$modal
    .confirm("确认要重置谷歌验证吗？重置后需要重新绑定谷歌验证器。")
    .then(() => resetGoogleAuth(currentUserId.value))
    .then(() => {
      googleEnabled.value = "1";
      proxy.$modal.msgSuccess("重置谷歌验证成功");
    })
    .catch(() => {});
}

watch(
  () => props.user,
  (user) => {
    if (user?.userId) assignUser(user);
  },
  { deep: true, immediate: true }
);

onMounted(() => {
  fetchUserProfile();
});
</script>

<style scoped lang="scss">
.profile-form {
  max-width: 860px;
}

.form-help {
  color: #8c8c8c;
  font-size: 12px;
}
</style>
