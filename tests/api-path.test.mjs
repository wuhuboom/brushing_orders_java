import assert from "node:assert/strict";
import test from "node:test";

import { apiPathParam, encodeApiPath } from "../src/utils/apiPath.js";

test("encodes user-controlled API path segments", () => {
  assert.equal(
    encodeApiPath("/monitor/logininfor/unlock/张 三"),
    "/monitor/logininfor/unlock/%E5%BC%A0%20%E4%B8%89"
  );
  assert.equal(
    encodeApiPath(
      `/monitor/cache/getValue/${apiPathParam("cache:name")}/${apiPathParam("a/b")}`
    ),
    "/monitor/cache/getValue/cache%3Aname/a%2Fb"
  );
});

test("does not double encode path values or alter query strings", () => {
  assert.equal(
    encodeApiPath("/system/config/configKey/site%2Ename?lang=zh_CN"),
    "/system/config/configKey/site.name?lang=zh_CN"
  );
  assert.equal(encodeApiPath("https://example.test/a b"), "https://example.test/a b");
});
