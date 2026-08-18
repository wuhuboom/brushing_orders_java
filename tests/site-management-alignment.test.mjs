import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { translationLanguages } from '../src/views/member/components/translationLanguages.js'

const frontendSource = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const backendSource = (path) => readFileSync(new URL(`../../java-admin-new-code/${path}`, import.meta.url), 'utf8')

test('banner keeps the reference values while adapting the local legacy contract', () => {
  const page = frontendSource('src/views/member/banner/index.vue')

  assert.match(page, /\{ label: "首页", value: "1" \}[\s\S]*\{ label: "任务", value: "2" \}[\s\S]*\{ label: "登录", value: "3" \}[\s\S]*\{ label: "合作伙伴", value: "4" \}/)
  assert.match(page, /<a-badge status="processing" :text="dictText\(bannerTypeOptions, record\.type\)"/)
  assert.match(page, /String\(record\.isEnabled\) === '1' \? 'success' : 'error'/)
  assert.match(page, /function toBackendBannerType\(value\)[\s\S]*mapNumberBoundary\(value, -1\)/)
  assert.match(page, /function toUiBannerType\(value\)[\s\S]*mapNumberBoundary\(value, 1\)/)
  assert.match(page, /type: toBackendBannerType\(query\.type\)[\s\S]*isEnabled: invertEnabled\(query\.isEnabled\)/)
  assert.match(page, /isEnabled: "1"/)
  assert.match(page, /width="60%"/)
  assert.match(page, /title: "名称"[^\n]*width: 200[^\n]*fixed: "left"/)
  assert.match(page, /title: "操作"[^\n]*width: 80[^\n]*fixed: "right"/)
  assert.match(page, /columnWidth: 32/)
  assert.match(page, /font-size: 15px/)
})

