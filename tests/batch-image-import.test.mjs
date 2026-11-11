import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import { computed, effectScope, nextTick, reactive, ref, shallowRef, watch } from 'vue'
import { parse } from '@vue/compiler-sfc'

const { descriptor } = parse(readFileSync(new URL('../src/components/BatchImageImport/index.vue', import.meta.url), 'utf8'))
const setup = descriptor.scriptSetup.content.replace(/^import[\s\S]*?from ["'][^"']+["'];?\r?\n/gm, '')
const validPreview = () => ({
  valid: true,
  entries: [
    { fileName: 'V01.PNG', group: 'vip', sequence: 1, targetId: 7, targetLabel: 'VIP 1' },
    { fileName: 's02.webp', group: 'event', sequence: 2, targetId: 8, targetLabel: '事件第 2 张图片' },
    { fileName: 'z01.gif', group: 'certificate', sequence: 1, targetId: 9, targetLabel: '证书第 1 张图片' },
  ],
  summary: { vip: 1, event: 1, certificate: 1 },
  errors: [],
})

function deferred() {
  let resolve, reject
  const promise = new Promise((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

function createDialog({ preview = async () => ({ data: validPreview() }), save = async () => ({ data: validPreview() }) } = {}) {
  const props = reactive({ open: true })
  const emissions = []
  const notices = []
  const calls = { preview: [], save: [] }
  let unmount
  const scope = effectScope()
  const state = scope.run(() => runInNewContext(`${setup}\n;({
    selectedFile, richTextMode, preview, previewing, importing, requestError, canImport,
    handleFileChange, handleModeChange, handlePreview, handleImport, handleClose
  });`, {
    computed, ref, shallowRef, watch,
    onBeforeUnmount: callback => { unmount = callback },
    defineProps: () => props,
    defineEmits: () => (...args) => emissions.push(args),
    message: { success: value => notices.push(value) },
    previewConfigurationImages: (...args) => { calls.preview.push(args); return preview(...args) },
    importConfigurationImages: (...args) => { calls.save.push(args); return save(...args) },
  }))
  const select = (name = 'images.zip', size = 100) => {
    const file = { name, size }
    state.handleFileChange({ target: { files: [file], value: name } })
    return file
  }
  return { ...state, props, emissions, notices, calls, select, close: () => { unmount?.(); scope.stop() } }
}

test('accepts ZIP names case-insensitively and rejects other files, empty files and oversized archives', () => {
  const dialog = createDialog()
  try {
    dialog.select('IMAGES.ZIP')
    assert.equal(dialog.selectedFile.value.name, 'IMAGES.ZIP')
    for (const [name, size, expected] of [
      ['photo.png', 100, 'ZIP'], ['empty.zip', 0, '不能为空'], ['large.zip', 10 * 1024 * 1024 + 1, '10 MB'],
    ]) {
      dialog.select(name, size)
      assert.equal(dialog.selectedFile.value, null)
      assert.match(dialog.requestError.value, new RegExp(expected))
      assert.equal(dialog.canImport.value, false)
    }
    dialog.select('limit.zip', 10 * 1024 * 1024)
    assert.ok(dialog.selectedFile.value)
  } finally { dialog.close() }
})

test('requires a valid nonempty preview and passes the exact selected archive and append mode', async () => {
  const dialog = createDialog()
  try {
    const file = dialog.select()
    await dialog.handleImport()
    assert.equal(dialog.calls.save.length, 0)
    await dialog.handlePreview()
    assert.equal(dialog.calls.preview[0][0], file)
    assert.equal(dialog.calls.preview[0][1], 'append')
    assert.equal(dialog.preview.value.entries[0].fileName, 'V01.PNG')
    assert.equal(dialog.canImport.value, true)
    for (const invalid of [
      { ...validPreview(), valid: false },
      { ...validPreview(), entries: [] },
      { ...validPreview(), errors: ['v01.png 与 v01.jpg 重复'] },
    ]) {
      dialog.preview.value = invalid
      assert.equal(dialog.canImport.value, false)
      await dialog.handleImport()
    }
    assert.equal(dialog.calls.save.length, 0)
  } finally { dialog.close() }
})

test('changing the archive or rich text mode invalidates the preview and blocks importing', async () => {
  const dialog = createDialog()
  try {
    dialog.select()
    await dialog.handlePreview()
    dialog.handleModeChange({ target: { value: 'replace' } })
    assert.equal(dialog.richTextMode.value, 'replace')
    assert.equal(dialog.preview.value, null)
    assert.equal(dialog.canImport.value, false)
    await dialog.handlePreview()
    assert.equal(dialog.calls.preview[1][1], 'replace')
    assert.equal(dialog.canImport.value, true)
    dialog.select('another.zip')
    assert.equal(dialog.preview.value, null)
    await dialog.handleImport()
    assert.equal(dialog.calls.save.length, 0)
  } finally { dialog.close() }
})

test('a late preview cannot overwrite results for a new archive or mode', async () => {
  const first = deferred()
  let requests = 0
  const fresh = { ...validPreview(), summary: { vip: 0, event: 2, certificate: 0 } }
  const dialog = createDialog({ preview: () => ++requests === 1 ? first.promise : Promise.resolve({ data: fresh }) })
  try {
    dialog.select('old.zip')
    const oldRequest = dialog.handlePreview()
    assert.equal(dialog.previewing.value, true)
    dialog.select('new.zip')
    dialog.handleModeChange({ target: { value: 'replace' } })
    await dialog.handlePreview()
    assert.equal(dialog.preview.value.summary.event, 2)
    first.resolve({ data: validPreview() })
    await oldRequest
    assert.equal(dialog.preview.value.summary.event, 2)
    assert.equal(dialog.selectedFile.value.name, 'new.zip')
    assert.equal(dialog.previewing.value, false)
  } finally { dialog.close() }
})

test('closing or unmounting prevents an old preview from restoring content', async () => {
  for (const action of ['close', 'unmount']) {
    const request = deferred()
    const dialog = createDialog({ preview: () => request.promise })
    try {
      dialog.select()
      const pending = dialog.handlePreview()
      if (action === 'close') dialog.handleClose()
      else dialog.close()
      request.resolve({ data: validPreview() })
      await pending
      assert.equal(dialog.preview.value, null)
      assert.equal(dialog.canImport.value, false)
    } finally { dialog.close() }
  }
})

test('importing prevents closure, mode and file changes, and duplicate submissions; success refreshes and resets', async () => {
  const request = deferred()
  const dialog = createDialog({ save: () => request.promise })
  try {
    const file = dialog.select()
    await dialog.handlePreview()
    const pending = dialog.handleImport()
    assert.equal(dialog.importing.value, true)
    dialog.handleClose()
    dialog.handleModeChange({ target: { value: 'replace' } })
    dialog.select('different.zip')
    await dialog.handleImport()
    assert.equal(dialog.calls.save.length, 1)
    assert.equal(dialog.selectedFile.value, file)
    assert.equal(dialog.richTextMode.value, 'append')
    assert.equal(dialog.emissions.length, 0)
    request.resolve({ data: validPreview() })
    await pending
    assert.equal(dialog.emissions[0][0], 'success')
    assert.equal(dialog.emissions[1][0], 'update:open')
    assert.equal(dialog.emissions[1][1], false)
    assert.equal(dialog.notices.length, 1)
    assert.equal(dialog.importing.value, false)
    assert.equal(dialog.selectedFile.value, null)
    assert.equal(dialog.preview.value, null)
  } finally { dialog.close() }
})

test('failed imports discard the preview so a retry must validate again', async () => {
  const dialog = createDialog({ save: async () => { throw new Error('导入失败') } })
  try {
    dialog.select()
    await dialog.handlePreview()
    await dialog.handleImport()
    assert.equal(dialog.preview.value, null)
    assert.equal(dialog.canImport.value, false)
    assert.equal(dialog.importing.value, false)
    assert.match(dialog.requestError.value, /导入失败/)
    assert.equal(dialog.emissions.length, 0)
    await dialog.handleImport()
    assert.equal(dialog.calls.save.length, 1)
  } finally { dialog.close() }
})

test('validation errors returned at import are displayed without reporting success', async () => {
  const dialog = createDialog({ save: async () => ({ data: { ...validPreview(), valid: false, errors: ['VIP 等级已删除'] } }) })
  try {
    dialog.select()
    await dialog.handlePreview()
    await dialog.handleImport()
    assert.equal(dialog.preview.value.errors[0], 'VIP 等级已删除')
    assert.equal(dialog.canImport.value, false)
    assert.equal(dialog.emissions.length, 0)
    assert.equal(dialog.notices.length, 0)
  } finally { dialog.close() }
})

test('closing and reopening clears the selected archive and resets replacement mode to append', async () => {
  const dialog = createDialog()
  try {
    dialog.select()
    dialog.handleModeChange({ target: { value: 'replace' } })
    await dialog.handlePreview()
    dialog.props.open = false
    await nextTick()
    assert.equal(dialog.selectedFile.value, null)
    assert.equal(dialog.preview.value, null)
    assert.equal(dialog.richTextMode.value, 'append')
    dialog.props.open = true
    await nextTick()
    assert.equal(dialog.canImport.value, false)
  } finally { dialog.close() }
})

test('multipart APIs keep archive and mode, use the two endpoints, and bypass JSON duplicate checks', async () => {
  const calls = []
  const source = readFileSync(new URL('../src/api/member/orderconfig.js', import.meta.url), 'utf8')
    .replace(/^import[^\n]*\n/gm, '').replace(/^export /gm, '')
  class TestFormData {
    constructor() { this.values = new Map() }
    append(key, value) { this.values.set(key, value) }
  }
  const api = runInNewContext(`${source}\n;({ previewConfigurationImages, importConfigurationImages });`, {
    FormData: TestFormData,
    request: config => { calls.push(config); return Promise.resolve({}) },
  })
  const file = { name: 'images.zip' }
  await api.previewConfigurationImages(file)
  await api.importConfigurationImages(file, 'replace')
  assert.equal(calls[0].url, '/member/orderconfig/images/preview')
  assert.equal(calls[1].url, '/member/orderconfig/images/import')
  assert.equal(calls[0].data.values.get('richTextMode'), 'append')
  assert.equal(calls[1].data.values.get('richTextMode'), 'replace')
  for (const call of calls) {
    assert.equal(call.data.values.get('file'), file)
    assert.equal(call.method, 'post')
    assert.equal(call.timeout, 180000)
    assert.equal(call.headers.repeatSubmit, false)
    assert.equal(call.headers['Content-Type'], 'multipart/form-data')
  }
})
