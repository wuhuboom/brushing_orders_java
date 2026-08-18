<template>
  <a-modal
    v-model:open="visible"
    title="赠送"
    width="800px"
    :mask-closable="false"
    :confirm-loading="submitting"
    :closable="!submitting"
    :keyboard="!submitting"
    :cancel-button-props="{ disabled: submitting }"
    ok-text="确定"
    cancel-text="取消"
    @ok="submit"
    @cancel="cancel"
    @after-close="handleAfterClose"
  >
    <a-form ref="formRef" :model="form" layout="vertical" :rules="rules">
      <a-row :gutter="[20, 0]">
        <a-col :span="12">
          <a-form-item label="用户名">
            <a-input :value="form.username" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="手机号">
            <a-input :value="form.phoneNumber" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="余额">
            <a-input :value="form.balance" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="冻结">
            <a-input :value="form.frozenBalance" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="总余额">
            <a-input :value="form.totalBalance" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="金额" name="amount">
            <a-input-number
              v-model:value="form.amount"
              :min="0.01"
              :precision="2"
              placeholder="请输入金额"
              class="full-width"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="form.remark" :rows="3" placeholder="请输入备注" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup name="OrderuserGiftModal">
import { computed, getCurrentInstance, reactive, ref, watch } from "vue";
import { getOrderuser, giftAmount } from "@/api/member/orderuser";

const props = defineProps({
  modelValue: Boolean,
  userId: [String, Number],
});
const emit = defineEmits(["update:modelValue", "success"]);
const proxy = getCurrentInstance().proxy;

const formRef = ref();
const submitting = ref(false);
let userRequestSequence = 0;
let modalSession = 0;
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit("update:modelValue", val),
});
const form = reactive({
  username: "",
  phoneNumber: "",
  balance: 0,
  frozenBalance: 0,
  totalBalance: 0,
  amount: null,
  remark: "",
});
const rules = reactive({
  amount: [
    { required: true, message: "请输入赠送金额", trigger: "blur" },
    {
      validator: (_rule, value) => isFinitePositiveAmount(value)
        ? Promise.resolve()
        : Promise.reject(new Error("赠送金额必须大于 0")),
      trigger: ["blur", "change"],
    },
  ],
});

watch(
  () => [props.modelValue, props.userId],
  ([open, userId]) => {
    modalSession += 1;
    const requestSequence = ++userRequestSequence;
    if (open) {
      resetForm();
      resetUserInfo();
      loadUserInfo(userId, requestSequence);
    } else {
      resetForm();
      resetUserInfo();
    }
  }
);

async function loadUserInfo(userId, requestSequence) {
  if (!userId) return;
  try {
    const res = await getOrderuser(userId);
    if (
      requestSequence !== userRequestSequence
      || !props.modelValue
      || String(props.userId) !== String(userId)
    ) return;
    const data = res.data || res;
    const balance = finiteNumber(data.balance);
    const frozenBalance = finiteNumber(data.frozenBalance);
    form.username = data.username || "";
    form.phoneNumber = data.phoneNumber || "";
    form.balance = balance;
    form.frozenBalance = frozenBalance;
    form.totalBalance = finiteNumber(data.totalBalance, balance + frozenBalance);
  } catch (error) {
    if (requestSequence === userRequestSequence && props.modelValue) {
      proxy.$modal.msgError(error?.message || "加载会员信息失败");
    }
  }
}

function finiteNumber(value, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function isFinitePositiveAmount(value) {
  const amount = Number(value);
  return Number.isFinite(amount) && amount > 0;
}

function resetUserInfo() {
  Object.assign(form, {
    username: "",
    phoneNumber: "",
    balance: 0,
    frozenBalance: 0,
    totalBalance: 0,
  });
}

function resetForm() {
  form.amount = null;
  form.remark = "";
}

async function submit() {
  if (submitting.value) return;
  const submitSession = modalSession;
  const targetUserId = props.userId;
  submitting.value = true;
  try {
    try {
      await formRef.value?.validate?.();
    } catch (validationError) {
      if (!validationError?.errorFields) {
        proxy.$modal.msgError(validationError?.message || "赠送金额校验失败");
      }
      return;
    }
    if (
      submitSession !== modalSession
      || !props.modelValue
      || String(props.userId) !== String(targetUserId)
    ) return;

    await giftAmount({
      userId: targetUserId,
      amount: Number(form.amount),
      remark: form.remark,
    });
    if (
      submitSession === modalSession
      && props.modelValue
      && String(props.userId) === String(targetUserId)
    ) {
      emit("update:modelValue", false);
    }
    emit("success");
    proxy.$modal.msgSuccess("赠送成功");
  } catch (error) {
    proxy.$modal.msgError(error?.message || "赠送失败");
  } finally {
    submitting.value = false;
  }
}

function cancel() {
  if (submitting.value) return;
  emit("update:modelValue", false);
}

function handleAfterClose() {
  if (props.modelValue) return;
  resetForm();
  resetUserInfo();
  formRef.value?.clearValidate?.();
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
