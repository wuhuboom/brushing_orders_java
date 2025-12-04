import request from '@/utils/request'

// 查询彩金列表
export function listBonus(query) {
  return request({
    url: '/member/bonus/list',
    method: 'get',
    params: query
  })
}

// 查询彩金详细
export function getBonus(id) {
  return request({
    url: '/member/bonus/' + id,
    method: 'get'
  })
}

// 新增彩金
export function addBonus(data) {
  return request({
    url: '/member/bonus',
    method: 'post',
    data: data
  })
}

// 修改彩金
export function updateBonus(data) {
  return request({
    url: '/member/bonus',
    method: 'put',
    data: data
  })
}

// 删除彩金
export function delBonus(id) {
  return request({
    url: '/member/bonus/' + id,
    method: 'delete'
  })
}
