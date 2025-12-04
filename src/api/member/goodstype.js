import request from '@/utils/request'

// 查询类目管理列表
export function listGoodstype(query) {
  return request({
    url: '/member/goodstype/list',
    method: 'get',
    params: query
  })
}

// 查询类目管理详细
export function getGoodstype(id) {
  return request({
    url: '/member/goodstype/' + id,
    method: 'get'
  })
}

// 新增类目管理
export function addGoodstype(data) {
  return request({
    url: '/member/goodstype',
    method: 'post',
    data: data
  })
}

// 修改类目管理
export function updateGoodstype(data) {
  return request({
    url: '/member/goodstype',
    method: 'put',
    data: data
  })
}

// 删除类目管理
export function delGoodstype(id) {
  return request({
    url: '/member/goodstype/' + id,
    method: 'delete'
  })
}
