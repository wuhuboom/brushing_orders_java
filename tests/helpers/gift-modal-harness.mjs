import { readFileSync } from "node:fs";
import vm from "node:vm";
import {
  transactionStorageKey, loadTransaction, prepareTransaction, clearTransaction, terminalTransaction,
} from "../../src/views/member/orderuser/components/transactionRequest.js";

const source = readFileSync(new URL("../../src/views/member/orderuser/components/OrderuserGiftModal.vue", import.meta.url), "utf8");
const script = source.match(/<script\b[^>]*>([\s\S]*?)<\/script>/)[1]
  .replace(/^import[\s\S]*?;\s*$/gm, "");

export function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

export function memoryStorage() {
  const map = new Map();
  return {
    getItem: key => map.get(key) ?? null,
    setItem: (key, value) => map.set(key, value),
    removeItem: key => map.delete(key),
  };
}

export function giftHarness(options = {}) {
  const props = { modelValue: true, userId: 7, ...options.props };
  const localStorage = options.storage ?? memoryStorage();
  const events = [];
  const messages = { success: [], errors: [], warnings: [] };
  const watchers = [];
  const timers = new Map();
  let timerId = 0;
  let unmount;
  const context = {
    defineProps: () => props,
    defineEmits: () => (...event) => events.push(event),
    ref: value => ({ value }),
    reactive: value => value,
    computed: definition => Object.defineProperty({}, "value", { get: definition.get, set: definition.set }),
    watch: (source, callback, config) => {
      watchers.push({ source, callback });
      if (config?.immediate) callback(source());
    },
    onBeforeUnmount: callback => { unmount = callback; },
    getCurrentInstance: () => ({ proxy: { $modal: {
      msgSuccess: message => messages.success.push(message),
      msgError: message => messages.errors.push(message),
      msgWarning: message => messages.warnings.push(message),
    } } }),
    useUserStore: () => ({ id: 42 }),
    window: { APP_CONFIG: { baseApiUrl: "https://example.test/api" } },
    localStorage,
    transactionStorageKey, loadTransaction, prepareTransaction, clearTransaction, terminalTransaction,
    getOrderuser: options.getOrderuser ?? (async id => ({ data: { id, username: `member-${id}`, balance: 0 } })),
    giftAmount: options.giftAmount ?? (async intent => ({ data: { requestId: intent.requestId, state: "SUCCEEDED" } })),
    transactionStatus: options.transactionStatus ?? (async requestId => ({ data: { requestId, state: "UNKNOWN" } })),
    setTimeout: callback => { const id = ++timerId; timers.set(id, callback); return id; },
    clearTimeout: id => timers.delete(id),
  };
  vm.createContext(context);
  vm.runInContext(`${script}\nglobalThis.state = {
    submit, acceptResult, pollResult, pending, form, submitting, journalError, loadingUser, userLoaded, formRef,
    get journalKey() { return journalKey; },
  };`, context);
  context.state.formRef.value = { validate: options.validate ?? (async () => {}), clearValidate() {} };
  return {
    state: context.state, context, props, events, messages, storage: localStorage, timers,
    setProps(next) {
      Object.assign(props, next);
      for (const { source, callback } of watchers) callback(source());
    },
    unmount: () => unmount(),
  };
}

export async function settle() {
  for (let index = 0; index < 5; index++) await Promise.resolve();
}
