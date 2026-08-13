<template>
  <div class="app-container">
    <el-card class="user-info-card" shadow="hover">
      <el-descriptions :title="$t('series.index.userInfo')" :column="3" border>
        <el-descriptions-item :label="$t('series.index.username')">{{
          userInfo.username || "N/A"
        }}</el-descriptions-item>
        <el-descriptions-item :label="$t('series.index.phone')">{{
          userInfo.phone || "N/A"
        }}</el-descriptions-item>
        <el-descriptions-item :label="$t('series.index.balance')">{{
          userInfo.balance || "0.00"
        }}</el-descriptions-item>
        <el-descriptions-item :label="$t('series.index.orderCount')">{{
          userInfo.dealCount || "0"
        }}</el-descriptions-item>
        <el-descriptions-item :label="$t('series.index.lastLoginTime')">{{
          parseTime(userInfo.lastLoginTime, "{y}-{m}-{d} {h}:{i}:{s}")
        }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">{{
          $t("series.index.addSeries")
        }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Plus" @click="handleAddTemplate">模板添加</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="RefreshRight"
          @click="handleQuery"
          >{{ $t("series.index.refreshData") }}</el-button
        >
      </el-col>

      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="seriesList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        width="100"
        :label="$t('series.index.primaryKeyId')"
        align="center"
        prop="id"
      />
      <el-table-column
        width="100"
        :label="$t('series.index.productId')"
        align="center"
        prop="productId"
      />
      <el-table-column
        :label="$t('series.index.productName')"
        align="center"
        prop="productName"
      />
      <el-table-column
        :label="$t('series.index.type')"
        width="160"
        align="center"
        prop="type"
      >
        <template #default="scope">
          <el-tag type="primary" v-if="scope.row.type == '1'">{{
            $t("series.index.typeOne")
          }}</el-tag>
          <el-tag type="info" v-else>{{ $t("series.index.typeTwo") }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('series.index.price')"
        width="160"
        align="center"
        prop="price"
      />
      <el-table-column
        :label="$t('series.index.orderIndex')"
        width="100"
        align="center"
        prop="orderIndex"
      />
      <el-table-column
        :label="$t('series.index.commissionRatio')"
        align="center"
        prop="commissionRatio"
        width="100"
      />
      <el-table-column
        :label="$t('series.index.status')"
        width="160"
        align="center"
        prop="status"
      >
        <template #default="scope">
          <dict-tag :options="series_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('series.index.createBy')"
        align="center"
        prop="createBy"
        width="120"
      />
      <el-table-column
        :label="$t('series.index.createTime')"
        align="center"
        prop="createTime"
        width="180"
      >
        <template #default="scope">
          {{ parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}:{s}") }}
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('series.index.action')"
        align="center"
        width="200"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            v-if="scope.row.status == '1'"
            @click="handleUpdatePrice(scope.row)"
            >{{ $t("series.index.modifyPrice") }}</el-button
          >
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-if="scope.row.status == '1'"
            >{{ $t("series.index.delete") }}</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-drawer :title="title" v-model="open" size="90%" append-to-body>
      <el-form
        ref="seriesRef"
        :model="form"
        label-width="100px"
        label-position="top"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              :label="$t('series.index.userBalance')"
              prop="balance"
            >
              <el-input
                v-model="userInfo.balance"
                disabled
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('series.index.currentOrderCount')"
              prop="dealCount"
            >
              <el-input
                v-model="userInfo.dealCount"
                disabled
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div style="margin-top: 20px">
        <h3>{{ $t("series.index.selectedGoods") }}</h3>
        <el-table
          :data="selectedGoods"
          :border="true"
          v-if="selectedGoods.length > 0"
        >
          <el-table-column
            :label="$t('series.index.productId')"
            align="center"
            prop="id"
            width="80"
          />
          <el-table-column
            :label="$t('series.index.productName')"
            align="center"
            prop="name"
            min-width="120"
            show-overflow-tooltip
          />

          <el-table-column
            :label="$t('series.index.type')"
            align="center"
            width="180"
          >
            <template #default="scope">
              <el-select v-model="scope.row.type">
                <el-option :label="$t('series.index.typeOne')" value="1" />
                <el-option :label="$t('series.index.typeTwo')" value="0" />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column
            :label="$t('series.index.price')"
            align="center"
            width="180"
          >
            <template #default="scope">
              <el-input-number
                v-model="scope.row.price"
                :min="0"
                :precision="2"
                :step="0.01"
                style="width: 100%"
                controls-position="right"
              />
            </template>
          </el-table-column>

          <el-table-column
            :label="$t('series.index.orderIndexLabel')"
            align="center"
            width="150"
          >
            <template #default="scope">
              <el-input-number
                v-model="scope.row.orderIndex"
                :min="1"
                :precision="0"
                style="width: 100%"
                controls-position="right"
              />
            </template>
          </el-table-column>

          <el-table-column
            :label="$t('series.index.commissionRatioLabel')"
            align="center"
            width="150"
          >
            <template #default="scope">
              <el-input-number
                v-model="scope.row.commissionRatio"
                :min="0"
                :precision="2"
                style="width: 100%"
                controls-position="right"
              />
            </template>
          </el-table-column>

          <el-table-column
            :label="$t('series.index.action')"
            align="center"
            width="100"
          >
            <template #default="scope">
              <el-button
                type="danger"
                size="small"
                @click="handleGoodsSelection(scope.row)"
              >
                {{ $t("series.index.remove") }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <p v-else>{{ $t("series.index.noData") }}</p>
      </div>

      <div style="margin-top: 20px; display: flex; justify-content: flex-end">
        <el-form
          :model="goodsQueryParams"
          ref="goodsQueryRef"
          :inline="true"
          label-width="70px"
        >
          <el-form-item :label="$t('series.index.productName')" prop="name">
            <el-input
              v-model="goodsQueryParams.name"
              :placeholder="$t('series.index.enterProductName')"
              clearable
              @keyup.enter="handleGoodsQuery"
            />
          </el-form-item>

          <el-form-item :label="$t('series.index.priceRange')">
            <el-input
              v-model.number="goodsQueryParams.minPrice"
              :placeholder="$t('series.index.minPrice')"
              style="width: 150px"
              type="number"
            />
            <span style="margin: 0 10px">-</span>
            <el-input
              v-model.number="goodsQueryParams.maxPrice"
              :placeholder="$t('series.index.maxPrice')"
              style="width: 150px"
              type="number"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleGoodsQuery">
              {{ $t("series.index.search") }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="goodsLoading" :data="goodsList" :border="true">
        <el-table-column
          :label="$t('series.index.productId')"
          align="center"
          prop="id"
          width="100"
        />
        <el-table-column
          :label="$t('series.index.productName')"
          align="center"
          prop="name"
        />
        <el-table-column
          :label="$t('series.index.price')"
          align="center"
          prop="price"
          width="100"
        />
        <el-table-column
          :label="$t('series.index.action')"
          align="center"
          width="150"
        >
          <template #default="scope">
            <el-button
              size="small"
              :type="
                form?.goodsIds?.includes(scope.row.id) ? 'danger' : 'success'
              "
              @click="handleGoodsSelection(scope.row)"
            >
              {{
                form?.goodsIds?.includes(scope.row.id)
                  ? $t("series.index.remove")
                  : $t("series.index.select")
              }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="goodsTotal > 0"
        :total="goodsTotal"
        v-model:page="goodsQueryParams.pageNum"
        v-model:limit="goodsQueryParams.pageSize"
        @pagination="getGoodsList"
      />

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{
            $t("common.confirm")
          }}</el-button>
          <el-button @click="cancel">{{ $t("common.cancel") }}</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 模板选择弹窗 -->
    <el-dialog
      title="模板添加"
      v-model="openTemplateDialog"
      width="620px"
      append-to-body
    >
      <div>
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="$t('series.index.userBalance')">
            {{ userInfo.balance || '0.00' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('series.index.currentOrderCount')">
            {{ userInfo.dealCount || '0' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-form :model="{ templateId: selectedTemplateId }" style="margin-top:16px">
          <el-form-item :label="$t('series.index.selectTemplate')">
            <el-select v-model="selectedTemplateId" placeholder="请选择模板" style="width:100%">
              <el-option
                v-for="item in templateOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitTemplateAdd">{{ $t('common.confirm') }}</el-button>
          <el-button @click="openTemplateDialog = false">{{ $t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      :title="$t('series.index.modifyPriceTitle')"
      v-model="openPriceDialog"
      width="400px"
      append-to-body
    >
      <el-form
        ref="priceFormRef"
        :model="priceForm"
        :rules="priceRules"
        label-width="80px"
        label-position="top"
      >
        <el-form-item :label="$t('series.index.price')" prop="price">
          <el-input-number
            v-model="priceForm.price"
            :min="0"
            :precision="2"
            :placeholder="$t('series.index.enterPrice')"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitPriceForm">{{
            $t("common.confirm")
          }}</el-button>
          <el-button @click="openPriceDialog = false">{{
            $t("common.cancel")
          }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Series">
import {
  ref,
  reactive,
  toRefs,
  watch,
  onMounted,
  onUnmounted,
  getCurrentInstance,

} from "vue";
import { useI18n } from "vue-i18n";
import {
  listSeries,
  getSeries,
  delSeries,
  addSeries,
  updateSeries,
  listGoods,
  getMember,
  addSeriesList,
  getTemplateOptions,
  addTemplateSeries
} from "@/api/member/series";

const { t } = useI18n();

const { proxy } = getCurrentInstance();
const { series_status } = proxy.useDict("series_status");

const seriesList = ref([]);
const goodsList = ref([]);
const selectedGoods = ref([]);
const selectedGoodsDetails = ref([]); // 存储选中商品的详细信息（包含用户编辑的数据）
const open = ref(false);
const openPriceDialog = ref(false);
const loading = ref(true);
const goodsLoading = ref(false);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const goodsTotal = ref(0);
const title = ref("");
const userInfo = ref({});
const timer = ref(null);

// 模板相关
const templateOptions = ref([]);
const selectedTemplateId = ref(null);
const openTemplateDialog = ref(false);

const data = reactive({
  form: {
    goodsIds: [],
    // orderIndex 和 commissionRatio 从此处移除逻辑绑定，转为列表内每一行的数据
  },
  priceForm: {
    id: null,
    price: null,
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    productId: null,
    price: null,
    orderIndex: null,
    status: null,
  },
  goodsQueryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    minPrice: null,
    maxPrice: null,
  },
  // 表单校验规则保留，但不再用于新增弹窗的 orderIndex/commissionRatio，仅用于其他可能复用的地方
  rules: {},
  priceRules: {
    price: [
      {
        required: true,
        message: t("series.index.priceRequired"),
        trigger: "blur",
      },
      {
        type: "number",
        message: t("series.index.priceNumber"),
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, priceForm, rules, priceRules, goodsQueryParams } =
  toRefs(data);

// 接收 userId 作为 prop
const props = defineProps({
  userId: {
    type: [Number, String],
    required: true,
  },
});

// 监听 props.userId 变化，同步更新 queryParams.userId
watch(
  () => props.userId,
  (newVal) => {
    data.queryParams.userId = newVal;
  },
  { immediate: true }
);

// 获取用户信息
function fetchUserInfo() {
  getMember(data.queryParams.userId).then((response) => {
    userInfo.value = response.data || {};
  });
}

// 获取商品列表
function getGoodsList() {
  goodsLoading.value = true;
  listGoods(goodsQueryParams.value).then((response) => {
    goodsList.value = response.rows;
    goodsTotal.value = response.total;
    goodsLoading.value = false;
    updateSelectedGoods();
  });
}

// 更新已选择商品列表
// 修改逻辑：确保不会覆盖已选中商品中用户编辑过的数据(price, type, etc.)
function updateSelectedGoods() {
  // 仅在 selectedGoodsDetails 中不存在时，才从 goodsList 中补充
  // 这样可以保留用户在 selectedGoods 表格中编辑的 price, orderIndex 等数据
  const currentDetailsIds = selectedGoodsDetails.value.map((item) => item.id);

  // 如果需要从服务端重新拉取详情来补充 (一般 handleGoodsSelection 处理了添加逻辑)
  // 这里主要用于数据同步展示
  selectedGoods.value = [...selectedGoodsDetails.value];
}

// 商品选择/移除
// 修改逻辑：添加时初始化默认字段 type, orderIndex, commissionRatio
function handleGoodsSelection(row) {
  const index = form.value.goodsIds.indexOf(row.id);
  if (index === -1) {
    // 选中：添加ID
    form.value.goodsIds.push(row.id);

    // 检查详情列表中是否存在，不存在则初始化并添加
    const exists = selectedGoodsDetails.value.some(
      (item) => item.id === row.id
    );
    if (!exists) {
      // 初始化行数据
      const newRow = {
        ...row,
        type: "1", // 默认类型：1 商品价格
        // 默认第几单：用户当前单数 + 1
        orderIndex: (userInfo.value.dealCount || 0) + 1,
        commissionRatio: 10, // 默认佣金倍数
        // price 已经包含在 ...row 中，但可以被编辑
      };
      selectedGoodsDetails.value.push(newRow);
    }
  } else {
    // 取消选中：移除ID
    form.value.goodsIds.splice(index, 1);
    // 从详情列表中移除
    selectedGoodsDetails.value = selectedGoodsDetails.value.filter(
      (item) => item.id !== row.id
    );
  }
  // 更新视图列表
  selectedGoods.value = [...selectedGoodsDetails.value];
}

// 商品搜索
function handleGoodsQuery() {
  goodsQueryParams.value.pageNum = 1;
  getGoodsList();
}

// 查询连单列表
function getList() {
  loading.value = true;
  listSeries(queryParams.value).then((response) => {
    seriesList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    userId: data.queryParams.userId,
    // 其他字段清理
    goodsIds: [],
  };
  selectedGoodsDetails.value = [];
  selectedGoods.value = [];
  proxy.resetForm("seriesRef");
}

// 搜索按钮操作
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
  fetchUserInfo();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

// 新增按钮操作
function handleAdd() {
  reset();
  getGoodsList();
  open.value = true;
  title.value = t("series.index.addSeriesTitle");
}

// 模板添加按钮操作
function handleAddTemplate() {
  selectedTemplateId.value = null;
  openTemplateDialog.value = true;
  // 获取模板选项
  getTemplateOptions().then((res) => {
    // 期望返回数组 [{id, name}, ...]
    templateOptions.value = res.data || [];
  });
}

// 修改价格按钮操作（列表页面的单个修改）
function handleUpdatePrice(row) {
  data.priceForm.id = row.id;
  data.priceForm.price = row.price;
  openPriceDialog.value = true;
}

// 提交列表页面的单个价格修改表单
function submitPriceForm() {
  proxy.$refs["priceFormRef"].validate((valid) => {
    if (valid) {
      const submitData = {
        id: priceForm.value.id,
        price: priceForm.value.price,
      };
      updateSeries(submitData).then((response) => {
        proxy.$modal.msgSuccess(t("series.index.modifyPriceSuccess"));
        openPriceDialog.value = false;
        getList();
      });
    }
  });
}

// 提交新增/修改连单表单
// 修改逻辑：改为调用 addSeriesList，并组装列表数据
function submitForm() {
  if (!form.value.goodsIds || form.value.goodsIds.length === 0) {
    proxy.$modal.msgWarning(t("series.index.selectAtLeastOneGood"));
    return;
  }

  // 校验逻辑：由于字段都在表格中，这里主要校验列表非空
  // 可以在这里添加额外的循环校验，比如校验每一行的 price 或 orderIndex 是否合法

  // 封装列表数据
  // 字段：userId, productId, price, orderIndex, type, commissionRatio
  const submitList = selectedGoods.value.map((item) => {
    return {
      userId: data.queryParams.userId,
      productId: item.id, // 商品ID
      price: item.price,
      orderIndex: item.orderIndex,
      type: item.type,
      commissionRatio: item.commissionRatio,
    };
  });

  // 调用批量新增接口
  addSeriesList(submitList).then((response) => {
    proxy.$modal.msgSuccess(t("series.index.addSuccess")); // 或 "批量添加成功"
    open.value = false;
    getList();
  });
}

// 提交模板添加
function submitTemplateAdd() {
  if (!selectedTemplateId.value) {
    proxy.$modal.msgWarning(t("series.index.selectTemplateRequired"));
    return;
  }
  const payload = {
    userId: data.queryParams.userId,
    templateId: selectedTemplateId.value,
  };
  addTemplateSeries(payload).then(() => {
    proxy.$modal.msgSuccess(t("series.index.addSuccess"));
    openTemplateDialog.value = false;
    getList();
  });
}

// 删除按钮操作
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(t("series.index.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delSeries(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("series.index.deleteSuccess"));
    })
    .catch(() => {});
}

// 监听 userId 变化，初始化数据
watch(
  () => data.queryParams.userId,
  (newVal) => {
    if (newVal) {
      fetchUserInfo();
      getList();
    }
  },
  { immediate: true }
);

// 组件挂载时启动定时器
onMounted(() => {
  // 初始获取用户信息
  fetchUserInfo();
  // 每3秒更新一次用户信息
  timer.value = setInterval(() => {
    fetchUserInfo();
  }, 3000);
});

// 组件卸载时清除定时器
onUnmounted(() => {
  if (timer.value) {
    clearInterval(timer.value);
  }
});
</script>

<style scoped>
.user-info-card {
  margin-bottom: 20px;
  padding: 10px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>
