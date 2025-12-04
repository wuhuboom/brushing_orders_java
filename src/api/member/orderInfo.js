import request from "@/utils/request";

// 查询订单列表列表
export function listOrderInfo(query) {
  return request({
    url: "/member/orderInfo/list",
    method: "get",
    params: query,
  });
}

// 查询订单列表详细
export function getOrderInfo(id) {
  return request({
    url: "/member/orderInfo/" + id,
    method: "get",
  });
}

// 新增订单列表
export function addOrderInfo(data) {
  return request({
    url: "/member/orderInfo",
    method: "post",
    data: data,
  });
}

// 修改订单列表
export function updateOrderInfo(data) {
  return request({
    url: "/member/orderInfo",
    method: "put",
    data: data,
  });
}

// 删除订单列表
export function delOrderInfo(id) {
  return request({
    url: "/member/orderInfo/" + id,
    method: "delete",
  });
}

export function countStatus() {
  return request({
    url: "/member/orderInfo/getStatusOneCount",
    method: "get",
  });
}