test('customer service preserves its opposite enabled semantics and reference layout', () => {
  const page = frontendSource('src/views/member/cusservice/index.vue')

  assert.match(page, /\{ label: "禁用", value: "0" \}[\s\S]*\{ label: "启用", value: "1" \}/)
  assert.match(page, /String\(record\.isEnabled\) === '1' \? 'success' : 'error'/)
  assert.match(page, /compactQuery = ref\(typeof window !== "undefined" && window\.innerWidth <= 1366\)/)
  assert.match(page, /v-show="!compactQuery \|\| expanded"/)
  assert.match(page, /width="60%"/)
  assert.match(page, /:width="32" :height="32"/)
  assert.match(page, /title: "名称"[^\n]*width: 200[^\n]*fixed: "left"/)
  assert.match(page, /title: "链接"[^\n]*width: 300/)
  assert.match(page, /title: "操作"[^\n]*width: 80[^\n]*fixed: "right"/)
  assert.match(page, /columnWidth: 32/)
  assert.match(page, /\.customer-service-page\s*\{[\s\S]*?margin-top:\s*44px/)
})

test('bulletin maps enabled values at every API boundary and uses both default sorts', () => {
  const page = frontendSource('src/views/member/bulletin/index.vue')

  assert.match(page, /\{ label: "禁用", value: "0" \}[\s\S]*\{ label: "启用", value: "1" \}/)
  assert.match(page, /function flipEnabled\(value\)[\s\S]*String\(value\) === "0"[\s\S]*return "1"[\s\S]*String\(value\) === "1"[\s\S]*return "0"/)
  assert.match(page, /payload\.isEnabled = flipEnabled\(payload\.isEnabled\)/)
  assert.match(page, /rows\.value = \(response\.rows \|\| \[\]\)\.map\(fromApiRecord\)/)
  assert.match(page, /updateLegacy\(resource, toApiRecord\(form\)\)/)
  assert.match(page, /addLegacy\(resource, toApiRecord\(form\)\)/)
  assert.match(page, /String\(record\.isEnabled\) === '1' \? 'success' : 'error'/)
  assert.match(page, /width="70%"/)
  assert.match(page, /title: "标题"[^\n]*width: 500[^\n]*fixed: "left"/)
  assert.match(page, /title: "序号"[^\n]*defaultSortOrder: "ascend"/)
  assert.match(page, /title: "创建时间"[^\n]*defaultSortOrder: "descend"/)
  assert.match(page, /<editor v-model="form\.content" :min-height="527"/)
})

test('bulletin translations expose the exact 17-language reference sequence in a drawer', () => {
  const drawer = frontendSource('src/views/member/message/NoticeTranslationModal.vue')
  const excluded = new Set(['zhCn', 'zhTw', 'thTh', 'ptPt'])
  const actual = translationLanguages.filter((language) => !excluded.has(language.field))

  assert.deepEqual(actual.map((language) => language.field), [
    'enUs', 'jaJp', 'arSa', 'esEs', 'svSe', 'itIt', 'deDe', 'noNo', 'ruRu',
    'huHu', 'plPl', 'skSk', 'frFr', 'csCz', 'ptBr', 'hiIn', 'koKr',
  ])
  assert.match(drawer, /<a-drawer/)
  assert.match(drawer, /width="70%"/)
  assert.match(drawer, /:tab="`\$\{language\.flag\} \$\{language\.label\}`"/)
  assert.match(drawer, /<a-textarea[^>]*:rows="3"/)
  assert.match(drawer, /:min-height="551"/)
  assert.match(drawer, /取 消[\s\S]*确 定/)
})

test('withdrawal type exposes original 1/2 values without leaking the local 0/1 storage contract', () => {
  const page = frontendSource('src/views/member/withdrawaltype/index.vue')

  assert.match(page, /\{ label: "银行卡", value: "1" \}[\s\S]*\{ label: "网络", value: "2" \}/)
  assert.match(page, /\(\{ "1": "0", "2": "1" \}\)\[String\(value\)\]/)
  assert.match(page, /\(\{ "0": "1", "1": "2" \}\)\[String\(value\)\]/)
  assert.match(page, /payload\.type = toApiType\(payload\.type\)/)
  assert.match(page, /rows\.value = \(response\.rows \|\| \[\]\)\.map\(fromApiRecord\)/)
  assert.match(page, /String\(record\.type\) === '2' \? 'success' : 'processing'/)
  assert.match(page, /String\(record\.hasPrivateKey\) === '1' \? 'success' : 'error'/)
  assert.match(page, /width="80%"/)
  assert.match(page, /title: "ID"[^\n]*width: 120[^\n]*fixed: "left"/)
  assert.match(page, /title: "参数"[^\n]*width: 400/)
  assert.match(page, /title: "操作"[^\n]*width: 80[^\n]*fixed: "right"/)
  assert.match(page, /v-if="form\.type !== '2'"/)
  assert.match(page, /v-if="form\.type === '2'"/)
  assert.match(page, /v-model:value="form\.abi" :rows="4"/)
  assert.match(page, /v-model:value="form\.networkName" :rows="10"/)
  assert.match(page, /feePrivateKey: undefined/)
})

test('local APIs and mappers retain the real backend data sources and safe private-key read contract', () => {
  const bannerApi = frontendSource('src/api/member/banner.js')
  const customerApi = frontendSource('src/api/member/cusservice.js')
  const legacyApi = frontendSource('src/api/member/legacy.js')
  const withdrawalApi = frontendSource('src/api/member/withdrawaltype.js')
  const bannerMapper = backendSource('order-member/src/main/resources/mapper/GoodsBannerMapper.xml')
  const customerMapper = backendSource('order-member/src/main/resources/mapper/GoodsCustomerServiceMapper.xml')
  const bulletinMapper = backendSource('order-member/src/main/resources/mapper/LegacyMarketingMapper.xml')
  const withdrawalMapper = backendSource('order-member/src/main/resources/mapper/OrderWithdrawalTypeMapper.xml')

  assert.match(bannerApi, /url: '\/member\/banner\/list'/)
  assert.match(customerApi, /url: '\/member\/cusservice\/list'/)
  assert.match(legacyApi, /const baseUrl = '\/member\/legacy'/)
  assert.match(withdrawalApi, /url: '\/member\/withdrawaltype\/list'/)
  assert.match(bannerMapper, /from goods_banner[\s\S]*order by sort_order asc, create_time desc, id desc/)
  assert.match(customerMapper, /from goods_customer_service[\s\S]*order by sort_order asc, id asc/)
  assert.match(bulletinMapper, /from legacy_bulletin[\s\S]*order by sort_order asc, create_time desc, id desc/)
  assert.match(withdrawalMapper, /case when fee_private_key is not null[\s\S]*end as has_private_key/)
  assert.doesNotMatch(withdrawalMapper.match(/<sql id="selectOrderWithdrawalTypeVo">[\s\S]*?<\/sql>/)?.[0] ?? '', /fee_private_key\s*,/)
  assert.match(withdrawalMapper, /order by sort_order asc, id asc/)
})
