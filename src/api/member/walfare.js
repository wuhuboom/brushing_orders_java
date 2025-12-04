import request from '@/utils/request'

// 查询福利配置列表
export function listWalfare(query) {
  return request({
    url: '/member/walfare/list',
    method: 'get',
    params: query
  })
}

// 查询福利配置详细
export function getWalfare(id) {
  return request({
    url: '/member/walfare/' + id,
    method: 'get'
  })
}

// 新增福利配置
export function addWalfare(data) {
  return request({
    url: '/member/walfare',
    method: 'post',
    data: data
  })
}

// 修改福利配置
export function updateWalfare(data) {
  return request({
    url: '/member/walfare',
    method: 'put',
    data: data
  })
}

// 删除福利配置
export function delWalfare(id) {
  return request({
    url: '/member/walfare/' + id,
    method: 'delete'
  })
}
