import test from "node:test";
import assert from "node:assert/strict";
import { resolveApiResourceUrl } from "../src/utils/apiResourceUrl.js";

test("runtime API config produces absolute rich-text resource URLs", () => {
  assert.equal(
    resolveApiResourceUrl(
      "/profile/upload/dev.jpg",
      "/dev-api",
      "http://admin.example"
    ),
    "http://admin.example/dev-api/profile/upload/dev.jpg"
  );
  assert.equal(
    resolveApiResourceUrl(
      "/profile/upload/prod.jpg",
      "https://api.example/prod-api/",
      "https://admin.example"
    ),
    "https://api.example/prod-api/profile/upload/prod.jpg"
  );
  assert.equal(
    resolveApiResourceUrl(
      "/profile/upload/root.jpg",
      "https://api.example",
      "https://admin.example"
    ),
    "https://api.example/profile/upload/root.jpg"
  );
});

test("already absolute upload URLs are preserved", () => {
  assert.equal(
    resolveApiResourceUrl(
      "https://cdn.example/upload/image.jpg",
      "/dev-api",
      "http://admin.example"
    ),
    "https://cdn.example/upload/image.jpg"
  );
});
