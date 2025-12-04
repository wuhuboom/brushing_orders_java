import request from '@/utils/request'

// 查询系统邮箱配置列表
export function listEmialconfig(query) {
  return request({
    url: '/set/emialconfig/list',
    method: 'get',
    params: query
  })
}

// 查询系统邮箱配置详细
export function getEmialconfig(id) {
  return request({
    url: '/set/emialconfig/' + id,
    method: 'get'
  })
}

// 新增系统邮箱配置
export function addEmialconfig(data) {
  return request({
    url: '/set/emialconfig',
    method: 'post',
    data: data
  })
}

// 修改系统邮箱配置
export function updateEmialconfig(data) {
  return request({
    url: '/set/emialconfig',
    method: 'put',
    data: data
  })
}

// 删除系统邮箱配置
export function delEmialconfig(id) {
  return request({
    url: '/set/emialconfig/' + id,
    method: 'delete'
  })
}
