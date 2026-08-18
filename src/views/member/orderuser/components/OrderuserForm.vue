<template>
  <a-drawer
    v-model:open="visible"
    :title="title"
    width="85%"
    :mask-closable="false"
    :closable="!submitting"
    :keyboard="!submitting"
    destroy-on-close
    @close="handleClose"
  >
    <a-form ref="formRef" :model="formData" :rules="rules" layout="vertical" :disabled="readonly">
      <a-row :gutter="[20, 0]">
        <a-col :span="6">
          <a-form-item label="用户名" name="username">
            <a-input
              v-model:value="formData.username"
              placeholder="用户名"
              :disabled="isEditMode"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="手机号码" name="phoneNumber">
            <a-input v-model:value="formData.phoneNumber" placeholder="手机号码" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="formData.email" placeholder="邮箱" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="生日" name="birthday">
            <a-date-picker
              v-model:value="formData.birthday"
              value-format="YYYY-MM-DD"
              placeholder="生日"
              class="full-width"
              allow-clear
            />
          </a-form-item>
        </a-col>

        <a-col :span="6">
          <a-form-item label="VIP等级" name="vipId">
            <a-select v-model:value="formData.vipId" placeholder="VIP等级" allow-clear>
              <a-select-option v-for="item in levelList" :key="item.id" :value="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col v-if="!isEditMode" :span="6">
          <a-form-item label="登录密码" name="password">
            <a-input-password v-model:value="formData.password" placeholder="登录密码" allow-clear />
          </a-form-item>
        </a-col>
        <a-col v-if="!isEditMode" :span="6">
          <a-form-item label="交易密码" name="tradePassword">
            <a-input-password v-model:value="formData.tradePassword" placeholder="交易密码" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="信誉分" name="reputationScore">
            <a-input-number
              v-model:value="formData.reputationScore"
              :min="0"
              :precision="0"
              placeholder="请输入信誉分"
              class="full-width"
            />
          </a-form-item>
        </a-col>

        <a-col :span="6">
          <a-form-item label="头像" name="avatar">
            <image-upload
              v-model="formData.avatar"
              :limit="1"
              :is-show-tip="false"
              :disabled="readonly"
            />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="上级邀请码" name="parentInviteCode">
            <a-input
              v-model:value="formData.parentInviteCode"
              placeholder="顶级用户留空，下级用户请输入上级邀请码"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="邀请码" name="inviteCode">
            <a-input v-model:value="formData.inviteCode" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="性别" name="gender">
            <a-radio-group v-model:value="formData.gender">
              <a-radio
                v-for="dict in orderedGenderOptions"
                :key="dict.value"
                :value="dict.value"
                :disabled="readonly || String(dict.value) === '2'"
              >
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>

        <a-col :span="6">
          <a-form-item label="是否启用" name="isEnabled">
            <a-radio-group v-model:value="formData.isEnabled">
              <a-radio v-for="dict in orderedEnabledOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="允许邀请" name="allowInvite">
            <a-radio-group v-model:value="formData.allowInvite">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="是否冻结" name="isFrozen">
            <a-radio-group v-model:value="formData.isFrozen">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="是否假人" name="isFake">
            <a-radio-group v-model:value="formData.isFake">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>

        <a-col :span="6">
          <a-form-item label="禁止工作" name="isBanned">
            <a-radio-group v-model:value="formData.isBanned">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="工作限额" name="workLimit">
            <a-input-number
              v-model:value="formData.workLimit"
              :min="0"
              placeholder="请输入工作限额"
              class="full-width"
            />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="账户状态" name="accountStatus">
            <a-radio-group v-model:value="formData.accountStatus">
              <a-radio v-for="dict in orderedEnabledOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="交易状态" name="transactionStatus">
            <a-radio-group v-model:value="formData.transactionStatus">
              <a-radio v-for="dict in orderedEnabledOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>

        <a-col :span="6">
          <a-form-item label="提现状态" name="withdrawalStatus">
            <a-radio-group v-model:value="formData.withdrawalStatus">
              <a-radio v-for="dict in orderedEnabledOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="协助金提现状态" name="assistWithdrawalStatus">
            <a-radio-group v-model:value="formData.assistWithdrawalStatus">
              <a-radio v-for="dict in orderedEnabledOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="充值后禁止提现" name="depositBlockWithdrawal">
            <a-radio-group v-model:value="formData.depositBlockWithdrawal">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="关闭提现通知" name="isWithdrawalNotification">
            <a-radio-group v-model:value="formData.isWithdrawalNotification">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>

        <a-col :span="6">
          <a-form-item label="启用Web3授权" name="web3AuthEnabled">
            <a-radio-group v-model:value="formData.web3AuthEnabled">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="是否无效" name="isInvalid">
            <a-radio-group v-model:value="formData.isInvalid">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="是否活动" name="isActivity">
            <a-radio-group v-model:value="formData.isActivity">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="任务开始前是否验证身份信息" name="verifyIdentityBeforeTask">
            <a-radio-group v-model:value="formData.verifyIdentityBeforeTask">
              <a-radio v-for="dict in orderedYesNoOptions" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item label="禁止客户提现所需交易密码失败次数(0-不限制)" name="withdrawalPasswordFailLimit">
            <a-input-number v-model:value="formData.withdrawalPasswordFailLimit" :min="0" :precision="0" class="full-width" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="禁止客户提现交易密码连续失败次数" name="withdrawalPasswordFailCount">
            <a-input-number v-model:value="formData.withdrawalPasswordFailCount" :min="0" :precision="0" class="full-width" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="单次最大提现金额(0-不限制)" name="maxSingleWithdrawal">
            <a-input-number v-model:value="formData.maxSingleWithdrawal" :min="0" class="full-width" />
          </a-form-item>
        </a-col>

        <a-col :span="24">
          <a-form-item label="备注" name="remarks">
            <a-textarea v-model:value="formData.remarks" placeholder="备注" :rows="3" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <template #footer>
      <div class="drawer-footer">
        <a-space>
          <a-button :disabled="submitting" @click="handleCancel">取消</a-button>
          <a-button
            v-if="!readonly"
            type="primary"
            :loading="submitting"
            :disabled="submitting"
            @click="handleSubmit"
          >确定</a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<script setup name="OrderuserForm">
