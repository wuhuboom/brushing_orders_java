import test from 'node:test'
import assert from 'node:assert/strict'

import { createTableColumnConfigWriter } from '../src/utils/table-column-config-writer.js'

function deferred() {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

function fakeTimers() {
  let nextId = 1
  const tasks = new Map()
  return {
    setTimer(callback) {
      const id = nextId++
      tasks.set(id, callback)
      return id
    },
    clearTimer(id) {
      tasks.delete(id)
    },
    runAll() {
      const callbacks = [...tasks.values()]
      tasks.clear()
      callbacks.forEach(callback => callback())
    },
    get size() {
      return tasks.size
    }
  }
}

function config(id) {
  return { version: 1, items: [{ id, visible: true, fixed: null }] }
}

async function drainMicrotasks() {
  await Promise.resolve()
  await Promise.resolve()
  await Promise.resolve()
}

test('rapid changes debounce to one save containing the latest snapshot', async () => {
  const timers = fakeTimers()
  const calls = []
  const writer = createTableColumnConfigWriter({
    save: async (tableKey, value) => calls.push({ tableKey, value }),
    reset: async () => {},
    setTimer: callback => timers.setTimer(callback),
    clearTimer: id => timers.clearTimer(id)
  })

  writer.schedule('member.orderuser.main', config('first'))
  writer.schedule('member.orderuser.main', config('latest'))
  assert.equal(timers.size, 1)

  timers.runAll()
  await writer.whenIdle()
  assert.deepEqual(calls, [{
    tableKey: 'member.orderuser.main',
    value: config('latest')
  }])
})

test('reset cancels a save that has not entered the serial queue', async () => {
  const timers = fakeTimers()
  const calls = []
  const writer = createTableColumnConfigWriter({
    save: async () => calls.push('save'),
    reset: async tableKey => calls.push(`reset:${tableKey}`),
    setTimer: callback => timers.setTimer(callback),
    clearTimer: id => timers.clearTimer(id)
  })

  writer.schedule('member.orderuser.main', config('pending'))
  await writer.reset('member.orderuser.main')

  assert.equal(timers.size, 0)
  assert.deepEqual(calls, ['reset:member.orderuser.main'])
})

test('in-flight save, reset and later save execute in strict server order', async () => {
  const firstSave = deferred()
  const deletion = deferred()
  const laterSave = deferred()
  const calls = []
  const writer = createTableColumnConfigWriter({
    save: (_tableKey, value) => {
      calls.push(`save:${value.items[0].id}`)
      return value.items[0].id === 'before-reset' ? firstSave.promise : laterSave.promise
    },
    reset: tableKey => {
      calls.push(`reset:${tableKey}`)
      return deletion.promise
    }
  })

  writer.schedule('member.orderuser.main', config('before-reset'))
  const firstRun = writer.flush()
  await drainMicrotasks()
  assert.deepEqual(calls, ['save:before-reset'])

  const resetRun = writer.reset('member.orderuser.main')
  writer.schedule('member.orderuser.main', config('after-reset'))
  const laterRun = writer.flush()
  assert.deepEqual(calls, ['save:before-reset'])

  firstSave.resolve()
  await firstRun
  await drainMicrotasks()
  assert.deepEqual(calls, ['save:before-reset', 'reset:member.orderuser.main'])

  deletion.resolve()
  await resetRun
  await drainMicrotasks()
  assert.deepEqual(calls, [
    'save:before-reset',
    'reset:member.orderuser.main',
    'save:after-reset'
  ])

  laterSave.resolve()
  await laterRun
})

test('a rejected write does not poison later queue operations', async () => {
  const calls = []
  const writer = createTableColumnConfigWriter({
    save: async (_tableKey, value) => {
      const id = value.items[0].id
      calls.push(id)
      if (id === 'fails') throw new Error('save failed')
    },
    reset: async () => {}
  })

  writer.schedule('member.orderuser.main', config('fails'))
  await assert.rejects(writer.flush(), /save failed/)
  writer.schedule('member.orderuser.main', config('recovers'))
  await writer.flush()

  assert.deepEqual(calls, ['fails', 'recovers'])
})

test('a failed background debounce is absorbed and later saves still run', async () => {
  const timers = fakeTimers()
  const calls = []
  const writer = createTableColumnConfigWriter({
    save: async (_tableKey, value) => {
      const id = value.items[0].id
      calls.push(id)
      if (id === 'background-fails') throw new Error('background save failed')
    },
    reset: async () => {},
    setTimer: callback => timers.setTimer(callback),
    clearTimer: id => timers.clearTimer(id)
  })

  writer.schedule('member.orderuser.main', config('background-fails'))
  timers.runAll()
  await writer.whenIdle()

  writer.schedule('member.orderuser.main', config('background-recovers'))
  timers.runAll()
  await writer.whenIdle()

  assert.deepEqual(calls, ['background-fails', 'background-recovers'])
})

test('dispose flushes the last pending snapshot and ignores later schedules', async () => {
  const calls = []
  const writer = createTableColumnConfigWriter({
    save: async (_tableKey, value) => calls.push(value.items[0].id),
    reset: async () => {}
  })

  writer.schedule('member.orderuser.main', config('on-unmount'))
  await writer.dispose()
  writer.schedule('member.orderuser.main', config('too-late'))
  await writer.whenIdle()

  assert.deepEqual(calls, ['on-unmount'])
})
