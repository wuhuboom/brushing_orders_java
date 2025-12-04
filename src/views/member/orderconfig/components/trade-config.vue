<!-- trade-config-dialog.vue (子组件，全代码) -->
<template>
  <el-form
    ref="tradeFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 第一行：注册赠送金额，交易最低余额，会员提现状态 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="注册赠送金额" prop="registerBonusAmount">
          <el-input-number
            v-model="localForm.registerBonusAmount"
            :min="0"
            :precision="2"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入金额"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="交易最低余额" prop="minTradeBalance">
          <el-input-number
            v-model="localForm.minTradeBalance"
            :min="0"
            :precision="2"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入最低余额"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="会员提现状态">
          <el-radio-group v-model="localForm.memberWithdrawalStatus">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二行：会员提现最低信誉分，会员最低提现金额，会员最高提现金额 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item
          label="会员提现最低信誉分"
          prop="minCreditScoreForWithdrawal"
        >
          <el-input-number
            v-model="localForm.minCreditScoreForWithdrawal"
            :min="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入最低信誉分"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="会员最低提现金额" prop="minWithdrawalAmount">
          <el-input-number
            v-model="localForm.minWithdrawalAmount"
            :min="0"
            :precision="2"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入最低金额"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="会员最高提现金额" prop="maxWithdrawalAmount">
          <el-input-number
            v-model="localForm.maxWithdrawalAmount"
            :min="0"
            :precision="2"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入最高金额"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第三行：平台单日最高提现金额，提现手续费率，上级返佣百分比 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item
          label="平台单日最高提现金额"
          prop="platformDailyMaxWithdrawal"
        >
          <el-input-number
            v-model="localForm.platformDailyMaxWithdrawal"
            :min="0"
            :precision="2"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入单日最高金额"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="提现手续费率 (%)" prop="withdrawalFeeRate">
          <el-input-number
            v-model="localForm.withdrawalFeeRate"
            :min="0"
            :max="100"
            :precision="2"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入费率"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="上级返佣百分比 (%)" prop="parentRebatePercentage">
          <el-input-number
            v-model="localForm.parentRebatePercentage"
            :min="0"
            :max="100"
            :precision="2"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入返佣百分比"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第四行：匹配范围，服务时间范围 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="匹配范围 (%)" prop="matchRangePercentage">
          <div style="display: flex; align-items: center; width: 100%">
            <el-input-number
              v-model="minRange"
              @change="updateRange"
              :min="0"
              :max="100"
              :precision="0"
              controls-position="right"
              placeholder="0"
              style="width: 49%; margin-right: 4px"
            />
            <span style="line-height: 32px; color: #999; white-space: nowrap"
              >-</span
            >
            <el-input-number
              v-model="maxRange"
              @change="updateRange"
              :min="0"
              :max="100"
              :precision="0"
              controls-position="right"
              placeholder="100"
              style="width: 49%; margin-left: 4px"
            />
          </div>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="服务时间范围" prop="serviceTimeRange">
          <el-time-picker
            v-model="localForm.serviceTimeRange"
            is-range
            range-separator="-"
            format="HH:mm"
            value-format="HH:mm"
            class="time-picker-narrow"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="交易时间范围" prop="tradeTimeRange">
          <el-time-picker
            v-model="localForm.tradeTimeRange"
            is-range
            range-separator="-"
            format="HH:mm"
            value-format="HH:mm"
            class="time-picker-narrow"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第五行：交易时间范围，充值后禁止提现 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="充值后禁止提现">
          <el-radio-group v-model="localForm.prohibitWithdrawalAfterRecharge">
            <el-radio label="0">是</el-radio>
            <el-radio label="1">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="提现是否限制等级最低余额">
          <el-radio-group v-model="localForm.withdrawalRestrictLevelMinBalance">
            <el-radio label="0">是</el-radio>
            <el-radio label="1">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="提现时间范围" prop="withdrawalTimeRange">
          <el-time-picker
            v-model="localForm.withdrawalTimeRange"
            is-range
            range-separator="-"
            format="HH:mm"
            value-format="HH:mm"
            class="time-picker-narrow"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第七行：是否自动提交任务，开始任务延迟毫秒，提交任务延迟毫秒 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="是否自动提交任务">
          <el-radio-group v-model="localForm.autoSubmitTask">
            <el-radio label="0">是</el-radio>
            <el-radio label="1">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="开始任务延迟毫秒" prop="startTaskDelayMs">
          <el-input-number
            v-model="localForm.startTaskDelayMs"
            :min="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入延迟毫秒"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="提交任务延迟毫秒" prop="submitTaskDelayMs">
          <el-input-number
            v-model="localForm.submitTaskDelayMs"
            :min="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入延迟毫秒"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第八行：订单过期时间，提交任务是否锁定额外佣金，是否允许修改提现地址 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="订单过期时间（秒）" prop="orderExpireSeconds">
          <el-input-number
            v-model="localForm.orderExpireSeconds"
            :min="0"
            controls-position="right"
            style="width: 100%"
            placeholder="0 表示不开启"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="提交任务是否锁定额外佣金">
          <el-radio-group v-model="localForm.lockExtraCommissionOnSubmit">
            <el-radio label="0">是</el-radio>
            <el-radio label="1">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="是否允许修改提现地址">
          <el-radio-group v-model="localForm.allowModifyWithdrawalAddress">
            <el-radio label="0">是</el-radio>
            <el-radio label="1">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第九行：提现需要完成的任务组数，充值赠送交易类型，任务进度是否计算连单明细 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item
          label="提现需要完成的任务组数"
          prop="requiredTaskGroupsForWithdrawal"
        >
          <el-input-number
            v-model="localForm.requiredTaskGroupsForWithdrawal"
            :min="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入任务组数"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="充值赠送交易类型">
          <el-select
            v-model="localForm.rechargeBonusTradeType"
            placeholder="请选择交易类型"
          >
            <el-option label="赠送" value="bonus" />
            <el-option label="扣款" value="deduction" />
            <el-option label="充值" value="recharge" />
            <el-option label="提现中" value="withdrawing" />
            <el-option label="提现解冻" value="withdrawalUnfreeze" />
            <el-option label="提现" value="withdrawal" />
            <el-option label="任务" value="task" />
            <el-option label="本金返还" value="principalReturn" />
            <el-option label="返佣" value="rebate" />
            <el-option label="下级返佣" value="subRebate" />
            <el-option label="签到" value="signIn" />
            <el-option label="手续费" value="fee" />
            <el-option label="存款" value="deposit" />
            <el-option label="奖金" value="bonus" />
            <el-option label="底薪" value="baseSalary" />
            <el-option label="援助金" value="aid" />
            <el-option label="注册赠送" value="registerBonus" />
            <el-option label="商品分润" value="productShare" />
            <el-option label="任务奖励" value="taskReward" />
            <el-option label="余额宝转出" value="balanceOut" />
            <el-option label="余额宝转入" value="balanceIn" />
            <el-option label="工作奖金" value="workBonus" />
            <el-option label="升级奖金" value="upgradeBonus" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="任务进度是否计算连单明细">
          <el-radio-group
            v-model="localForm.includeContinuousOrderInTaskProgress"
          >
            <el-radio label="0">是</el-radio>
            <el-radio label="1">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第十行：任务进度是否包含待提交任务，禁止客户提现所需交易密码失败次数 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="任务进度是否包含待提交任务">
          <el-radio-group v-model="localForm.includePendingTasksInProgress">
            <el-radio label="0">是</el-radio>
            <el-radio label="1">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item
          label="禁止客户提现所需交易密码失败次数 (0-不限制)"
          prop="maxPasswordFailuresForWithdrawal"
        >
          <el-input-number
            v-model="localForm.maxPasswordFailuresForWithdrawal"
            :min="0"
            controls-position="right"
            style="width: 100%"
            placeholder="请输入失败次数"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup>
