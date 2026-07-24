import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  createColumnStates,
  groupColumns,
  moveColumn,
  moveColumnByOffset,
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
  const reloadBody = source.match(/function reloadTable\(\) \{[\s\S]*?\n\}/)?.[0] || ''
  assert.equal((reloadBody.match(/emit\("refresh"\)/g) || []).length, 1)
  assert.doesNotMatch(reloadBody, /emit\("reload"\)/)
})
