<template>
  <div class="app-container">
    <el-card>
      <div class="center-container">
        <el-form
          ref="tardeconfigRef"
          :model="form"
          :rules="rules"
          label-width="150px"
          label-position="top"
          class="space-y-4"
        >
          <el-form-item
            :label="$t('tradeconfig.withdrawEnabled')"
            prop="withdrawEnabled"
          >
            <el-switch
              v-model="form.withdrawEnabled"
              active-value="0"
              inactive-value="1"
            />
          </el-form-item>
          <el-form-item
            :label="$t('tradeconfig.minWithdrawCreditScore')"
            prop="minWithdrawCreditScore"
          >
            <el-input-number
              v-model="form.minWithdrawCreditScore"
              :min="1"
              :max="100"
              :step="1"
              :precision="0"
              step-strictly
              style="width: 20%"
              class="input-number-wide"
              :placeholder="$t('tradeconfig.enterMinWithdrawCreditScore')"
            />
          </el-form-item>
          <el-form-item
            :label="$t('tradeconfig.withdrawFeePercent')"
            prop="withdrawFeePercent"
          >
            <el-input-number
              v-model="form.withdrawFeePercent"
              :min="0"
              :max="100"
              :precision="2"
              style="width: 20%"
              class="input-number-wide"
              :placeholder="$t('tradeconfig.enterWithdrawFeePercent')"
            />
          </el-form-item>
          <el-form-item
            :label="$t('tradeconfig.level1CommissionPercent')"
            prop="level1CommissionPercent"
          >
            <el-input-number
              v-model="form.level1CommissionPercent"
              :min="0"
              :max="100"
              style="width: 20%"
              :precision="2"
              class="input-number-wide"
              :placeholder="$t('tradeconfig.enterLevel1CommissionPercent')"
            />
          </el-form-item>
          <el-form-item
            :label="$t('tradeconfig.level2CommissionPercent')"
            prop="level2CommissionPercent"
          >
            <el-input-number
              v-model="form.level2CommissionPercent"
              :min="0"
              :max="100"
              style="width: 20%"
              :precision="2"
              class="input-number-wide"
              :placeholder="$t('tradeconfig.enterLevel2CommissionPercent')"
            />
          </el-form-item>
          <el-form-item
            :label="$t('tradeconfig.level3CommissionPercent')"
            prop="level3CommissionPercent"
          >
            <el-input-number
              v-model="form.level3CommissionPercent"
              :min="0"
              :max="100"
              style="width: 20%"
              :precision="2"
              class="input-number-wide"
              :placeholder="$t('tradeconfig.enterLevel3CommissionPercent')"
            />
          </el-form-item>
          <el-form-item
            :label="$t('tradeconfig.level4CommissionPercent')"
            prop="level4CommissionPercent"
          >
            <el-input-number
              v-model="form.level4CommissionPercent"
              :min="0"
              :max="100"
              style="width: 20%"
              :precision="2"
              class="input-number-wide"
              :placeholder="$t('tradeconfig.enterLevel4CommissionPercent')"
            />
          </el-form-item>
          <el-form-item
            :label="$t('tradeconfig.level5CommissionPercent')"
            prop="level5CommissionPercent"
          >
            <el-input-number
              v-model="form.level5CommissionPercent"
              :min="0"
              :max="100"
              :precision="2"
              style="width: 20%"
              class="input-number-wide"
              :placeholder="$t('tradeconfig.enterLevel5CommissionPercent')"
            />
          </el-form-item>
          <el-form-item :label="$t('tradeconfig.tradeRange')" prop="tradeRange">
            <el-slider
              v-model="form.tradeRange"
              range
              :min="0"
              :max="100"
              :step="0.1"
              show-input
              class="slider-medium"
            />
          </el-form-item>
          <el-form-item
            :label="$t('tradeconfig.rechargeTimeRange')"
            style="width: 20%"
            prop="rechargeTimeRange"
          >
            <el-time-picker
              v-model="form.rechargeTimeRange"
              is-range
              range-separator="-"
              :start-placeholder="$t('common.startTime')"
              :end-placeholder="$t('common.endTime')"
              format="HH:mm"
              value-format="HH:mm"
              class="time-picker-narrow"
            />
          </el-form-item>
          <el-form-item
            style="width: 20%"
            :label="$t('tradeconfig.withdrawTimeRange')"
            prop="withdrawTimeRange"
          >
            <el-time-picker
              v-model="form.withdrawTimeRange"
              is-range
              range-separator="-"
              :start-placeholder="$t('common.startTime')"
              :end-placeholder="$t('common.endTime')"
              format="HH:mm"
              value-format="HH:mm"
              class="time-picker-narrow"
            />
          </el-form-item>

          <el-form-item
            :label="$t('tradeconfig.orderTimeRange')"
            style="width: 20%"
            prop="orderTimeRange"
          >
            <el-time-picker
              v-model="form.orderTimeRange"
              is-range
              range-separator="-"
              :start-placeholder="$t('common.startTime')"
              :end-placeholder="$t('common.endTime')"
              format="HH:mm"
              value-format="HH:mm"
              class="time-picker-narrow"
            />
          </el-form-item>
          <el-form-item
            :label="$t('tradeconfig.workTimeRange')"
            style="width: 20%"
            prop="workTimeRange"
          >
            <el-time-picker
              v-model="form.workTimeRange"
              is-range
              range-separator="-"
              :start-placeholder="$t('common.startTime')"
              :end-placeholder="$t('common.endTime')"
              format="HH:mm"
              value-format="HH:mm"
              class="time-picker-narrow"
            />
          </el-form-item>
        </el-form>
        <div class="dialog-footer" style="margin-top: 20px; text-align: right">
          <el-button type="primary" @click="submitForm">{{
            $t("common.save")
          }}</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, toRefs, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import {
  getTardeconfig,
  addTardeconfig,
  updateTardeconfig,
} from "@/api/set/tardeconfig";
import { getCurrentInstance } from "vue";