import { computed, getCurrentInstance, nextTick, ref, watch } from "vue";
import { addOrderuser, updateOrderuser } from "@/api/member/orderuser";
import { buildMemberSubmitPayload } from "./orderuserFormPayload";

const { proxy } = getCurrentInstance();
const { user_yes_no, sys_user_sex, sys_enabled } = proxy.useDict(
  "user_yes_no",
  "sys_user_sex",
  "sys_enabled"
);

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: "添加用户",
  },
  formData: {
    type: Object,
    default: () => ({}),
  },
  levelList: {
    type: Array,
    default: () => [],
  },
  readonly: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["update:modelValue", "success"]);
const formRef = ref();
const submitting = ref(false);
let formSession = 0;
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit("update:modelValue", val),
});
const isEditMode = computed(() => props.formData?.id != null);

const requiredTrimmedRule = (label) => ({
  required: true,
  whitespace: true,
  message: `${label}是必填项！`,
  trigger: "blur",
});
const passwordLengthRule = (label) => ({
  validator: (_rule, value) => {
    const password = String(value || "").trim();
    return !password || password.length >= 6
      ? Promise.resolve()
      : Promise.reject(new Error(`${label}长度不能少于6位`));
  },
  trigger: "blur",
});
const rules = computed(() => ({
  username: isEditMode.value ? [] : [requiredTrimmedRule("用户名")],
  tradePassword: isEditMode.value
    ? []
    : [requiredTrimmedRule("交易密码"), passwordLengthRule("交易密码")],
  password: isEditMode.value
    ? []
    : [requiredTrimmedRule("登录密码"), passwordLengthRule("登录密码")],
  vipId: [{ required: true, message: "VIP等级不能为空", trigger: "change" }],
  reputationScore: [{ required: true, message: "信誉分不能为空", trigger: "change" }],
  gender: [{ required: true, message: "性别不能为空", trigger: "change" }],
  isEnabled: [{ required: true, message: "请选择是否启用", trigger: "change" }],
  allowInvite: [{ required: true, message: "请选择是否允许邀请", trigger: "change" }],
  isFrozen: [{ required: true, message: "请选择是否冻结", trigger: "change" }],
  isFake: [{ required: true, message: "请选择是否假人", trigger: "change" }],
  isBanned: [{ required: true, message: "请选择是否禁止工作", trigger: "change" }],
  workLimit: [{ required: true, message: "工作限额不能为空", trigger: "change" }],
  accountStatus: [{ required: true, message: "请选择账户状态", trigger: "change" }],
  transactionStatus: [{ required: true, message: "请选择交易状态", trigger: "change" }],
  withdrawalStatus: [{ required: true, message: "请选择提现状态", trigger: "change" }],
  assistWithdrawalStatus: [{ required: true, message: "请选择协助金提现状态", trigger: "change" }],
  depositBlockWithdrawal: [{ required: true, message: "请选择充值后提现限制", trigger: "change" }],
  isWithdrawalNotification: [{ required: true, message: "请选择提现通知状态", trigger: "change" }],
  web3AuthEnabled: [{ required: true, message: "请选择 Web3 授权状态", trigger: "change" }],
  isInvalid: [{ required: true, message: "请选择是否无效", trigger: "change" }],
  isActivity: [{ required: true, message: "请选择是否活动", trigger: "change" }],
  verifyIdentityBeforeTask: [{ required: true, message: "请选择身份验证状态", trigger: "change" }],
  withdrawalPasswordFailLimit: [{ required: true, message: "失败次数限制不能为空", trigger: "change" }],
  withdrawalPasswordFailCount: [{ required: true, message: "连续失败次数不能为空", trigger: "change" }],
  maxSingleWithdrawal: [{ required: true, message: "单次最大提现金额不能为空", trigger: "change" }],
}));

