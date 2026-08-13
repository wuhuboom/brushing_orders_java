<template>
  <el-drawer :title="title" v-model="open" size="90%" append-to-body>
    <div style="margin-top: 20px">
      <h3>{{ t('series.index.selectedGoods') }}</h3>
      <el-table :data="selectedGoods" :border="true" v-if="selectedGoods.length > 0">
        <el-table-column :label="t('series.index.productId')" align="center" prop="id" width="80" />
        <el-table-column :label="t('series.index.productName')" align="center" prop="name" min-width="120" show-overflow-tooltip />

        <el-table-column :label="t('series.index.type')" align="center" width="180">
          <template #default="scope">
            <el-select v-model="scope.row.type">
              <el-option :label="t('series.index.typeOne')" value="1" />
              <el-option :label="t('series.index.typeTwo')" value="0" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column :label="t('series.index.price')" align="center" width="180">
          <template #default="scope">
            <el-input-number v-model="scope.row.price" :min="0" :precision="2" :step="0.01" style="width: 100%" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column :label="t('series.index.orderIndexLabel')" align="center" width="150">
          <template #default="scope">
            <el-input-number v-model="scope.row.orderIndex" :min="1" :precision="0" style="width: 100%" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column :label="t('series.index.commissionRatioLabel')" align="center" width="150">
          <template #default="scope">
            <el-input-number v-model="scope.row.commissionRatio" :min="0" :precision="2" style="width: 100%" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column :label="t('series.index.action')" align="center" width="100">
          <template #default="scope">
            <el-button type="danger" size="small" @click="handleGoodsSelection(scope.row)">{{ t('series.index.remove') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-else>{{ t('series.index.noData') }}</p>
    </div>

    <div style="margin-top: 20px; display: flex; justify-content: flex-end">
      <el-form :model="goodsQueryParams" ref="goodsQueryRef" :inline="true" label-width="70px">
        <el-form-item :label="t('series.index.productName')" prop="name">
          <el-input v-model="goodsQueryParams.name" :placeholder="t('series.index.enterProductName')" clearable @keyup.enter="handleGoodsQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleGoodsQuery">{{ t('series.index.search') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table v-loading="goodsLoading" :data="goodsList" :border="true">
      <el-table-column :label="t('series.index.productId')" align="center" prop="id" width="100" />
      <el-table-column :label="t('series.index.productName')" align="center" prop="name" />
      <el-table-column :label="t('series.index.price')" align="center" prop="price" width="100" />
      <el-table-column :label="t('series.index.action')" align="center" width="150">
        <template #default="scope">
          <el-button size="small" :type="form.goodsIds.includes(scope.row.id) ? 'danger' : 'success'" @click="handleGoodsSelection(scope.row)">
            {{ form.goodsIds.includes(scope.row.id) ? t('series.index.remove') : t('series.index.select') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="goodsTotal > 0" :total="goodsTotal" v-model:page="goodsQueryParams.pageNum" v-model:limit="goodsQueryParams.pageSize" @pagination="getGoodsList" />

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="onSubmit">{{ t('common.confirm') }}</el-button>
        <el-button @click="onCancel">{{ t('common.cancel') }}</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, toRefs, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { listGoods } from '@/api/member/series';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '新增商品' }
});
const emit = defineEmits(['update:modelValue', 'confirm']);

const { t } = useI18n();

const open = ref(false);
watch(() => props.modelValue, (v) => {
  open.value = v;
  // when opening the drawer for a new action, reset internal selection
  if (v) {
    resetState();
  }
});
watch(open, (v) => emit('update:modelValue', v));

const title = props.title;

const goodsList = ref([]);
const goodsLoading = ref(false);
const goodsTotal = ref(0);

const data = reactive({
  form: { goodsIds: [] },
  goodsQueryParams: { pageNum: 1, pageSize: 10, name: null }
});
const { form, goodsQueryParams } = toRefs(data);

const selectedGoods = ref([]);

function resetState() {
  form.value.goodsIds = [];
  selectedGoods.value = [];
  goodsQueryParams.value.pageNum = 1;
}

function getGoodsList() {
  goodsLoading.value = true;
  listGoods(goodsQueryParams.value).then(response => {
    goodsList.value = response.rows;
    goodsTotal.value = response.total;
    goodsLoading.value = false;
  });
}

function handleGoodsQuery() {
  goodsQueryParams.value.pageNum = 1;
  getGoodsList();
}

function handleGoodsSelection(row) {
  const index = form.value.goodsIds.indexOf(row.id);
  if (index === -1) {
    form.value.goodsIds.push(row.id);
    // add to selectedGoods with defaults matching Series.vue
    selectedGoods.value.push({ ...row, type: '1', orderIndex: 1, commissionRatio: 10, price: row.price });
  } else {
    form.value.goodsIds.splice(index, 1);
    selectedGoods.value = selectedGoods.value.filter(item => item.id !== row.id);
  }
}

function onSubmit() {
  emit('confirm', selectedGoods.value);
  resetState();
  open.value = false;
}

function onCancel() {
  resetState();
  open.value = false;
}

getGoodsList();
</script>
