import request from '@/utils/request'

// 查询充值地址列表
export function listAddress(query) {
  return request({
    url: '/member/address/list',
    method: 'get',
    params: query
  })
}

// 查询充值地址详细
export function getAddress(id) {
  return request({
    url: '/member/address/' + id,
    method: 'get'
  })
}

// 新增充值地址
export function addAddress(data) {
  return request({
    url: '/member/address',
    method: 'post',
    data: data
  })
}

// 修改充值地址
export function updateAddress(data) {
  return request({
    url: '/member/address',
    method: 'put',
    data: data
  })
}

// 删除充值地址
export function delAddress(id) {
  return request({
    url: '/member/address/' + id,
    method: 'delete'
  })
}

// 修改充值地址状态（仅 id 和 status ）
export function updateAddressStatus(data) {
  return request({
    url: '/member/address/changeStatus',
    method: 'put',
    data: data
  })
}
