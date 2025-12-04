<template>
  <div class="app-container">
    <!-- 上部：活动配置表单 -->
    <el-form
      ref="configFormRef"
      :model="configForm"
      label-width="120px"
      class="config-form"
    >
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="是否开启" prop="status">
            <el-switch
              v-model="configForm.status"
              active-value="0"
              inactive-value="1"
              active-text="开启"
              inactive-text="关闭"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="中奖总额" prop="totalAmount" required>
            <el-input-number
              v-model="configForm.totalAmount"
              :min="0"
              :precision="2"
              placeholder="请输入中奖总额"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="抽奖总数" prop="totalCount" required>
            <el-input-number
              v-model="configForm.totalCount"
              :min="1"
              controls-position="right"
              placeholder="请输入抽奖总数"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="货币符号" prop="activityName" required>
            <el-input
              v-model="configForm.activityName"
              placeholder="请输入货币符号"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item class="form-buttons">
            <el-button type="primary" @click="saveConfig">保存配置</el-button>
            <el-button @click="resetConfig">重置</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 下部：奖品表格 -->
    <div class="prize-section">
      <el-row :gutter="20" align="middle">
        <el-col :span="12">
          <h3>奖品设置（个数 × 金额）</h3>
        </el-col>
        <el-col :span="12" style="text-align: right">
          <el-button
            type="success"
            @click="addPrizeRow"
            :disabled="!configForm.totalCount"
            >添加奖品</el-button
          >
          <el-button
            type="warning"
            @click="savePrizes"
            :disabled="!configId || prizes.length === 0"
            >保存奖品</el-button
          >
        </el-col>
      </el-row>
      <el-table
        ref="prizeTableRef"
        :data="prizes"
        border
        style="width: 100%; margin-top: 10px"
        :summary-method="getPrizeSummary"
        show-summary
      >
        <el-table-column label="序号" type="index" width="160" align="center" />
        <el-table-column label="奖品金额 (元)">
          <template #default="{ $index }">
            <el-input-number
              v-model="prizes[$index].amount"
              :min="0"
              :precision="2"
              @change="calculateTotals"
              controls-position="right"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="中奖个数">
          <template #default="{ $index }">
            <el-input-number
              v-model="prizes[$index].count"
              :min="0"
              :max="configForm.totalCount"
              @change="calculateTotals"
              controls-position="right"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计 (金额)" width="220" align="center">
          <template #default="{ row }">
            {{ (row.amount * row.count).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ $index }">
            <el-button
              type="danger"
              size="small"
              @click="deletePrizeRow($index)"
              :disabled="prizes.length <= 1"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div
        v-if="configForm.totalAmount && configForm.totalCount"
        class="total-check"
        style="
          margin-top: 10px;
          padding: 10px;
          background: #f5f7fa;
          border-radius: 4px;
        "
      >
        <p>
          <strong>当前总金额：</strong>{{ currentTotalAmount.toFixed(2) }} /
          {{ configForm.totalAmount }} 元
          <el-tag :type="amountMatch ? 'success' : 'warning'">{{
            amountMatch ? "已达标" : "未达标"
          }}</el-tag>
        </p>
        <p>
          <strong>当前总个数：</strong>{{ currentTotalCount }} /
          {{ configForm.totalCount }} 个
          <el-tag :type="countMatch ? 'success' : 'warning'">{{
            countMatch ? "已达标" : "未达标"
          }}</el-tag>
        </p>
        <p v-if="amountMatch && countMatch" class="success-tip">
          配置完整，可开始抽奖！
        </p>
      </div>
    </div>
  </div>
</template>

<script setup name="LotteryConfig">
import { ref, reactive, computed, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  listLotteryconfig,
  getLotteryconfig,
  delLotteryconfig,
  addLotteryconfig,
  updateLotteryconfig,
} from "@/api/member/lotteryconfig";

import {
  listLotteryprize,
  getLotteryprize,
  delLotteryprize,
  addLotteryprize,
  updateLotteryprize,
} from "@/api/member/lotteryprize";

// 表单引用
const configFormRef = ref();
const prizeTableRef = ref();

// 响应式数据
const configForm = reactive({
  id: null, // 编辑时使用
  activityName: "",
  totalAmount: 0,
  totalCount: 0,
  status: 0, // 0:未开始
});

const prizes = ref([]); // 奖品列表 [{ amount: 0, count: 0 }]
const configId = ref(null); // 当前配置ID

loadConfig();

// 计算属性：当前总金额和总个数（改为严格相等）
const currentTotalAmount = computed(() => {
  return prizes.value.reduce(
    (sum, prize) => sum + prize.amount * prize.count,
    0
  );
});

const currentTotalCount = computed(() => {
  return prizes.value.reduce((sum, prize) => sum + prize.count, 0);
});

const amountMatch = computed(
  () => currentTotalAmount.value === configForm.totalAmount
);
const countMatch = computed(
  () => currentTotalCount.value === configForm.totalCount
);

// 监听总数变化，初始化奖品行（未中奖用0元补足）
watch(
  () => configForm.totalCount,
  (newVal) => {
    if (newVal && prizes.value.length === 0) {
      addPrizeRow(); // 默认添加一行
    }
  }
);

// 方法：添加奖品行
const addPrizeRow = () => {
  prizes.value.push({ amount: 0, count: 0 });
};

// 删除奖品行
const deletePrizeRow = (index) => {
  ElMessageBox.confirm("确认删除该奖品?", "提示", {
    type: "warning",
  }).then(() => {
    prizes.value.splice(index, 1);
    calculateTotals();
  });
};

// 计算总数（触发校验）
const calculateTotals = () => {
  // 可添加校验逻辑，如总个数不超过totalCount
  if (currentTotalCount.value > configForm.totalCount) {
    ElMessage.warning("总中奖个数不能超过抽奖总数！");
    // 可回滚或自动调整
  }
};

// 表格汇总方法
const getPrizeSummary = ({ columns, data }) => {
  const sums = [];
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = "合计";
      return;
    }
    const values = data.map((item) => Number(item[column.property]));
    if (column.property === "amount" || column.property === "count") {
      sums[index] = values.reduce((prev, curr) => prev + curr, 0);
    } else if (column.label === "小计 (金额)") {
      sums[index] = currentTotalAmount.value.toFixed(2);
    } else {
      sums[index] = "";
    }
  });
  return sums;
};

