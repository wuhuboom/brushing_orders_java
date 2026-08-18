import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const authSource = await readFile(
  new URL("../src/utils/auth.js", import.meta.url),
  "utf8",
);

test("admin token cookie is scoped to the order project", () => {
  assert.match(authSource, /const TokenKey = ['"]Order-Admin-Token['"]/);
  assert.doesNotMatch(authSource, /const TokenKey = ['"]Admin-Token['"]/);
});
