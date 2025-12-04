import request from '@/utils/request'

// 查询充值记录列表
export function listTopup(query) {
  return request({
    url: '/member/topup/list',
    method: 'get',
    params: query
  })
}

// 查询充值记录详细
export function getTopup(id) {
  return request({
    url: '/member/topup/' + id,
    method: 'get'
  })
}

// 新增充值记录
export function addTopup(data) {
  return request({
    url: '/member/topup',
    method: 'post',
    data: data
  })
}

// 修改充值记录
export function updateTopup(data) {
  return request({
    url: '/member/topup',
    method: 'put',
    data: data
  })
}

// 删除充值记录
export function delTopup(id) {
  return request({
    url: '/member/topup/' + id,
    method: 'delete'
  })
}
