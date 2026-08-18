import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('timezone management is restored beneath customer website management', () => {
  const permission = source('src/store/modules/permission.js')

  assert.match(permission, /route\.path === 'siteManage' \|\| route\.meta\?\.title === '网站管理'/)
  assert.match(permission, /component: 'system\/zone\/index'/)
  assert.match(permission, /auth\.hasPermi\('system:zone:list'\)/)
  assert.match(permission, /siteManagementRoute\.children\.push\(timeZoneRoute\)/)
})

test('header shows a live clock in the active timezone and links to its settings page', () => {
  const navbar = source('src/layout/components/Navbar.vue')

  assert.match(navbar, /class="timezone-clock"/)
  assert.match(navbar, /当前时区/)
  assert.match(navbar, /getActiveTimeZone\(\)/)
  assert.match(navbar, /window\.setInterval\(refreshTimeZoneClock, 1000\)/)
  assert.match(navbar, /:disabled="!hasTimeZoneSettingsRoute"/)
  assert.match(navbar, /if \(!hasTimeZoneSettingsRoute\.value\) return/)
  assert.match(navbar, /router\.push\(\{ name: "Zone" \}\)/)
  assert.doesNotMatch(navbar, /router\.push\(\{ path:/)
})
