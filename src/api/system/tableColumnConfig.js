import request from "@/utils/request";
import { apiPathParam } from "@/utils/apiPath";

const TABLE_COLUMN_CONFIG_URL = "/system/user/table-column-config";

export function getTableColumnConfig(tableKey) {
  return request({
    url: `${TABLE_COLUMN_CONFIG_URL}/${apiPathParam(tableKey)}`,
    method: "get",
    params: { _t: Date.now() },
    headers: {
      "Cache-Control": "no-cache",
      Pragma: "no-cache",
    },
  });
}

export function saveTableColumnConfig(tableKey, config) {
  return request({
    url: `${TABLE_COLUMN_CONFIG_URL}/${apiPathParam(tableKey)}`,
    method: "put",
    headers: { repeatSubmit: false },
    data: config,
  });
}

export function resetTableColumnConfig(tableKey) {
  return request({
    url: `${TABLE_COLUMN_CONFIG_URL}/${apiPathParam(tableKey)}`,
    method: "delete",
  });
}
