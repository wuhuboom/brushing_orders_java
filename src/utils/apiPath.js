/**
 * Encodes every relative API path segment once while leaving query strings intact.
 * This centralizes path-parameter handling for all wrappers under src/api.
 *
 * @param {string} url relative request URL
 * @returns {string} URL safe for transport
 */
export function encodeApiPath(url) {
  if (typeof url !== "string" || !url.startsWith("/")) {
    return url;
  }
  const queryIndex = url.indexOf("?");
  const path = queryIndex < 0 ? url : url.slice(0, queryIndex);
  const query = queryIndex < 0 ? "" : url.slice(queryIndex);
  const encodedPath = path
    .split("/")
    .map((segment) => {
      if (!segment) return segment;
      try {
        return encodeURIComponent(decodeURIComponent(segment));
      } catch {
        return encodeURIComponent(segment);
      }
    })
    .join("/");
  return encodedPath + query;
}

/**
 * Encodes one value before it is interpolated into a path template.
 *
 * @param {unknown} value path parameter value
 * @returns {string} encoded path parameter
 */
export function apiPathParam(value) {
  return encodeURIComponent(String(value ?? ""));
}
