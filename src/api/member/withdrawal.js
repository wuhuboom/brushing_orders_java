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

// 修改提现备注/显示状态
export function updateWithdrawal(data) {
  return request({
    url: '/member/withdrawal',
    method: 'put',
    data
  })
}

// 审核提现
export function reviewWithdrawal(data) {
  return request({
    url: '/member/withdrawal/' + data.id + '/status',
    method: 'put',
    data: {
      status: data.status,
      remarks: data.remarks
    }
  })
}

// 查看完整付款账户（需要独立敏感信息权限）
export function getSensitiveWithdrawalAccount(id) {
  return request({
    url: '/member/withdrawal/' + id + '/sensitive-account',
    method: 'get'
  })
}

// 修改提现地址
export function updateSensitiveWithdrawalAccount(id, data) {
  return request({
    url: '/member/withdrawal/' + id + '/sensitive-account',
    method: 'put',
    data
  })
}

// 删除提现
export function delWithdrawal(id) {
  return request({
    url: '/member/withdrawal/' + id,
    method: 'delete'
  })
}
