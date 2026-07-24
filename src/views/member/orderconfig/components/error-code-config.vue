<template>
  <a-form ref="errorFormRef" :model="localForm" layout="vertical" class="config-form">
    <a-form-item label="错误代码">
      <a-table
        :columns="columns"
        :data-source="localForm.codes"
        :pagination="false"
        row-key="_key"
        bordered
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'code'">
            <a-input v-model:value="record.code" placeholder="代码" />
          </template>
          <template v-else-if="column.dataIndex === 'message'">
            <a-input v-model:value="record.message" placeholder="消息" />
          </template>
          <template v-else-if="column.key === 'operation'">
            <a-button type="link" danger class="table-link" @click="removeRow(index)">
              删除
            </a-button>
          </template>
        </template>
      </a-table>
      <a-button type="dashed" block class="add-button" @click="addRow">
        <PlusOutlined />
        添加一行数据
      </a-button>
    </a-form-item>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});
const emit = defineEmits(["update:form", "submit", "cancel"]);
const errorFormRef = ref();
const localForm = reactive({ codes: [] });
const columns = [
  { title: "代码", dataIndex: "code", width: 180 },
  { title: "消息", dataIndex: "message" },
  { title: "操作", key: "operation", width: 100, align: "center" },
];

watch(
  () => props.form.content,
  (content) => {
    try {
      const parsed = content ? JSON.parse(content) : {};
      localForm.codes = (Array.isArray(parsed.codes) ? parsed.codes : []).map(
        (item, index) => ({
          ...item,
          _key: `${Date.now()}-${index}`,
        })
      );
    } catch {
      localForm.codes = [];
      message.error("解析错误代码配置失败");
    }
  },
  { immediate: true }
);

watch(
  localForm,
  () => {
    emit("update:form", {
      ...props.form,
      content: JSON.stringify({
        codes: localForm.codes.map(({ _key, ...item }) => item),
      }),
    });
  },
  { deep: true }
);

function addRow() {
  localForm.codes.push({
    _key: `${Date.now()}-${localForm.codes.length}`,
    code: String(localForm.codes.length + 1),
    message: "",
  });
}

function removeRow(index) {
  localForm.codes.splice(index, 1);
}

function handleSubmit() {
  errorFormRef.value
    ?.validate?.()
    .then(() => emit("submit"))
    .catch(() => {});
}

defineExpose({ handleSubmit });
</script>

<style scoped>
.table-link {
  height: auto;
  padding: 0;
}

.add-button {
  margin-top: 12px;
}
</style>
