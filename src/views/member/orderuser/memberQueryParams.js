import { normalizeMemberIdQuery } from "./memberQueryValidation.js";

// 列表和导出使用同一套筛选；导出只移除分页参数。
export function buildMemberQueryParams(query, { paginated = true } = {}) {
  const { createTimeRange, params, pageNum, pageSize, ...filters } = query;
  if (filters.id !== null && filters.id !== undefined && filters.id !== "") {
    const id = normalizeMemberIdQuery(filters.id);
    if (!id) throw new Error("ID 请输入有效的正整数");
    filters.id = id;
  }

  const requestParams = {
    ...filters,
    params: {
      ...params,
      beginTime: createTimeRange?.[0] || null,
      endTime: createTimeRange?.[1] || null,
    },
  };
  if (paginated) {
    requestParams.pageNum = pageNum;
    requestParams.pageSize = pageSize;
  }
  return requestParams;
}
