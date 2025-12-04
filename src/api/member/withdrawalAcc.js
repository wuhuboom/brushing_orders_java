import request from "@/utils/request";

// 查询提现账户列表
export function listWithdrawalAcc(query) {
  return request({
    url: "/member/withdrawalAcc/list",
    method: "get",
    params: query,
  });
}

// 查询提现账户详细
export function getWithdrawalAcc(id) {
  return request({
    url: "/member/withdrawalAcc/" + id,
    method: "get",
  });
}

// 新增提现账户
export function addWithdrawalAcc(data) {
  return request({
    url: "/member/withdrawalAcc",
    method: "post",
    data: data,
  });
}

// 修改提现账户
export function updateWithdrawalAcc(data) {
  return request({
    url: "/member/withdrawalAcc",
    method: "put",
    data: data,
  });
}

// 删除提现账户
export function delWithdrawalAcc(id) {
  return request({
    url: "/member/withdrawalAcc/" + id,
    method: "delete",
  });
}

export function getType(query) {
  return request({
    url: "/member/withdrawalAcc/getType",
    method: "get",
    params: query,
  });
}
