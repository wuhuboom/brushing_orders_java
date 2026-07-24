<template>
  <a-form ref="rateFormRef" :model="localForm" layout="vertical" class="config-form">
    <a-form-item label="限流设置">
      <a-table
        :columns="columns"
        :data-source="localForm.value"
        :pagination="false"
        row-key="_key"
        bordered
        :scroll="{ x: 820 }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'id'">
            <a-input v-model:value="record.id" placeholder="ID" />
          </template>
          <template v-else-if="column.dataIndex === 'kind'">
            <a-select v-model:value="record.kind" :options="kindOptions" class="full-width" />
          </template>
          <template v-else-if="column.dataIndex === 'url'">
            <a-input v-model:value="record.url" placeholder="URL" />
          </template>
          <template v-else-if="column.dataIndex === 'interval'">
            <a-input-number
              v-model:value="record.interval"
              :min="0"
              :precision="0"
              class="full-width"
              placeholder="间隔"
            />
          </template>
          <template v-else-if="column.dataIndex === 'limit'">
            <a-input-number
              v-model:value="record.limit"
              :min="0"
              :precision="0"
              class="full-width"
              placeholder="限制"
            />
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
const rateFormRef = ref();
const localForm = reactive({ value: [] });
const kindOptions = [
  { value: "1", label: "IP" },
  { value: "2", label: "设备" },
  { value: "3", label: "用户" },
  { value: "100", label: "全局" },
];
const columns = [
  { title: "ID", dataIndex: "id", width: 150 },
  { title: "类型", dataIndex: "kind", width: 120 },
  { title: "URL", dataIndex: "url", width: 260 },
  { title: "间隔", dataIndex: "interval", width: 120 },
  { title: "限制", dataIndex: "limit", width: 120 },
  { title: "操作", key: "operation", width: 100, align: "center" },
];

watch(
  () => props.form.content,
  (content) => {
    try {
      const parsed = content ? JSON.parse(content) : {};
      const rows = Array.isArray(parsed.value) ? parsed.value : [];
      localForm.value = rows.map((item, index) => ({
        ...item,
        kind: String(item.kind ?? "1"),
        _key: `${Date.now()}-${index}`,
      }));
    } catch {
      localForm.value = [];
      message.error("解析限流配置失败");
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
        value: localForm.value.map(({ _key, ...item }) => item),
      }),
    });
  },
  { deep: true }
);

function addRow() {
  localForm.value.push({
    _key: `${Date.now()}-${localForm.value.length}`,
    id: String(localForm.value.length + 1),
    kind: "1",
    url: "",
    interval: 0,
    limit: 0,
  });
}

function removeRow(index) {
  localForm.value.splice(index, 1);
}

function handleSubmit() {
  rateFormRef.value
    ?.validate?.()
    .then(() => emit("submit"))
    .catch(() => {});
}

defineExpose({ handleSubmit });
</script>

<style scoped>
.full-width {
  width: 100%;
}

.table-link {
  height: auto;
  padding: 0;
}

.add-button {
  margin-top: 12px;
}
</style>