import { ref, reactive, watch, nextTick } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);

const tradeFormRef = ref();
const localForm = reactive({ ...props.form });

// 初始化后立即删除通用字段，确保 localForm 干净
["id", "type", "name", "createTime", "updateTime", "content"].forEach((key) => {
  delete localForm[key];
});

// 新增：范围输入的 ref（数字类型）
const minRange = ref(0);
const maxRange = ref(100);

// 新增：更新范围方法（校验并组合到 localForm）
const updateRange = () => {
  const min = Number(minRange.value);
  const max = Number(maxRange.value);
  if (isNaN(min) || isNaN(max) || min < 0 || max > 100 || min >= max) {
    localForm.matchRangePercentage = ""; // 无效时清空
    ElMessage.warning("请输入有效的范围 (e.g., 0-100, min < max)");
    return;
  }
  localForm.matchRangePercentage = `${min}-${max}`;
  // 手动触发验证（针对 prop="matchRangePercentage"）
  nextTick(() => {
    tradeFormRef.value?.validateField?.("matchRangePercentage");
  });
};

// 所有规则都在子组件中定义（保持不变，但注意时间范围规则可能需调整为 serviceTimeRange 等）
const localRules = reactive({
  registerBonusAmount: [
    { required: true, message: "请输入注册赠送金额", trigger: "blur" },
  ],
  minTradeBalance: [
    { required: true, message: "请输入交易最低余额", trigger: "blur" },
  ],
  memberWithdrawalStatus: [
    { required: true, message: "请选择会员提现状态", trigger: "change" },
  ],
  minCreditScoreForWithdrawal: [
    { required: true, message: "请输入最低信誉分", trigger: "blur" },
  ],
  minWithdrawalAmount: [
    { required: true, message: "请输入最低提现金额", trigger: "blur" },
  ],
  maxWithdrawalAmount: [
    { required: true, message: "请输入最高提现金额", trigger: "blur" },
  ],
  platformDailyMaxWithdrawal: [
    { required: true, message: "请输入单日最高提现金额", trigger: "blur" },
  ],
  withdrawalFeeRate: [
    { required: true, message: "请输入提现手续费率", trigger: "blur" },
  ],
  parentRebatePercentage: [
    { required: true, message: "请输入上级返佣百分比", trigger: "blur" },
  ],
  matchRangePercentage: [
    {
      required: true,
      message: "请输入匹配范围 (e.g., 0-100)",
      trigger: "blur",
    },
  ],
  serviceTimeRange: [
    // 修正：匹配模板中的 serviceTimeRange
    { required: true, message: "请选择服务时间范围", trigger: "change" },
  ],
  tradeTimeRange: [
    // 修正：匹配模板
    { required: true, message: "请选择交易时间范围", trigger: "change" },
  ],
  prohibitWithdrawalAfterRecharge: [
    { required: true, message: "请选择充值后禁止提现", trigger: "change" },
  ],
  withdrawalRestrictLevelMinBalance: [
    {
      required: true,
      message: "请选择是否限制等级最低余额",
      trigger: "change",
    },
  ],
  withdrawalTimeRange: [
    // 修正：匹配模板
    { required: true, message: "请选择提现时间范围", trigger: "change" },
  ],
  autoSubmitTask: [
    { required: true, message: "请选择是否自动提交任务", trigger: "change" },
  ],
  startTaskDelayMs: [
    { required: true, message: "请输入开始任务延迟毫秒", trigger: "blur" },
  ],
  submitTaskDelayMs: [
    { required: true, message: "请输入提交任务延迟毫秒", trigger: "blur" },
  ],
  orderExpireSeconds: [
    { required: true, message: "请输入订单过期时间", trigger: "blur" },
  ],
  lockExtraCommissionOnSubmit: [
    { required: true, message: "请选择是否锁定额外佣金", trigger: "change" },
  ],
  allowModifyWithdrawalAddress: [
    {
      required: true,
      message: "请选择是否允许修改提现地址",
      trigger: "change",
    },
  ],
  requiredTaskGroupsForWithdrawal: [
    { required: true, message: "请输入任务组数", trigger: "blur" },
  ],
  rechargeBonusTradeType: [
    { required: true, message: "请选择充值赠送交易类型", trigger: "change" },
  ],
  includeContinuousOrderInTaskProgress: [
    { required: true, message: "请选择是否计算连单明细", trigger: "change" },
  ],
  includePendingTasksInProgress: [
    { required: true, message: "请选择是否包含待提交任务", trigger: "change" },
  ],
  maxPasswordFailuresForWithdrawal: [
    { required: true, message: "请输入密码失败次数", trigger: "blur" },
  ],
});

