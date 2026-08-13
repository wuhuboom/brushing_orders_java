<template>
  <div class="app-container home">
    <div class="monitor-count-row">
      <!-- 每个卡片数据直接从实体中获取 -->
      <div class="monitor-count-card">
        <el-card class="monitor-count-card-item">
          <div class="monitor-count-card-num">{{ cardData.totalUsers }}</div>
          <div class="monitor-count-card-text">
            {{ t("dashboard.totalUsers") }}
          </div>
        </el-card>
      </div>
      <div class="monitor-count-card">
        <el-card class="monitor-count-card-item">
          <div class="monitor-count-card-num">{{ cardData.todayUsers }}</div>
          <div class="monitor-count-card-text">
            {{ t("dashboard.todayUsers") }}
          </div>
        </el-card>
      </div>
      <div class="monitor-count-card">
        <el-card class="monitor-count-card-item">
          <div class="monitor-count-card-num">
            {{ cardData.totalRecharge }}
          </div>
          <div class="monitor-count-card-text">
            {{ t("dashboard.totalRecharge") }}
          </div>
        </el-card>
      </div>
      <div class="monitor-count-card">
        <el-card class="monitor-count-card-item">
          <div class="monitor-count-card-num">
            {{ cardData.todayRecharge }}
          </div>
          <div class="monitor-count-card-text-red">
            {{ t("dashboard.todayRecharge") }}
          </div>
        </el-card>
      </div>
      <div class="monitor-count-card">
        <el-card class="monitor-count-card-item">
          <div class="monitor-count-card-num">
            {{ cardData.totalWithdrawal }}
          </div>
          <div class="monitor-count-card-text">
            {{ t("dashboard.totalWithdrawal") }}
          </div>
        </el-card>
      </div>
      <div class="monitor-count-card">
        <el-card class="monitor-count-card-item">
          <div class="monitor-count-card-num">
            {{ cardData.todayWithdrawal }}
          </div>
          <div class="monitor-count-card-text">
            {{ t("dashboard.todayWithdrawal") }}
          </div>
        </el-card>
      </div>
      <div class="monitor-count-card">
        <el-card class="monitor-count-card-item">
          <div class="monitor-count-card-num">{{ cardData.todayOrders }}</div>
          <div class="monitor-count-card-text">
            {{ t("dashboard.todayOrders") }}
          </div>
        </el-card>
      </div>
    </div>

    <div class="table-header">
      <span class="left">{{ t("dashboard.latestOrders") }}</span>
      <el-button class="right" @click="handleMore" type="text" size="small">
        {{ t("dashboard.viewMore") }}
      </el-button>
    </div>
    <el-table v-loading="loading" :data="orderInfoList" :border="true">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        :label="t('order.id')"
        align="center"
        prop="id"
        width="80"
      />
      <el-table-column
        :label="t('order.orderNo')"
        align="center"
        prop="orderNo"
      />
      <el-table-column
        :label="t('order.username')"
        align="center"
        prop="username"
        width="120"
      />
      <el-table-column
        :label="t('order.quantity')"
        align="center"
        prop="quantity"
        width="80"
      />
      <el-table-column :label="t('order.productName')" align="center">
        <template #default="scope">
          <div>{{ scope.row.product.name }}</div>
        </template>
      </el-table-column>
      <el-table-column :label="t('order.price')" align="center" width="120">
        <template #default="scope">
          <div>{{ scope.row.price }}</div>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('order.commission')"
        align="center"
        prop="commission"
        width="120"
      />
      <el-table-column
        :label="t('order.commissionRate')"
        align="center"
        width="80"
        prop="commissionRate"
        :formatter="
          (row) => (row.commissionRate != null ? row.commissionRate + '%' : '')
        "
      />
      <el-table-column
        :label="t('order.commissionStatus')"
        align="center"
        prop="commissionStatus"
        width="120"
      >
        <template #default="scope">
          <dict-tag
            :options="commission_status"
            :value="scope.row.commissionStatus"
          />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('order.orderTime')"
        align="center"
        prop="orderTime"
        width="160"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.orderTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('order.submitTime')"
        align="center"
        prop="submitTime"
        width="160"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.submitTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('order.orderType')"
        align="center"
        prop="orderType"
        width="100"
      >
        <template #default="scope">
          <dict-tag :options="order_type" :value="scope.row.orderType" />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('order.status')"
        align="center"
        prop="status"
        width="100"
      >
        <template #default="scope">
          <dict-tag :options="order_status" :value="scope.row.status" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, reactive, toRefs } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { getCurrentInstance } from "vue";

const router = useRouter(); // 创建路由实例
const { t } = useI18n(); // i18n 实例
console.log("index");

// 模拟从后台返回的数据（单个实体）
const cardData = ref({});

import { listOrderInfo, getOrderInfo } from "@/api/member/orderInfo";
import { getDashboardData } from "@/api/member/member";

const { proxy } = getCurrentInstance();
const { commission_status, order_status, order_type } = proxy.useDict(
  "commission_status",
  "order_status",
  "order_type"
);

const orderInfoList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 15,
  },
});
const { queryParams } = toRefs(data);

function init() {
  getDashboardData().then((res) => {
    cardData.value = res.data;
  });
}

/** 查询订单列表列表 */
function getList() {
  loading.value = true;
  listOrderInfo(queryParams.value).then((response) => {
    orderInfoList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}
function handleMore() {
  // 使用router.push进行页面跳转
  router.push("/orderInfolist/orderInfo");
}

getList();
init();
</script>

<style scoped lang="scss">
.home {
  .monitor-count-row {
    display: flex;
    justify-content: space-between; /* 均匀分布卡片 */
    align-items: center;
    flex-wrap: nowrap; /* 不换行 */
    overflow-x: auto; /* 如果宽度不足，启用水平滚动 */
  }

  .monitor-count-card {
    width: 12%; /* 设置每个卡片的宽度，确保一行显示 */
    min-width: 150px; /* 设置最小宽度，确保在小屏幕上的显示 */
    margin: 10px;
  }

  .monitor-count-card-item {
    display: flex;
    flex-direction: column;
    justify-content: center; /* 垂直居中 */
    align-items: center;
    padding: 20px;
    text-align: center;
    border-radius: 5px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
    background-color: #fff;
    color: #333;
  }

  .monitor-count-card-num {
    font-weight: 500;
    font-size: 32px;
    margin-top: 12px;
  }

  .monitor-count-card-text {
    font-size: 16px;
    margin: 10px 0;
    color: rgb(103, 194, 58);
  }

  .monitor-count-card-text-red {
    font-size: 16px;
    margin: 10px 0;
    color: rgb(245, 108, 108);
  }

  /* 颜色控制 */
  .green {
    background-color: rgb(103, 194, 58);
    color: white;
  }

  .yellow {
    background-color: rgb(230, 162, 60);
    color: white;
  }

  .red {
    background-color: rgb(245, 108, 108);
    color: white;
  }
  .order-table {
    margin-top: 30px;
  }

  .table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
  }

  .left {
    font-size: 18px;
    font-weight: bold;
  }

  .right {
    font-size: 14px;
  }

  .el-table {
    margin-top: 20px;
  }
}
</style>
