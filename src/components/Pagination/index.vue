<template>
  <div :class="{ 'hidden': hidden }" class="pagination-container">
    <a-pagination
      v-model:current="currentPage"
      v-model:page-size="pageSize"
      :page-size-options="pageSizeOptions"
      :total="total"
      show-size-changer
      show-quick-jumper
      :show-total="showTotal"
      @change="handlePageChange"
      @showSizeChange="handlePageChange"
    />
  </div>
</template>

<script setup>
import { scrollTo } from '@/utils/scroll-to'

const props = defineProps({
  total: {
    required: true,
    type: Number
  },
  page: {
    type: Number,
    default: 1
  },
  limit: {
    type: Number,
    default: 20
  },
  pageSizes: {
    type: Array,
    default() {
      return [10, 20, 30, 50]
    }
  },
  // 移动端页码按钮的数量端默认值5
  pagerCount: {
    type: Number,
    default: document.body.clientWidth < 992 ? 5 : 7
  },
  layout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper'
  },
  background: {
    type: Boolean,
    default: true
  },
  autoScroll: {
    type: Boolean,
    default: true
  },
  hidden: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits()
const currentPage = computed({
  get() {
    return props.page
  },
  set(val) {
    emit('update:page', val)
  }
})
const pageSize = computed({
  get() {
    return props.limit
  },
  set(val){
    emit('update:limit', val)
  }
})

const pageSizeOptions = computed(() => props.pageSizes.map(item => String(item)))

function showTotal(total) {
  return `共 ${total} 条`
}

function handlePageChange(page, limit) {
  if (page * limit > props.total) {
    currentPage.value = 1
  }
  emit('pagination', { page: currentPage.value, limit })
  if (props.autoScroll) {
    scrollTo(0, 800)
  }
}
</script>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
  background: #fff;
}
.pagination-container.hidden {
  display: none;
}
</style>
