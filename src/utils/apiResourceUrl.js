/**
 * 使用运行时 API 配置生成可被其他前端直接访问的绝对资源地址。
 */
export function resolveApiResourceUrl(resourcePath, baseApiUrl = "", pageOrigin = "") {
  const resource = String(resourcePath || "").trim();
  if (!resource) {
    return "";
  }
  if (/^(?:https?:)?\/\//i.test(resource) || /^(?:data|blob):/i.test(resource)) {
    return resource;
  }

  const origin = String(pageOrigin || "").replace(/\/+$/, "");
  const apiBase = String(baseApiUrl || "").trim().replace(/\/+$/, "");
  const resourceName = resource.replace(/^\/+/, "");
  const configuredUrl = apiBase
    ? `${apiBase}/${resourceName}`
    : `/${resourceName}`;

  return new URL(configuredUrl, `${origin}/`).toString();
}
