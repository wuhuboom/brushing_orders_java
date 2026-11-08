import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import { computed, effectScope, nextTick, reactive, ref, watch } from 'vue'
import { compileTemplate, parse } from '@vue/compiler-sfc'
import { createEmptyTranslations, translationLanguages } from '../src/views/member/components/translationLanguages.js'
import { notificationKinds } from '../src/views/member/orderconfig/notificationKinds.js'

const filename = new URL('../src/views/member/components/TranslationDrawer.vue', import.meta.url)
const { descriptor } = parse(readFileSync(filename, 'utf8'))
const setupSource = descriptor.scriptSetup.content.replace(/^import[\s\S]*?;\r?\n/gm, '')

function openTranslations(translations = {}, type = 'certificate', languageFields = ['enUs', 'esEs']) {
  const emissions = []
  const scope = effectScope()
  const props = reactive({ modelValue: true, translations, type, languageFields })
  const state = scope.run(() => runInNewContext(`${setupSource}\n({ localModels, translationMode, handleSubmit });`, {
    computed, ref, watch, createEmptyTranslations, translationLanguages, notificationKinds,
    defineProps: () => props,
    defineEmits: () => (...args) => emissions.push(args),
  }))
  return {
    ...state, props, close: () => scope.stop(),
    save() {
      state.handleSubmit()
      const [event, payload] = emissions.at(-1)
      assert.equal(event, 'submit')
      return payload
    },
  }
}

test('certificate translations display raw HTML and JSON strings without copying body into the tip', () => {
  for (const raw of ['<p>Legacy certificate</p>', JSON.stringify('<p>Legacy certificate</p>')]) {
    const editor = openTranslations({ enUs: raw })
    try {
      assert.equal(editor.translationMode.value, 'certificate')
      assert.equal(editor.localModels.value.enUs.content, '<p>Legacy certificate</p>')
      assert.equal(editor.localModels.value.enUs.certificateTip, '')
      assert.deepEqual(JSON.parse(editor.save().enUs), {
        certificateContent: '<p>Legacy certificate</p>', certificateTip: '',
      })
    } finally {
      editor.close()
    }
  }
})

test('certificate translations prefer canonical content and preserve unknown fields while syncing legacy aliases', () => {
  const original = {
    certificateContent: '<p>Canonical</p>', certificate: '<p>Old alias</p>', content: '<p>Older alias</p>',
    certificateTip: 'Unavailable', certificateSwitch: '1', extension: { provider: 'kept' },
  }
  const editor = openTranslations({ enUs: JSON.stringify(original) })
  try {
    assert.equal(editor.localModels.value.enUs.content, '<p>Canonical</p>')
    assert.equal(editor.localModels.value.enUs.certificateTip, 'Unavailable')
    editor.localModels.value.enUs.content = '<p>Updated</p>'
    editor.localModels.value.enUs.certificateTip = 'Try again later'
    const saved = JSON.parse(editor.save().enUs)
    assert.equal(saved.certificateContent, '<p>Updated</p>')
    assert.equal(saved.certificate, '<p>Updated</p>')
    assert.equal(saved.content, '<p>Updated</p>')
    assert.equal(saved.certificateTip, 'Try again later')
    assert.deepEqual(saved.extension, original.extension)
    assert.equal(Object.hasOwn(saved, 'certificateSwitch'), false)
    assert.equal(original.certificate, '<p>Old alias</p>')
  } finally {
    editor.close()
  }
})

test('legacy certificate and generic content aliases can be read and saved as canonical JSON', () => {
  for (const alias of ['certificate', 'content']) {
    const editor = openTranslations({ enUs: JSON.stringify({ [alias]: '<p>Legacy body</p>', certificateTip: 'Legacy tip' }) })
    try {
      assert.equal(editor.localModels.value.enUs.content, '<p>Legacy body</p>')
      assert.equal(editor.localModels.value.enUs.certificateTip, 'Legacy tip')
      const saved = JSON.parse(editor.save().enUs)
      assert.equal(saved.certificateContent, '<p>Legacy body</p>')
      assert.equal(saved[alias], '<p>Legacy body</p>')
      assert.equal(saved.certificateTip, 'Legacy tip')
    } finally {
      editor.close()
    }
  }
})

