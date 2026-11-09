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
    :ok-button-props="{ disabled: !!journalError || (!pending && (loadingUser || !userLoaded)) }"
    :ok-text="pending ? '查询 / 重试原请求' : '确定'"
    cancel-text="取消"
    @ok="submit"
    @cancel="cancel"
    @after-close="handleAfterClose"
  >
    <a-alert v-if="pending" type="warning" show-icon class="request-notice"
      message="这笔赠送的结果尚未确认，请勿重复赠送。系统会查询结果，也可使用原请求安全重试。" />
    <a-alert v-if="journalError" type="error" show-icon class="request-notice" :message="journalError" />
    <a-form ref="formRef" :model="form" layout="vertical" :rules="rules"
      :disabled="submitting || !!pending || !!journalError || loadingUser || !userLoaded">
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
import { computed, getCurrentInstance, onBeforeUnmount, reactive, ref, watch } from "vue";
import { getOrderuser, giftAmount, transactionStatus } from "@/api/member/orderuser";
import useUserStore from "@/store/modules/user";
import { transactionStorageKey, loadTransaction, prepareTransaction, clearTransaction, terminalTransaction } from "./transactionRequest";

const props = defineProps({
  modelValue: Boolean,
  userId: [String, Number],
});
const emit = defineEmits(["update:modelValue", "success"]);
const proxy = getCurrentInstance().proxy;
const operator = useUserStore();

const formRef = ref();
const submitting = ref(false);
const loadingUser = ref(false);
const userLoaded = ref(false);
const pending = ref(null);
const journalError = ref("");
let journalKey = null;
let pollTimer;
let pollSequence = 0;
let userRequestSequence = 0;
let modalSession = 0;
function stopPolling() {
  clearTimeout(pollTimer);
  pollSequence += 1;
}
onBeforeUnmount(() => {
  modalSession += 1;
  userRequestSequence += 1;
  stopPolling();
});
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
  () => [props.modelValue, props.userId, operator.id],
  ([open, userId]) => {
    modalSession += 1;
    const requestSequence = ++userRequestSequence;
    stopPolling();
    resetForm();
    resetUserInfo();
    submitting.value = false;
    pending.value = null;
    journalKey = null;
    journalError.value = "";
    if (open && userId != null) {
      try {
        // Keep gifts separate from the general balance-adjustment journal.
        journalKey = transactionStorageKey(`${window.APP_CONFIG.baseApiUrl}/member/orderuser/giftAmount`, operator.id, userId);
        pending.value = loadTransaction(localStorage, journalKey);
        if (pending.value) {
          if (String(pending.value.userId) !== String(userId)) {
            throw new Error("待确认赠送记录与当前会员不一致，请联系管理员核对流水");
          }
          form.amount = pending.value.amount;
          form.remark = pending.value.remark ?? "";
        }
      } catch (error) {
        journalError.value = error.message || "无法读取赠送状态，请联系管理员";
      }
      loadUserInfo(userId, requestSequence);
      if (pending.value && !journalError.value) pollResult(modalSession);
    }
  },
  { immediate: true }
);

async function loadUserInfo(userId, requestSequence) {
  if (userId == null) return;
  loadingUser.value = true;
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
    userLoaded.value = true;
  } catch (error) {
    if (requestSequence === userRequestSequence && props.modelValue) {
      proxy.$modal.msgError(error?.message || "加载会员信息失败");
    }
  } finally {
    if (requestSequence === userRequestSequence) loadingUser.value = false;
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
  loadingUser.value = false;
  userLoaded.value = false;
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
  formRef.value?.clearValidate?.();
}

function acceptResult(result, intent, key, submitSession) {
  if (!terminalTransaction(result, intent.requestId)) return false;
  // A late response may only clear its own journal entry, never a newer intent.
  clearTransaction(localStorage, key, intent.requestId);
  if (submitSession !== modalSession || key !== journalKey
      || pending.value?.requestId !== intent.requestId) return true;
  pending.value = null;
  stopPolling();
  if (result.state === "SUCCEEDED") {
    emit("success");
    visible.value = false;
    proxy.$modal.msgSuccess("赠送成功，已确认入账");
  } else {
    proxy.$modal.msgError(result.message || "本次赠送未入账");
  }
  return true;
}

async function pollResult(submitSession, attempt = 0, sequence = pollSequence) {
  if (submitSession !== modalSession || sequence !== pollSequence
      || !props.modelValue || !pending.value || journalError.value) return;
  const intent = pending.value;
  const key = journalKey;
  try {
    const response = await transactionStatus(intent.requestId);
    if (sequence !== pollSequence) return;
    if (acceptResult(response.data, intent, key, submitSession)) return;
  } catch { /* Retain the original intent through network errors and refreshes. */ }
  if (submitSession === modalSession && sequence === pollSequence
      && pending.value?.requestId === intent.requestId && attempt < 30) {
    pollTimer = setTimeout(() => pollResult(submitSession, attempt + 1, sequence), 2000);
  }
}

async function submit() {
  if (submitting.value) return;
  if (!props.modelValue || props.userId == null || journalError.value
      || (!pending.value && (loadingUser.value || !userLoaded.value))) return;
  const submitSession = modalSession;
  const targetUserId = props.userId;
  const key = journalKey;
  submitting.value = true;
  stopPolling();
  try {
    if (!pending.value) {
      try {
        await formRef.value?.validate?.();
      } catch (validationError) {
        if (!validationError?.errorFields && submitSession === modalSession) {
          proxy.$modal.msgError(validationError?.message || "赠送金额校验失败");
        }
        return;
      }
      if (submitSession !== modalSession || !props.modelValue
          || String(props.userId) !== String(targetUserId)) return;
      pending.value = prepareTransaction(localStorage, key, {
        userId: targetUserId,
        amount: Number(form.amount),
        remark: form.remark,
      });
      form.amount = pending.value.amount;
      form.remark = pending.value.remark ?? "";
    } else {
      const intent = pending.value;
      try {
        const response = await transactionStatus(intent.requestId);
        if (acceptResult(response.data, intent, key, submitSession)) return;
      } catch { /* Replaying the same saved request is safe when status lookup fails. */ }
    }
    if (submitSession !== modalSession || !props.modelValue
        || String(props.userId) !== String(targetUserId) || !pending.value) return;
    const intent = pending.value;
    const response = await giftAmount(intent);
    if (!acceptResult(response.data, intent, key, submitSession)) pollResult(submitSession);
  } catch (error) {
    if (submitSession !== modalSession) return;
    if (pending.value) {
      proxy.$modal.msgWarning("赠送结果尚未确认，正在查询，请勿重复赠送");
      pollResult(submitSession);
    } else {
      proxy.$modal.msgError(error?.message || "无法保存赠送请求，未提交");
    }
  } finally {
    if (submitSession === modalSession) submitting.value = false;
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
.request-notice {
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}
</style>
