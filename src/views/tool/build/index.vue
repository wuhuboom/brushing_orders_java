<template>
  <div class="app-container ant-pro-member-page">
    <a-card title="表单构建器" :bordered="false">
      <a-alert
        type="info"
        show-icon
        message="表单构建器已切换为轻量模式。后台业务页面已统一为 Ant Design Vue；复杂拖拽式构建器后续可单独重建。"
        class="mb16"
      />

      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :lg="10">
          <a-form layout="vertical">
            <a-form-item label="组件名称">
              <a-input v-model:value="formName" placeholder="例如 UserForm" />
            </a-form-item>
            <a-form-item label="字段定义">
              <a-textarea
                v-model:value="fieldText"
                :rows="14"
                placeholder="每行一个字段：label,prop,type&#10;用户名,username,input&#10;状态,status,select"
              />
            </a-form-item>
            <a-space>
              <a-button type="primary" @click="copyCode">复制代码</a-button>
              <a-button @click="reset">重置</a-button>
            </a-space>
          </a-form>
        </a-col>
        <a-col :xs="24" :lg="14">
          <pre class="code-preview">{{ generatedCode }}</pre>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup name="FormBuild">
const { proxy } = getCurrentInstance();
const formName = ref("GeneratedForm");
const fieldText = ref("用户名,username,input\n状态,status,select\n备注,remark,textarea");

const fields = computed(() =>
  fieldText.value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [label, prop, type = "input"] = line.split(",").map((item) => item.trim());
      return { label, prop, type };
    })
    .filter((field) => field.label && field.prop)
);

const generatedCode = computed(() => {
  const formItems = fields.value.map((field) => renderField(field)).join("\n");
  const modelFields = fields.value.map((field) => `  ${field.prop}: null,`).join("\n");
  return `<template>
  <a-form :model="form" layout="vertical">
${formItems}
  </a-form>
</template>

<script setup>
const form = reactive({
${modelFields}
});
<\/script>`;
});

function renderField(field) {
  if (field.type === "textarea") {
    return `    <a-form-item label="${field.label}" name="${field.prop}">
      <a-textarea v-model:value="form.${field.prop}" />
    </a-form-item>`;
  }
  if (field.type === "select") {
    return `    <a-form-item label="${field.label}" name="${field.prop}">
      <a-select v-model:value="form.${field.prop}" />
    </a-form-item>`;
  }
  if (field.type === "number") {
    return `    <a-form-item label="${field.label}" name="${field.prop}">
      <a-input-number v-model:value="form.${field.prop}" class="full-width" />
    </a-form-item>`;
  }
  return `    <a-form-item label="${field.label}" name="${field.prop}">
      <a-input v-model:value="form.${field.prop}" />
    </a-form-item>`;
}

function copyCode() {
  navigator.clipboard?.writeText(generatedCode.value);
  proxy.$modal.msgSuccess("复制成功");
}

function reset() {
  formName.value = "GeneratedForm";
  fieldText.value = "用户名,username,input\n状态,status,select\n备注,remark,textarea";
}
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}

.code-preview {
  min-height: 420px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  background: #f6f8fa;
  border-radius: 6px;
  font-size: 12px;
}

.full-width {
  width: 100%;
}
</style>
