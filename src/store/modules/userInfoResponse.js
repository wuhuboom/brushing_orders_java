const CONTRACT_ERROR_MESSAGE = "获取用户信息失败：后台用户信息接口返回格式不正确，请刷新后重新登录";

function isObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

/**
 * Normalize the management getInfo response without accepting the H5 profile
 * contract, whose data object does not contain roles or permissions.
 */
export function normalizeUserInfoResponse(response) {
  const payload = isObject(response?.user)
    ? response
    : isObject(response?.data?.user)
      ? response.data
      : null;
  const user = payload?.user;

  if (!isObject(user) || user.userId === undefined || typeof user.userName !== "string") {
    throw new Error(CONTRACT_ERROR_MESSAGE);
  }

  return payload;
}

export { CONTRACT_ERROR_MESSAGE };
