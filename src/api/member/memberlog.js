import request from '@/utils/request'

// 查询会员登录日志列表
export function listMemberlog(query) {
  return request({
    url: '/member/memberlog/list',
    method: 'get',
    params: query
  })
}

// 查询会员登录日志详细
export function getMemberlog(id) {
  return request({
    url: '/member/memberlog/' + id,
    method: 'get'
  })
}

// 新增会员登录日志
export function addMemberlog(data) {
  return request({
    url: '/member/memberlog',
    method: 'post',
    data: data
  })
}

// 修改会员登录日志
export function updateMemberlog(data) {
  return request({
    url: '/member/memberlog',
    method: 'put',
    data: data
  })
}

// 删除会员登录日志
export function delMemberlog(id) {
  return request({
    url: '/member/memberlog/' + id,
    method: 'delete'
  })
}
