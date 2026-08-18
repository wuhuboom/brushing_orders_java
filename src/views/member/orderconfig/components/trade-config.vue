<template>
  <a-form
    ref="tradeFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    size="large"
    class="config-form"
  >
    <a-row :gutter="[24, 0]">
      <a-col
        v-for="item in formItems"
        :key="item.prop"
        :xs="24"
        :sm="12"
        :lg="8"
        :xl="item.span || 6"
      >
        <a-form-item :label="item.label" :name="item.prop">
          <a-input-number
            v-if="item.type === 'number'"
            v-model:value="localForm[item.prop]"
            :min="item.min ?? 0"
            :max="item.max"
            :precision="item.precision"
            :placeholder="item.placeholder"
            :addon-after="item.suffix"
            class="full-width"
          />

          <a-radio-group
            v-else-if="item.type === 'radio'"
            v-model:value="localForm[item.prop]"
          >
            <a-radio v-for="option in item.options" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-radio>
          </a-radio-group>

          <a-select
            v-else-if="item.type === 'select'"
            v-model:value="localForm[item.prop]"
            :placeholder="item.placeholder"
            allow-clear
            class="full-width"
          >
            <a-select-option v-for="option in item.options" :key="`${option.value}-${option.label}`" :value="option.value">
              {{ option.label }}
            </a-select-option>
          </a-select>

          <a-time-range-picker
            v-else-if="item.type === 'timeRange'"
            v-model:value="localForm[item.prop]"
            format="HH:mm"
            value-format="HH:mm"
            class="full-width"
          />

          <div v-else-if="item.type === 'range'" class="range-input">
            <a-input-number
              v-model:value="minRange"
              :min="1"
              :max="100"
              :precision="0"
              placeholder="1"
              @change="updateRange"
            />
            <span>~</span>
            <a-input-number
              v-model:value="maxRange"
              :min="1"
              :max="100"
              :precision="0"
              placeholder="100"
              @change="updateRange"
            />
          </div>
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
import { nextTick, reactive, ref, watch } from "vue"
import { message } from "ant-design-vue"

const props = defineProps({
  form: {
    type: Object,
    default: () => ({})
  },
  loading: Boolean
})

const emit = defineEmits(["update:form", "submit", "cancel"])

const yesNoOptions = [
  { label: "否", value: "1" },
  { label: "是", value: "0" }
]

const enabledOptions = [
  { label: "禁用", value: "1" },
  { label: "启用", value: "0" }
]

const tradeTypeOptions = [
  { label: "赠送", value: "bonus" },
  { label: "扣款", value: "deduction" },
  { label: "充值", value: "recharge" },
  { label: "提现中", value: "withdrawing" },
  { label: "提现解冻", value: "withdrawalUnfreeze" },
  { label: "提现", value: "withdrawal" },
  { label: "任务", value: "task" },
  { label: "本金返还", value: "principalReturn" },
  { label: "返佣", value: "rebate" },
  { label: "下级返佣", value: "subRebate" },
  { label: "签到", value: "signIn" },
  { label: "手续费", value: "fee" },
  { label: "存款", value: "deposit" },
  { label: "奖金", value: "bonus" },
  { label: "底薪", value: "baseSalary" },
  { label: "援助金", value: "aid" },
  { label: "注册赠送", value: "registerBonus" },
  { label: "商品分润", value: "productShare" },
  { label: "任务奖励", value: "taskReward" },
  { label: "余额宝转出", value: "balanceOut" },
  { label: "余额宝转入", value: "balanceIn" },
  { label: "工作奖金", value: "workBonus" },
  { label: "升级奖金", value: "upgradeBonus" },
  { label: "其他", value: "other" }
]

