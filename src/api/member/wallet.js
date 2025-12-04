import request from '@/utils/request'

// 查询银行钱包列表
export function listWallet(query) {
  return request({
    url: '/member/wallet/list',
    method: 'get',
    params: query
  })
}

// 查询银行钱包详细
export function getWallet(id) {
  return request({
    url: '/member/wallet/' + id,
    method: 'get'
  })
}

// 新增银行钱包
export function addWallet(data) {
  return request({
    url: '/member/wallet',
    method: 'post',
    data: data
  })
}

// 修改银行钱包
export function updateWallet(data) {
  return request({
    url: '/member/wallet',
    method: 'put',
    data: data
  })
}

// 删除银行钱包
export function delWallet(id) {
  return request({
    url: '/member/wallet/' + id,
    method: 'delete'
  })
}
