import request from '@/utils/request'

// 查询出金类型列表
export function listWithdrawaltype(query) {
  return request({
    url: '/member/withdrawaltype/list',
    method: 'get',
    params: query
  })
}

// 查询出金类型详细
export function getWithdrawaltype(id) {
  return request({
    url: '/member/withdrawaltype/' + id,
    method: 'get'
  })
}

// 新增出金类型
export function addWithdrawaltype(data) {
  return request({
    url: '/member/withdrawaltype',
    method: 'post',
    data: data
  })
}

// 修改出金类型
export function updateWithdrawaltype(data) {
  return request({
    url: '/member/withdrawaltype',
    method: 'put',
    data: data
  })
}

// 删除出金类型
export function delWithdrawaltype(id) {
  return request({
    url: '/member/withdrawaltype/' + id,
    method: 'delete'
  })
}
