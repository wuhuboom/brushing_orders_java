<template>
  <a-modal
    v-model:open="visible"
    title="编辑合同"
    width="960px"
    :mask-closable="false"
    :confirm-loading="submitting"
    destroy-on-close
    @ok="handleSubmit"
  >
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="[20, 0]">
          <a-col v-for="field in contractSwitchFields" :key="field.key" :span="6">
            <a-form-item :label="field.label">
              <a-radio-group v-model:value="form[field.key]">
                <a-radio value="1">否</a-radio>
                <a-radio value="0">是</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="用户合同" name="userContractContent">
              <editor v-model="form.userContractContent" :min-height="220" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="正式合同" name="formalContractContent">
              <editor v-model="form.formalContractContent" :min-height="220" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup name="OrderuserContractModal">
import { computed, reactive, ref, watch } from "vue";
import { getOrderuser, updateOrderuser } from "@/api/member/orderuser";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  userId: {
    type: [Number, String],
    default: null,
  },
});

const emit = defineEmits(["update:modelValue", "success"]);
const { proxy } = getCurrentInstance();
const loading = ref(false);
const submitting = ref(false);
const formRef = ref();
const contractSwitchFields = [
  { key: "userContractEnabled", label: "是否启用用户合同" },
  { key: "userContractSigned", label: "是否签署用户合同" },
  { key: "formalContractEnabled", label: "是否启用正式合同" },
  { key: "formalContractSigned", label: "是否签署正式合同" },
];
const form = reactive({
  id: null,
  userContractEnabled: "1",
  userContractSigned: "1",
  formalContractEnabled: "1",
  formalContractSigned: "1",
  userContractContent: "",
  formalContractContent: "",
});

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

watch(
  () => [props.modelValue, props.userId],
  ([open, userId]) => {
    if (!open || !userId) return;
    loading.value = true;
    getOrderuser(userId)
      .then((response) => {
        const data = response.data || {};
        Object.assign(form, {
          id: data.id,
          userContractEnabled: data.userContractEnabled || "1",
          userContractSigned: data.userContractSigned || "1",
          formalContractEnabled: data.formalContractEnabled || "1",
          formalContractSigned: data.formalContractSigned || "1",
          userContractContent: data.userContractContent || "",
          formalContractContent: data.formalContractContent || "",
        });
      })
      .finally(() => {
        loading.value = false;
      });
  },
  { immediate: true }
);

async function handleSubmit() {
  try {
    await formRef.value?.validate();
    submitting.value = true;
    await updateOrderuser({ ...form });
    proxy.$modal.msgSuccess("合同保存成功");
    visible.value = false;
    emit("success");
  } finally {
    submitting.value = false;
  }
}
</script>
