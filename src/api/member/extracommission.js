import request from '@/utils/request'

// 查询额外佣金设置列表
export function listExtracommission(query) {
  return request({
    url: '/member/extracommission/list',
    method: 'get',
    params: query
  })
}

// 查询额外佣金设置详细
export function getExtracommission(id) {
  return request({
    url: '/member/extracommission/' + id,
    method: 'get'
  })
}

// 新增额外佣金设置
export function addExtracommission(data) {
  return request({
    url: '/member/extracommission',
    method: 'post',
    data: data
  })
}

// 修改额外佣金设置
export function updateExtracommission(data) {
  return request({
    url: '/member/extracommission',
    method: 'put',
    data: data
  })
}

// 删除额外佣金设置
export function delExtracommission(id) {
  return request({
    url: '/member/extracommission/' + id,
    method: 'delete'
  })
}
