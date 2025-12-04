import request from '@/utils/request'

// 查询客服列表
export function listCusservice(query) {
  return request({
    url: '/member/cusservice/list',
    method: 'get',
    params: query
  })
}

// 查询客服详细
export function getCusservice(id) {
  return request({
    url: '/member/cusservice/' + id,
    method: 'get'
  })
}

// 新增客服
export function addCusservice(data) {
  return request({
    url: '/member/cusservice',
    method: 'post',
    data: data
  })
}

// 修改客服
export function updateCusservice(data) {
  return request({
    url: '/member/cusservice',
    method: 'put',
    data: data
  })
}

// 删除客服
export function delCusservice(id) {
  return request({
    url: '/member/cusservice/' + id,
    method: 'delete'
  })
}
