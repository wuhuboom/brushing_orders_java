<template>
  <a-drawer
    v-model:open="visible"
    title="编辑合同"
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
          <a-col
            v-for="field in contractSwitchFields"
            :key="field.key"
            :xs="24"
            :md="12"
            :xl="6"
          >
            <a-form-item :label="field.label" :name="field.key">
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

<script setup name="OrderuserContractModal">
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

const contractSwitchFields = [
  { key: "userContractEnabled", label: "是否启用用户合同" },
  { key: "userContractSigned", label: "是否签署用户合同" },
  { key: "formalContractEnabled", label: "是否启用正式合同" },
  { key: "formalContractSigned", label: "是否签署正式合同" },
];
const contractDefaults = {
  id: null,
  userContractEnabled: "1",
  userContractSigned: "1",
  formalContractEnabled: "1",
  formalContractSigned: "1",
  userContractContent: "",
  formalContractContent: "",
};
const form = reactive({ ...contractDefaults });
const rules = Object.fromEntries(
  contractSwitchFields.map(({ key, label }) => [
    key,
    [{ required: true, message: `请选择${label}`, trigger: "change" }],
  ]),
);

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
  ([open, userId]) => loadContractInfo(open, userId),
  { immediate: true },
);

function hasIdentifier(value) {
  return value !== null && value !== undefined && value !== "";
}

function sameIdentifier(left, right) {
  return hasIdentifier(left) && hasIdentifier(right) && String(left) === String(right);
}

function resetForm() {
  Object.assign(form, contractDefaults);
  formRef.value?.clearValidate();
}

function normalizeSwitch(value) {
  return value === null || value === undefined || value === "" ? "1" : String(value);
}

async function loadContractInfo(open, userId) {
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
      proxy.$modal.msgError("合同信息加载失败：返回的会员不匹配");
      return;
    }
    Object.assign(form, {
      id: data.id,
      userContractEnabled: normalizeSwitch(data.userContractEnabled),
      userContractSigned: normalizeSwitch(data.userContractSigned),
      formalContractEnabled: normalizeSwitch(data.formalContractEnabled),
      formalContractSigned: normalizeSwitch(data.formalContractSigned),
      userContractContent: data.userContractContent ?? "",
      formalContractContent: data.formalContractContent ?? "",
    });
  } catch (error) {
    if (
      requestSequence === detailRequestSequence &&
      props.modelValue &&
      sameIdentifier(props.userId, userId)
    ) {
      proxy.$modal.msgError(error?.msg || error?.message || "合同信息加载失败，请重试");
    }
  } finally {
    if (requestSequence === detailRequestSequence) loading.value = false;
  }
}

function buildContractPayload() {
  return {
    id: form.id,
    userContractEnabled: form.userContractEnabled,
    userContractSigned: form.userContractSigned,
    formalContractEnabled: form.formalContractEnabled,
    formalContractSigned: form.formalContractSigned,
    userContractContent: form.userContractContent ?? "",
    formalContractContent: form.formalContractContent ?? "",
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
    proxy.$modal.msgError("会员合同信息尚未加载完成，请稍后重试");
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

    const payload = buildContractPayload();
    try {
      await updateOrderuser(payload);
      emit("success");
      if (!isCurrentEditSession(submittedUserId, submittedSessionSequence)) return;
      proxy.$modal.msgSuccess("合同保存成功");
      visible.value = false;
    } catch (error) {
      if (isCurrentEditSession(submittedUserId, submittedSessionSequence)) {
        proxy.$modal.msgError(error?.msg || error?.message || "合同保存失败，请重试");
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
