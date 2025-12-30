<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="$t('account.index.changeType')" prop="type">
        <el-select
          v-model="queryParams.type"
          :placeholder="$t('account.index.selectChangeType')"
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="dict in trade_ype"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">{{
          $t("account.index.search")
        }}</el-button>
        <el-button icon="Refresh" @click="resetQuery">{{
          $t("account.index.reset")
        }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="changeList" :border="true">
      <el-table-column
        :label="$t('account.index.primaryKey')"
        align="center"
        prop="id"
      />
      <el-table-column
        :label="$t('account.index.changeNo')"
        align="center"
        prop="changeNo"
      />
      <el-table-column
        :label="$t('account.index.changeType')"
        align="center"
        prop="type"
      >
        <template #default="scope">
          <dict-tag :options="trade_ype" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('account.index.beforeAmount')"
        align="center"
        prop="beforeAmount"
      />
      <el-table-column
        :label="$t('account.index.changeAmount')"
        align="center"
        prop="changeAmount"
      >
        <template #default="scope">
          <el-tag
            :type="scope.row.changeAmount > 0 ? 'success' : 'danger'"
            disable-transitions
          >
            {{ scope.row.changeAmount }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        :label="$t('account.index.afterAmount')"
        align="center"
        prop="afterAmount"
      />
       <el-table-column
        :label="$t('member.topup.createTime')"
        align="center"
        width="160"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('account.index.description')"
        align="center"
        prop="description"
      />
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="Account">
import { reactive, toRefs, watch } from "vue";
import { useI18n } from "vue-i18n";
import { listChange } from "@/api/member/change";

const { t } = useI18n();

const props = defineProps({
  userId: {
    type: [Number, String],
    required: true,
  },
});

const { proxy } = getCurrentInstance();
const { trade_ype } = proxy.useDict("trade_ype");

const changeList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    type: null,
  },
});

const { queryParams } = toRefs(data);

// Watch for userId changes to update queryParams and refresh list
watch(
  () => props.userId,
  (newUserId) => {
    queryParams.value.userId = newUserId;
    getList();
  },
  { immediate: true }
);

/** 查询账户变动列表 */
function getList() {
  loading.value = true;
  listChange(queryParams.value).then((response) => {
    changeList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.type = null;
  proxy.resetForm("queryRef");
  handleQuery();
}
</script>
