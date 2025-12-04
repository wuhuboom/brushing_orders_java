import request from '@/utils/request'

// 查询商品类别列表
export function listGoodstype(query) {
  return request({
    url: '/member/goodstype/list',
    method: 'get',
    params: query
  })
}

// 查询商品类别详细
export function getGoodstype(id) {
  return request({
    url: '/member/goodstype/' + id,
    method: 'get'
  })
}

// 新增商品类别
export function addGoodstype(data) {
  return request({
    url: '/member/goodstype',
    method: 'post',
    data: data
  })
}

// 修改商品类别
export function updateGoodstype(data) {
  return request({
    url: '/member/goodstype',
    method: 'put',
    data: data
  })
}

// 删除商品类别
export function delGoodstype(id) {
  return request({
    url: '/member/goodstype/' + id,
    method: 'delete'
  })
}
