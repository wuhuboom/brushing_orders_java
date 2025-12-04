import request from '@/utils/request'

// 查询连单列表
export function listOrderlink(query) {
  return request({
    url: '/member/orderlink/list',
    method: 'get',
    params: query
  })
}

// 查询连单详细
export function getOrderlink(id) {
  return request({
    url: '/member/orderlink/' + id,
    method: 'get'
  })
}

// 新增连单
export function addOrderlink(data) {
  return request({
    url: '/member/orderlink',
    method: 'post',
    data: data
  })
}

// 修改连单
export function updateOrderlink(data) {
  return request({
    url: '/member/orderlink',
    method: 'put',
    data: data
  })
}

// 删除连单
export function delOrderlink(id) {
  return request({
    url: '/member/orderlink/' + id,
    method: 'delete'
  })
}
