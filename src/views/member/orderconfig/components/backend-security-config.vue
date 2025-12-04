<!-- backend-security-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="securityFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 第一行：启用登录验证码 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="启用登录验证码" prop="enabledLoginCaptcha">
          <el-radio-group v-model="localForm.enabledLoginCaptcha">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二行：连续登录失败次数，失败冻结时长 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="连续登录失败次数" prop="consecutiveLoginFailures">
          <el-input-number
            v-model="localForm.consecutiveLoginFailures"
            :min="1"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入失败次数"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="失败冻结时长（分钟）" prop="freezeDurationMinutes">
          <el-input-number
            v-model="localForm.freezeDurationMinutes"
            :min="0"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入冻结时长"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第三行：白名单列表 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="白名单列表" prop="whitelist">
          <el-input
            v-model="localForm.whitelist"
            type="textarea"
            :rows="3"
            placeholder="请输入白名单（逗号分隔，如 IP1,IP2）"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第四行：黑名单国家代码列表 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="黑名单国家代码列表" prop="blacklistCountryCodes">
          <el-input
            v-model="localForm.blacklistCountryCodes"
            type="textarea"
            :rows="3"
            placeholder="请输入黑名单国家代码（逗号分隔，如 US,CN）"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第五行：黑名单国家数据请求地址 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item
          label="黑名单国家数据请求地址"
          prop="blacklistCountryApiUrl"
        >
          <el-input
            v-model="localForm.blacklistCountryApiUrl"
            placeholder="请输入 API 地址（如 https://api.example.com/blacklist）"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第六行：黑名单国家数据分隔符 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item
          label="黑名单国家数据分隔符"
          prop="blacklistCountrySeparator"
        >
          <el-input
            v-model="localForm.blacklistCountrySeparator"
            placeholder="请输入分隔符（如 , 或 |）"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第七行：黑名单列表 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="黑名单列表" prop="blacklist">
          <el-input
            v-model="localForm.blacklist"
            type="textarea"
            :rows="3"
            placeholder="请输入黑名单（逗号分隔，如 IP1,IP2）"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);

const securityFormRef = ref();
const localForm = reactive({ ...props.form });

// 初始化后立即删除通用字段，确保 localForm 干净
["id", "type", "name", "createTime", "updateTime", "content"].forEach((key) => {
  delete localForm[key];
});

// 规则
const localRules = reactive({
  enabledLoginCaptcha: [
    { required: true, message: "请选择是否启用登录验证码", trigger: "change" },
  ],
  consecutiveLoginFailures: [
    { required: true, message: "请输入连续登录失败次数", trigger: "blur" },
  ],
  freezeDurationMinutes: [
    { required: true, message: "请输入失败冻结时长", trigger: "blur" },
  ],
  whitelist: [{ required: true, message: "请输入白名单列表", trigger: "blur" }],
  blacklistCountryCodes: [
    { required: true, message: "请输入黑名单国家代码列表", trigger: "blur" },
  ],
  blacklistCountryApiUrl: [
    {
      required: true,
      message: "请输入黑名单国家数据请求地址",
      trigger: "blur",
    },
  ],
  blacklistCountrySeparator: [
    { required: true, message: "请输入黑名单国家数据分隔符", trigger: "blur" },
  ],
  blacklist: [{ required: true, message: "请输入黑名单列表", trigger: "blur" }],
});

// 初始化：监听父 form 变化，parse content 到 localForm（仅 security 字段）
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        // 只合并 security 特定字段，排除通用字段
        const securityFields = {
          enabledLoginCaptcha: parsed.enabledLoginCaptcha,
          consecutiveLoginFailures: parsed.consecutiveLoginFailures,
          freezeDurationMinutes: parsed.freezeDurationMinutes,
          whitelist: parsed.whitelist,
          blacklistCountryCodes: parsed.blacklistCountryCodes,
          blacklistCountryApiUrl: parsed.blacklistCountryApiUrl,
          blacklistCountrySeparator: parsed.blacklistCountrySeparator,
          blacklist: parsed.blacklist,
        };
        Object.assign(localForm, securityFields);
      } catch (e) {
        ElMessage.error("解析配置失败");
      }
    }
  },
  { immediate: true }
);

// 更新父 form：localForm 变化时，过滤不想要的字段后 stringify 到 content
watch(
  localForm,
  () => {
    // 创建只含 security 字段的 toSave
    const toSave = {
      enabledLoginCaptcha: localForm.enabledLoginCaptcha,
      consecutiveLoginFailures: localForm.consecutiveLoginFailures,
      freezeDurationMinutes: localForm.freezeDurationMinutes,
      whitelist: localForm.whitelist,
      blacklistCountryCodes: localForm.blacklistCountryCodes,
      blacklistCountryApiUrl: localForm.blacklistCountryApiUrl,
      blacklistCountrySeparator: localForm.blacklistCountrySeparator,
      blacklist: localForm.blacklist,
    };

    emit("update:form", {
      ...props.form,
      content: JSON.stringify(toSave),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  securityFormRef.value.validate((valid) => {
    if (valid) {
      emit("submit");
    }
  });
}

// 取消
function handleCancel() {
  emit("cancel");
}
</script>

<style scoped>
/* 样式调整 textarea */
:deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.5;
  min-height: 100px;
}
</style>
