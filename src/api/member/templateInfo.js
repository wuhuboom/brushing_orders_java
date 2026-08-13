import request from '@/utils/request'

// 查询连单模板任务
export function listTemplateInfo(query) {
  return request({
    url: '/member/templateInfo/list',
    method: 'get',
    params: query
  })
}

// 查询连单模板任务
export function getTemplateInfo(id) {
  return request({
    url: '/member/templateInfo/' + id,
    method: 'get'
  })
}

// 新增连单模板任务
export function addTemplateInfo(data) {
  return request({
    url: '/member/templateInfo',
    method: 'post',
    data: data
  })
}

// 修改连单模板任务
export function updateTemplateInfo(data) {
  return request({
    url: '/member/templateInfo',
    method: 'put',
    data: data
  })
}

// 删除连单模板任务
export function delTemplateInfo(id) {
  return request({
    url: '/member/templateInfo/' + id,
    method: 'delete'
  })
}
