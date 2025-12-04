import request from '@/utils/request'

// 查询客服管理列表
export function listCustomer(query) {
  return request({
    url: '/set/customer/list',
    method: 'get',
    params: query
  })
}

// 查询客服管理详细
export function getCustomer(id) {
  return request({
    url: '/set/customer/' + id,
    method: 'get'
  })
}

// 新增客服管理
export function addCustomer(data) {
  return request({
    url: '/set/customer',
    method: 'post',
    data: data
  })
}

// 修改客服管理
export function updateCustomer(data) {
  return request({
    url: '/set/customer',
    method: 'put',
    data: data
  })
}

// 删除客服管理
export function delCustomer(id) {
  return request({
    url: '/set/customer/' + id,
    method: 'delete'
  })
}
