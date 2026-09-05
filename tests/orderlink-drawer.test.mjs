import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const drawer = readFileSync(
  new URL('../src/views/member/orderuser/components/OrderlinkDrawer.vue', import.meta.url),
  'utf8'
).replace(/\r\n/g, '\n')

test('continuous-order list and both editors use the reference 85% drawers', () => {
  assert.equal((drawer.match(/<a-drawer\b/g) || []).length, 3)
  assert.equal((drawer.match(/width="85%"/g) || []).length, 3)
  assert.match(drawer, /title="创建"/)
  assert.doesNotMatch(drawer, /<a-modal\b/)
})

test('continuous-order requests discard stale member sessions and normalize table results', () => {
  const fetchGoods = drawer.match(
    /async function fetchGoods\(session = drawerSession, targetUserId = props\.userId\) \{[\s\S]*?\n\}\n\nfunction handleGoodsPageChange/
  )?.[0] || ''

  assert.match(drawer, /getOrderuserOperationSummary/)
  assert.match(drawer, /let drawerSession = 0/)
  assert.match(drawer, /function isCurrentDrawerRequest\(session, targetUserId\)/)
  assert.match(drawer, /requestSequence !== listRequestSequence/)
  assert.match(drawer, /requestSequence !== userRequestSequence/)
  assert.match(drawer, /requestSequence !== goodsRequestSequence/)
  assert.match(drawer, /@click="refreshAll\(\)"/)
  assert.match(drawer, /@refresh="fetchGoods\(\)"/)
  assert.match(drawer, /function normalizeTableResponse\(response\)/)
  assert.match(drawer, /Array\.isArray\(response\?\.rows\)/)
  assert.match(drawer, /total:\s*Number\.isFinite\(normalizedTotal\)/)
  assert.match(drawer, /watch\(\s*\[\(\) => props\.modelValue, \(\) => props\.userId\]/)
  assert.match(drawer, /\{ immediate: true \}/)
  assert.match(fetchGoods, /orderByColumn:\s*"g\.price"/)
  assert.match(fetchGoods, /isAsc:\s*"desc"/)
})

test('continuous-order mutations are guarded and send explicit payload whitelists', () => {
  assert.match(drawer, /v-if="hasPermission\('member:goods:list'\)"[\s\S]*?v-hasPermi="\['member:orderlink:add'\]"/)
  assert.match(drawer, /const deleteSaving = ref\(false\)/)
  assert.match(drawer, /const editSaving = ref\(false\)/)
  assert.match(drawer, /const saving = ref\(false\)/)
  assert.match(drawer, /function createMutationGuard\(pendingRef\)/)
  assert.match(drawer, /if \(activeToken != null\) return null/)
  assert.match(drawer, /if \(activeToken !== token\) return false/)

  const editSubmit = drawer.match(/async function submitEdit\(\) \{[\s\S]*?\n\}\n\nfunction isCurrentEditSession/)?.[0] || ''
  const updatePayload = editSubmit.match(/const payload = \{[\s\S]*?\n\s*\};/)?.[0] || ''
  assert.match(updatePayload, /id:\s*editForm\.id/)
  assert.match(updatePayload, /priceType:\s*String\(editForm\.priceType\)/)
  assert.match(updatePayload, /price:\s*editForm\.price/)
  assert.doesNotMatch(updatePayload, /\.\.\./)
  assert.match(editSubmit, /await updateOrderlink\(payload\)/)

  const addSubmit = drawer.match(/async function submitAdd\(\) \{[\s\S]*?\n\}\n\nfunction resetDrawerState/)?.[0] || ''
  const addPayload = addSubmit.match(/const payload = \{[\s\S]*?\n\s*\};/)?.[0] || ''
  assert.match(addPayload, /userId:\s*targetUserId/)
  assert.match(addPayload, /status:\s*"1"/)
  assert.match(addPayload, /details:\s*details\.value\.map/)
  assert.match(addPayload, /goodsId:\s*item\.id/)
  assert.doesNotMatch(addPayload, /\.\.\./)
  assert.match(addSubmit, /await addOrderlink\(payload\)/)
})

test('mutation guards lock before validation or confirmation and always release', () => {
  const deleteSubmit = drawer.match(/async function handleDelete\(row\) \{[\s\S]*?\n\}\n\nconst editModalVisible/)?.[0] || ''
  const editSubmit = drawer.match(/async function submitEdit\(\) \{[\s\S]*?\n\}\n\nfunction isCurrentEditSession/)?.[0] || ''
  const addSubmit = drawer.match(/async function submitAdd\(\) \{[\s\S]*?\n\}\n\nfunction resetDrawerState/)?.[0] || ''

  assert.ok(deleteSubmit.indexOf('deleteMutationGuard.acquire()') < deleteSubmit.indexOf('proxy.$modal.confirm'))
  assert.match(deleteSubmit, /catch \{\s*return;\s*\}[\s\S]*?finally \{\s*deleteMutationGuard\.release\(mutationToken\)/)
  assert.ok(editSubmit.indexOf('editMutationGuard.acquire()') < editSubmit.indexOf('editFormRef.value?.validate()'))
  assert.match(editSubmit, /finally \{\s*editMutationGuard\.release\(mutationToken\)/)
  assert.ok(addSubmit.indexOf('addMutationGuard.acquire()') < addSubmit.indexOf('addFormRef.value?.validate()'))
  assert.match(addSubmit, /finally \{\s*addMutationGuard\.release\(mutationToken\)/)
})

test('drawer resets preserve in-flight guards and stale requests cannot close a new editor session', () => {
  const resetDrawer = drawer.match(/function resetDrawerState\(\) \{[\s\S]*?\n\}\n\nfunction beginDrawerSession/)?.[0] || ''
  assert.match(resetDrawer, /resetEditModalState\(\)/)
  assert.match(resetDrawer, /resetAddDrawerState\(\)/)
  assert.doesNotMatch(resetDrawer, /(?:deleteSaving|editSaving|saving)\.value\s*=\s*false/)
  assert.match(drawer, /succeeded && isCurrentEditSession\(session, targetUserId, editorSequence\)[\s\S]*?resetEditModalState\(\)/)
  assert.match(drawer, /succeeded && isCurrentAddSession\(session, targetUserId, editorSequence\)[\s\S]*?resetAddDrawerState\(\)/)
})

test('completed or locked rows cannot be selected, edited, or deleted', () => {
  assert.match(drawer, /disabled:\s*!isEditableStatus\(resolvedStatus\(record\)\)/)
  assert.match(drawer, /:disabled="!isEditableStatus\(resolvedStatus\(record\)\) \|\| deleteSaving \|\| editSaving"/)
  assert.match(drawer, /const targetsAreCurrentAndEditable = \(\) => \{[\s\S]*?return targetIds\.every/)
  assert.match(drawer, /!isEditableStatus\(record\.status\)/)
  assert.match(drawer, /failureMessage\(error, "删除失败"\)/)
  assert.match(drawer, /failureMessage\(error, "修改失败"\)/)
  assert.match(drawer, /failureMessage\(error, "新增失败"\)/)
})
