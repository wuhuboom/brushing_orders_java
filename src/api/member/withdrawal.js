import request from '@/utils/request'

// 查询提现列表
export function listWithdrawal(query) {
  return request({
    url: '/member/withdrawal/list',
    method: 'get',
    params: query
  })
}

// 查询提现详细
export function getWithdrawal(id) {
  return request({
    url: '/member/withdrawal/' + id,
    method: 'get'
  })
}

// 新增提现
export function addWithdrawal(data) {
  return request({
    url: '/member/withdrawal',
    method: 'post',
    data: data
  })
}

// 修改提现
export function updateWithdrawal(data) {
  return request({
    url: '/member/withdrawal',
    method: 'put',
    data: data
  })
}

// 删除提现
export function delWithdrawal(id) {
  return request({
    url: '/member/withdrawal/' + id,
    method: 'delete'
  })
}
