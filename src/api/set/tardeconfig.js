import request from '@/utils/request'

// 查询交易控制配置列表
export function listTardeconfig(query) {
  return request({
    url: '/set/tardeconfig/list',
    method: 'get',
    params: query
  })
}

// 查询交易控制配置详细
export function getTardeconfig(id) {
  return request({
    url: '/set/tardeconfig/' + id,
    method: 'get'
  })
}

// 新增交易控制配置
export function addTardeconfig(data) {
  return request({
    url: '/set/tardeconfig',
    method: 'post',
    data: data
  })
}

// 修改交易控制配置
export function updateTardeconfig(data) {
  return request({
    url: '/set/tardeconfig',
    method: 'put',
    data: data
  })
}

// 删除交易控制配置
export function delTardeconfig(id) {
  return request({
    url: '/set/tardeconfig/' + id,
    method: 'delete'
  })
}
