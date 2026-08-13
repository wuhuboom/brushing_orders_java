import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'

const source = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('startup configuration has an immediate environment fallback and no polling', () => {
  const html = source('index.html')
  const main = source('src/main.js')

  assert.match(html, /\/config\/config\.js/)
  assert.match(html, /%VITE_APP_BASE_API%/)
  assert.doesNotMatch(html, /Date\.now\(\)|new Date\(\)\.getTime\(\)/)
  assert.doesNotMatch(main, /waitForConfig|setTimeout\(waitForConfig/)
  assert.match(main, /import\.meta\.env\.VITE_APP_BASE_API/)
})

test('runtime configuration is included in the public build input', () => {
  assert.equal(existsSync(new URL('../public/config/config.js', import.meta.url)), true)
})

test('protected timezone initialization only runs after authentication', () => {
  const main = source('src/main.js')
  const permission = source('src/permission.js')
  const timezoneHelper = source('src/utils/timezone-helper.js')
  const request = source('src/utils/request.js')

  assert.doesNotMatch(main, /refreshActiveTimeZone/)
  assert.ok(permission.indexOf('refreshActiveTimeZone()') > permission.indexOf('.getInfo()'))
  assert.match(timezoneHelper, /if \(!getToken\(\)\) \{\s*return getActiveTimeZone\(\)/)
  assert.match(request, /if \(!getToken\(\) \|\| isAuthenticationPage\) \{\s*return Promise\.reject\(new Error\(msg\)\)/)
})
