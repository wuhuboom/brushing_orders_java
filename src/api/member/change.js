import request from '@/utils/request'

// 查询账户变动列表
export function listChange(query) {
  return request({
    url: '/member/change/list',
    method: 'get',
    params: query
  })
}

// 查询账户变动详细
export function getChange(id) {
  return request({
    url: '/member/change/' + id,
    method: 'get'
  })
}

// 新增账户变动
export function addChange(data) {
  return request({
    url: '/member/change',
    method: 'post',
    data: data
  })
}

// 修改账户变动
export function updateChange(data) {
  return request({
    url: '/member/change',
    method: 'put',
    data: data
  })
}

// 删除账户变动
export function delChange(id) {
  return request({
    url: '/member/change/' + id,
    method: 'delete'
  })
}
