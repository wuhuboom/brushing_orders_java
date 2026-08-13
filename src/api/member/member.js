import request from "@/utils/request";

// 查询会员用户列表
export function listMember(query) {
  return request({
    url: "/member/member/list",
    method: "get",
    params: query,
    silentError: true,
  });
}

// 查询会员用户详细
export function getMember(id) {
  return request({
    url: "/member/member/" + id,
    method: "get",
  });
}

// 新增会员用户
export function addMember(data) {
  return request({
    url: "/member/member",
    method: "post",
    data: data,
  });
}

// 修改会员用户
export function updateMember(data) {
  return request({
    url: "/member/member",
    method: "put",
    data: data,
  });
}

// 修改会员上级（专用权限接口）
export function changeParent(data) {
  return request({
    url: "/member/member/changeParent",
    method: "put",
    data: data,
    silentError: true,
  });
}

// 删除会员用户
export function delMember(id) {
  return request({
    url: "/member/member/" + id,
    method: "delete",
  });
}

export function levelList() {
  return request({
    url: "/member/member/levelList",
    method: "get",
  });
}

export function topupAmount(data) {
  return request({
    url: "/member/member/topupAmount",
    method: "post",
    data: data,
  });
}

export function restDealCount(id) {
  return request({
    url: "/member/member/restDealCount/" + id,
    method: "get",
  });
}

export function upAmount(data) {
  return request({
    url: "/member/member/upAmount",
    method: "post",
    data: data,
  });
}

export function updateAmount(data) {
  return request({
    url: "/member/member/updateAmount",
    method: "post",
    data: data,
  });
}

export function scopeList(query) {
  return request({
    url: "/member/member/scopeList",
    method: "get",
    params: query,
  });
}

export function getDashboardData() {
  return request({
    url: "/member/member/getDashboardData",
    method: "get",
  });
}

export function userBankList(userId) {
  return request({
    url: "/member/wallet/userBankList/" + userId,
    method: "get",
  });
}

// 层级统计
export function hierarchyStats(query) {
  return request({
    url: "/member/member/hierarchy/stats",
    method: "get",
    params: query,
  });
}

// 查询顶级节点统计（parent_id = 0）
export function topLevelStats(query) {
  return request({
    url: "/member/member/hierarchy/topLevelStats",
    method: "get",
    params: query,
  });
}

// 查询指定用户的下级节点统计信息
export function subStats(query) {
  return request({
    url: "/member/member/hierarchy/subStats",
    method: "get",
    params: query,
  });
}

export function getPhoneFieldSet() {
  return request({
    url: "/member/member/getPhoneFieldSet",
    method: "get",
  });
}
