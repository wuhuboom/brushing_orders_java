import test from 'node:test'
import assert from 'node:assert/strict'
import { expandDateRanges, isOrderActionDisabled, resolveDeleteIds, selectionFlags, signedAmount } from '../src/utils/management-rules.js'

test('selection flags match table selection behavior', () => {
  assert.deepEqual(selectionFlags(0), { single: true, multiple: true })
  assert.deepEqual(selectionFlags(1), { single: false, multiple: false })
  assert.deepEqual(selectionFlags(2), { single: true, multiple: false })
})

test('bulk delete ignores the click event and keeps selected record ids', () => {
  assert.deepEqual(resolveDeleteIds({ type: 'click' }, [23, 24]), [23, 24])
  assert.deepEqual(resolveDeleteIds({ id: 23 }, [24]), [23])
  assert.deepEqual(resolveDeleteIds(undefined, []), [])
})

test('adjustment operation converts to signed delta', () => {
  assert.equal(signedAmount('add', 3), 3)
  assert.equal(signedAmount('subtract', 3), -3)
})

test('gift order buttons follow the legacy state machine', () => {
  assert.equal(isOrderActionDisabled('1', 'ship'), false)
  assert.equal(isOrderActionDisabled('2', 'ship'), true)
  assert.equal(isOrderActionDisabled('2', 'receive'), false)
  assert.equal(isOrderActionDisabled('4', 'cancel'), true)
})

test('date range is expanded into API parameters without leaking UI field', () => {
  const result = expandDateRanges(
    { pageNum: 1, created: ['2026-07-01 00:00:00', '2026-07-02 23:59:59'] },
    [{ type: 'daterange', prop: 'created', startProp: 'startTime', endProp: 'endTime' }]
  )
  assert.equal(result.startTime, '2026-07-01 00:00:00')
  assert.equal(result.endTime, '2026-07-02 23:59:59')
  assert.equal('created' in result, false)
})
