import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import { computed, effectScope, nextTick, reactive, ref, watch } from 'vue'
import { parse } from '@vue/compiler-sfc'
import {
  DEFAULT_CERTIFICATE_TIP,
  encodeCertificateConfig,
  normalizeCertificateSwitch,
  parseCertificateConfig,
} from '../src/views/member/orderconfig/certificateConfig.js'

function setupSourceFor(name) {
  const { descriptor } = parse(readFileSync(
    new URL(`../src/views/member/orderconfig/components/${name}.vue`, import.meta.url), 'utf8'
  ))
  return descriptor.scriptSetup.content.replace(/^import[\s\S]*?from ["'][^"']+["'];?\r?\n/gm, '')
}
const certificateSetup = setupSourceFor('certificate-config')
const websiteSetup = setupSourceFor('website-config')

function openSettings(content, { raw = false, website = false, id = 7 } = {}) {
  const emissions = []
  const scope = effectScope()
  const props = reactive({ form: { id, type: website ? 'website' : 'certificate', content: raw ? content : JSON.stringify(content) } })
  const state = scope.run(() => runInNewContext(
    `${website ? websiteSetup : certificateSetup}\n;({ localForm${website ? '' : ', configFormRef, handleSubmit'} });`, {
      computed, reactive, ref, watch, nextTick,
      DEFAULT_CERTIFICATE_TIP, encodeCertificateConfig, normalizeCertificateSwitch, parseCertificateConfig,
      defineProps: () => props,
      defineEmits: () => (...args) => emissions.push(args),
      defineExpose: () => {},
      message: { error: text => { throw new Error(text) } },
    }
  ))
  return { ...state, emissions, props, close: () => scope.stop() }
}

function latestSavedForm(editor) {
  const update = editor.emissions.filter(([event]) => event === 'update:form').at(-1)
  assert.ok(update, 'the component emits its JSON configuration')
  return { ...update[1], content: JSON.parse(update[1].content) }
}

test('a legacy certificate defaults off with the requested tip and saves all fields even without editing', async () => {
  const editor = openSettings({ certificateContent: '<p>Original certificate</p>', extension: { provider: 'kept' } })
  try {
    assert.equal(editor.localForm.certificateSwitch, '0')
    assert.equal(editor.localForm.certificateTip, '非正式员工不可以查看证书')
    editor.configFormRef.value = { validate: async () => {} }
    await editor.handleSubmit()
    const saved = latestSavedForm(editor)
    assert.equal(saved.id, 7)
    assert.equal(saved.content.certificateSwitch, '0')
    assert.equal(saved.content.certificateTip, '非正式员工不可以查看证书')
    assert.equal(saved.content.certificateContent, '<p>Original certificate</p>')
    assert.deepEqual(saved.content.extension, { provider: 'kept' })
    assert.equal(editor.emissions.at(-1)[0], 'submit')
  } finally {
    editor.close()
  }
})

test('enable, save, reopen and disable preserve the certificate body, tip and unknown configuration', async () => {
  const untouched = {
    certificateContent: '<p>Original certificate</p>',
    certificateTip: 'A custom message',
    extension: { enabled: true, options: ['one', 'two'] },
    futureSetting: 42,
  }
  const editor = openSettings(untouched)
  let reopened
  let disabledEditor
  try {
    editor.localForm.certificateSwitch = '1'
    await nextTick()
    const enabled = latestSavedForm(editor)
    assert.equal(enabled.content.certificateSwitch, '1')
    for (const [key, value] of Object.entries(untouched)) assert.deepEqual(enabled.content[key], value)
    reopened = openSettings(enabled.content)
    assert.equal(reopened.localForm.certificateSwitch, '1')
    assert.equal(reopened.localForm.certificateTip, 'A custom message')
    reopened.localForm.certificateSwitch = '0'
    await nextTick()
    const disabled = latestSavedForm(reopened)
    assert.equal(disabled.content.certificateSwitch, '0')
    for (const [key, value] of Object.entries(untouched)) assert.deepEqual(disabled.content[key], value)
    disabledEditor = openSettings(disabled.content)
    assert.equal(disabledEditor.localForm.certificateSwitch, '0')
    assert.equal(disabledEditor.localForm.certificateContent, '<p>Original certificate</p>')
  } finally {
    disabledEditor?.close()
    reopened?.close()
    editor.close()
  }
})

test('legacy raw and JSON certificate aliases remain editable and existing aliases stay synchronized', async () => {
  for (const content of [
    '<p>Legacy body</p>',
    JSON.stringify('<p>Legacy body</p>'),
    JSON.stringify({ certificateContent: '<p>Legacy body</p>', certificate: 'stale', extension: 1 }),
    JSON.stringify({ certificate: '<p>Legacy body</p>', extension: 1 }),
    JSON.stringify({ certificateContent: '  ', certificate: '<p>Legacy body</p>', extension: 1 }),
    JSON.stringify({ content: '<p>Legacy body</p>', extension: 1 }),
  ]) {
    const editor = openSettings(content, { raw: true })
    try {
      assert.equal(editor.localForm.certificateContent, '<p>Legacy body</p>')
      editor.localForm.certificateContent = '<p>Edited body</p>'
      await nextTick()
      const saved = latestSavedForm(editor).content
      assert.equal(saved.certificateContent, '<p>Edited body</p>')
      if (content.includes('"certificate":')) assert.equal(saved.certificate, '<p>Edited body</p>')
      if (content.includes('"content":')) assert.equal(saved.content, '<p>Edited body</p>')
      if (content.includes('"extension":')) assert.equal(saved.extension, 1)
    } finally {
      editor.close()
    }
  }
})

