<template>
  <a-modal
    v-model:open="visible"
    title="编辑身份信息"
    width="760px"
    :mask-closable="false"
    :confirm-loading="submitting"
    destroy-on-close
    @ok="handleSubmit"
  >
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="[20, 0]">
          <a-col :span="12">
            <a-form-item label="类型" name="identityType">
              <a-radio-group v-model:value="form.identityType">
                <a-radio value="id_card">身份证</a-radio>
                <a-radio value="driver_license">驾驶证</a-radio>
                <a-radio value="passport">护照</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="名称" name="identityName">
              <a-input v-model:value="form.identityName" allow-clear placeholder="请输入名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="证件号码" name="identityNumber">
              <a-input v-model:value="form.identityNumber" allow-clear placeholder="请输入证件号码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="identityStatus">
              <a-radio-group v-model:value="form.identityStatus">
                <a-radio value="0">待审核</a-radio>
                <a-radio value="1">已通过</a-radio>
                <a-radio value="2">已拒绝</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="证件图片正面" name="identityFrontImage">
              <image-upload v-model="form.identityFrontImage" :limit="1" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="证件图片反面" name="identityBackImage">
              <image-upload v-model="form.identityBackImage" :limit="1" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="手持证件图片" name="identityHandheldImage">
              <image-upload v-model="form.identityHandheldImage" :limit="1" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="identityRemarks">
              <a-textarea v-model:value="form.identityRemarks" :rows="3" placeholder="请输入备注" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup name="OrderuserIdentityModal">
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
const form = reactive({
  id: null,
  identityType: "id_card",
  identityName: null,
  identityNumber: null,
  identityFrontImage: null,
  identityBackImage: null,
  identityHandheldImage: null,
  identityStatus: "0",
  identityRemarks: null,
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
          identityType: data.identityType || "id_card",
          identityName: data.identityName || null,
          identityNumber: data.identityNumber || null,
          identityFrontImage: data.identityFrontImage || null,
          identityBackImage: data.identityBackImage || null,
          identityHandheldImage: data.identityHandheldImage || null,
          identityStatus: data.identityStatus || "0",
          identityRemarks: data.identityRemarks || null,
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
    proxy.$modal.msgSuccess("身份信息保存成功");
    visible.value = false;
    emit("success");
  } finally {
    submitting.value = false;
  }
}
</script>
