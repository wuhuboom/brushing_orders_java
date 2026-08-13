import request from '@/utils/request'

// 查询订单列表
export function listOrderinfo(query) {
  return request({
    url: '/member/orderinfo/list',
    method: 'get',
    params: query
  })
}

// 查询订单详细
export function getOrderinfo(id) {
  return request({
    url: '/member/orderinfo/' + id,
    method: 'get'
  })
}

// 新增订单
export function addOrderinfo(data) {
  return request({
    url: '/member/orderinfo',
    method: 'post',
    data: data
  })
}

// 修改订单
export function updateOrderinfo(data) {
  return request({
    url: '/member/orderinfo',
    method: 'put',
    data: data
  })
}

// 取消待提交订单
export function cancelOrderinfo(id) {
  return request({
    url: '/member/orderinfo/' + id + '/cancel',
    method: 'put'
  })
}
