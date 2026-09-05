const DEFAULT_SAVE_DELAY = 400;

/**
 * Debounces table-column saves while serializing every request that has already
 * entered the write queue. A reset therefore always runs after older writes and
 * before any edits queued after the reset.
 */
export function createTableColumnConfigWriter({
  save,
  reset,
  delay = DEFAULT_SAVE_DELAY,
  setTimer = setTimeout,
  clearTimer = clearTimeout,
}) {
  let timer;
  let pendingSave;
  let disposed = false;
  let writeTail = Promise.resolve();

  function enqueue(operation) {
    const run = writeTail.catch(() => undefined).then(operation);
    // Keep the queue usable after a failed request while leaving `run`
    // rejectable for callers that need to react to this specific operation.
    writeTail = run.catch(() => undefined);
    return run;
  }

  function clearSaveTimer() {
    if (timer === undefined) return;
    clearTimer(timer);
    timer = undefined;
  }

  function flush() {
    clearSaveTimer();
    if (!pendingSave) return Promise.resolve();
    const job = pendingSave;
    pendingSave = undefined;
    return enqueue(() => save(job.tableKey, job.config));
  }

  function schedule(tableKey, config) {
    if (disposed || !tableKey) return;
    pendingSave = { tableKey, config };
    clearSaveTimer();
    timer = setTimer(() => {
      timer = undefined;
      void flush().catch(() => undefined);
    }, delay);
  }

  function cancelPending() {
    clearSaveTimer();
    pendingSave = undefined;
  }

  function resetConfig(tableKey) {
    cancelPending();
    if (!tableKey) return Promise.resolve();
    return enqueue(() => reset(tableKey));
  }

  function dispose() {
    if (disposed) return writeTail;
    const flushed = flush();
    disposed = true;
    return flushed;
  }

  function whenIdle() {
    return writeTail;
  }

  return {
    schedule,
    flush,
    cancelPending,
    reset: resetConfig,
    dispose,
    whenIdle,
  };
}