const orderedYesNoOptions = computed(() =>
  [...(user_yes_no.value || [])].sort((left, right) => Number(right.value) - Number(left.value))
);
const orderedEnabledOptions = computed(() =>
  [...(sys_enabled.value || [])].sort((left, right) => Number(right.value) - Number(left.value))
);
const orderedGenderOptions = computed(() => {
  const order = ["2", "0", "1"];
  return [...(sys_user_sex.value || [])].sort(
    (left, right) => order.indexOf(String(left.value)) - order.indexOf(String(right.value))
  );
});

function handleClose() {
  if (submitting.value) return;
  formRef.value?.clearValidate?.();
  visible.value = false;
}

function handleCancel() {
  if (submitting.value) return;
  formRef.value?.clearValidate?.();
  visible.value = false;
}

function birthdayToTimestamp(value) {
  if (!value || typeof value === "number") return value;
  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return value;
  return Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3]), 12);
}

async function handleSubmit() {
  if (submitting.value) return;
  if (props.readonly) {
    visible.value = false;
    return;
  }
  const submitSession = formSession;
  const editing = isEditMode.value;
  submitting.value = true;
  try {
    await formRef.value?.validate?.();
    if (submitSession !== formSession || !props.modelValue) return;
    const payload = buildMemberSubmitPayload(props.formData, { birthdayToTimestamp });
    if (editing) {
      await updateOrderuser(payload);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addOrderuser(payload);
      proxy.$modal.msgSuccess("新增成功");
    }
    if (submitSession === formSession && props.modelValue) {
      visible.value = false;
    }
    emit("success");
  } catch (error) {
    if (!error?.errorFields) {
      proxy.$modal.msgError(error?.message || `${editing ? "修改" : "新增"}失败`);
    }
  } finally {
    submitting.value = false;
  }
}

watch(visible, (open) => {
  const session = ++formSession;
  if (!open) return;
  nextTick(() => {
    if (session === formSession && visible.value) {
      formRef.value?.clearValidate?.();
    }
  });
});

defineExpose({
  formRef,
});
</script>

<style scoped>
.full-width {
  width: 100%;
}

.drawer-footer {
  text-align: right;
}
</style>
