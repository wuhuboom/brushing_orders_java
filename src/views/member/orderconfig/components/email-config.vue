<!-- email-config.vue (子组件，全代码) -->
<template>
  <el-form
    ref="emailFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 第一行：状态 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="localForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="验证码状态" prop="captchaStatus">
          <el-radio-group v-model="localForm.captchaStatus">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二行：主机，端口 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="主机" prop="host">
          <el-input
            v-model="localForm.host"
            placeholder="请输入主机地址"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="端口" prop="port">
          <el-input-number
            v-model="localForm.port"
            :min="1"
            :max="65535"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入端口"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第三行：用户名，密码 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="localForm.username"
            placeholder="请输入用户名"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="localForm.password"
            type="password"
            show-password
            placeholder="请输入密码"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第四行：协议，默认编码 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="协议" prop="protocol">
          <el-input v-model="localForm.protocol" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="默认编码" prop="defaultEncoding">
          <el-input v-model="localForm.defaultEncoding" style="width: 100%" />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第五行：默认验证码，验证码有效期 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="默认验证码" prop="defaultCaptcha">
          <el-input
            v-model="localForm.defaultCaptcha"
            placeholder="请输入默认验证码"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="验证码有效期" prop="captchaExpiry">
          <el-input-number
            v-model="localForm.captchaExpiry"
            :min="1"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入有效期（分钟）"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第六行：标题 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="localForm.title"
            placeholder="请输入邮件标题"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第七行：内容 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="localForm.contents"
            type="textarea"
            :rows="6"
            placeholder="请输入邮件内容"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>
<script setup>
import { ref, reactive, watch, computed } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);

const emailFormRef = ref();
const localForm = reactive({ ...props.form });

// 初始化后立即删除通用字段，确保 localForm 干净
["id", "type", "name", "createTime", "updateTime", "content"].forEach((key) => {
  delete localForm[key];
});

// 规则（修正为 email 字段）
const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  captchaStatus: [
    { required: true, message: "请选择验证码状态", trigger: "change" },
  ],
  host: [{ required: true, message: "请输入主机地址", trigger: "blur" }],
  port: [{ required: true, message: "请输入端口", trigger: "blur" }],
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  protocol: [{ required: true, message: "请输入协议", trigger: "blur" }],
  defaultEncoding: [
    { required: true, message: "请输入默认编码", trigger: "blur" },
  ],
  defaultCaptcha: [
    { required: true, message: "请输入默认验证码", trigger: "blur" },
  ],
  captchaExpiry: [
    { required: true, message: "请输入验证码有效期", trigger: "blur" },
  ],
  title: [{ required: true, message: "请输入邮件标题", trigger: "blur" }],
  contents: [
    { required: true, message: "请输入邮件内容", trigger: "blur" },
    { min: 10, message: "内容长度不能少于10字符", trigger: "blur" },
  ],
});

// 初始化：监听父 form 变化，parse content 到 localForm（仅 email 字段）
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        // 只合并 email 特定字段，排除通用字段
        const emailFields = {
          status: parsed.status,
          captchaStatus: parsed.captchaStatus,
          host: parsed.host,
          port: parsed.port,
          username: parsed.username,
          password: parsed.password,
          protocol: parsed.protocol,
          defaultEncoding: parsed.defaultEncoding,
          defaultCaptcha: parsed.defaultCaptcha,
          captchaExpiry: parsed.captchaExpiry,
          title: parsed.title,
          contents: parsed.contents,
        };
        Object.assign(localForm, emailFields);
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
    // 创建只含 email 字段的 toSave
    const toSave = {
      status: localForm.status,
      captchaStatus: localForm.captchaStatus,
      host: localForm.host,
      port: localForm.port,
      username: localForm.username,
      password: localForm.password,
      protocol: localForm.protocol,
      defaultEncoding: localForm.defaultEncoding,
      defaultCaptcha: localForm.defaultCaptcha,
      captchaExpiry: localForm.captchaExpiry,
      title: localForm.title,
      contents: localForm.contents,
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
  emailFormRef.value.validate((valid) => {
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
