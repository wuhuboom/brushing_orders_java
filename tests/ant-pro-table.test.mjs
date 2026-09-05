import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  createColumnStateConfig,
  createColumnStates,
  deserializeColumnStates,
  groupColumns,
  mergeColumnStatePreferences,
  moveColumn,
  moveColumnByOffset,
  serializeColumnStates,
  setAllColumnsVisible,
  setColumnVisible
} from '../src/utils/ant-pro-table.js'

const columns = [
  { title: 'ID', dataIndex: 'id', fixed: 'left', width: 80 },
  { title: '标题', dataIndex: 'title', width: 220 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'operation', fixed: 'right', width: 160 }
]

test('column settings preserve fixed groups and support moving in both directions', () => {
  let states = createColumnStates(columns)
  assert.deepEqual(groupColumns(states).left.map(item => item.id), ['id'])
  assert.deepEqual(groupColumns(states).right.map(item => item.id), ['operation'])

  states = moveColumnByOffset(states, 'status', -1)
  assert.deepEqual(groupColumns(states).none.map(item => item.id), ['status', 'title'])
  states = moveColumnByOffset(states, 'status', 1)
  assert.deepEqual(groupColumns(states).none.map(item => item.id), ['title', 'status'])

  states = moveColumn(states, 'title', 'left', 1)
  assert.deepEqual(groupColumns(states).left.map(item => item.id), ['id', 'title'])
})

test('column visibility supports single, all and reset operations', () => {
  let states = createColumnStates(columns)
  states = setColumnVisible(states, 'status', false)
  assert.equal(states.find(item => item.id === 'status').visible, false)
  states = setAllColumnsVisible(states, false)
  assert.equal(states.some(item => item.visible), false)
  states = createColumnStates(columns)
  assert.equal(states.every(item => item.visible), true)
})

test('hidden columns move to a separate group and survive visible column moves', () => {
  let states = setColumnVisible(createColumnStates(columns), 'status', false)
  let grouped = groupColumns(states)
  assert.deepEqual(grouped.none.map(item => item.id), ['title'])
  assert.deepEqual(grouped.hidden.map(item => item.id), ['status'])

  states = moveColumn(states, 'title', 'left', 1)
  grouped = groupColumns(states)
  assert.deepEqual(states.map(item => item.id).sort(), ['id', 'operation', 'status', 'title'])
  assert.deepEqual(grouped.left.map(item => item.id), ['id', 'title'])
  assert.deepEqual(grouped.hidden.map(item => item.id), ['status'])

  states = setColumnVisible(states, 'status', true)
  grouped = groupColumns(states)
  assert.deepEqual(grouped.none.map(item => item.id), ['status'])
  assert.deepEqual(grouped.hidden, [])
})

test('visible drag indexes remain correct when hidden columns sit between anchors', () => {
  const threeColumns = [
    { title: 'A', dataIndex: 'a' },
    { title: 'B', dataIndex: 'b' },
    { title: 'C', dataIndex: 'c' }
  ]
  let states = setColumnVisible(createColumnStates(threeColumns), 'b', false)
  states = moveColumn(states, 'a', 'none', 1)
  assert.deepEqual(groupColumns(states).none.map(item => item.id), ['c', 'a'])
  assert.deepEqual(groupColumns(states).hidden.map(item => item.id), ['b'])
})

test('column preferences serialize safely and reconcile with new definitions', () => {
  let states = setColumnVisible(createColumnStates(columns), 'status', false)
  states = moveColumn(states, 'title', 'left', 1)
  const config = createColumnStateConfig(states)
  const serialized = serializeColumnStates(states)
  const payload = JSON.parse(serialized)
  assert.deepEqual(config, payload)
  assert.equal(payload.version, 1)
  assert.deepEqual(payload.items, [
    { id: 'id', visible: true, fixed: 'left' },
    { id: 'title', visible: true, fixed: 'left' },
    { id: 'status', visible: false, fixed: null },
    { id: 'operation', visible: true, fixed: 'right' }
  ])

  const restored = createColumnStates(
    [...columns.slice(0, 3), { title: '创建时间', dataIndex: 'createTime' }, columns[3]],
    deserializeColumnStates(serialized)
  )
  assert.deepEqual(restored.map(item => item.id), ['id', 'title', 'status', 'createTime', 'operation'])
  assert.equal(restored.find(item => item.id === 'status').visible, false)
  assert.equal(restored.find(item => item.id === 'title').fixed, 'left')
})