test('stored string, number and boolean certificate flags display the corresponding selection', () => {
  for (const [value, expected] of [
    ['1', '1'], [1, '1'], [true, '1'], ['true', '1'], ['TRUE', '1'], [' 1 ', '1'], [' TRUE ', '1'],
    ['0', '0'], [0, '0'], [false, '0'], [' 0 ', '0'], [' false ', '0'], [null, '0'], ['', '0'], ['invalid', '0'],
  ]) {
    const editor = openSettings({ certificateSwitch: value })
    try {
      assert.equal(editor.localForm.certificateSwitch, expected, `stored ${JSON.stringify(value)}`)
    } finally {
      editor.close()
    }
  }
})

test('switching records resets all certificate fields and saves only the current record extension fields', async () => {
  const editor = openSettings({ certificateContent: 'First body', certificateTip: 'First tip', certificateSwitch: '1', firstOnly: { value: 1 } })
  try {
    editor.props.form = {
      id: 19, type: 'certificate', content: JSON.stringify({ certificate: 'Second body', secondOnly: { value: 2 } }),
    }
    await nextTick()
    assert.equal(editor.localForm.certificateSwitch, '0')
    assert.equal(editor.localForm.certificateTip, '非正式员工不可以查看证书')
    assert.equal(editor.localForm.certificateContent, 'Second body')
    editor.localForm.certificateSwitch = '1'
    await nextTick()
    const saved = latestSavedForm(editor)
    assert.equal(saved.id, 19)
    assert.equal(saved.content.certificateContent, 'Second body')
    assert.deepEqual(saved.content.secondOnly, { value: 2 })
    assert.equal(Object.hasOwn(saved.content, 'firstOnly'), false)
    editor.props.form.content = null
    await nextTick()
    assert.equal(editor.localForm.certificateSwitch, '0')
    assert.equal(editor.localForm.certificateTip, '非正式员工不可以查看证书')
    assert.equal(editor.localForm.certificateContent, '')
  } finally {
    editor.close()
  }
})

test('a rejected certificate form validation prevents submit and a valid submit includes the edited tip', async () => {
  const editor = openSettings({ certificateSwitch: '1', certificateContent: 'Original' })
  try {
    editor.configFormRef.value = { validate: async () => { throw new Error('Required tip') } }
    await editor.handleSubmit()
    assert.equal(editor.emissions.some(([event]) => event === 'submit'), false)
    editor.localForm.certificateTip = 'Members only'
    editor.configFormRef.value = { validate: async () => {} }
    await editor.handleSubmit()
    assert.equal(latestSavedForm(editor).content.certificateTip, 'Members only')
    assert.equal(editor.emissions.at(-1)[0], 'submit')
  } finally {
    editor.close()
  }
})

test('editing website settings preserves the old certificate switch for legacy fallback', async () => {
  const editor = openSettings({ name: 'Site', certificateSwitch: '1', extension: 1 }, { website: true })
  try {
    editor.localForm.email = 'owner@example.com'
    await nextTick()
    const saved = latestSavedForm(editor)
    assert.equal(saved.content.certificateSwitch, '1')
    assert.equal(saved.content.extension, 1)
    assert.equal(saved.content.email, 'owner@example.com')
  } finally {
    editor.close()
  }
})

test('wrapped certificate values remain editable and clearing updates aliases at both levels', async () => {
  const editor = openSettings({
    certificateContent: ' ', wrapperMeta: 7,
    value: { certificate: '<p>Wrapped body</p>', certificateTip: 'Wrapped tip', other: [1, 2] },
  })
  try {
    assert.equal(editor.localForm.certificateContent, '<p>Wrapped body</p>')
    assert.equal(editor.localForm.certificateTip, 'Wrapped tip')
    editor.localForm.certificateContent = ''
    editor.localForm.certificateTip = ''
    await nextTick()
    const saved = latestSavedForm(editor).content
    assert.equal(saved.certificateContent, '')
    assert.equal(saved.value.certificate, '')
    assert.equal(saved.value.certificateContent, '')
    assert.equal(saved.value.certificateTip, '')
    assert.equal(saved.wrapperMeta, 7)
    assert.deepEqual(saved.value.other, [1, 2])
  } finally {
    editor.close()
  }
})

test('invalid non-string base tips use the default and wrapped switches never enable the base setting', () => {
  for (const value of [42, false, {}, [], null]) {
    const editor = openSettings({ certificateTip: value, value: { certificateSwitch: '1' } })
    try {
      assert.equal(editor.localForm.certificateTip, DEFAULT_CERTIFICATE_TIP)
      assert.equal(editor.localForm.certificateSwitch, '0')
    } finally {
      editor.close()
    }
  }
})
