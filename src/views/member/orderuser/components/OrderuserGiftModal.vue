<template>
  <el-dialog
    title="赠送金额"
    v-model="visible"
    width="800px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" label-position="top" :rules="rules">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="用户名">
            <el-input :value="form.username" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号">
            <el-input :value="form.phoneNumber" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="余额">
            <el-input :value="form.balance" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="冻结金额">
            <el-input :value="form.frozenBalance" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="总余额">
            <el-input :value="form.totalBalance" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="赠送金额" prop="amount">
        <el-input-number
          v-model="form.amount"
          :min="1"
          :precision="2"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="OrderuserGiftModal">
import { reactive, ref, watch } from "vue";
import { getOrderuser, giftAmount } from "@/api/member/orderuser";
const props = defineProps({
  modelValue: Boolean,
  userId: [String, Number],
});
const emit = defineEmits(["update:modelValue", "success"]);

const formRef = ref();
const visible = ref(false); // Local reactive state for dialog visibility
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
    visible.value = val; // Sync prop to local state
    if (val) {
      loadUserInfo();
    } else {
      resetForm();
    }
  }
);

watch(visible, (val) => {
  emit("update:modelValue", val); // Sync local state back to prop
});

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

async function submit() {
  console.log(123);
  formRef.value.validate(async (valid) => {
    if (!valid) {
      console.log("Validation failed");
      return;
    }
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
  });
}

function cancel() {
  emit("update:modelValue", false);
}

const proxy = getCurrentInstance().proxy;
</script>
