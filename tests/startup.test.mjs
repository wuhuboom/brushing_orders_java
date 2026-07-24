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
