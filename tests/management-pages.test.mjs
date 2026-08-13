import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createNotificationTemplate } from '../src/views/member/orderconfig/notificationKinds.js'

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
  assert.match(member, /String\(tradeForm\.tradePassword \|\| ""\)\.trim\(\)/)
  assert.match(member, /tradePassword:\s*normalizedPassword/)
  assert.match(form, /:disabled="readonly"/)
})

test('continuous order settings preserve the legacy list and editor workflow', () => {
  const drawer = source('src/views/member/orderuser/components/OrderlinkDrawer.vue')

  assert.match(drawer, /width="85%"/)
  assert.match(drawer, /member-summary-tip/)
  assert.match(drawer, /priceRange/)
  assert.match(drawer, /createTimeRange/)
  assert.match(drawer, /negativePriceCount/)
  assert.match(drawer, /label:\s*"待提交",\s*value:\s*"3"/)
  assert.match(drawer, /record\?\.displayStatus\s*\?\?\s*record\?\.status/)
  assert.match(drawer, /isEditableStatus\(resolvedStatus\(record\)\)/)
  assert.match(drawer, /onClick:\s*\(\)\s*=>\s*addProductToDetails\(record\)/)
  assert.match(drawer, /goodsQuery\.typeId/)
  assert.match(drawer, /goodsQuery\.priceRange/)
  assert.doesNotMatch(drawer, /onDblclick/)
})

test('bonus member search never serializes focus events as username parameters', () => {
  const drawer = source('src/views/member/orderuser/components/OrderuserBonusDrawer.vue')

  assert.match(drawer, /@focus="loadUsers\(\)"/)
  assert.match(drawer, /typeof keyword === "string"\s*\?\s*keyword\.trim\(\)\s*:\s*""/)
  assert.match(drawer, /username:\s*normalizedKeyword\s*\|\|\s*undefined/)
})

test('bonus receipt and distribution remain explicit administrator actions', () => {
  const drawer = source('src/views/member/orderuser/components/OrderuserBonusDrawer.vue')

  assert.match(drawer, /@click="handleReceive\(record\)"/)
  assert.match(drawer, /@click="handleGive\(record\)"/)
  assert.match(drawer, /:disabled="!canGive\(record\)"/)
  assert.match(drawer, /row\.distributionType !== "2" \|\| taskGroupCompleted\.value/)
  assert.match(drawer, /receiveBonus\(row\.id\)/)
  assert.match(drawer, /giveBonus\(row\.id\)/)
})

test('notification settings expose validation failures and wait for submission', () => {
  const settings = source('src/views/member/orderconfig/components/notification-config.vue')
  const page = source('src/views/member/orderconfig/index.vue')

  assert.match(settings, /const firstError = error\?\.errorFields\?\.\[0\]/)
  assert.match(settings, /activeTab\.value = invalidTab/)
  assert.match(settings, /message\.error\(firstError\?\.errors\?\.\[0\]/)
  assert.doesNotMatch(settings, /\.catch\(\(\) => \{\}\)/)
  assert.match(page, /await componentRef\.value\.handleSubmit\(\)/)
})

test('shared rich text editor matches legacy pixel font sizes', () => {
  const editor = source('src/components/Editor/index.vue')
  const expectedSizes = [
    '12px', '14px', '16px', '18px', '20px', '24px', '28px', '30px',
    '32px', '36px', '40px', '48px', '56px', '64px', '72px', '96px',
    '120px', '144px'
  ]

  for (const size of expectedSizes) {
    assert.match(editor, new RegExp(`"${size}"`))
  }
  assert.match(editor, /Quill\.import\("attributors\/style\/size"\)/)
  assert.match(editor, /size === "14px" \? false : size/)
  assert.match(editor, /\[\{\s*size:\s*FONT_SIZE_OPTIONS\s*\}\]/)
  assert.match(editor, /font-size:\s*14px/)
  assert.match(editor, /content:\s*attr\(data-value\)/)
  assert.match(editor, /\.ql-toolbar\.ql-snow\)[\s\S]*?z-index:\s*2/)
  assert.match(editor, /max-height:\s*320px[\s\S]*?overflow-y:\s*auto/)
})

test('incomplete legacy notification templates are normalized to disabled', () => {
  assert.equal(createNotificationTemplate({
    enabled: 1,
    title: '',
    content: '<p>Deposit completed</p>',
  }).enabled, 0)
  assert.equal(createNotificationTemplate({
    enabled: 1,
    title: 'Balance changed',
    content: '<p>Deposit completed</p>',
  }).enabled, 1)
})

