<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    width="800px"
    :mask-closable="false"
    :confirm-loading="submitting"
    :closable="!submitting"
    :keyboard="!submitting"
    :ok-button-props="{ disabled: loadingUser || !!journalError || user.id == null }"
    :ok-text="pending ? '查询 / 重试原请求' : '确定'"
    cancel-text="取消"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <a-alert v-if="pending" type="warning" show-icon class="request-notice"
      message="这笔操作的结果尚未确认，请勿另开页面重复上分。系统会查询结果，也可使用原请求安全重试。" />
    <a-alert v-if="journalError" type="error" show-icon :message="journalError" />
    <div v-if="loadingUser" class="modal-loading">
      <a-spin />
    </div>

    <a-form v-else ref="formRef" :model="form" :rules="rules" :disabled="submitting || !!pending || !!journalError" layout="vertical">
      <a-row :gutter="[20, 0]">
        <a-col :span="12">
          <a-form-item label="用户名">
            <a-input :value="user.username" placeholder="用户名" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="手机号码">
            <a-input :value="user.phoneNumber" placeholder="手机号码" disabled />
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item label="余额">
            <a-input-number :value="user.balance" :min="0" placeholder="余额" class="full-width" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="冻结余额">
            <a-input-number :value="user.frozenBalance" :min="0" placeholder="冻结余额" class="full-width" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="总余额">
            <a-input-number :value="totalBalance" :min="0" placeholder="总余额" class="full-width" disabled />
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item label="操作类型" name="operationType">
            <a-select v-model:value="form.operationType" placeholder="请选择操作类型">
              <a-select-option :value="OPERATION_ADD">加</a-select-option>
              <a-select-option :value="OPERATION_SUBTRACT">减</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="交易类型" name="transactionType">
            <a-select v-model:value="form.transactionType" placeholder="请选择交易类型">
              <a-select-option v-for="option in transactionTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="金额" name="amount">
            <a-input-number v-model:value="form.amount" :min="0" placeholder="金额" class="full-width" />
          </a-form-item>
        </a-col>

        <template v-if="showGiftRow">
          <a-col :span="8">
            <a-form-item label="赠送类型" name="giftType">
              <a-select v-model:value="form.giftType" placeholder="请选择赠送类型">
                <a-select-option :value="GIFT_BY_RATIO">比例</a-select-option>
                <a-select-option :value="GIFT_BY_AMOUNT">金额</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="赠送比例" name="giftRatio" required>
              <a-input-number
                v-model:value="form.giftRatio"
                :min="0"
                :disabled="submitting || !!pending || !!journalError || form.giftType === GIFT_BY_AMOUNT"
                placeholder="赠送比例"
                addon-after="%"
                class="full-width"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="赠送金额" name="giftAmount" required>
              <a-input-number
                v-model:value="form.giftAmount"
                :min="0"
                :disabled="submitting || !!pending || !!journalError || form.giftType === GIFT_BY_RATIO"
                placeholder="赠送金额"
                class="full-width"
              />
            </a-form-item>
          </a-col>
        </template>

        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="form.remark" :rows="3" placeholder="备注" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, getCurrentInstance, onBeforeUnmount, reactive, ref, watch } from "vue";
import { getOrderuser, transaction, transactionStatus } from "@/api/member/orderuser";
import {
  GIFT_BY_AMOUNT,
  GIFT_BY_RATIO,
  OPERATION_ADD,
  OPERATION_SUBTRACT,
  buildTransactionPayload,
  calculateGiftAmount,
  createTransactionForm,
  isGiftTransaction,
  isPositiveTransactionAmount,
  isNonNegativeTransactionNumber,
  restoreTransactionForm,
  transactionTypeOptions,
} from "./orderuserTransaction";

import useUserStore from "@/store/modules/user";
import { transactionStorageKey, loadTransaction, prepareTransaction, clearTransaction, terminalTransaction } from "./transactionRequest";

const { proxy } = getCurrentInstance();
const operator = useUserStore();
const pending = ref(null);
const journalError = ref("");
let journalKey = null;
let pollTimer;
let generation = 0;
function stopPolling() { clearTimeout(pollTimer); }
onBeforeUnmount(() => { generation++; stopPolling(); });

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  userId: {
    type: [String, Number],
    default: null,
  },
  title: {
    type: String,
    default: "上下分",
  },
});

const emit = defineEmits(["update:modelValue", "success"]);

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});

const loadingUser = ref(false);
const submitting = ref(false);
const user = reactive({
  id: null,
  username: null,
  phoneNumber: null,
  balance: 0,
  frozenBalance: 0,
});

const formRef = ref(null);
const form = reactive(createTransactionForm());

const totalBalance = computed(() => Number(user.frozenBalance || 0) + Number(user.balance || 0));

async function validatePositiveAmount(_rule, value) {
  if (!isPositiveTransactionAmount(value)) {
    return Promise.reject(value === null || value === undefined || value === ""
      ? "金额是必填项！"
      : "金额必须大于0！");
  }
  return Promise.resolve();
}

async function validateGiftRatio(_rule, value) {
  if (!showGiftRow.value || form.giftType !== GIFT_BY_RATIO) return Promise.resolve();
  return isNonNegativeTransactionNumber(value)
    ? Promise.resolve()
    : Promise.reject("赠送比例必须是大于或等于0的有效数字！");
}

