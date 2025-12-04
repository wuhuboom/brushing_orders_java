import request from '@/utils/request'

// 查询商品店铺列表
export function listShop(query) {
  return request({
    url: '/member/shop/list',
    method: 'get',
    params: query
  })
}

// 查询商品店铺详细
export function getShop(id) {
  return request({
    url: '/member/shop/' + id,
    method: 'get'
  })
}

// 新增商品店铺
export function addShop(data) {
  return request({
    url: '/member/shop',
    method: 'post',
    data: data
  })
}

// 修改商品店铺
export function updateShop(data) {
  return request({
    url: '/member/shop',
    method: 'put',
    data: data
  })
}

// 删除商品店铺
export function delShop(id) {
  return request({
    url: '/member/shop/' + id,
    method: 'delete'
  })
}