const { t } = useI18n();

const { proxy } = getCurrentInstance();

const open = ref(true);
const title = ref(t("tradeconfig.title"));
const loading = ref(false);

const data = reactive({
  form: {
    id: null,
    withdrawEnabled: "0",
    minUserBalance: null,
    minWithdrawCreditScore: null,
    minWithdrawAmount: null,
    maxWithdrawAmount: null,
    dailyWithdrawLimit: null,
    withdrawFeePercent: null,
    level1CommissionPercent: null,
    level2CommissionPercent: null,
    level3CommissionPercent: null,
    level4CommissionPercent: null,
    level5CommissionPercent: null,
    tradeRange: [0, 100],
    rechargeTimeRange: ["00:00", "23:59"],
    withdrawTimeRange: ["00:00", "23:59"],
    orderTimeRange: ["00:00", "23:59"],
    workTimeRange: ["00:00", "23:59"],
  },
  rules: {
    minUserBalance: [
      {
        type: "number",
        min: 0,
        message: t("tradeconfig.minUserBalanceMin"),
        trigger: "blur",
      },
    ],
    minWithdrawCreditScore: [
      {
        required: true,
        type: "number",
        min: 1,
        max: 100,
        message: t("tradeconfig.minWithdrawCreditScoreRange"),
        trigger: "blur",
      },
    ],
    minWithdrawAmount: [
      {
        type: "number",
        min: 0,
        message: t("tradeconfig.minWithdrawAmountMin"),
        trigger: "blur",
      },
    ],
    maxWithdrawAmount: [
      {
        type: "number",
        min: 0,
        message: t("tradeconfig.maxWithdrawAmountMin"),
        trigger: "blur",
      },
    ],
    dailyWithdrawLimit: [
      {
        type: "number",
        min: 0,
        message: t("tradeconfig.dailyWithdrawLimitMin"),
        trigger: "blur",
      },
    ],
    withdrawFeePercent: [
      {
        type: "number",
        min: 0,
        max: 100,
        message: t("tradeconfig.withdrawFeePercentRange"),
        trigger: "blur",
      },
    ],
    level1CommissionPercent: [
      {
        type: "number",
        min: 0,
        max: 100,
        message: t("tradeconfig.levelCommissionPercentRange"),
        trigger: "blur",
      },
    ],
    level2CommissionPercent: [
      {
        type: "number",
        min: 0,
        max: 100,
        message: t("tradeconfig.levelCommissionPercentRange"),
        trigger: "blur",
      },
    ],
    level3CommissionPercent: [
      {
        type: "number",
        min: 0,
        max: 100,
        message: t("tradeconfig.levelCommissionPercentRange"),
        trigger: "blur",
      },
    ],
    level4CommissionPercent: [
      {
        type: "number",
        min: 0,
        max: 100,
        message: t("tradeconfig.levelCommissionPercentRange"),
        trigger: "blur",
      },
    ],
    level5CommissionPercent: [
      {
        type: "number",
        min: 0,
        max: 100,
        message: t("tradeconfig.levelCommissionPercentRange"),
        trigger: "blur",
      },
    ],
    tradeRange: [
      {
        type: "array",
        message: t("tradeconfig.tradeRangeRequired"),
        trigger: "change",
      },
    ],
    rechargeTimeRange: [
      {
        type: "array",
        message: t("tradeconfig.rechargeTimeRangeRequired"),
        trigger: "change",
      },
    ],
    withdrawTimeRange: [
      {
        type: "array",
        message: t("tradeconfig.withdrawTimeRangeRequired"),
        trigger: "change",
      },
    ],
    orderTimeRange: [
      {
        type: "array",
        message: t("tradeconfig.orderTimeRangeRequired"),
        trigger: "change",
      },
    ],
    workTimeRange: [
      {
        type: "array",
        message: t("tradeconfig.workTimeRangeRequired"),
        trigger: "change",
      },
    ],
  },
});

