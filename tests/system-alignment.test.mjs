import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

function assertOrdered(content, values) {
  let cursor = -1
  for (const value of values) {
    const next = content.indexOf(value, cursor + 1)
    assert.ok(next > cursor, `expected ${value} after position ${cursor}`)
    cursor = next
  }
}

test('system navigation exposes exactly the four legacy groups and ten canonical pages', () => {
  const permission = source('src/store/modules/permission.js')
  assertOrdered(permission, ["path: 'user'", "path: 'permission'", "path: 'file'", "path: 'operationLog'"])
  for (const route of ['orgs', 'positions', 'users', 'strategies', 'roles', 'groups', 'fileReferences', 'files', 'operationLogs', 'userLoginLogs']) {
    assert.match(permission, new RegExp(`path: '${route}'`))
  }
  assert.doesNotMatch(permission, /LegacySystemMenus/)
})

test('organization, position, strategy and group lists use legacy action boundaries', () => {
  for (const path of ['dept/index.vue', 'post/index.vue', 'strategy/index.vue', 'group/index.vue']) {
    const content = source(`src/views/system/${path}`)
    assert.match(content, /删除/)
    assert.match(content, /创建/)
    assert.match(content, />修改<\/a-button>/)
    assert.match(content, />复制<\/a-button>/)
  }
  assert.match(source('src/views/system/strategy/index.vue'), /资源列表/)
  assert.match(source('src/views/system/group/index.vue'), /策略列表/)
  assert.match(source('src/views/system/group/index.vue'), /资源列表/)
  assert.match(source('src/views/system/dept/index.vue'), /width="800px"[\s\S]*layout="vertical"/)
  assert.match(source('src/views/system/post/index.vue'), /width="800px"[\s\S]*a-checkbox-group/)
  assert.match(source('src/views/system/strategy/index.vue'), /a-drawer[\s\S]*width="800px"/)
  assert.match(source('src/views/system/group/index.vue'), /a-drawer[\s\S]*width="800px"[\s\S]*a-checkbox-group/)
})

test('users expose legacy filters, columns, editor lists and google authenticator', () => {
  const user = source('src/views/system/user/index.vue')
  assertOrdered(user, ['用户名', '手机号码', '是否在线', '组织', '名称', '性别', '最后登录IP', '是否启用', '是否冻结', '邮箱', '是否内置', '创建时间'])
  for (const label of ['登录信息', '登录解冻', '谷歌验证器', '角色列表', '职位列表', '分组列表']) assert.match(user, new RegExp(label))
  assert.match(user, /a-drawer[\s\S]*width="1000px"/)
  assert.equal((user.match(/<a-checkbox-group\b/g) || []).length, 3)
  assert.doesNotMatch(user, /initPassword = ref\('123456'\)/)
  assert.doesNotMatch(user, /重置密码/)
})

test('roles save strategy, resource and data-row permissions in one payload', () => {
  const role = source('src/views/system/role/index.vue')
  assertOrdered(role, ['代码', '名称', '是否内置', '备注', '策略列表', '资源列表', '数据行权限'])
  assert.doesNotMatch(role, /hidePhone|隐藏手机号码/)
  assert.match(role, /strategyIds/)
  assert.match(role, /dataRules/)
  assert.match(role, /DEPT_AND_CHILD/)
  assert.match(role, /a-drawer[\s\S]*width="70%"/)
  assert.match(role, /添加一行数据/)
  assert.doesNotMatch(role, /saveRoleAlignment\(form/)
})

test('file pages keep references and physical files separate with legacy metadata', () => {
  const reference = source('src/views/system/file/reference.vue')
  const file = source('src/views/system/file/index.vue')
  assertOrdered(reference, ["title: 'ID'", "title: '文件ID'", "title: '名称'", "title: '媒体'", "title: '关联类型'", "title: '关联ID'", "title: '创建时间'", "title: '备注'"])
  assertOrdered(file, ["title: 'ID'", "title: '桶'", "title: '类型'", "title: '哈希'", "title: '内容类型'", "title: '媒体'", "title: '大小'", "title: '创建时间'", "title: '备注'"])
  assert.match(reference, /ADMIN_UPLOAD/)
  assert.match(reference, /EyeOutlined/)
  assert.match(file, /record\.fileSize \?\? '-'/)
  assert.doesNotMatch(file, /formatSize/)
  assert.doesNotMatch(reference, /key: 'operation'/)
  assert.doesNotMatch(file, /key: 'operation'/)
})

test('operation and login logs expose all legacy audit fields', () => {
  const operation = source('src/views/monitor/operlog/index.vue')
  for (const label of ['资源', '用户名', 'IP', '地址', '状态', '创建时间', '耗时(毫秒)', '方法', '请求地址', '请求头', '查询', '参数', '消息体', '消息']) assert.match(operation, new RegExp(label.replace(/[()]/g, '\\$&')))
  const login = source('src/views/monitor/logininfor/index.vue')
  for (const label of ['用户名', 'IP', '地址', '是否成功', '创建时间', '请求头', '参数']) assert.match(login, new RegExp(label))
})
