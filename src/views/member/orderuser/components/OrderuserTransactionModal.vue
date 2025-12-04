<template>
  <el-dialog
    :title="title"
    v-model="visible"
    width="800px"
    :close-on-click-modal="false"
  >
    <div v-if="loadingUser" class="pa20" style="text-align: center">
      <el-spin />
    </div>

    <div v-else>
      <!-- 第一排：用户名，手机号码 -->

      <!-- 第三排：操作类型、交易类型、金额 -->
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="110px"
        label-position="top"
      >
        <el-row :gutter="20" class="mb12">
          <el-col :span="12">
            <el-form-item label="用户名称" prop="username">
              <el-input :value="user.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phoneNumber">
              <el-input :value="user.phoneNumber" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第二排：余额，冻结余额，总余额 -->
        <el-row :gutter="20" class="mb12">
          <el-col :span="8">
            <el-form-item label="用户余额" prop="balance">
              <el-input :value="user.balance" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="冻结余额" prop="frozenBalance">
              <el-input :value="user.frozenBalance" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8"
            ><el-form-item label="总余额" prop="totalBalance">
              <el-input
                :value="user.frozenBalance + user.balance"
                disabled
              /> </el-form-item
          ></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="操作类型" prop="operationType">
              <el-select
                v-model="form.operationType"
                placeholder="请选择操作类型"
              >
                <el-option :label="'加'" :value="0" />
                <el-option :label="'减'" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="交易类型" prop="transactionType">
              <el-select
                v-model="form.transactionType"
                placeholder="请选择交易类型"
              >
                <el-option label="手续费" value="sxf" />
                <el-option label="充值" value="cz" />
                <el-option label="奖金" value="jj" />
                <el-option label="底薪" value="dx" />
                <el-option label="援助金" value="yzj" />
                <el-option label="商品分润" value="spfr" />
                <el-option label="其他" value="qt" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="金额" prop="amount">
              <el-input-number
                v-model="form.amount"
                :min="0"
                :step="0.01"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第四排：赠送类型(仅在操作类型为加时显示) -->
        <el-row v-if="showGiftRow" :gutter="20">
          <el-col :span="8">
            <el-form-item label="赠送类型" prop="giftType">
              <el-select v-model="form.giftType" placeholder="请选择赠送类型">
                <el-option label="比列" :value="0" />
                <el-option label="金额" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="赠送比列" prop="giftRatio">
              <el-input-number
                v-model="form.giftRatio"
                :min="0"
                :max="100"
                :step="0.01"
                style="width: 100%"
                :disabled="form.giftType === 1"
              />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="赠送金额" prop="giftAmount">
              <el-input-number
                v-model="form.giftAmount"
                :min="0"
                :step="0.01"
                style="width: 100%"
                :disabled="form.giftType === 0"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第五排：备注 -->
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed, getCurrentInstance } from "vue";
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
  transactionType: "cz",
  amount: null,
  giftType: 0,
  giftRatio: null,
  giftAmount: null,
  remark: null,
});

const rules = {
  operationType: [
    { required: true, message: "请选择操作类型", trigger: "change" },
  ],
  transactionType: [
    { required: true, message: "请选择交易类型", trigger: "change" },
  ],
  amount: [{ required: true, message: "请输入金额", trigger: "blur" }],
  // gift fields only validated when visible
};

const showGiftRow = computed(() => Number(form.operationType) === 0);

// Watchers for gift logic
watch(
  () => form.giftType,
  (newType) => {
    if (newType === 1) {
      form.giftRatio = null; // Reset ratio when switching to amount mode
    } else {
      // When switching to ratio mode, reset giftAmount if needed, calculation will handle on changes
      if (!form.amount || !form.giftRatio) {
        form.giftAmount = null;
      }
    }
    // Trigger validation clear if needed
    formRef.value?.clearValidate?.(["giftRatio", "giftAmount"]);
  }
);

watch(
  [() => form.amount, () => form.giftRatio],
  () => {
    if (form.giftType === 0 && form.amount && form.giftRatio != null) {
      form.giftAmount = Number(
        (form.amount * (form.giftRatio / 100)).toFixed(2)
      );
    }
  },
  { immediate: true }
);

// watch userId and visible
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
    // API returns data in res.data per other code patterns
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
  form.transactionType = "cz";
  form.amount = null;
  form.giftType = 0;
  form.giftRatio = 0;
  form.giftAmount = null;
  form.remark = null;
  formRef.value && formRef.value.clearValidate && formRef.value.clearValidate();
  user.id = null;
  user.username = null;
  user.phoneNumber = null;
  user.balance = 0;
  user.frozenBalance = 0;
}

function handleCancel() {
  visible.value = false;
}

function handleConfirm() {
  // Apply dynamic validation rules to the bound `rules` object depending on gift visibility/type
  if (showGiftRow.value) {
    rules.giftType = [
      { required: true, message: "请选择赠送类型", trigger: "change" },
    ];
    if (Number(form.giftType) === 0) {
      rules.giftRatio = [
        { required: true, message: "请输入赠送比列", trigger: "blur" },
      ];
      rules.giftAmount = []; // optional when ratio mode
    } else {
      rules.giftAmount = [
        { required: true, message: "请输入赠送金额", trigger: "blur" },
      ];
      rules.giftRatio = []; // optional when amount mode
    }
  } else {
    // remove gift-related validations when not shown
    delete rules.giftType;
    delete rules.giftRatio;
    delete rules.giftAmount;
  }

  formRef.value?.validate(async (valid) => {
    if (!valid) return;

    const payload = {
      userId: user.id,
      operationType: form.operationType,
      transactionType: form.transactionType,
      amount: form.amount,
      giftType: form.giftType,
      giftRatio: form.giftRatio,
      giftAmount: form.giftAmount,
      remark: form.remark,
    };

    try {
      await transaction(payload);
      // notify parent
      emit("success");
      visible.value = false;
      // show message using global modal if available
      proxy?.$modal?.msgSuccess && proxy.$modal.msgSuccess("操作成功");
    } catch (err) {
      // If API returns error, show message
      proxy?.$modal?.msgError &&
        proxy.$modal.msgError(err?.message || "操作失败");
      console.error(err);
    }
  });
}
</script>

<style scoped>
.mb12 {
  margin-bottom: 12px;
}
.pa20 {
  padding: 20px;
}
</style>
