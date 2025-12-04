import request from "@/utils/request";

// 查询订单用户列表
export function listOrderuser(query) {
  return request({
    url: "/member/orderuser/list",
    method: "get",
    params: query,
  });
}

// 查询订单用户详细
export function getOrderuser(id) {
  return request({
    url: "/member/orderuser/" + id,
    method: "get",
  });
}

// 新增订单用户
export function addOrderuser(data) {
  return request({
    url: "/member/orderuser",
    method: "post",
    data: data,
  });
}

// 修改订单用户
export function updateOrderuser(data) {
  return request({
    url: "/member/orderuser",
    method: "put",
    data: data,
  });
}

// 删除订单用户
export function delOrderuser(id) {
  return request({
    url: "/member/orderuser/" + id,
    method: "delete",
  });
}

export function getLevel() {
  return request({
    url: "/member/orderuser/getLevel",
    method: "get",
  });
}

export function transaction(data) {
  return request({
    url: "/member/orderuser/transaction",
    method: "post",
    data: data,
  });
}

export function resetOrder(id) {
  return request({
    url: "/member/orderuser/resetOrder/" + id,
    method: "get",
  });
}

export function allUser() {
  return request({
    url: "/member/orderuser/allUser",
    method: "get",
  });
}

export function editTradePassword(data) {
  return request({
    url: "/member/orderuser/editTradePassword",
    method: "put",
    data: data,
  });
}

export function editPassword(data) {
  return request({
    url: "/member/orderuser/editPassword",
    method: "put",
    data: data,
  });
}

export function editParentId(data) {
  return request({
    url: "/member/orderuser/editParentId",
    method: "put",
    data: data,
  });
}

export function giftAmount(data) {
  return request({
    url: "/member/orderuser/giftAmount",
    method: "post",
    data: data,
  });
}

export function selectChildrenById(query) {
  return request({
    url: "/member/orderuser/selectChildrenById",
    method: "get",
    params: query,
  });
}
