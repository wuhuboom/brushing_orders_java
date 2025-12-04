import request from "@/utils/request";

// 查询连单列表
export function listSeries(query) {
  return request({
    url: "/member/series/list",
    method: "get",
    params: query,
  });
}

// 查询连单详细
export function getSeries(id) {
  return request({
    url: "/member/series/" + id,
    method: "get",
  });
}

// 新增连单
export function addSeries(data) {
  return request({
    url: "/member/series",
    method: "post",
    data: data,
  });
}

// 修改连单
export function updateSeries(data) {
  return request({
    url: "/member/series",
    method: "put",
    data: data,
  });
}

// 删除连单
export function delSeries(id) {
  return request({
    url: "/member/series/" + id,
    method: "delete",
  });
}

export function listGoods(query) {
  return request({
    url: "/member/goods/list",
    method: "get",
    params: query,
  });
}

export function getMember(id) {
  return request({
    url: "/member/member/" + id,
    method: "get",
  });
}

export function addSeriesList(data) {
  return request({
    url: "/member/series/addSeries",
    method: "post",
    data: data,
  });
}
