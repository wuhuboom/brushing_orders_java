export const POLL_INTERVAL = 5000;
const LEASE_MS = 20000;

// Avoid a production compiler bug in optional method calls in default parameters.
function createOwnerId() {
  const crypto = globalThis.crypto;
  if (crypto && typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  return `${Date.now()}-${Math.random()}`;
}

export function updateSnapshot(previous, ids, now) {
  if (!Array.isArray(ids) || ids.some(id => typeof id !== 'string' || !/^\d+$/.test(id))) {
    throw new Error('Invalid withdrawal notification response');
  }
  const current = [...new Set(ids)];
  const seen = new Set(previous?.seen || []);
  const added = previous ? current.filter(id => !seen.has(id)) : [];
  current.forEach(id => seen.add(id));
  return { added, snapshot: { seen: [...seen], pending: current.length, updatedAt: now } };
}

// A short shared lease keeps polling and announcements in a single tab. A tab
// whose audio was activated by the user can take over from a silent tab.
export function createWithdrawalMonitor({
  scope, storage, fetchIds, canPlay, onNew, onState, onCount,
  locks = globalThis.navigator?.locks,
  now = Date.now,
  delay = ms => new Promise(resolve => setTimeout(resolve, ms)),
  owner = createOwnerId(),
}) {
  const leaseKey = `${scope}:lease`;
  const snapshotKey = `${scope}:snapshot`;
  let stopped = false;
  let busy = false;
  let retryAt = 0;
  let controller;
  let joinedSession = false;
  let resetBaseline = false;
  const read = key => {
    const value = storage.getItem(key);
    if (!value) return null;
    try { return JSON.parse(value); } catch { return null; }
  };
  const write = (key, value) => storage.setItem(key, JSON.stringify(value));
  const ownsLease = () => read(leaseKey)?.owner === owner;

  async function poll() {
    if (stopped || now() < retryAt) return;
    const lease = read(leaseKey);
    if (!joinedSession) resetBaseline = !lease || lease.until <= now();
    joinedSession = true;
    if (lease && lease.owner !== owner && lease.until > now() && (!canPlay() || lease.audio)) {
      onState('standby');
      const snapshot = read(snapshotKey);
      if (snapshot) onCount(snapshot.pending);
      return;
    }
    write(leaseKey, { owner, audio: canPlay(), until: now() + LEASE_MS });
    // Also arbitrate tabs in browsers without Web Locks (e.g. plain HTTP).
    await delay(80);
    if (stopped || !ownsLease()) return;
    controller = new AbortController();
    const ids = await fetchIds(controller.signal);
    if (stopped || !ownsLease()) return;
    const previous = resetBaseline ? null : read(snapshotKey);
    const { added, snapshot } = updateSnapshot(previous, ids, now());
    // Commit before invoking UI/audio so competing tabs cannot replay this batch.
    write(snapshotKey, snapshot);
    resetBaseline = false;
    write(leaseKey, { owner, audio: canPlay(), until: now() + LEASE_MS });
    onCount(snapshot.pending);
    onState('connected');
    if (added.length) await onNew(added);
  }

  return {
    async tick() {
      if (stopped || busy) return;
      busy = true;
      try {
        if (locks) {
          await locks.request(`${scope}:poll`, { ifAvailable: true }, async lock => {
            if (lock) await poll();
          });
        } else {
          await poll();
        }
      } catch (error) {
        if (stopped) return;
        const status = error.status || error.response?.status;
        if (status === 401 || status === 403) {
          onState('unauthorized');
          this.stop();
        } else {
          retryAt = now() + 30000;
          onState('error');
        }
      } finally {
        busy = false;
      }
    },
    stop() {
      stopped = true;
      controller?.abort();
      try { if (ownsLease()) storage.removeItem(leaseKey); } catch { /* storage unavailable */ }
    },
  };
}