// 保存活动配置
const saveConfig = async () => {
  if (
    !configForm.activityName ||
    configForm.totalAmount <= 0 ||
    configForm.totalCount <= 0
  ) {
    ElMessage.error("请完善活动配置！");
    return;
  }
  try {
    let result;
    if (configForm.id) {
      result = await updateLotteryconfig(configForm);
    } else {
      result = await addLotteryconfig(configForm);
      configForm.id = result.data.id; // 假设返回ID，设置用于后续
      configId.value = result.data.id;
    }
    ElMessage.success("活动配置保存成功！");
    // 加载奖品（如果编辑）
    if (configForm.id || configId.value) {
      await loadPrizes(configId.value || configForm.id);
    }
  } catch (error) {
    ElMessage.error("保存失败：" + error.message);
  }
};

// 保存奖品（收集成列表，一次性调用addLotteryprize，后端自动处理删除）
const savePrizes = async () => {
  if (!configId.value) {
    ElMessage.warning("请先保存活动配置！");
    return;
  }
  if (!amountMatch.value || !countMatch.value) {
    ElMessage.warning("总金额和总个数必须严格相等，无法保存奖品！");
    return;
  }
  try {
    // 准备批量数据：映射到OrderLotteryPrize，设置configId，只发送count>0的
    const prizeList = prizes.value
      .filter((prize) => prize.count > 0)
      .map((prize) => ({
        configId: configId.value,
        amount: prize.amount,
        totalCount: prize.count,
        remainCount: prize.count, // 初始剩余
        // 其他字段如createTime由后端设置
      }));

    if (prizeList.length === 0) {
      ElMessage.warning("无有效奖品数据！");
      return;
    }

    // 一次性调用批量添加接口（后端期望List）
    const result = await addLotteryprize(prizeList);
    if (result.code !== 200) {
      // 假设AjaxResult success code
      throw new Error(result.msg || "添加失败");
    }

    // 更新配置状态为进行中
    await updateLotteryconfig({ id: configId.value, status: 0 });
    ElMessage.success("奖品保存成功，活动已激活！");
  } catch (error) {
    ElMessage.error("保存失败：" + error.message);
  }
};

// 加载奖品（编辑模式）
const loadPrizes = async (id) => {
  try {
    const result = await listLotteryprize({ configId: id });
    prizes.value = result.data.map((p) => ({
      amount: p.amount,
      count: p.totalCount,
    }));
  } catch (error) {
    ElMessage.error("加载奖品失败");
  }
};

// 重置配置
const resetConfig = () => {
  configForm.activityName = "";
  configForm.totalAmount = 0;
  configForm.totalCount = 0;
  configForm.id = null;
  configId.value = null;
  prizes.value = [];
};

function loadConfig() {
  try {
    getLotteryconfig(1)
      .then((res) => {
        configForm.id = res.data.id;
        configId.value = res.data.id;
        configForm.activityName = res.data.activityName;
        configForm.totalAmount = res.data.totalAmount;
        configForm.totalCount = res.data.totalCount;
        configForm.status = res.data.status || 0;
        // 加载奖品
        loadPrizes(configId.value);
      })
      .catch((error) => {
        ElMessage.error("加载配置失败");
        // 无数据时，可初始化空表单
        addPrizeRow();
      });
  } catch (error) {
    ElMessage.error("加载配置失败");
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.config-form {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
.form-buttons {
  text-align: right;
}
.prize-section h3 {
  margin: 0 0 10px 0;
  color: #303133;
}
.total-check p {
  margin: 5px 0;
}
.success-tip {
  color: #67c23a;
  font-weight: bold;
}
</style>
