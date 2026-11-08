import assert from 'node:assert/strict';
import test from 'node:test';
import { createWithdrawalMonitor, updateSnapshot } from '../src/components/WithdrawalReminder/monitor.js';

function memoryStorage() {
  const data = new Map();
  return { getItem: k => data.get(k) ?? null, setItem: (k, v) => data.set(k, v), removeItem: k => data.delete(k) };
}
function fixture(overrides = {}) {
  let time = 1000;
  let ids = ['10'];
  const events = [], states = [], counts = [];
  const options = { scope: 'admin:1', storage: memoryStorage(), fetchIds: async () => ids,
    canPlay: () => true, onNew: added => events.push(added), onState: s => states.push(s),
    onCount: n => counts.push(n), locks: null, now: () => time, delay: async () => {}, owner: 'one', ...overrides };
  const monitor = createWithdrawalMonitor(options);
  return { monitor, options, events, states, counts, setIds: v => { ids = v; }, advance: ms => { time += ms; } };
}

test('baseline is quiet; replacing a processed withdrawal with a new one alerts despite unchanged count', async () => {
  const f = fixture();
  await f.monitor.tick();
  assert.deepEqual(f.events, []);
  f.setIds(['11']); await f.monitor.tick();
  assert.deepEqual(f.counts, [1, 1]);
  assert.deepEqual(f.events, [['11']]);
  await f.monitor.tick();
  f.setIds(['10', '11']); await f.monitor.tick();
  assert.deepEqual(f.events, [['11']], 'previously seen IDs never repeat');
});

test('merge a batch, preserve 64-bit IDs and identify late commits with lower IDs', () => {
  const { snapshot } = updateSnapshot(null, ['9007199254740993'], 1);
  assert.deepEqual(updateSnapshot(snapshot, ['9007199254740992', '9007199254740993', '20'], 2).added,
    ['9007199254740992', '20']);
  assert.throws(() => updateSnapshot(snapshot, null, 3));
});

test('only one tab announces; an audio-ready tab takes over from a silent tab', async () => {
  const f = fixture({ canPlay: () => false });
  await f.monitor.tick();
  const otherEvents = [];
  const other = createWithdrawalMonitor({ ...f.options, owner: 'two', canPlay: () => true, onNew: ids => otherEvents.push(ids) });
  f.setIds(['10', '11']);
  await other.tick();
  await f.monitor.tick();
  assert.deepEqual(otherEvents, [['11']]);
  assert.deepEqual(f.events, []);
  assert.equal(f.states.at(-1), 'standby');
  other.stop();
  await f.monitor.tick();
  assert.deepEqual(f.events, [], 'takeover does not replay already announced records');
});

test('expired tab lease is recoverable and refresh shares the existing baseline', async () => {
  const f = fixture(); await f.monitor.tick();
  const other = createWithdrawalMonitor({ ...f.options, owner: 'two' });
  await other.tick();
  f.advance(21000);
  f.setIds(['10', '12']); await other.tick();
  assert.deepEqual(f.events, [['12']]);
});

test('no overlapping requests; logout aborts and suppresses a late response', async () => {
  let resolve, signal, calls = 0;
  const f = fixture({ fetchIds: s => { signal = s; calls++; return new Promise(r => { resolve = r; }); } });
  const pending = f.monitor.tick();
  await Promise.resolve();
  await f.monitor.tick();
  assert.equal(calls, 1);
  f.monitor.stop();
  assert.equal(signal.aborted, true);
  resolve(['1']); await pending;
  assert.deepEqual(f.counts, []);
  assert.equal(f.options.storage.getItem('admin:1:snapshot'), null);
});

test('failed requests do not consume new IDs; transport errors back off; forbidden stops', async () => {
  let fail = false;
  const f = fixture({ fetchIds: async () => { if (fail) throw new Error('offline'); return ['1']; } });
  await f.monitor.tick(); fail = true; await f.monitor.tick();
  assert.equal(f.states.at(-1), 'error');
  const before = f.states.length; await f.monitor.tick(); assert.equal(f.states.length, before);
  assert.deepEqual(JSON.parse(f.options.storage.getItem('admin:1:snapshot')).seen, ['1']);
  let calls = 0;
  const forbidden = fixture({ fetchIds: async () => { calls++; throw Object.assign(new Error('denied'), { status: 403 }); } });
  await forbidden.monitor.tick(); await forbidden.monitor.tick();
  assert.equal(calls, 1); assert.deepEqual(forbidden.states, ['unauthorized']);
});

test('storage failure stops notification rather than announcing independently in each tab', async () => {
  const f = fixture({ storage: { getItem() { throw new Error('disabled'); } } });
  await f.monitor.tick(); assert.deepEqual(f.events, []); assert.deepEqual(f.states, ['error']);
});

test('different administrator scopes do not inherit or announce each other’s IDs', async () => {
  const f = fixture(); await f.monitor.tick();
  const otherEvents = [];
  const other = createWithdrawalMonitor({ ...f.options, scope: 'admin:2', owner: 'two', onNew: ids => otherEvents.push(ids) });
  f.setIds(['77']); await other.tick();
  assert.deepEqual(otherEvents, []);
});

test('a fresh session after all tabs close establishes a quiet baseline', async () => {
  const f = fixture(); await f.monitor.tick(); f.monitor.stop();
  const other = createWithdrawalMonitor({ ...f.options, owner: 'two' });
  f.setIds(['10', '90']); await other.tick();
  assert.deepEqual(f.events, []);
});

test('failed first check keeps a quiet baseline even when an old session snapshot exists', async () => {
  const f = fixture(); await f.monitor.tick(); f.monitor.stop();
  let fail = true;
  const other = createWithdrawalMonitor({ ...f.options, owner: 'two', fetchIds: async () => {
    if (fail) throw new Error('offline'); return ['10', '99'];
  } });
  await other.tick(); fail = false; f.advance(31000); await other.tick();
  assert.deepEqual(f.events, []);
});

test('simultaneous fallback elections still choose a single polling tab', async () => {
  let calls = 0;
  const f = fixture({ fetchIds: async () => { calls++; return ['1']; } });
  const other = createWithdrawalMonitor({ ...f.options, owner: 'two' });
  await Promise.all([f.monitor.tick(), other.tick()]);
  assert.equal(calls, 1);
});
