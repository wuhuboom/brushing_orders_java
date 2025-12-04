import request from '@/utils/request'

// 查询交易流水列表
export function listFlow(query) {
  return request({
    url: '/member/flow/list',
    method: 'get',
    params: query
  })
}

// 查询交易流水详细
export function getFlow(id) {
  return request({
    url: '/member/flow/' + id,
    method: 'get'
  })
}

// 新增交易流水
export function addFlow(data) {
  return request({
    url: '/member/flow',
    method: 'post',
    data: data
  })
}

// 修改交易流水
export function updateFlow(data) {
  return request({
    url: '/member/flow',
    method: 'put',
    data: data
  })
}

// 删除交易流水
export function delFlow(id) {
  return request({
    url: '/member/flow/' + id,
    method: 'delete'
  })
}