const formItems = [
  { prop: "registerBonusAmount", label: "注册赠送金额", type: "number", precision: 2, placeholder: "请输入金额" },
  { prop: "minTradeBalance", label: "交易最低余额", type: "number", precision: 2, placeholder: "请输入最低余额" },
  { prop: "memberWithdrawalStatus", label: "会员提现状态", type: "radio", options: enabledOptions },
  { prop: "minCreditScoreForWithdrawal", label: "会员提现最低信誉分", type: "number", placeholder: "请输入最低信誉分" },
  { prop: "minWithdrawalAmount", label: "会员最低提现金额", type: "number", precision: 2, placeholder: "请输入最低金额" },
  { prop: "maxWithdrawalAmount", label: "会员最高提现金额", type: "number", precision: 2, placeholder: "请输入最高金额" },
  { prop: "platformDailyMaxWithdrawal", label: "平台单日最高提现金额", type: "number", precision: 2, placeholder: "请输入单日最高金额" },
  { prop: "withdrawalFeeRate", label: "提现手续费率", type: "number", max: 100, precision: 2, suffix: "%", placeholder: "请输入费率" },
  { prop: "parentRebatePercentage", label: "上级返佣百分比", type: "number", max: 100, precision: 2, suffix: "%", placeholder: "请输入返佣百分比" },
  { prop: "matchRangePercentage", label: "匹配范围(%)", type: "range" },
  { prop: "serviceTimeRange", label: "服务时间范围", type: "timeRange" },
  { prop: "tradeTimeRange", label: "交易时间范围", type: "timeRange" },
  { prop: "prohibitWithdrawalAfterRecharge", label: "充值后禁止提现", type: "radio", options: yesNoOptions },
  { prop: "withdrawalRestrictLevelMinBalance", label: "提现是否限制等级最低余额", type: "radio", options: yesNoOptions },
  { prop: "withdrawalTimeRange", label: "提现时间范围", type: "timeRange" },
  { prop: "autoSubmitTask", label: "是否自动提交任务", type: "radio", options: yesNoOptions },
  { prop: "startTaskDelayMs", label: "开始任务延迟毫秒", type: "number", placeholder: "请输入延迟毫秒" },
  { prop: "submitTaskDelayMs", label: "提交任务延迟毫秒", type: "number", placeholder: "请输入延迟毫秒" },
  { prop: "orderExpireSeconds", label: "订单过期时间（单位秒），为0则表示不开启", type: "number", placeholder: "请输入" },
  { prop: "lockExtraCommissionOnSubmit", label: "提交任务是否锁定额外佣金", type: "radio", options: yesNoOptions },
  { prop: "allowModifyWithdrawalAddress", label: "是否允许修改提现地址", type: "radio", options: yesNoOptions },
  { prop: "requiredTaskGroupsForWithdrawal", label: "提现需要完成的任务组数", type: "number", placeholder: "请输入任务组数" },
  { prop: "rechargeBonusTradeType", label: "充值赠送交易类型", type: "select", options: tradeTypeOptions, placeholder: "请选择交易类型" },
  { prop: "includeContinuousOrderInTaskProgress", label: "任务进度是否计算连单明细", type: "radio", options: yesNoOptions },
  { prop: "includePendingTasksInProgress", label: "任务进度是否包含待提交任务", type: "radio", options: yesNoOptions },
  { prop: "maxPasswordFailuresForWithdrawal", label: "禁止客户提现所需交易密码失败次数(0-不限制)", type: "number", required: false, placeholder: "请输入失败次数" }
]

const formFieldKeys = formItems.map(item => item.prop)
const tradeFormRef = ref()
const localForm = reactive({ ...props.form })
const minRange = ref(1)
const maxRange = ref(100)

const localRules = reactive(
  formItems.reduce((rules, item) => {
    if (item.required === false) {
      return rules
    }
    const trigger = item.type === "number" || item.type === "range" ? "blur" : "change"
    const verb = ["radio", "select", "timeRange"].includes(item.type) ? "请选择" : "请输入"
    rules[item.prop] = [{ required: true, message: `${verb}${item.label}`, trigger }]
    return rules
  }, {})
)

;["id", "type", "name", "createTime", "updateTime", "content"].forEach(key => {
  delete localForm[key]
})

function updateRange() {
  const min = Number(minRange.value)
  const max = Number(maxRange.value)
  if (Number.isNaN(min) || Number.isNaN(max) || min <= 0 || max > 100 || min > max) {
    localForm.matchRangePercentage = ""
    message.warning("请输入 1-100 的有效范围，且最小值不大于最大值")
    return
  }
  localForm.matchRangePercentage = `${min}-${max}`
  nextTick(() => {
    tradeFormRef.value?.validateFields?.(["matchRangePercentage"])
  })
}

watch(
  () => props.form.content,
  newContent => {
    if (!newContent) {
      return
    }
    try {
      const parsed = JSON.parse(newContent)
      const tradeFields = formFieldKeys.reduce((fields, key) => {
        if (Object.prototype.hasOwnProperty.call(parsed, key)) {
          fields[key] = parsed[key]
        }
        return fields
      }, {})
      Object.assign(localForm, tradeFields)
      if (localForm.matchRangePercentage) {
        const [min, max] = String(localForm.matchRangePercentage).split("-")
        minRange.value = Number(min) || 1
        maxRange.value = Number(max) || 100
      }
    } catch (e) {
      message.error("解析配置失败")
    }
  },
  { immediate: true }
)

watch(
  localForm,
  () => {
    const toSave = formFieldKeys.reduce((fields, key) => {
      fields[key] = localForm[key]
      return fields
    }, {})

    emit("update:form", {
      ...props.form,
      content: JSON.stringify(toSave)
    })
  },
  { deep: true }
)

function handleSubmit() {
  tradeFormRef.value?.validate?.().then(() => {
    emit("submit")
  }).catch(() => {})
}

function handleCancel() {
  emit("cancel")
}

defineExpose({ handleSubmit, handleCancel })
</script>

<style scoped>
.full-width {
  width: 100%;
}

.range-input {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.range-input :deep(.ant-input-number) {
  flex: 1;
}
</style>