// 初始化：监听父 form 变化，parse content 到 localForm（仅 trade 字段）
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        // 只合并 trade 特定字段，排除通用字段
        const tradeFields = {
          registerBonusAmount: parsed.registerBonusAmount,
          minTradeBalance: parsed.minTradeBalance,
          memberWithdrawalStatus: parsed.memberWithdrawalStatus,
          minCreditScoreForWithdrawal: parsed.minCreditScoreForWithdrawal,
          minWithdrawalAmount: parsed.minWithdrawalAmount,
          maxWithdrawalAmount: parsed.maxWithdrawalAmount,
          platformDailyMaxWithdrawal: parsed.platformDailyMaxWithdrawal,
          withdrawalFeeRate: parsed.withdrawalFeeRate,
          parentRebatePercentage: parsed.parentRebatePercentage,
          matchRangePercentage: parsed.matchRangePercentage,
          serviceTimeRange: parsed.serviceTimeRange,
          tradeTimeRange: parsed.tradeTimeRange,
          prohibitWithdrawalAfterRecharge:
            parsed.prohibitWithdrawalAfterRecharge,
          withdrawalRestrictLevelMinBalance:
            parsed.withdrawalRestrictLevelMinBalance,
          withdrawalTimeRange: parsed.withdrawalTimeRange,
          autoSubmitTask: parsed.autoSubmitTask,
          startTaskDelayMs: parsed.startTaskDelayMs,
          submitTaskDelayMs: parsed.submitTaskDelayMs,
          orderExpireSeconds: parsed.orderExpireSeconds,
          lockExtraCommissionOnSubmit: parsed.lockExtraCommissionOnSubmit,
          allowModifyWithdrawalAddress: parsed.allowModifyWithdrawalAddress,
          requiredTaskGroupsForWithdrawal:
            parsed.requiredTaskGroupsForWithdrawal,
          rechargeBonusTradeType: parsed.rechargeBonusTradeType,
          includeContinuousOrderInTaskProgress:
            parsed.includeContinuousOrderInTaskProgress,
          includePendingTasksInProgress: parsed.includePendingTasksInProgress,
          maxPasswordFailuresForWithdrawal:
            parsed.maxPasswordFailuresForWithdrawal,
        };
        Object.assign(localForm, tradeFields);
        // 从 matchRangePercentage 拆分初始化 min/max
        if (localForm.matchRangePercentage) {
          const parts = localForm.matchRangePercentage.split("-");
          minRange.value = Number(parts[0]) || 0;
          maxRange.value = Number(parts[1]) || 100;
        }
      } catch (e) {
        ElMessage.error("解析配置失败");
      }
    }
  },
  { immediate: true }
);