test('wrapped object translations preserve inner and outer metadata and synchronize recognized fields', () => {
  for (const serialize of [value => value, JSON.stringify]) {
    const original = {
      wrapperMeta: { version: 2 }, certificateSwitch: '1',
      value: { certificate: '<p>Wrapped</p>', certificateTip: 'Wrapped tip', extension: ['kept'], certificateSwitch: true },
    }
    const editor = openTranslations({ enUs: serialize(original) })
    try {
      assert.equal(editor.localModels.value.enUs.content, '<p>Wrapped</p>')
      assert.equal(editor.localModels.value.enUs.certificateTip, 'Wrapped tip')
      editor.localModels.value.enUs.content = '<p>Updated wrapped</p>'
      editor.localModels.value.enUs.certificateTip = 'Updated tip'
      const saved = JSON.parse(editor.save().enUs)
      assert.equal(saved.certificateContent, '<p>Updated wrapped</p>')
      assert.equal(saved.certificateTip, 'Updated tip')
      assert.equal(saved.value.certificateContent, '<p>Updated wrapped</p>')
      assert.equal(saved.value.certificate, '<p>Updated wrapped</p>')
      assert.equal(saved.value.certificateTip, 'Updated tip')
      assert.deepEqual(saved.wrapperMeta, original.wrapperMeta)
      assert.deepEqual(saved.value.extension, original.value.extension)
      assert.equal(Object.hasOwn(saved, 'certificateSwitch'), false)
      assert.equal(Object.hasOwn(saved.value, 'certificateSwitch'), false)
      assert.equal(original.value.certificate, '<p>Wrapped</p>')
    } finally {
      editor.close()
    }
  }
})

test('canonical and alias lookup matches backend field order, root priority and blank-value fallback', () => {
  for (const [source, expected] of [
    [{ certificateContent: 'Root canonical', value: { certificateContent: 'Wrapped canonical' } }, 'Root canonical'],
    [{ certificateContent: ' ', certificate: 'Root alias', value: { certificateContent: 'Wrapped canonical' } }, 'Wrapped canonical'],
    [{ certificateContent: '', certificate: 'Root alias', value: { certificate: 'Wrapped alias' } }, 'Root alias'],
    [{ certificateContent: null, value: { certificate: 'Wrapped alias', content: 'Wrapped content' } }, 'Wrapped alias'],
    [{ certificate: '', content: 'Root content', value: { content: 'Wrapped content' } }, 'Root content'],
    [{ certificateContent: '\n', value: { content: 'Wrapped content' } }, 'Wrapped content'],
  ]) {
    const editor = openTranslations({ enUs: JSON.stringify(source) })
    try {
      assert.equal(editor.localModels.value.enUs.content, expected)
      assert.equal(JSON.parse(editor.save().enUs).certificateContent, expected)
    } finally {
      editor.close()
    }
  }
})

test('certificate tips accept only text and prefer root text before wrapped text', () => {
  for (const [source, expected] of [
    [{ certificateTip: 'Root tip', value: { certificateTip: 'Wrapped tip' } }, 'Root tip'],
    [{ certificateTip: ' ', value: { certificateTip: 'Wrapped tip' } }, 'Wrapped tip'],
    [{ certificateTip: 42, value: { certificateTip: 'Wrapped tip' } }, 'Wrapped tip'],
    [{ certificateTip: { nested: 'Not a text tip' } }, ''],
  ]) {
    const editor = openTranslations({ enUs: JSON.stringify(source) })
    try {
      assert.equal(editor.localModels.value.enUs.certificateTip, expected)
      assert.equal(JSON.parse(editor.save().enUs).certificateTip, expected)
    } finally {
      editor.close()
    }
  }
})

test('body and tip edits remain independent across languages and preserve hidden languages and translation identity', () => {
  const hidden = JSON.stringify({ certificate: '<p>Hidden body</p>', certificateTip: 'Hidden tip', extra: true })
  const editor = openTranslations({
    id: 42, zhCn: hidden, futureMetadata: { retained: true },
    enUs: JSON.stringify({ certificateContent: '<p>English</p>', certificateTip: 'English tip' }),
    esEs: JSON.stringify({ certificateContent: '<p>Español</p>', certificateTip: 'Spanish tip' }),
  })
  try {
    editor.localModels.value.enUs.content = '<p>Changed English body</p>'
    editor.localModels.value.esEs.certificateTip = 'Nuevo aviso'
    const saved = editor.save()
    assert.deepEqual(JSON.parse(saved.enUs), { certificateContent: '<p>Changed English body</p>', certificateTip: 'English tip' })
    assert.deepEqual(JSON.parse(saved.esEs), { certificateContent: '<p>Español</p>', certificateTip: 'Nuevo aviso' })
    assert.equal(saved.zhCn, hidden)
    assert.equal(saved.id, 42)
    assert.equal(saved.futureMetadata.retained, true)
  } finally {
    editor.close()
  }
})

