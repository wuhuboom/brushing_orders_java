import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('account pages navigate by registered route name and enforce target-page permissions', () => {
  const points = source('src/views/points/accounts/index.vue')
  const activities = source('src/views/activities/accounts/index.vue')

  assert.match(points, /name:\s*["']PointsFlowList["']/)
  assert.match(points, /points:flow:list/)
  assert.match(activities, /name:\s*["']ActivityPartnerList["']/)
  assert.match(activities, /activity:partner:list/)
})

test('management pages use Chinese Ant Design locale and explicit modal labels', () => {
  const app = source('src/App.vue')
  const points = source('src/views/points/accounts/index.vue')
  const activities = source('src/views/activities/accounts/index.vue')

  assert.match(app, /locale="zhCN"/)
  assert.match(points, /ok-text="确\s*定"/)
  assert.match(points, /cancel-text="取\s*消"/)
  assert.match(activities, /ok-text="确\s*定"/)
  assert.match(activities, /cancel-text="取\s*消"/)
})

test('member sign-in statistics are data-backed and editable', () => {
  const member = source('src/views/member/orderuser/index.vue')

  assert.match(member, /record\.signDays/)
  assert.match(member, /record\.todaySignCount/)
  assert.match(member, /record\.totalSignDays/)
  assert.match(member, /submitModifySignDays/)
})

test('member management keeps root-member hierarchy and legacy actions aligned', () => {
  const member = source('src/views/member/orderuser/index.vue')
  const form = source('src/views/member/orderuser/components/OrderuserForm.vue')

  assert.match(form, /顶级用户/)
  assert.match(form, /下级用户/)
  assert.match(form, /parentId:\s*parentInviteCode\s*\?\s*props\.formData\.parentId\s*:\s*0/)
  assert.match(form, /parentInviteCode:\s*parentInviteCode\s*\|\|\s*null/)
  assert.doesNotMatch(form, /parentInviteCode:\s*\[\{/)
  assert.doesNotMatch(form, /phoneNumber:\s*\[\{\s*required:\s*true/)
  assert.doesNotMatch(form, /name="userContractEnabled"/)
  assert.doesNotMatch(form, /name="withdrawalBlockRemark"/)

  assert.match(member, /advancedSearchVisible/)
  assert.match(member, /usernameList/)
  assert.match(member, /balanceMin/)
  assert.match(member, /reputationMax/)
  assert.match(member, /handleCopyMember/)
  assert.match(member, /编辑身份信息/)
  assert.match(member, /编辑合同/)
  assert.match(member, /:trigger="\['click'\]"/)
  assert.match(member, /pageSize:\s*20/)
  assert.match(member, /sorter:\s*true/)
  assert.match(member, /column\.key === 'taskProgress'[\s\S]*?@click="openModifyCount\(record\)"/)
  assert.match(member, /column\.key === 'id'[\s\S]*?@click="handleView\(record\)"/)
  assert.match(member, /@click="handleModifySignDays\(record\)"/)
  assert.match(member, /@click="handleModifyReputation\(record\)"/)
  assert.match(member, /@click="handleToggleProductMatching\(record\)"/)
  assert.match(form, /:disabled="readonly"/)
})

test('layout settings persist the active schema and independent visual choices', () => {
  const settings = source('src/layout/components/Settings/index.vue')
  const app = source('src/App.vue')

  assert.match(settings, /layoutVersion:\s*settingsStore\.layoutVersion/)
  assert.match(settings, /isDark:\s*settingsStore\.isDark/)
  assert.match(settings, /settingsStore\.sideTheme === item\.value/)
  const overallStyleFunction = settings.match(/function setOverallStyle\(value\) \{[\s\S]*?\n\}/)?.[0] || ''
  assert.doesNotMatch(overallStyleFunction, /sideTheme\s*=/)
  assert.match(app, /:theme="antdThemeConfig"/)
  assert.match(app, /antdTheme\.darkAlgorithm/)
})

test('top-level menu switches groups without opening its first child', () => {
  const topNav = source('src/components/TopNav/index.vue')

  assert.match(topNav, /activeMenu\.value\s*=\s*key/)
  assert.match(topNav, /if \(parent && targetRoutes\.length > 0\) \{[\s\S]*?return;/)
  assert.doesNotMatch(topNav, /findFirstRoute/)
})

test('sidebar parent menus only toggle and never navigate to the first child', () => {
  const sidebar = source('src/layout/components/Sidebar/index.vue')

  assert.match(sidebar, /if \(isOpen\(node\.key\)\)/)
  assert.match(sidebar, /openKeys\.value = openKeys\.value\.filter/)
  assert.match(sidebar, /if \(node\.children\.length\) \{\s*toggleNode\(node\);\s*return;/)
  assert.doesNotMatch(sidebar, /findFirstLeaf/)
})

test('navbar uses live counters and authenticated user identity', () => {
  const navbar = source('src/layout/components/Navbar.vue')

  assert.match(navbar, /getHeaderStats/)
  assert.match(navbar, /userStore\.name/)
  assert.match(navbar, /headerAvatar/)
  assert.match(navbar, /totalWithdrawals/)
  assert.doesNotMatch(navbar, /<span class="quick-count">1979<\/span>/)
  assert.doesNotMatch(navbar, /<span class="account-name">gv01<\/span>/)
  assert.doesNotMatch(navbar, /class="account-brand"/)
  assert.doesNotMatch(navbar, /class="account-divider"/)
  assert.match(navbar, /name:\s*"Profile"/)
})
