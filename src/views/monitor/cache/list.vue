<template>
  <div class="app-container ant-pro-member-page">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="8">
        <a-card class="cache-card" title="缓存列表" :bordered="false">
          <template #extra>
            <a-button type="link" size="small" @click="refreshCacheNames">刷新</a-button>
          </template>
          <a-table
            size="middle"
            row-key="cacheName"
            :columns="nameColumns"
            :data-source="cacheNames"
            :loading="loading"
            :pagination="false"
            :scroll="{ y: tableHeight }"
            :custom-row="cacheNameRow"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'index'">{{ index + 1 }}</template>
              <template v-else-if="column.dataIndex === 'cacheName'">{{ nameFormatter(record) }}</template>
              <template v-else-if="column.dataIndex === 'action'">
                <a-button type="link" danger size="small" @click.stop="handleClearCacheName(record)">删除</a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="8">
        <a-card class="cache-card" title="键名列表" :bordered="false">
          <template #extra>
            <a-button type="link" size="small" @click="refreshCacheKeys">刷新</a-button>
          </template>
          <a-table
            size="middle"
            row-key="cacheKey"
            :columns="keyColumns"
            :data-source="cacheKeyRows"
            :loading="subLoading"
            :pagination="false"
            :scroll="{ y: tableHeight }"
            :custom-row="cacheKeyRow"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'index'">{{ index + 1 }}</template>
              <template v-else-if="column.dataIndex === 'cacheKey'">{{ keyFormatter(record.cacheKey) }}</template>
              <template v-else-if="column.dataIndex === 'action'">
                <a-button type="link" danger size="small" @click.stop="handleClearCacheKey(record.cacheKey)">删除</a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="8">
        <a-card class="cache-card" title="缓存内容" :bordered="false">
          <template #extra>
            <a-button type="link" danger size="small" @click="handleClearCacheAll">清理全部</a-button>
          </template>
          <a-form :model="cacheForm" layout="vertical">
            <a-form-item label="缓存名称">
              <a-input v-model:value="cacheForm.cacheName" readonly />
            </a-form-item>
            <a-form-item label="缓存键名">
              <a-input v-model:value="cacheForm.cacheKey" readonly />
            </a-form-item>
            <a-form-item label="缓存内容">
              <a-textarea v-model:value="cacheForm.cacheValue" :rows="8" readonly />
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup name="CacheList">
import {
  listCacheName,
  listCacheKey,
  getCacheValue,
  clearCacheName,
  clearCacheKey,
  clearCacheAll,
} from "@/api/monitor/cache";

const { proxy } = getCurrentInstance();

const cacheNames = ref([]);
const cacheKeys = ref([]);
const cacheForm = ref({});
const loading = ref(true);
const subLoading = ref(false);
const nowCacheName = ref("");
const tableHeight = ref(window.innerHeight - 260);

const nameColumns = [
  { title: "序号", dataIndex: "index", width: 70, align: "center" },
  { title: "缓存名称", dataIndex: "cacheName", ellipsis: true },
  { title: "备注", dataIndex: "remark", ellipsis: true },
  { title: "操作", dataIndex: "action", width: 80, align: "center" },
];

const keyColumns = [
  { title: "序号", dataIndex: "index", width: 70, align: "center" },
  { title: "缓存键名", dataIndex: "cacheKey", ellipsis: true },
  { title: "操作", dataIndex: "action", width: 80, align: "center" },
];

const cacheKeyRows = computed(() => cacheKeys.value.map((cacheKey) => ({ cacheKey })));

function cacheNameRow(record) {
  return {
    onClick: () => getCacheKeys(record),
  };
}

function cacheKeyRow(record) {
  return {
    onClick: () => handleCacheValue(record.cacheKey),
  };
}

function getCacheNames() {
  loading.value = true;
  listCacheName()
    .then((response) => {
      cacheNames.value = response.data || [];
    })
    .finally(() => {
      loading.value = false;
    });
}

function refreshCacheNames() {
  getCacheNames();
  proxy.$modal.msgSuccess("刷新缓存列表成功");
}

function handleClearCacheName(row) {
  clearCacheName(row.cacheName).then(() => {
    proxy.$modal.msgSuccess("清理缓存名称[" + row.cacheName + "]成功");
    getCacheKeys();
  });
}

function getCacheKeys(row) {
  const cacheName = row !== undefined ? row.cacheName : nowCacheName.value;
  if (cacheName === "") {
    return;
  }
  subLoading.value = true;
  listCacheKey(cacheName)
    .then((response) => {
      cacheKeys.value = response.data || [];
      nowCacheName.value = cacheName;
    })
    .finally(() => {
      subLoading.value = false;
    });
}

function refreshCacheKeys() {
  getCacheKeys();
  proxy.$modal.msgSuccess("刷新键名列表成功");
}

function handleClearCacheKey(cacheKey) {
  clearCacheKey(cacheKey).then(() => {
    proxy.$modal.msgSuccess("清理缓存键名[" + cacheKey + "]成功");
    getCacheKeys();
  });
}

function nameFormatter(row) {
  return row.cacheName.replace(":", "");
}

function keyFormatter(cacheKey) {
  return cacheKey.replace(nowCacheName.value, "");
}

function handleCacheValue(cacheKey) {
  getCacheValue(nowCacheName.value, cacheKey).then((response) => {
    cacheForm.value = response.data || {};
  });
}

function handleClearCacheAll() {
  clearCacheAll().then(() => {
    proxy.$modal.msgSuccess("清理全部缓存成功");
  });
}

getCacheNames();
</script>

<style scoped>
.cache-card {
  min-height: calc(100vh - 125px);
}
</style>
