<template>
  <a-drawer
    v-model:open="visible"
    title="编辑身份信息"
    width="70%"
    :mask-closable="false"
    :closable="!submitting"
    :keyboard="!submitting"
    destroy-on-close
    @close="handleClose"
  >
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
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
            <a-form-item label="姓名" name="identityName">
              <a-input v-model:value="form.identityName" allow-clear placeholder="请输入姓名" />
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
                <a-radio value="1">通过</a-radio>
                <a-radio value="2">拒绝</a-radio>
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

    <template #footer>
      <div class="drawer-footer">
        <a-space>
          <a-button :disabled="submitting" @click="handleCancel">取消</a-button>
          <a-button
            type="primary"
            :loading="submitting"
            :disabled="submitting || loading || !isLoadedUser"
            @click="handleSubmit"
          >确定</a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<script setup name="OrderuserIdentityModal">
import { computed, getCurrentInstance, nextTick, reactive, ref, watch } from "vue";
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
let detailRequestSequence = 0;

const identityDefaults = {
  id: null,
  identityType: "id_card",
  identityName: null,
  identityNumber: null,
  identityFrontImage: null,
  identityBackImage: null,
  identityHandheldImage: null,
  identityStatus: null,
  identityRemarks: null,
};
const form = reactive({ ...identityDefaults });
const rules = {
  identityType: [{ required: true, message: "请选择类型", trigger: "change" }],
  identityName: [{ required: true, whitespace: true, message: "请输入姓名", trigger: "blur" }],
  identityNumber: [{ required: true, whitespace: true, message: "请输入证件号码", trigger: "blur" }],
  identityStatus: [{ required: true, message: "请选择状态", trigger: "change" }],
};

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});
const isLoadedUser = computed(
  () =>
    props.modelValue &&
    !loading.value &&
    hasIdentifier(props.userId) &&
    hasIdentifier(form.id) &&
    sameIdentifier(form.id, props.userId),
);

watch(
  () => [props.modelValue, props.userId],
  ([open, userId]) => loadIdentityInfo(open, userId),
  { immediate: true },
);

function hasIdentifier(value) {
  return value !== null && value !== undefined && value !== "";
}

function sameIdentifier(left, right) {
  return hasIdentifier(left) && hasIdentifier(right) && String(left) === String(right);
}

function resetForm() {
  Object.assign(form, identityDefaults);
  formRef.value?.clearValidate();
}

async function loadIdentityInfo(open, userId) {
  const requestSequence = ++detailRequestSequence;
  loading.value = false;
  resetForm();
  await nextTick();
  if (requestSequence !== detailRequestSequence) return;
  formRef.value?.clearValidate();
  if (!open || !hasIdentifier(userId)) return;

  loading.value = true;
  try {
    const response = await getOrderuser(userId);
    if (
      requestSequence !== detailRequestSequence ||
      !props.modelValue ||
      !sameIdentifier(props.userId, userId)
    ) return;

    const data = response.data || {};
    if (!sameIdentifier(data.id, userId)) {
      proxy.$modal.msgError("身份信息加载失败：返回的会员不匹配");
      return;
    }
    Object.assign(form, {
      id: data.id,
      identityType: data.identityType || "id_card",
      identityName: data.identityName ?? null,
      identityNumber: data.identityNumber ?? null,
      identityFrontImage: data.identityFrontImage ?? null,
      identityBackImage: data.identityBackImage ?? null,
      identityHandheldImage: data.identityHandheldImage ?? null,
      identityStatus:
        data.identityStatus === null || data.identityStatus === undefined
          ? null
          : String(data.identityStatus),
      identityRemarks: data.identityRemarks ?? null,
    });
  } catch (error) {
    if (
      requestSequence === detailRequestSequence &&
      props.modelValue &&
      sameIdentifier(props.userId, userId)
    ) {
      proxy.$modal.msgError(error?.msg || error?.message || "身份信息加载失败，请重试");
    }
  } finally {
    if (requestSequence === detailRequestSequence) loading.value = false;
  }
}

function buildIdentityPayload() {
  return {
    id: form.id,
    identityType: form.identityType,
    identityName: form.identityName?.trim(),
    identityNumber: form.identityNumber?.trim(),
    identityFrontImage: form.identityFrontImage ?? null,
    identityBackImage: form.identityBackImage ?? null,
    identityHandheldImage: form.identityHandheldImage ?? null,
    identityStatus: form.identityStatus,
    identityRemarks: form.identityRemarks ?? null,
  };
}

function isCurrentEditSession(userId, sessionSequence) {
  return (
    sessionSequence === detailRequestSequence &&
    props.modelValue &&
    sameIdentifier(props.userId, userId) &&
    sameIdentifier(form.id, userId)
  );
}

async function handleSubmit() {
  if (submitting.value || loading.value) return;
  if (!isLoadedUser.value) {
    proxy.$modal.msgError("会员身份信息尚未加载完成，请稍后重试");
    return;
  }

  const submittedUserId = props.userId;
  const submittedSessionSequence = detailRequestSequence;
  submitting.value = true;
  try {
    try {
      await formRef.value?.validate();
    } catch {
      return;
    }
    if (!isCurrentEditSession(submittedUserId, submittedSessionSequence)) return;

    const payload = buildIdentityPayload();
    try {
      await updateOrderuser(payload);
      emit("success");
      if (!isCurrentEditSession(submittedUserId, submittedSessionSequence)) return;
      proxy.$modal.msgSuccess("身份信息保存成功");
      visible.value = false;
    } catch (error) {
      if (isCurrentEditSession(submittedUserId, submittedSessionSequence)) {
        proxy.$modal.msgError(error?.msg || error?.message || "身份信息保存失败，请重试");
      }
    }
  } finally {
    submitting.value = false;
  }
}

function handleCancel() {
  if (submitting.value) return;
  visible.value = false;
}

function handleClose() {
  if (submitting.value) return;
  detailRequestSequence += 1;
  loading.value = false;
  resetForm();
  visible.value = false;
}
</script>

<style scoped>
.drawer-footer {
  text-align: right;
}
</style>
