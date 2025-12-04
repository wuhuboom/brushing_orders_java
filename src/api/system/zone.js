import request from "@/utils/request";

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
    url: "/system/zone/" + id,
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
    url: "/system/zone/" + id,
    method: "delete",
  });
}

export function setActiveZone(id) {
  return request({
    url: `/system/zone/active/${id}`, // 如你的后端是 /system/timezone/active/{id} 就改这行
    method: "post",
  });
}

export function getZoneActive() {
  return request({
    url: "/system/zone/getActive",
    method: "get",
  });
}