// 更新父 form：localForm 变化时，过滤不想要的字段后 stringify 到 content
watch(
  localForm,
  () => {
    // 创建只含 trade 字段的 toSave
    const toSave = {
      registerBonusAmount: localForm.registerBonusAmount,
      minTradeBalance: localForm.minTradeBalance,
      memberWithdrawalStatus: localForm.memberWithdrawalStatus,
      minCreditScoreForWithdrawal: localForm.minCreditScoreForWithdrawal,
      minWithdrawalAmount: localForm.minWithdrawalAmount,
      maxWithdrawalAmount: localForm.maxWithdrawalAmount,
      platformDailyMaxWithdrawal: localForm.platformDailyMaxWithdrawal,
      withdrawalFeeRate: localForm.withdrawalFeeRate,
      parentRebatePercentage: localForm.parentRebatePercentage,
      matchRangePercentage: localForm.matchRangePercentage,
      serviceTimeRange: localForm.serviceTimeRange,
      tradeTimeRange: localForm.tradeTimeRange,
      prohibitWithdrawalAfterRecharge:
        localForm.prohibitWithdrawalAfterRecharge,
      withdrawalRestrictLevelMinBalance:
        localForm.withdrawalRestrictLevelMinBalance,
      withdrawalTimeRange: localForm.withdrawalTimeRange,
      autoSubmitTask: localForm.autoSubmitTask,
      startTaskDelayMs: localForm.startTaskDelayMs,
      submitTaskDelayMs: localForm.submitTaskDelayMs,
      orderExpireSeconds: localForm.orderExpireSeconds,
      lockExtraCommissionOnSubmit: localForm.lockExtraCommissionOnSubmit,
      allowModifyWithdrawalAddress: localForm.allowModifyWithdrawalAddress,
      requiredTaskGroupsForWithdrawal:
        localForm.requiredTaskGroupsForWithdrawal,
      rechargeBonusTradeType: localForm.rechargeBonusTradeType,
      includeContinuousOrderInTaskProgress:
        localForm.includeContinuousOrderInTaskProgress,
      includePendingTasksInProgress: localForm.includePendingTasksInProgress,
      maxPasswordFailuresForWithdrawal:
        localForm.maxPasswordFailuresForWithdrawal,
    };

    emit("update:form", {
      ...props.form,
      content: JSON.stringify(toSave),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  tradeFormRef.value.validate((valid) => {
    if (valid) {
      emit("submit");
    }
  });
}

// 取消
function handleCancel() {
  emit("cancel");
}
</script>

<style scoped>
/* 如需样式，可添加 */
</style>
