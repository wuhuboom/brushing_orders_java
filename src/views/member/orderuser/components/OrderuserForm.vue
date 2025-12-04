<template>
  <el-drawer
    :title="title"
    v-model="visible"
    size="50%"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      label-width="80px"
    >
      <!-- 第一排：用户名、手机号、邮箱、生日 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="formData.username" placeholder="请输入用户名" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="手机号" prop="phoneNumber">
            <el-input
              v-model="formData.phoneNumber"
              placeholder="请输入手机号"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="生日" prop="birthday">
            <el-date-picker
              clearable
              v-model="formData.birthday"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择生日"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 第二排：vipID、登录密码、交易密码、信誉分 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="会员等级" prop="vipId">
            <el-select
              v-model="formData.vipId"
              placeholder="请选择会员等级"
              clearable
            >
              <el-option
                v-for="item in levelList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="登录密码" prop="password">
            <el-input
              type="password"
              v-model="formData.password"
              placeholder="请输入登录密码"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="交易密码" prop="tradePassword">
            <el-input
              type="password"
              v-model="formData.tradePassword"
              placeholder="请输入交易密码"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="信誉分" prop="reputationScore">
            <el-input
              v-model="formData.reputationScore"
              placeholder="请输入信誉分"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 第三排：头像、上级ID、邀请码、性别 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="头像" prop="avatar">
            <image-upload v-model="formData.avatar" :limit="1" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="上级ID" prop="parentId">
            <el-input v-model="formData.parentId" placeholder="请输入上级ID" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="邀请码" prop="inviteCode">
            <el-input disabled v-model="formData.inviteCode" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="formData.gender">
              <el-radio
                v-for="dict in sys_user_sex"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 第四排：是否启用、允许邀请、是否冻结、是否假人 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="是否启用" prop="isEnabled">
            <el-radio-group v-model="formData.isEnabled">
              <el-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="允许邀请" prop="allowInvite">
            <el-radio-group v-model="formData.allowInvite">
              <el-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="是否冻结" prop="isFrozen">
            <el-radio-group v-model="formData.isFrozen">
              <el-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="是否假人" prop="isFake">
            <el-radio-group v-model="formData.isFake">
              <el-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 第五排：禁止工作、工作限额、账户状态、交易状态 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="禁止工作" prop="isBanned">
            <el-radio-group v-model="formData.isBanned">
              <el-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="工作限额" prop="workLimit">
            <el-input
              v-model="formData.workLimit"
              placeholder="请输入工作限额"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="账户状态" prop="accountStatus">
            <el-radio-group v-model="formData.accountStatus">
              <el-radio
                v-for="dict in sys_enabled"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="交易状态" prop="transactionStatus">
            <el-radio-group v-model="formData.transactionStatus">
              <el-radio
                v-for="dict in sys_enabled"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 第六排：提现状态、协助金提现状态、充值后禁止提现、关闭提现通知 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="提现状态" prop="withdrawalStatus">
            <el-radio-group v-model="formData.withdrawalStatus">
              <el-radio
                v-for="dict in sys_enabled"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="协助金提现状态" prop="assistWithdrawalStatus">
            <el-radio-group v-model="formData.assistWithdrawalStatus">
              <el-radio
                v-for="dict in sys_enabled"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="充值后禁止提现" prop="depositBlockWithdrawal">
            <el-radio-group v-model="formData.depositBlockWithdrawal">
              <el-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="关闭提现通知" prop="isWithdrawalNotification">
            <el-radio-group v-model="formData.isWithdrawalNotification">
              <el-radio
                v-for="dict in user_yes_no"
                :key="dict.value"
                :label="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 第七排：备注 -->
      <el-form-item label="备注" prop="remarks">
        <el-input
          v-model="formData.remarks"
          type="textarea"
          placeholder="请输入备注"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleSubmit">确定</el-button>
        <el-button @click="handleCancel">取消</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup name="OrderuserForm">
import { addOrderuser, updateOrderuser } from "@/api/member/orderuser";

const { proxy } = getCurrentInstance();
const { user_yes_no, sys_user_sex, sys_enabled } = proxy.useDict(
  "user_yes_no",
  "sys_user_sex",
  "sys_enabled"
);

// Props
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
});

// Emits
const emit = defineEmits(["update:modelValue", "success"]);

// 表单引用
const formRef = ref();

// 控制显示隐藏
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit("update:modelValue", val),
});

// 表单验证规则
const rules = {
  username: [{ required: true, message: "用户名不能为空", trigger: "blur" }],
  tradePassword: [
    { required: true, message: "交易密码不能为空", trigger: "blur" },
  ],
  password: [{ required: true, message: "密码不能为空", trigger: "blur" }],
  phoneNumber: [{ required: true, message: "手机号不能为空", trigger: "blur" }],
  vipId: [{ required: true, message: "会员等级不能为空", trigger: "blur" }],
  parentId: [{ required: true, message: "上级ID不能为空", trigger: "blur" }],
};

// 关闭抽屉
const handleClose = () => {
  visible.value = false;
};

// 取消操作
const handleCancel = () => {
  visible.value = false;
};

// 提交表单
const handleSubmit = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      if (props.formData.id != null) {
        // 修改
        updateOrderuser(props.formData).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          visible.value = false;
          emit("success");
        });
      } else {
        // 新增
        addOrderuser(props.formData).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          visible.value = false;
          emit("success");
        });
      }
    }
  });
};

// 暴露方法给父组件
defineExpose({
  formRef,
});
</script>
