import request from "@/utils/request";
import { apiPathParam } from "@/utils/apiPath";

// 查询时区管理列表
export function listZone(query) {
  return request({
    url: "/system/zone/list",
    method: "get",
    params: query,
  });
}

// 查询时区管理详细
export function getZone(id) {
  return request({
    url: "/system/zone/" + apiPathParam(id),
    method: "get",
  });
}

// 新增时区管理
export function addZone(data) {
  return request({
    url: "/system/zone",
    method: "post",
    data: data,
  });
}

// 修改时区管理
export function updateZone(data) {
  return request({
    url: "/system/zone",
    method: "put",
    data: data,
  });
}

// 删除时区管理
export function delZone(id) {
  return request({
    url: "/system/zone/" + apiPathParam(id),
    method: "delete",
  });
}

export function setActiveZone(id) {
  return request({
    url: `/system/zone/active/${apiPathParam(id)}`,
    method: "post",
  });
}

export function getZoneActive() {
  return request({
    url: "/system/zone/getActive",
    method: "get",
  });
}
