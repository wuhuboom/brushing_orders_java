<template>
  <a-modal
    v-model:open="visible"
    title="赠送金额"
    width="800px"
    :mask-closable="false"
    ok-text="确定"
    cancel-text="取消"
    @ok="submit"
    @cancel="cancel"
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
          <a-form-item label="冻结金额">
            <a-input :value="form.frozenBalance" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="总余额">
            <a-input :value="form.totalBalance" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="赠送金额" name="amount">
            <a-input-number v-model:value="form.amount" :min="1" :precision="2" class="full-width" />
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
  amount: 0,
  remark: "",
});
const rules = reactive({
  amount: [{ required: true, message: "请输入赠送金额", trigger: "blur" }],
});

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      loadUserInfo();
    } else {
      resetForm();
    }
  }
);

async function loadUserInfo() {
  if (!props.userId) return;
  try {
    const res = await getOrderuser(props.userId);
    const data = res.data;
    form.username = data.username || "";
    form.phoneNumber = data.phoneNumber || "";
    form.balance = data.balance || 0;
    form.frozenBalance = data.frozenBalance || 0;
    form.totalBalance = (data.balance || 0) + (data.frozenBalance || 0);
  } catch (error) {
    console.error("加载用户信息失败", error);
  }
}

function resetForm() {
  form.amount = 0;
  form.remark = "";
}

function submit() {
  formRef.value?.validate?.().then(async () => {
    try {
      await giftAmount({
        userId: props.userId,
        amount: form.amount,
        remark: form.remark,
      });
      emit("success");
      emit("update:modelValue", false);
      proxy.$modal.msgSuccess("赠送成功");
    } catch (error) {
      proxy.$modal.msgError(error.message || "赠送失败");
    }
  }).catch(() => {});
}

function cancel() {
  emit("update:modelValue", false);
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