test('clearing a language clears recognized body aliases and the tip so backend fallback remains available', () => {
  const editor = openTranslations({ enUs: JSON.stringify({
    certificateContent: '<p>Body</p>', certificate: '<p>Alias</p>', content: '<p>Old</p>', certificateTip: 'Tip', extension: 7,
  }) })
  try {
    editor.localModels.value.enUs.content = ''
    editor.localModels.value.enUs.certificateTip = ''
    const saved = JSON.parse(editor.save().enUs)
    assert.deepEqual(saved, { certificateContent: '', certificate: '', content: '', certificateTip: '', extension: 7 })
    assert.deepEqual(JSON.parse(editor.save().esEs), { certificateContent: '', certificateTip: '' })
  } finally {
    editor.close()
  }
})

test('empty and non-object stored values keep certificate translations blank without inserting the base Chinese tip', () => {
  for (const raw of ['', null, undefined, 'null', '[]', '42']) {
    const editor = openTranslations({ enUs: raw })
    try {
      assert.equal(editor.localModels.value.enUs.content, '')
      assert.equal(editor.localModels.value.enUs.certificateTip, '')
      assert.deepEqual(JSON.parse(editor.save().enUs), { certificateContent: '', certificateTip: '' })
    } finally {
      editor.close()
    }
  }
})

test('other rich translation types retain their existing raw string save format', () => {
  const definitions = {
    register: 'registrationAgreement', about: 'aboutUs', help: 'helpMe', terms: 'clause', event: 'event',
    transaction: 'tradingDescription', order: 'orderDescription', usage: 'useDescription',
    bonus: 'awardDescription', balance: 'rule', privacy: 'privacyPolicy',
  }
  for (const [type, field] of Object.entries(definitions)) {
    const editor = openTranslations({ enUs: JSON.stringify({ [field]: '<p>Existing content</p>', certificateTip: 'Unrelated' }) }, type)
    try {
      assert.equal(editor.translationMode.value, 'rich')
      assert.equal(editor.localModels.value.enUs.content, '<p>Existing content</p>')
      editor.localModels.value.enUs.content = '<p>Updated existing content</p>'
      assert.equal(editor.save().enUs, '<p>Updated existing content</p>')
      assert.equal(Object.hasOwn(editor.localModels.value.enUs, 'certificateTip'), false)
    } finally {
      editor.close()
    }
  }
})

test('structured translation modes keep their existing fields and JSON save format', () => {
  for (const [type, model] of [
    ['customerService', { name: 'Support' }],
    ['level', { name: 'VIP1', description: '<p>Level</p>' }],
    ['noticeItem', { title: 'Notice', content: '<p>Announcement</p>' }],
  ]) {
    const editor = openTranslations({ enUs: JSON.stringify(model) }, type)
    try {
      assert.equal(editor.translationMode.value, type)
      assert.deepEqual(JSON.parse(editor.save().enUs), model)
    } finally {
      editor.close()
    }
  }
})

test('reusing a translation drawer resets certificate models when the setting type changes', async () => {
  const editor = openTranslations({ enUs: JSON.stringify({ certificateContent: '<p>Certificate</p>', certificateTip: 'Tip' }) })
  try {
    editor.props.type = 'about'
    editor.props.translations = { id: 99, enUs: '<p>About</p>' }
    await nextTick()
    assert.equal(editor.translationMode.value, 'rich')
    assert.equal(editor.localModels.value.enUs.content, '<p>About</p>')
    assert.equal(Object.hasOwn(editor.localModels.value.enUs, 'certificateTip'), false)
    assert.equal(editor.save().enUs, '<p>About</p>')
    assert.equal(editor.save().id, 99)
  } finally {
    editor.close()
  }
})

test('translation drawer template compiles with the certificate body and tip controls', () => {
  const compiled = compileTemplate({ source: descriptor.template.content, filename: filename.pathname, id: 'certificate-translations' })
  assert.deepEqual(compiled.errors, [])
})
