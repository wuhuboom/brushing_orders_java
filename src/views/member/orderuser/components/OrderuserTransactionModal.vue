<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    width="800px"
    :mask-closable="false"
    :confirm-loading="submitting"
    ok-text="确定"
    cancel-text="取消"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <div v-if="loadingUser" class="modal-loading">
      <a-spin />
    </div>

    <a-form v-else ref="formRef" :model="form" :rules="rules" layout="vertical">
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
                :disabled="form.giftType === GIFT_BY_AMOUNT"
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
                :disabled="form.giftType === GIFT_BY_RATIO"
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
import { computed, getCurrentInstance, reactive, ref, watch } from "vue";
import { getOrderuser, transaction } from "@/api/member/orderuser";
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
  transactionTypeOptions,
} from "./orderuserTransaction";

const { proxy } = getCurrentInstance();
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
  return value === null || value === undefined || value === ""
    ? Promise.reject("赠送比例是必填项！")
    : Promise.resolve();
}

async function validateGiftAmount(_rule, value) {
  if (!showGiftRow.value || form.giftType !== GIFT_BY_AMOUNT) return Promise.resolve();
  return value === null || value === undefined || value === ""
    ? Promise.reject("赠送金额是必填项！")
    : Promise.resolve();
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
  () => props.userId,
  (id) => {
    if (id != null && visible.value) {
      fetchUser(id);
    }
  }
);

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.userId != null) {
      fetchUser(props.userId);
    } else if (!val) {
      resetForm();
    }
  }
);

async function fetchUser(id) {
  loadingUser.value = true;
  try {
    const res = await getOrderuser(id);
    const u = res.data || res;
    user.id = u.id;
    user.username = u.username;
    user.phoneNumber = u.phoneNumber;
    user.balance = u.balance ?? 0;
    user.frozenBalance = u.frozenBalance ?? 0;
  } catch (err) {
    console.error(err);
  } finally {
    loadingUser.value = false;
  }
}

function resetForm() {
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

async function handleConfirm() {
  if (submitting.value || !formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    await transaction(buildTransactionPayload(user.id, form));
    emit("success");
    visible.value = false;
    proxy?.$modal?.msgSuccess?.("操作成功");
  } catch (err) {
    proxy?.$modal?.msgError?.(err?.message || "操作失败");
    console.error(err);
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.modal-loading {
  padding: 24px;
  text-align: center;
}

.full-width {
  width: 100%;
}
</style>
