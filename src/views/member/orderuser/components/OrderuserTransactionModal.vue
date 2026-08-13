<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    width="800px"
    :mask-closable="false"
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
          <a-form-item label="用户名称">
            <a-input :value="user.username" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="手机号码">
            <a-input :value="user.phoneNumber" disabled />
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item label="用户余额">
            <a-input :value="user.balance" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="冻结余额">
            <a-input :value="user.frozenBalance" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="总余额">
            <a-input :value="Number(user.frozenBalance || 0) + Number(user.balance || 0)" disabled />
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item label="操作类型" name="operationType">
            <a-select v-model:value="form.operationType" placeholder="请选择操作类型">
              <a-select-option :value="0">加</a-select-option>
              <a-select-option :value="1">减</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="交易类型" name="transactionType">
            <a-select v-model:value="form.transactionType" placeholder="请选择交易类型">
              <a-select-option value="sxf">手续费</a-select-option>
              <a-select-option value="ck">存款</a-select-option>
              <a-select-option value="cz">充值</a-select-option>
              <a-select-option value="jj">奖金</a-select-option>
              <a-select-option value="dx">底薪</a-select-option>
              <a-select-option value="yzj">援助金</a-select-option>
              <a-select-option value="spfr">商品分润</a-select-option>
              <a-select-option value="qt">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="金额" name="amount">
            <a-input-number v-model:value="form.amount" :min="0" :step="0.01" class="full-width" />
          </a-form-item>
        </a-col>

        <template v-if="showGiftRow">
          <a-col :span="8">
            <a-form-item label="赠送类型" name="giftType">
              <a-select v-model:value="form.giftType" placeholder="请选择赠送类型">
                <a-select-option :value="0">比例</a-select-option>
                <a-select-option :value="1">金额</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="赠送比例" name="giftRatio">
              <a-input-number
                v-model:value="form.giftRatio"
                :min="0"
                :max="100"
                :step="0.01"
                :disabled="form.giftType === 1"
                class="full-width"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="赠送金额" name="giftAmount">
              <a-input-number
                v-model:value="form.giftAmount"
                :min="0"
                :step="0.01"
                :disabled="form.giftType === 0"
                class="full-width"
              />
            </a-form-item>
          </a-col>
        </template>

        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="form.remark" :rows="3" placeholder="请输入备注" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref, watch } from "vue";
import { getOrderuser, transaction } from "@/api/member/orderuser";

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
const user = reactive({
  id: null,
  username: null,
  phoneNumber: null,
  balance: 0,
  frozenBalance: 0,
});

const formRef = ref(null);
const form = reactive({
  operationType: 0,
  transactionType: "ck",
  amount: null,
  giftType: 0,
  giftRatio: null,
  giftAmount: null,
  remark: null,
});

const rules = reactive({
  operationType: [{ required: true, message: "请选择操作类型", trigger: "change" }],
  transactionType: [{ required: true, message: "请选择交易类型", trigger: "change" }],
  amount: [{ required: true, message: "请输入金额", trigger: "blur" }],
});

const showGiftRow = computed(() => Number(form.operationType) === 0);

watch(
  () => form.giftType,
  (newType) => {
    if (newType === 1) {
      form.giftRatio = null;
    } else if (!form.amount || !form.giftRatio) {
      form.giftAmount = null;
    }
    formRef.value?.clearValidate?.(["giftRatio", "giftAmount"]);
  }
);

watch(
  [() => form.amount, () => form.giftRatio],
  () => {
    if (form.giftType === 0 && form.amount && form.giftRatio != null) {
      form.giftAmount = Number((form.amount * (form.giftRatio / 100)).toFixed(2));
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
  form.operationType = 0;
  form.transactionType = "ck";
  form.amount = null;
  form.giftType = 0;
  form.giftRatio = 0;
  form.giftAmount = null;
  form.remark = null;
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
  visible.value = false;
}

function applyDynamicRules() {
  if (showGiftRow.value) {
    rules.giftType = [{ required: true, message: "请选择赠送类型", trigger: "change" }];
    if (Number(form.giftType) === 0) {
      rules.giftRatio = [{ required: true, message: "请输入赠送比例", trigger: "blur" }];
      rules.giftAmount = [];
    } else {
      rules.giftAmount = [{ required: true, message: "请输入赠送金额", trigger: "blur" }];
      rules.giftRatio = [];
    }
  } else {
    delete rules.giftType;
    delete rules.giftRatio;
    delete rules.giftAmount;
  }
}

function handleConfirm() {
  applyDynamicRules();

  formRef.value?.validate?.().then(async () => {
    try {
      await transaction({
        userId: user.id,
        operationType: form.operationType,
        transactionType: form.transactionType,
        amount: form.amount,
        giftType: form.giftType,
        giftRatio: form.giftRatio,
        giftAmount: form.giftAmount,
        remark: form.remark,
      });
      emit("success");
      visible.value = false;
      proxy?.$modal?.msgSuccess?.("操作成功");
    } catch (err) {
      proxy?.$modal?.msgError?.(err?.message || "操作失败");
      console.error(err);
    }
  }).catch(() => {});
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