test('invalid or incompatible column preference payloads fall back safely', () => {
  assert.deepEqual(deserializeColumnStates('not-json'), [])
  assert.deepEqual(deserializeColumnStates(JSON.stringify({ version: 2, items: [] })), [])
  assert.deepEqual(deserializeColumnStates(JSON.stringify({ version: 1, items: 'invalid' })), [])
  assert.deepEqual(deserializeColumnStates({ version: 2, items: [] }), [])
})

test('backend column preference objects deserialize without a JSON round trip', () => {
  const config = createColumnStateConfig(
    setColumnVisible(createColumnStates(columns), 'status', false)
  )
  const restored = deserializeColumnStates(config)
  assert.equal(restored.find(item => item.id === 'status').visible, false)
  assert.equal(restored.find(item => item.id === 'id').fixed, 'left')
})

test('staged column definitions retain preferences for columns that arrive later', () => {
  const stagedColumns = [
    { title: 'A', dataIndex: 'a' },
    { title: 'B', dataIndex: 'b' },
    { title: 'C', dataIndex: 'c' }
  ]
  const preferences = deserializeColumnStates(serializeColumnStates(
    setColumnVisible(createColumnStates(stagedColumns), 'b', false)
  ))
  let partial = createColumnStates([stagedColumns[0], stagedColumns[2]], preferences)
  partial = moveColumn(partial, 'a', 'none', 1)

  const mergedPreferences = mergeColumnStatePreferences(preferences, partial)
  const completed = createColumnStates(stagedColumns, mergedPreferences)
  assert.deepEqual(completed.map(item => item.id), ['c', 'a', 'b'])
  assert.equal(completed.find(item => item.id === 'b').visible, false)
  assert.deepEqual(groupColumns(completed).none.map(item => item.id), ['c', 'a'])

  const newColumn = { title: 'D', dataIndex: 'd' }
  const withNewColumn = [createColumnStates([newColumn])[0], ...completed]
  assert.deepEqual(
    mergeColumnStatePreferences(preferences, withNewColumn).map(item => item.id),
    ['d', 'c', 'a', 'b']
  )
})

test('column definitions reconcile without losing current page settings', () => {
  let states = setColumnVisible(createColumnStates(columns), 'status', false)
  states = moveColumnByOffset(states, 'status', -1)
  states = createColumnStates([...columns, { title: '创建时间', dataIndex: 'createTime' }], states)
  assert.equal(states.find(item => item.id === 'status').visible, false)
  assert.equal(states.at(-2).id, 'createTime')
  assert.equal(states.at(-1).id, 'operation')
})

test('table toolbar exposes legacy density choices and a single refresh event', () => {
  const source = readFileSync(new URL('../src/components/AntProTable/index.vue', import.meta.url), 'utf8')
  assert.match(source, /key="large">宽松/)
  assert.match(source, /key="middle">中等/)
  assert.match(source, /key="small">紧凑/)
  assert.match(source, /:size="tableSize"/)
  assert.match(source, /:size="pagination\.size"/)
  assert.match(source, /configuredSelectionWidth/)
  assert.match(source, /configuredSelectionWidth : 48/)
  const reloadBody = source.match(/function reloadTable\(\) \{[\s\S]*?\n\}/)?.[0] || ''
  assert.equal((reloadBody.match(/emit\("refresh"\)/g) || []).length, 1)
  assert.doesNotMatch(reloadBody, /emit\("reload"\)/)
  assert.match(source, />隐藏列</)
  assert.match(source, /columnStateKey/)
  assert.doesNotMatch(source, /localStorage/)
  assert.match(source, /getTableColumnConfig/)
  assert.match(source, /createTableColumnConfigWriter/)
  assert.match(source, /columnPreferenceLoadSequence/)
  assert.match(source, /columnPreferenceLoading \? \[\] : \['click'\]/)
  assert.match(source, /flushColumnStateSave/)
  assert.match(source, /persistedColumnStates/)
})

test('column config API follows the authenticated backend contract', () => {
  const source = readFileSync(new URL('../src/api/system/tableColumnConfig.js', import.meta.url), 'utf8')
  assert.match(source, /TABLE_COLUMN_CONFIG_URL = "\/system\/user\/table-column-config"/)
  assert.equal((source.match(/apiPathParam\(tableKey\)/g) || []).length, 3)
  assert.match(source, /method: "get"/)
  assert.match(source, /method: "put"/)
  assert.match(source, /method: "delete"/)
  assert.match(source, /headers: \{ repeatSubmit: false \}/)
  assert.match(source, /data: config/)
})

test('member list opts into an isolated persistent column state', () => {
  const source = readFileSync(new URL('../src/views/member/orderuser/index.vue', import.meta.url), 'utf8')
  assert.match(source, /column-state-key="member\.orderuser\.main"/)
})
