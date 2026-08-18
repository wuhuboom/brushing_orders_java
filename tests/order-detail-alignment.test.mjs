import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const page = readFileSync(
  new URL('../src/views/member/orderinfo/index.vue', import.meta.url),
  'utf8'
)

function between(source, start, end) {
  const startIndex = source.indexOf(start)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(startIndex, -1, `missing start marker: ${start}`)
  assert.notEqual(endIndex, -1, `missing end marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

function labels(source) {
  return [...source.matchAll(/<a-form-item label="([^"]+)"/g)].map((match) => match[1])
}

test('order detail search keeps only two collapsed fields and the live expanded order', () => {
  const search = between(page, '<template #search>', '<template #bodyCell')
  const advancedIndex = search.indexOf('<template v-if="advancedSearchVisible"')

  assert.notEqual(advancedIndex, -1)
  assert.deepEqual(labels(search.slice(0, advancedIndex)), ['用户名', '创建时间'])
  assert.deepEqual(labels(search), [
    '用户名',
    '创建时间',
    '明细编号',
    '类型',
    '单数',
    '金额',
    '状态',
    '过期时间',
    '商品标题',
    '额外佣金',
  ])
})

test('search fields use the live three-column flowing grid and actions stay last', () => {
  const search = between(page, '<template #search>', '<template #bodyCell')
  assert.equal((search.match(/<a-row/g) || []).length, 1)
  assert.match(search, /:gutter="\[24, 24\]"/)
  assert.doesNotMatch(search, /:lg="6"|flex="auto"|advanced-query-row/)
  assert.equal((search.match(/:lg="8"/g) || []).length, 11)
  assert.match(search, /<template v-if="advancedSearchVisible">[\s\S]*<\/template>[\s\S]*class="ant-pro-query-actions"/)
})

test('order detail columns retain the live sequence, exact widths and fixed edges', () => {
  const columns = between(page, 'const orderinfoColumns = [', '];\n\nconst commentColumns')
  const parsed = [...columns.matchAll(/\{ title: "([^"]+)"(?:, dataIndex: "([^"]+)")?[^{\n]*?width: (\d+)/g)]
    .map((match) => ({ title: match[1], dataIndex: match[2], width: Number(match[3]) }))

  assert.deepEqual(parsed.map(({ title }) => title), [
    '明细编号',
    '用户名',
    '类型',
    '单数',
    '金额',
    '返佣百分比',
    '返佣',
    '上级返佣百分比',
    '上级返佣',
    '状态',
    '过期时间',
    '商品图片',
    '商品标题',
    '额外佣金',
    '创建时间',
    '备注',
    '操作',
  ])
  assert.deepEqual(parsed.map(({ width }) => width), [
    200, 140, 80, 80, 120, 110, 80, 140, 100, 100, 180, 100, 300, 120, 170, 200, 120,
  ])
  assert.equal(parsed.reduce((total, column) => total + column.width, 0), 2340)
  assert.match(page, /:scroll="\{ x: 2340, y: 'calc\(100vh - 440px\)' \}"/)
  assert.match(columns, /title: "明细编号"[^\n]*fixed: "left"[^\n]*defaultSortOrder: "descend"/)
  assert.match(columns, /title: "操作"[^\n]*fixed: "right"[^\n]*width: 120/)
})

test('type and order states use explicit badge-dot semantics', () => {
  assert.match(page, /<a-badge :status="typeBadge\(record\.type\)\.status"/)
  assert.match(page, /<a-badge :status="statusBadge\(record\.status\)\.status"/)
  assert.doesNotMatch(page, /<dict-tag/)

  for (const contract of [
    /"0": \{ text: "正常", status: "processing" \}/,
    /"1": \{ text: "连单", status: "error" \}/,
    /"0": \{ text: "已完成", status: "success" \}/,
    /"1": \{ text: "待提交", status: "processing" \}/,
    /"2": \{ text: "已冻结", status: "warning" \}/,
    /"3": \{ text: "已取消", status: "default" \}/,
  ]) {
    assert.match(page, contract)
  }
})

test('money formatting preserves decimal strings without Number conversion', () => {
  const source = between(page, 'function formatMoney', 'function formatPercentage').trim()
  const formatMoney = Function(`return (${source});`)()

  assert.doesNotMatch(source, /\bNumber\s*\(/)
  assert.equal(formatMoney('9007199254740993.10'), '9007199254740993.1')
  assert.equal(formatMoney('12'), '12')
  assert.equal(formatMoney('-0.5'), '-0.5')
  assert.equal(formatMoney(null), '-')
  assert.equal(formatMoney(null, '0'), '0')
  assert.doesNotMatch(between(page, 'function formatPercentage', 'function formatDateTime'), /\bNumber\s*\(/)
  assert.match(page, /v-model:value="queryParams\.amountMin"\s+string-mode/)
  assert.match(page, /v-model:value="queryParams\.amountMax"\s+string-mode/)
  assert.match(page, /v-model:value="queryParams\.extraCommission"\s+string-mode/)
  assert.match(page, /formatPercentage\(record\.rebatePercentage\)/)
  assert.match(page, /formatPercentage\(record\.upperRebatePercentage\)/)
})

test('sorting is server-side, allowlisted and resets pagination', () => {
  const columns = between(page, 'const orderinfoColumns = [', '];\n\nconst commentColumns')
  const sortMap = between(page, 'const SORT_FIELD_MAP = Object.freeze({', '});\n\nconst orderStatusOptions')
  const sortHandler = between(page, 'function handleTableChange', 'async function handleUpdate')
  const sortableFields = [...columns.matchAll(/dataIndex: "([^"]+)"[^\n]*sorter: true/g)]
    .map((match) => match[1])

  for (const field of sortableFields) {
    assert.match(sortMap, new RegExp(`\\b${field}: "${field}"`))
  }
  assert.match(page, /@change="handleTableChange"/)
  assert.match(sortHandler, /SORT_FIELD_MAP\[field\]/)
  assert.match(sortHandler, /queryParams\.pageNum = 1/)
  assert.match(sortHandler, /"ascending"[\s\S]*"descending"/)
  assert.doesNotMatch(sortHandler, /queryParams\.orderByColumn = field/)
  assert.match(page, /orderByColumn: "orderNumber"[\s\S]*isAsc: "descending"/)
  assert.match(page, /pageSize === queryParams\.pageSize \? page : 1/)
})

test('query lifecycle always clears loading and reset preserves expansion while restoring default sort', () => {
  const getList = between(page, 'async function getList', 'function displayValue')
  const reset = between(page, 'function resetQuery', 'function handleAntPageChange')

  assert.match(page, /const listRequestVersion = ref\(0\)/)
  assert.match(getList, /const requestVersion = \+\+listRequestVersion\.value/)
  assert.match(getList, /loading\.value = true/)
  assert.match(getList, /await listOrderinfo\(requestParams\(\)\)[\s\S]*requestVersion !== listRequestVersion\.value[\s\S]*orderinfoList\.value/)
  assert.match(getList, /finally \{[\s\S]*requestVersion === listRequestVersion\.value[\s\S]*loading\.value = false/)
  assert.match(page, /function handleQuery\(\) \{[\s\S]*queryParams\.pageNum = 1/)
  assert.match(reset, /pageNum: 1/)
  assert.match(reset, /orderByColumn: "orderNumber"/)
  assert.match(reset, /isAsc: "descending"/)
  assert.match(reset, /createdDateRange\.value = \[\]/)
  assert.doesNotMatch(reset, /advancedSearchVisible\.value\s*=/)
  assert.match(reset, /tableResetKey\.value \+= 1/)
})

test('live modal sizes and fields are present without fabricated comments', () => {
  const editModal = between(page, '<a-modal\n      v-model:open="editOpen"', '</a-modal>')
  const commentModal = between(page, '<a-modal v-model:open="commentOpen"', '</a-modal>')
  const openComment = between(page, 'function openCommentDialog', 'async function handleCancelOrder')

  assert.match(editModal, /width="400px"/)
  assert.deepEqual(labels(editModal), ['过期时间', '备注', '版本号'])
  assert.match(editModal, /v-model:value="form\.version" disabled/)
  assert.match(page, /expiryTime: toEpochMilliseconds\(form\.value\.expiryTime\)/)
  assert.match(commentModal, /width="800px"/)
  assert.match(page, /title: "评论", dataIndex: "comment"/)
  assert.match(page, /title: "评分", dataIndex: "rating"/)
  assert.match(openComment, /commentRows\.value = \[\]/)
  assert.doesNotMatch(page, /commentContent|commentText|commentRating/)
  assert.match(page, /<image-preview[\s\S]*:width="50"[\s\S]*:height="50"/)
  assert.match(page, /@confirm="handleCancelOrder\(record\)"/)
})

test('table geometry uses the live natural header and full-size row actions', () => {
  assert.doesNotMatch(page, /ant-table-thead[\s\S]*?height:\s*48\.56px/)
  assert.doesNotMatch(page, /type="link"\s+size="small"/)
  assert.match(page, /size: 'small', showSizeChanger: true/)
  assert.match(page, /:scroll="\{ x: 2340, y: 'calc\(100vh - 440px\)' \}"/)
  assert.match(page, /\.order-detail-page \{[\s\S]*?padding-top: 28px;/)
  assert.match(page, /\.order-detail-page \{[\s\S]*?margin-bottom: 0;/)
  assert.doesNotMatch(page, /margin-right:/)
  assert.match(page, /:global\(body:has\(\.order-detail-page\)::\-webkit\-scrollbar\) \{[\s\S]*?width: 15px;/)
  assert.match(page, /:global\(body:has\(\.order-detail-page\) \.copyright\) \{[\s\S]*?display: none;/)
  assert.match(page, /:global\(body:has\(\.order-detail-page\) \.app-main\) \{[\s\S]*?padding-bottom: 0 !important;/)
  assert.match(page, /ant-pro-query-form \.ant-form-item-label[\s\S]*?flex: 0 0 80px;[\s\S]*?max-width: 80px;/)
  assert.match(page, /ant-pro-query-form \.ant-picker[\s\S]*?height: 32px;[\s\S]*?padding-block: 4px;/)
})

test('page-scoped table metrics match the live header and body', () => {
  assert.match(page, /\.order-detail-page :deep\(\.ant-pro-table \.ant-table-thead > tr > th\) \{[\s\S]*padding: 12px 8px;[\s\S]*font-size: 15px;[\s\S]*line-height: 23\.57px;/)
  assert.match(page, /ant-table-column-sorters\) \{[\s\S]*?height: 23\.57px;/)
  assert.match(page, /\.order-detail-page :deep\(\.ant-pro-table \.ant-table-tbody > tr > td\) \{[\s\S]*padding: 12px 8px;[\s\S]*font-size: 15px;/)
})
