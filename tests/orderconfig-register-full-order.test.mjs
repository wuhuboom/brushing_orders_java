import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import { effectScope, nextTick, reactive, ref, watch } from 'vue'
import { parse } from '@vue/compiler-sfc'

const componentSource = readFileSync(
  new URL('../src/views/member/orderconfig/components/website-config.vue', import.meta.url),
  'utf8'
)
const { descriptor } = parse(componentSource)
const setupSource = descriptor.scriptSetup.content.replace(/^import .*;\r?\n/gm, '')

function openWebsiteSettings(content) {
  const emissions = []
  const scope = effectScope()
  const props = reactive({ form: { id: 7, type: 'website', content: JSON.stringify(content) } })
  const state = scope.run(() => runInNewContext(`${setupSource}\n({ localForm });`, {
    reactive,
    ref,
    watch,
    defineProps: () => props,
    defineEmits: () => (...args) => emissions.push(args),
    defineExpose: () => {},
    message: { error: (text) => { throw new Error(text) } },
  }))
  return { ...state, emissions, props, close: () => scope.stop() }
}

test('existing website settings without the full-order key start disabled and save disabled', async () => {
  const editor = openWebsiteSettings({ name: 'Existing site', anotherSetting: 'preserved' })
  try {
    assert.equal(editor.localForm.registerFullOrder, '0')
    editor.localForm.email = 'owner@example.com'
    await nextTick()
    const [event, payload] = editor.emissions.at(-1)
    assert.equal(event, 'update:form')
    assert.equal(payload.id, 7)
    assert.equal(JSON.parse(payload.content).registerFullOrder, '0')
    assert.equal(JSON.parse(payload.content).anotherSetting, 'preserved')
  } finally {
    editor.close()
  }
})

test('full-order setting reads string, numeric and boolean values with disabled fallback', () => {
  for (const [value, expected] of [
    ['1', '1'], [1, '1'], [true, '1'], ['true', '1'], ['TRUE', '1'],
    ['0', '0'], [0, '0'], [false, '0'],
    [null, '0'], ['', '0'], ['unexpected', '0'],
  ]) {
    const editor = openWebsiteSettings({ registerFullOrder: value })
    try {
      assert.equal(editor.localForm.registerFullOrder, expected, `value ${JSON.stringify(value)}`)
    } finally {
      editor.close()
    }
  }
})

test('turning full orders on and off emits the persisted website JSON and survives reload', async () => {
  const editor = openWebsiteSettings({ name: 'Site', registerFullOrder: 0, customOption: 12 })
  try {
    editor.localForm.registerFullOrder = '1'
    await nextTick()
    const enabled = editor.emissions.at(-1)[1]
    assert.equal(JSON.parse(enabled.content).registerFullOrder, '1')
    assert.equal(JSON.parse(enabled.content).customOption, 12)

    const reopened = openWebsiteSettings(JSON.parse(enabled.content))
    try {
      assert.equal(reopened.localForm.registerFullOrder, '1')
      reopened.localForm.registerFullOrder = '0'
      await nextTick()
      const disabled = JSON.parse(reopened.emissions.at(-1)[1].content)
      assert.equal(disabled.registerFullOrder, '0')
      assert.equal(disabled.customOption, 12)
    } finally {
      reopened.close()
    }
  } finally {
    editor.close()
  }
})
