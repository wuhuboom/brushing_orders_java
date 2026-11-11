import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import { computed, effectScope, nextTick, reactive, ref, watch } from 'vue'
import { parse } from '@vue/compiler-sfc'

const { descriptor } = parse(readFileSync(new URL('../src/views/member/orderconfig/components/RichTextConfigForm.vue', import.meta.url), 'utf8'))
const setup = descriptor.scriptSetup.content.replace(/^import[\s\S]*?from ["'][^"']+["'];?\r?\n/gm, '')

function openEditor(content, extraProps = {}) {
  const props = reactive({ field: 'eventContent', form: { id: 501, type: 'event', sort: 4, content }, showSort: false, ...extraProps })
  const emissions = []
  const scope = effectScope()
  const state = scope.run(() => runInNewContext(`${setup}\n;({ localForm, configFormRef, handleSubmit });`, {
    computed, nextTick, reactive, ref, watch,
    defineProps: () => props,
    defineEmits: () => (...args) => emissions.push(args),
    defineExpose: () => {},
  }))
  state.configFormRef.value = { validate: async () => {} }
  return { ...state, props, emissions, close: () => scope.stop() }
}

function savedForm(editor) {
  const update = editor.emissions.filter(([name]) => name === 'update:form').at(-1)
  assert.ok(update, 'saving synchronizes the content before submit')
  return { ...update[1], content: JSON.parse(update[1].content) }
}

test('opening and saving without edits preserves imported images and extra configuration', async () => {
  const source = {
    eventContent: '<p>Existing body</p><p><img src="https://example.test/s01.webp"></p><p><img src="https://example.test/s02.bmp"></p>',
    customMetadata: { e2e: 'event', preserve: true },
    futureSetting: ['one', 'two'],
  }
  const editor = openEditor(JSON.stringify(source))
  try {
    await editor.handleSubmit()
    const saved = savedForm(editor)
    assert.deepEqual(saved.content, source)
    assert.equal(saved.id, 501)
    assert.equal(saved.sort, 4)
    assert.equal(editor.emissions.at(-1)[0], 'submit')
  } finally { editor.close() }
})

test('editing the body only replaces the canonical field and retains nested unknown configuration', async () => {
  const editor = openEditor(JSON.stringify({ eventContent: '<p>Before</p>', customMetadata: { preserve: true }, version: 7 }))
  try {
    editor.localForm.eventContent = '<p><img src="https://example.test/new.png"></p>'
    await nextTick()
    const saved = savedForm(editor)
    assert.equal(saved.content.eventContent, editor.localForm.eventContent)
    assert.deepEqual(saved.content.customMetadata, { preserve: true })
    assert.equal(saved.content.version, 7)
  } finally { editor.close() }
})

test('legacy raw HTML and JSON string bodies remain editable and save as canonical JSON', async () => {
  const body = '<p>Legacy</p><p><img src="https://example.test/old.webp"></p>'
  for (const raw of [body, JSON.stringify(body)]) {
    const editor = openEditor(raw)
    try {
      assert.equal(editor.localForm.eventContent, body)
      await editor.handleSubmit()
      assert.deepEqual(savedForm(editor).content, { eventContent: body })
    } finally { editor.close() }
  }
})

test('event aliases follow backend lookup priority and existing aliases stay synchronized when clearing the body', async () => {
  const source = {
    eventContent: ' ', event: 'Root alias', content: 'Root content alias',
    value: { eventContent: 'Wrapped canonical body', event: 'Wrapped alias', content: 'Wrapped content', value: 'Wrapped value', nestedMetadata: { retained: true } },
    customMetadata: { body: 'Keep this metadata text' },
  }
  const editor = openEditor(JSON.stringify(source))
  try {
    assert.equal(editor.localForm.eventContent, 'Wrapped canonical body')
    editor.localForm.eventContent = ''
    await editor.handleSubmit()
    const saved = savedForm(editor).content
    for (const container of [saved, saved.value]) {
      assert.equal(container.eventContent, '')
      assert.equal(container.event, '')
      assert.equal(container.content, '')
    }
    assert.equal(saved.value.value, '')
    assert.deepEqual(saved.value.nestedMetadata, { retained: true })
    assert.deepEqual(saved.customMetadata, { body: 'Keep this metadata text' })
  } finally { editor.close() }
})

test('legacy textual event value aliases remain editable without mutating nontext metadata', async () => {
  for (const source of [{ event: 'Legacy event' }, { content: 'Legacy event' }, { value: 'Legacy event' }]) {
    const editor = openEditor(JSON.stringify({ ...source, customMetadata: { event: { retained: true } } }))
    try {
      assert.equal(editor.localForm.eventContent, 'Legacy event')
      editor.localForm.eventContent = 'New body'
      await editor.handleSubmit()
      const saved = savedForm(editor).content
      assert.equal(saved.eventContent, 'New body')
      for (const key of Object.keys(source)) assert.equal(saved[key], 'New body')
      assert.deepEqual(saved.customMetadata, { event: { retained: true } })
    } finally { editor.close() }
  }
})

test('switching to another or empty record resets the body and never copies the previous metadata', async () => {
  const editor = openEditor(JSON.stringify({ eventContent: 'First', firstOnly: true }))
  try {
    editor.props.form = { id: 502, type: 'event', sort: 9, content: JSON.stringify({ eventContent: 'Second', secondOnly: true }) }
    await nextTick()
    await editor.handleSubmit()
    assert.deepEqual(savedForm(editor).content, { eventContent: 'Second', secondOnly: true })
    editor.props.form = { id: 503, type: 'event', content: null }
    await nextTick()
    assert.equal(editor.localForm.eventContent, '')
    await editor.handleSubmit()
    assert.deepEqual(savedForm(editor).content, { eventContent: '' })
    assert.equal(savedForm(editor).id, 503)
  } finally { editor.close() }
})

test('validation errors prevent save while visible sort changes preserve the content metadata', async () => {
  const editor = openEditor(JSON.stringify({ eventContent: 'Body', metadata: { retained: true } }), { showSort: true })
  try {
    editor.configFormRef.value = { validate: async () => { throw new Error('Invalid sort') } }
    await editor.handleSubmit()
    assert.equal(editor.emissions.some(([name]) => name === 'submit'), false)
    editor.localForm.sort = 12
    editor.configFormRef.value = { validate: async () => {} }
    await editor.handleSubmit()
    const saved = savedForm(editor)
    assert.equal(saved.sort, 12)
    assert.deepEqual(saved.content, { eventContent: 'Body', metadata: { retained: true } })
    assert.equal(editor.emissions.at(-1)[0], 'submit')
  } finally { editor.close() }
})
