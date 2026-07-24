<template>
  <a-modal
    title="创建表"
    v-model:open="visible"
    width="800px"
    ok-text="确 定"
    cancel-text="取 消"
    :confirm-loading="submitting"
    @ok="handleImportTable"
    @cancel="visible = false"
  >
    <a-form layout="vertical">
      <a-form-item label="创建表语句">
        <a-textarea v-model:value="content" :rows="10" placeholder="请输入文本" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { createTable } from "@/api/tool/gen";

const visible = ref(false);
const content = ref("");
const submitting = ref(false);
const { proxy } = getCurrentInstance();
const emit = defineEmits(["ok"]);

function show() {
  visible.value = true;
}

async function handleImportTable() {
  if (content.value === "") {
    proxy.$modal.msgError("请输入建表语句");
    return;
  }
  submitting.value = true;
  try {
    const res = await createTable({ sql: content.value });
    proxy.$modal.msgSuccess(res.msg);
    if (res.code === 200) {
      visible.value = false;
      emit("ok");
    }
  } finally {
    submitting.value = false;
  }
}

defineExpose({
  show,
});
</script>