async function validateGiftAmount(_rule, value) {
  if (!showGiftRow.value || form.giftType !== GIFT_BY_AMOUNT) return Promise.resolve();
  return isNonNegativeTransactionNumber(value)
    ? Promise.resolve()
    : Promise.reject("赠送金额必须是大于或等于0的有效数字！");
}

const rules = {
  operationType: [{ required: true, message: "操作类型是必填项！", trigger: "change" }],
  transactionType: [{ required: true, message: "交易类型是必填项！", trigger: "change" }],
  amount: [{ required: true, validator: validatePositiveAmount, trigger: ["change", "blur"] }],
  giftType: [{ required: true, message: "赠送类型是必填项！", trigger: "change" }],
  giftRatio: [{ validator: validateGiftRatio, trigger: ["change", "blur"] }],
  giftAmount: [{ validator: validateGiftAmount, trigger: ["change", "blur"] }],
};

const showGiftRow = computed(() => isGiftTransaction(form));

watch(
  () => form.giftType,
  () => {
    formRef.value?.clearValidate?.(["giftRatio", "giftAmount"]);
  }
);

watch(
  [() => form.amount, () => form.giftRatio, () => form.giftType],
  () => {
    if (form.giftType === GIFT_BY_RATIO) {
      form.giftAmount = calculateGiftAmount(form.amount, form.giftRatio);
    }
  },
  { immediate: true }
);

watch(
  [() => props.modelValue, () => props.userId],
  ([open, id]) => {
    generation++;
    stopPolling();
    if (open && id != null) {
      resetForm();
      journalError.value = "";
      try {
        journalKey = transactionStorageKey(window.APP_CONFIG.baseApiUrl, operator.id, id);
        pending.value = loadTransaction(localStorage, journalKey);
        if (pending.value) Object.assign(form, restoreTransactionForm(pending.value));
      } catch (error) {
        journalError.value = error.message || "无法保存交易状态，请联系管理员";
      }
      fetchUser(id);
      if (pending.value) pollResult(generation);
    } else {
      resetForm();
    }
  },
  { immediate: true }
);

async function fetchUser(id) {
  const version = generation;
  loadingUser.value = true;
  try {
    const res = await getOrderuser(id);
    if (version !== generation) return;
    const u = res.data || res;
    user.id = u.id;
    user.username = u.username;
    user.phoneNumber = u.phoneNumber;
    user.balance = u.balance ?? 0;
    user.frozenBalance = u.frozenBalance ?? 0;
  } catch (err) {
    console.error(err);
  } finally {
    if (version === generation) loadingUser.value = false;
  }
}

function resetForm() {
  pending.value = null;
  Object.assign(form, createTransactionForm());
  submitting.value = false;
  formRef.value?.clearValidate?.();
  Object.assign(user, {
    id: null,
    username: null,
    phoneNumber: null,
    balance: 0,
    frozenBalance: 0,
  });
}

function handleCancel() {
  if (submitting.value) return;
  visible.value = false;
}

function acceptResult(result, intent, key, version) {
  if (!terminalTransaction(result, intent.requestId)) return false;
  // Keep the saved intent if clearing storage fails, so replay remains safe.
  clearTransaction(localStorage, key, intent.requestId);
  if (version !== generation || pending.value?.requestId !== intent.requestId) return true;
  pending.value = null;
  stopPolling();
  if (result.state === "SUCCEEDED") {
    emit("success");
    visible.value = false;
    proxy?.$modal?.msgSuccess?.("操作成功，已确认入账");
  } else {
    proxy?.$modal?.msgError?.(result.message || "本次操作未入账");
  }
  return true;
}

async function pollResult(version, attempt = 0) {
  if (version !== generation || !visible.value || !pending.value) return;
  const intent = pending.value;
  const key = journalKey;
  try {
    const response = await transactionStatus(intent.requestId);
    if (acceptResult(response.data, intent, key, version)) return;
  } catch { /* Keep the intent across network failures and page reloads. */ }
  if (version === generation && pending.value?.requestId === intent.requestId && attempt < 30) {
    pollTimer = setTimeout(() => pollResult(version, attempt + 1), 2000);
  }
}

async function handleConfirm() {
  if (submitting.value || loadingUser.value || journalError.value || user.id == null || !formRef.value) return;
  // Set before async validation to prevent two clicks from starting parallel submissions.
  submitting.value = true;
  const version = generation;
  const key = journalKey;
  stopPolling();
  try {
    if (!pending.value) {
      await formRef.value.validate();
      if (version !== generation) return;
      pending.value = prepareTransaction(localStorage, key, buildTransactionPayload(user.id, form));
    } else {
      const intent = pending.value;
      const status = await transactionStatus(intent.requestId);
      if (acceptResult(status.data, intent, key, version)) return;
    }
    if (version !== generation || !pending.value) return;
    const intent = pending.value;
    const response = await transaction(intent);
    if (!acceptResult(response.data, intent, key, version)) pollResult(version);
  } catch (error) {
    if (version !== generation) return;
    if (pending.value) {
      proxy?.$modal?.msgWarning?.("结果尚未确认，正在查询，请勿重复发起新交易");
      pollResult(version);
    } else if (!error?.errorFields) {
      proxy?.$modal?.msgError?.(error?.message || "无法保存交易请求，未提交");
    }
  } finally {
    if (version === generation) submitting.value = false;
  }
}

</script>

<style scoped>
.request-notice { margin-bottom: 16px; }
.modal-loading {
  padding: 24px;
  text-align: center;
}

.full-width {
  width: 100%;
}
</style>
