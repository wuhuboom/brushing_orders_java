import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import {
  CONTRACT_ERROR_MESSAGE,
  normalizeUserInfoResponse,
} from "../src/store/modules/userInfoResponse.js";

const adminUser = {
  userId: 1,
  userName: "admin",
  nickName: "Administrator",
  avatar: "",
};

test("normalizes the current top-level management getInfo contract", () => {
  const response = {
    code: 200,
    user: adminUser,
    roles: ["admin"],
    permissions: ["*:*:*"],
  };

  assert.equal(normalizeUserInfoResponse(response), response);
});

test("accepts an extra response data wrapper only when it still contains a management user", () => {
  const payload = {
    user: adminUser,
    roles: ["admin"],
    permissions: ["*:*:*"],
  };

  assert.equal(normalizeUserInfoResponse({ code: 200, data: payload }), payload);
});

test("allows a management user without an avatar so the store can use its default image", () => {
  const response = { user: { userId: 1, userName: "admin" } };

  assert.equal(normalizeUserInfoResponse(response).user.avatar, undefined);
});

test("rejects the H5 profile contract instead of reading avatar from undefined", () => {
  assert.throws(
    () => normalizeUserInfoResponse({ code: 200, data: { id: 1, username: "admin", avatar: "" } }),
    { message: CONTRACT_ERROR_MESSAGE },
  );
});

test("rejects missing, HTML, and incomplete management users with a clear contract error", () => {
  for (const response of [
    undefined,
    "<!DOCTYPE html><html><body>old SPA</body></html>",
    {},
    { user: null },
    { user: [] },
    { user: { avatar: "" } },
  ]) {
    assert.throws(() => normalizeUserInfoResponse(response), { message: CONTRACT_ERROR_MESSAGE });
  }
});

test("the user store normalizes getInfo before reading the avatar", () => {
  const source = readFileSync(new URL("../src/store/modules/user.js", import.meta.url), "utf8");
  const normalizeIndex = source.indexOf("const payload = normalizeUserInfoResponse(res)");
  const avatarIndex = source.indexOf("let avatar = user.avatar ||");

  assert.ok(normalizeIndex >= 0);
  assert.ok(avatarIndex > normalizeIndex);
  assert.match(source, /this\.permissions = payload\.permissions \|\| \[\]/);
});

test("getInfo bypasses stale browser caches that can return the old SPA HTML", () => {
  const source = readFileSync(new URL("../src/api/login.js", import.meta.url), "utf8");
  const getInfoBody = source.match(/export function getInfo\(\) \{[\s\S]*?\n\}/)?.[0] || "";

  assert.match(getInfoBody, /params:\s*\{\s*_t:\s*Date\.now\(\)\s*\}/);
  assert.match(getInfoBody, /['"]Cache-Control['"]:\s*['"]no-cache['"]/);
  assert.match(getInfoBody, /Pragma:\s*['"]no-cache['"]/);
});