test('layout settings persist the active schema and independent visual choices', () => {
  const settings = source('src/layout/components/Settings/index.vue')
  const defaults = source('src/settings.js')
  const layout = source('src/layout/index.vue')
  const app = source('src/App.vue')

  assert.match(defaults, /showSettings:\s*true/)
  assert.doesNotMatch(defaults, /showSettings:\s*import\.meta\.env\.DEV/)
  assert.match(layout, /class="layout-setting-trigger"/)
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

test('financial audit lists match the legacy columns, filters and page size', () => {
  const recharge = source('src/views/member/recharge/index.vue')
  const withdrawal = source('src/views/member/withdrawal/index.vue')
  const flow = source('src/views/member/flow/index.vue')

  assert.match(recharge, /title="充值记录列表"/)
  assert.match(recharge, /title:\s*"充值账户"/)
  assert.match(recharge, /title:\s*"出金类型"/)
  assert.match(recharge, /title:\s*"最后修改人"/)
  assert.match(recharge, /title:\s*"最后修改时间"/)
  assert.match(recharge, /title:\s*"转换后金额"[\s\S]*?hidden:\s*true/)
  assert.match(recharge, /title="通过选中的记录？"/)
  assert.match(recharge, /openReviewDialog\(record, 'reject'\)/)
  assert.match(recharge, /openReviewDialog\(record, 'remark'\)/)
  assert.match(recharge, /dialogMode\.value = mode/)
  assert.match(recharge, /mode === "reject" \? "拒绝" : "备注"/)
  assert.match(recharge, /title:\s*"操作"[\s\S]*?width:\s*180/)
  assert.match(recharge, /title="显示"[\s\S]*?handleHidden\('1'\)/)
  assert.match(recharge, /title="隐藏"[\s\S]*?handleHidden\('0'\)/)
  assert.match(recharge, /<EyeOutlined \/>显示/)
  assert.match(recharge, /<EyeInvisibleOutlined \/>隐藏/)
  assert.match(recharge, /queryParams\.accountAddress/)
  assert.match(recharge, /queryParams\.amountMin/)
  assert.match(recharge, /queryParams\.amountMax/)
  assert.match(recharge, /queryParams\.isFake/)
  assert.match(recharge, /pageSize:\s*20/)
  assert.match(recharge, /v-model:value="queryParams\.username"/)
  assert.doesNotMatch(recharge, /v-model:value="queryParams\.userName"/)

  assert.match(withdrawal, /title="提现记录列表"/)
  assert.match(withdrawal, /title:\s*"附件"/)
  assert.match(withdrawal, /title:\s*"最后修改人"/)
  assert.match(withdrawal, /title:\s*"最后修改时间"/)
  assert.match(withdrawal, /title:\s*"转换后金额"[\s\S]*?hidden:\s*true/)
  assert.match(withdrawal, /title="通过选中的记录？"/)
  assert.match(withdrawal, /openReviewDialog\(record, 'reject'\)/)
  assert.match(withdrawal, /openReviewDialog\(record, 'remark'\)/)
  assert.match(withdrawal, /dialogMode\.value = mode/)
  assert.match(withdrawal, /mode === "reject" \? "拒绝" : "备注"/)
  assert.match(withdrawal, /title="提现地址"/)
  assert.match(withdrawal, /label="账户名称"/)
  assert.match(withdrawal, /label="钱包名称"/)
  assert.match(withdrawal, /label="用户钱包地址"/)
  assert.match(withdrawal, /updateSensitiveWithdrawalAccount/)
  assert.match(withdrawal, /<CopyOutlined \/>/)
  assert.match(withdrawal, /<EyeOutlined \/>显示/)
  assert.match(withdrawal, /<EyeInvisibleOutlined \/>隐藏/)
  assert.match(withdrawal, /title="显示"[\s\S]*?handleHidden\('1'\)/)
  assert.match(withdrawal, /title="隐藏"[\s\S]*?handleHidden\('0'\)/)
  assert.match(withdrawal, /import useUserStore from "@\/store\/modules\/user"/)
  assert.match(withdrawal, />提现地址<\/a-button>/)
  assert.match(withdrawal, /queryParams\.accountAddress/)
  assert.match(withdrawal, /queryParams\.amountMin/)
  assert.match(withdrawal, /queryParams\.amountMax/)
  assert.match(withdrawal, /queryParams\.isFake/)
  assert.match(withdrawal, /order_zhlx\.value \|\| \[\]/)
  assert.match(withdrawal, /pageSize:\s*20/)

  assert.match(flow, /title="交易流水列表"/)
  assert.match(flow, /label="金额"/)
  assert.doesNotMatch(flow, /label="金额范围"/)
  assert.match(flow, /key:\s*"serialCode"[\s\S]*?fixed:\s*"left"/)
  assert.match(flow, /<EyeOutlined \/>显示/)
  assert.match(flow, /<EyeInvisibleOutlined \/>隐藏/)
  assert.match(flow, /title="显示"[\s\S]*?handleHidden\('1'\)/)
  assert.match(flow, /title="隐藏"[\s\S]*?handleHidden\('0'\)/)
  assert.match(flow, /pageSize:\s*20/)
})

test('order details match the legacy filters, columns and row actions', () => {
  const page = source('src/views/member/orderinfo/index.vue')
  const api = source('src/api/member/orderinfo.js')

  assert.match(page, /title="订单明细列表"/)
  assert.match(page, /label="用户名"/)
  assert.match(page, /label="创建时间"/)
  assert.match(page, /label="明细编号"/)
  assert.match(page, /label="类型"/)
  assert.match(page, /label="单数"/)
  assert.match(page, /label="金额"/)
  assert.match(page, /label="状态"/)
  assert.match(page, /label="过期时间"/)
  assert.match(page, /label="商品标题"/)
  assert.match(page, /label="额外佣金"/)
  assert.match(page, /pageSize:\s*20/)
  assert.doesNotMatch(page, /:row-selection=/)
  assert.match(page, /title:\s*"明细编号"[\s\S]*?fixed:\s*"left"/)
  assert.match(page, /title:\s*"操作"[\s\S]*?fixed:\s*"right"/)
  assert.match(page, /formatPercentage\(record\.rebatePercentage\)/)
  assert.match(page, /formatPercentage\(record\.upperRebatePercentage\)/)
  assert.match(page, /title="取消选中的记录？"/)
  assert.match(page, /String\(record\.status\) !== '1'/)
  assert.match(page, /v-model:open="editOpen"[\s\S]*?title="修改"/)
  assert.match(page, /label="过期时间"[\s\S]*?placeholder="过期时间"/)
  assert.match(page, /v-model:open="commentOpen"[\s\S]*?title="评论"/)
  assert.match(page, /title:\s*"评论"/)
  assert.match(page, /title:\s*"评分"/)
  assert.match(api, /url:\s*'\/member\/orderinfo\/'\s*\+\s*id\s*\+\s*'\/cancel'/)
  assert.match(api, /method:\s*'put'/)
})

test('member sensitive drawers and mutations are hidden behind matching permissions', () => {
  const member = source('src/views/member/orderuser/index.vue')
  const bonus = source('src/views/member/orderuser/components/OrderuserBonusDrawer.vue')
  const extraCommission = source('src/views/member/orderuser/components/OrderuserExtracommissionDrawer.vue')

  assert.match(member, /@click="handleTransaction\(record\)" v-hasPermi="\['member:orderuser:edit'\]"/)
  assert.match(member, /@click="handleOpenLink\(record\)" v-hasPermi="\['member:orderlink:list'\]"/)
  assert.match(member, /@click="handleOpenBonus\(record\)" v-hasPermi="\['member:bonus:list'\]"/)
  assert.match(member, /@click="handleOpenWithdrawal\(record\)" v-hasPermi="\['member:withdrawalAcc:list'\]"/)
  assert.match(member, /@click="handleExtraCommission\(record\)" v-hasPermi="\['member:extracommission:list'\]"/)
  assert.match(member, /@click="handleModifyLoginPassword\(record\)" v-hasPermi="\['member:orderuser:edit'\]"/)
  assert.match(member, /@click="handleModifyTradePassword\(record\)" v-hasPermi="\['member:orderuser:edit'\]"/)
  assert.match(member, /if \(hasAnyPermission\(\[[\s\S]*?member:orderuser:query[\s\S]*?getLevel\(\)/)

  assert.match(bonus, /@click="handleReceive\(record\)"[\s\S]{0,100}v-hasPermi="\['member:bonus:receive'\]"/)
  assert.match(bonus, /@click="handleGive\(record\)"[\s\S]{0,100}v-hasPermi="\['member:bonus:give'\]"/)
  assert.match(bonus, /v-hasPermi="\['member:bonus:query'\]"[\s\S]{0,120}@click="handleView\(record\)"/)
  assert.match(bonus, /v-hasPermi="\['member:bonus:edit'\]"[\s\S]{0,120}@click="handlePushEdit\(record\)"/)

  assert.match(extraCommission, /@click="handleAdd" v-hasPermi="\['member:extracommission:add'\]"/)
  assert.match(extraCommission, /@click="handleEdit\(record\)" v-hasPermi="\['member:extracommission:edit'\]"/)
  assert.match(extraCommission, /@click="handleDelete\(record\)" v-hasPermi="\['member:extracommission:remove'\]"/)
})
