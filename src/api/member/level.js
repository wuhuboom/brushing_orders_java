import request from '@/utils/request'

// 查询等级列表
export function listLevel(query) {
  return request({
    url: '/member/level/list',
    method: 'get',
    params: query
  })
}

// 查询等级详细
export function getLevel(id) {
  return request({
    url: '/member/level/' + id,
    method: 'get'
  })
}

// 新增等级
export function addLevel(data) {
  return request({
    url: '/member/level',
    method: 'post',
    data: data
  })
}

// 修改等级
export function updateLevel(data) {
  return request({
    url: '/member/level',
    method: 'put',
    data: data
  })
}

// 删除等级
export function delLevel(id) {
  return request({
    url: '/member/level/' + id,
    method: 'delete'
  })
}
