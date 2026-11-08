import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import { ref } from 'vue'
import { normalizeCertificateSwitch, parseCertificateConfig } from '../src/views/member/orderconfig/certificateConfig.js'

const source = readFileSync(new URL('../src/views/member/orderconfig/index.vue', import.meta.url), 'utf8')
const drawerSource = source.slice(source.indexOf('async function openSettingsDrawer('), source.indexOf('function handleUpdate('))

function createDrawer({ content, type = 'certificate', websiteRows = [], websiteError } = {}) {
  const form = ref({})
  const currentType = ref('')
  const drawerReadonly = ref(false)
  const title = ref('')
  const open = ref(false)
  const queries = [], errors = [], componentLoads = []
  const openSettingsDrawer = runInNewContext(`${drawerSource}\nopenSettingsDrawer`, {
    form, currentType, drawerReadonly, title, open, normalizeCertificateSwitch, parseCertificateConfig,
    reset: () => { form.value = {} },
    getOrderconfig: async id => ({ data: { id, type, content } }),
    listOrderconfig: async query => {
      queries.push(query)
      if (websiteError) throw websiteError
      return { rows: websiteRows }
    },
    loadComponent: async value => { componentLoads.push(value); return true },
    proxy: { $modal: { msgError: message => errors.push(message) } },
  })
  return { openSettingsDrawer, form, open, drawerReadonly, queries, errors, componentLoads }
}

test('opening a legacy certificate requests website settings separately and displays the enabled legacy switch', async () => {
  const drawer = createDrawer({
    content: JSON.stringify({ certificate: '<p>Body</p>', extension: { value: 2 } }),
    websiteRows: [{ type: 'website', content: JSON.stringify({ certificateSwitch: ' TRUE ' }) }],
  })
  await drawer.openSettingsDrawer({ id: 8, type: 'certificate' }, false)
  assert.deepEqual(JSON.parse(JSON.stringify(drawer.queries)), [{ type: 'website', pageNum: 1, pageSize: 1 }])
  assert.equal(drawer.open.value, true)
  const content = JSON.parse(drawer.form.value.content)
  assert.equal(content.certificateSwitch, '1')
  assert.equal(content.certificateContent, '<p>Body</p>')
  assert.equal(content.certificate, '<p>Body</p>')
  assert.deepEqual(content.extension, { value: 2 })
})

test('an explicitly saved certificate flag takes precedence and does not read the legacy website', async () => {
  for (const flag of ['0', '1', null]) {
    const content = JSON.stringify({ certificateSwitch: flag, certificateContent: 'Body' })
    const drawer = createDrawer({ content, websiteError: new Error('Must not be queried') })
    await drawer.openSettingsDrawer({ id: 8 }, true)
    assert.equal(drawer.queries.length, 0)
    assert.equal(drawer.form.value.content, content)
    assert.equal(drawer.open.value, true)
    assert.equal(drawer.drawerReadonly.value, true)
  }
})

test('a certificate defaults off when no legacy website record exists and preserves raw legacy content', async () => {
  const drawer = createDrawer({ content: '<p>Raw legacy certificate</p>' })
  await drawer.openSettingsDrawer({ id: 8 }, false)
  const content = JSON.parse(drawer.form.value.content)
  assert.equal(content.certificateSwitch, '0')
  assert.equal(content.certificateContent, '<p>Raw legacy certificate</p>')
  assert.equal(content.certificateTip, '非正式员工不可以查看证书')
  assert.equal(drawer.open.value, true)
})

test('failed legacy website requests show an error and never open a drawer with a fabricated off value', async () => {
  for (const failure of [
    { websiteError: new Error('Network failed') },
    { websiteRows: null },
  ]) {
    const original = JSON.stringify({ certificateContent: 'Original' })
    const drawer = createDrawer({ content: original, ...failure })
    await drawer.openSettingsDrawer({ id: 8 }, false)
    assert.equal(drawer.open.value, false)
    assert.equal(drawer.errors.length, 1)
    assert.equal(drawer.componentLoads.length, 0)
    assert.equal(drawer.form.value.content, original)
  }
})

test('empty and unusable legacy website JSON matches the backend disabled fallback', async () => {
  for (const websiteContent of [null, '', 'null', '{invalid', '[]', '42', '"legacy"']) {
    const drawer = createDrawer({
      content: JSON.stringify({ certificateContent: 'Original' }),
      websiteRows: [{ type: 'website', content: websiteContent }],
    })
    await drawer.openSettingsDrawer({ id: 8 }, false)
    assert.equal(drawer.open.value, true)
    assert.equal(drawer.errors.length, 0)
    assert.equal(JSON.parse(drawer.form.value.content).certificateSwitch, '0')
    assert.equal(JSON.parse(drawer.form.value.content).certificateContent, 'Original')
  }
})

test('other setting types never request website data for certificate migration', async () => {
  const drawer = createDrawer({ type: 'website', content: '{}', websiteError: new Error('Must not be queried') })
  await drawer.openSettingsDrawer({ id: 3 }, false)
  assert.equal(drawer.queries.length, 0)
  assert.equal(drawer.open.value, true)
})
