import request from '@/utils/request'

// 查询网站设置列表
export function listOrderconfig(query) {
  return request({
    url: '/member/orderconfig/list',
    method: 'get',
    params: query
  })
}

// 查询网站设置详细
export function getOrderconfig(id) {
  return request({
    url: '/member/orderconfig/' + id,
    method: 'get'
  })
}

// 新增网站设置
export function addOrderconfig(data) {
  return request({
    url: '/member/orderconfig',
    method: 'post',
    data: data
  })
}

// 修改网站设置
export function updateOrderconfig(data) {
  return request({
    url: '/member/orderconfig',
    method: 'put',
    data: data
  })
}

// 删除网站设置
export function delOrderconfig(id) {
  return request({
    url: '/member/orderconfig/' + id,
    method: 'delete'
  })
}
