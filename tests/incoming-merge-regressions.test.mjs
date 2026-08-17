import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const source = (path) => readFileSync(new URL(`../${path}`, import.meta.url), "utf8");

test("merged goods pages preserve the active timezone and backend query contract", () => {
  const goods = source("src/views/member/goods/index.vue");
  const goodsTypes = source("src/views/member/goodstype/index.vue");

  assert.match(goods, /column\.key === 'createTime'[\s\S]*?parseTime\(record\.createTime\)/);
  assert.match(goodsTypes, /column\.dataIndex === 'createTime'[\s\S]*?parseTime\(record\.createTime\)/);
  assert.doesNotMatch(goods, /function formatDateTime\(/);
  assert.doesNotMatch(goodsTypes, /function formatDateTime\(/);

  assert.match(goods, /beginPrice:\s*minPrice \?\? undefined/);
  assert.match(goods, /endPrice:\s*maxPrice \?\? undefined/);
  assert.match(goods, /typeTitle:\s*"g\.typeId"/);
  assert.match(goods, /enabled:\s*"g\.isEnabled"/);
});

test("merged navigation keeps dark-mode visibility, closable tabs and top-level icon fallbacks", () => {
  const variables = source("src/assets/styles/variables.module.scss");
  const tags = source("src/layout/components/TagsView/index.vue");
  const layout = source("src/layout/index.vue");
  const topNav = source("src/components/TopNav/index.vue");
  const sidebar = source("src/layout/components/Sidebar/index.vue");

  assert.match(variables, /html\.dark\s*\{[\s\S]*?--navbar-active-text:\s*#ffffff/);
  assert.match(variables, /html\.dark\s*\{[\s\S]*?--navbar-muted-text:\s*#d0d0d0/);
  assert.doesNotMatch(tags, /\.tags-view-close\s*\{\s*display:\s*none/);
  assert.doesNotMatch(layout, /openSideBar\(true\)/);
  assert.match(topNav, /v-else-if="item\.meta && item\.meta\.icon/);
  assert.match(sidebar, /v-else-if="node\.icon && node\.icon !== '#'"/);
  assert.doesNotMatch(sidebar, /child\.icon && child\.icon !== '#'/);
  assert.match(sidebar, /会员管理:\s*\[[\s\S]*?"会员管理",[\s\S]*?"授权记录",[\s\S]*?"等级管理"/);
});

test("goods import entry follows the existing goods creation permission", () => {
  const goods = source("src/views/member/goods/index.vue");

  assert.match(goods, /<a-button type="primary" v-hasPermi="\['member:goods:add'\]">[\s\S]*?<UploadOutlined/);
});
