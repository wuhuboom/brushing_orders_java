import request from '@/utils/request'

// 查询登录日志列表
export function listMemberloginlog(query) {
  return request({
    url: '/member/memberloginlog/list',
    method: 'get',
    params: query
  })
}

// 查询登录日志详细
export function getMemberloginlog(id) {
  return request({
    url: '/member/memberloginlog/' + id,
    method: 'get'
  })
}

// 新增登录日志
export function addMemberloginlog(data) {
  return request({
    url: '/member/memberloginlog',
    method: 'post',
    data: data
  })
}

// 修改登录日志
export function updateMemberloginlog(data) {
  return request({
    url: '/member/memberloginlog',
    method: 'put',
    data: data
  })
}

// 删除登录日志
export function delMemberloginlog(id) {
  return request({
    url: '/member/memberloginlog/' + id,
    method: 'delete'
  })
}
