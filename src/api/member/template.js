import request from '@/utils/request'

// 查询连单模板列表
export function listTemplate(query) {
  return request({
    url: '/member/template/list',
    method: 'get',
    params: query
  })
}

// 查询连单模板详细
export function getTemplate(id) {
  return request({
    url: '/member/template/' + id,
    method: 'get'
  })
}

// 新增连单模板
export function addTemplate(data) {
  return request({
    url: '/member/template',
    method: 'post',
    data: data
  })
}

// 修改连单模板
export function updateTemplate(data) {
  return request({
    url: '/member/template',
    method: 'put',
    data: data
  })
}

// 删除连单模板
export function delTemplate(id) {
  return request({
    url: '/member/template/' + id,
    method: 'delete'
  })
}
