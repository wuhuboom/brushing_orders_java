import request from '@/utils/request'

// 查询字段设置列表
export function listField(query) {
  return request({
    url: '/member/field/list',
    method: 'get',
    params: query
  })
}

// 查询字段设置详细
export function getField(id) {
  return request({
    url: '/member/field/' + id,
    method: 'get'
  })
}

// 新增字段设置
export function addField(data) {
  return request({
    url: '/member/field',
    method: 'post',
    data: data
  })
}

// 修改字段设置
export function updateField(data) {
  return request({
    url: '/member/field',
    method: 'put',
    data: data
  })
}

// 删除字段设置
export function delField(id) {
  return request({
    url: '/member/field/' + id,
    method: 'delete'
  })
}