const { form, rules } = toRefs(data);

// 页面加载时获取 ID=1 的配置
onMounted(() => {
  loading.value = true;
  getTardeconfig(1)
    .then((response) => {
      if (response.data) {
        form.value = {
          ...response.data,
          tradeRange: [
            response.data.tradeRangePercentMin || 0,
            response.data.tradeRangePercentMax || 100,
          ],
          rechargeTimeRange: [
            response.data.rechargeTimeStart || "00:00",
            response.data.rechargeTimeEnd || "23:59",
          ],
          withdrawTimeRange: [
            response.data.withdrawTimeStart || "00:00",
            response.data.withdrawTimeEnd || "23:59",
          ],
          orderTimeRange: [
            response.data.orderTimeStart || "00:00",
            response.data.orderTimeEnd || "23:59",
          ],
          workTimeRange: [
            response.data.workTimeStart || "00:00",
            response.data.workTimeEnd || "23:59",
          ],
        };
        title.value = t("tradeconfig.editTitle");
      }
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
});

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    withdrawEnabled: "0",
    minUserBalance: null,
    minWithdrawCreditScore: null,
    minWithdrawAmount: null,
    maxWithdrawAmount: null,
    dailyWithdrawLimit: null,
    withdrawFeePercent: null,
    level1CommissionPercent: null,
    level2CommissionPercent: null,
    level3CommissionPercent: null,
    level4CommissionPercent: null,
    level5CommissionPercent: null,
    tradeRange: [0, 100],
    rechargeTimeRange: ["00:00", "23:59"],
    withdrawTimeRange: ["00:00", "23:59"],
    orderTimeRange: ["00:00", "23:59"],
    workTimeRange: ["00:00", "23:59"],
  };
  proxy.resetForm("tardeconfigRef");
}

// 提交表单
function submitForm() {
  proxy.$refs["tardeconfigRef"].validate((valid) => {
    if (valid) {
      const submitData = {
        ...form.value,
        tradeRangePercentMin: form.value.tradeRange[0],
        tradeRangePercentMax: form.value.tradeRange[1],
        rechargeTimeStart: form.value.rechargeTimeRange[0],
        rechargeTimeEnd: form.value.rechargeTimeRange[1],
        withdrawTimeStart: form.value.withdrawTimeRange[0],
        withdrawTimeEnd: form.value.withdrawTimeRange[1],
        orderTimeStart: form.value.orderTimeRange[0],
        orderTimeEnd: form.value.orderTimeRange[1],
        workTimeStart: form.value.workTimeRange[0],
        workTimeEnd: form.value.workTimeRange[1],
      };
      if (form.value.id != null) {
        updateTardeconfig(submitData).then((response) => {
          proxy.$modal.msgSuccess(t("tradeconfig.updateSuccess"));
          open.value = false;
        });
      } else {
        addTardeconfig(submitData).then((response) => {
          proxy.$modal.msgSuccess(t("tradeconfig.addSuccess"));
          open.value = false;
        });
      }
    }
  });
}
</script>

<style scoped>
.center-container {
  width: 60%;
  margin: 0 auto;
  padding-top: 20px;
}
</style>
