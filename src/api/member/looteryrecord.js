import request from '@/utils/request'

// 查询抽奖记录列表
export function listLooteryrecord(query) {
  return request({
    url: '/member/looteryrecord/list',
    method: 'get',
    params: query
  })
}

// 查询抽奖记录详细
export function getLooteryrecord(id) {
  return request({
    url: '/member/looteryrecord/' + id,
    method: 'get'
  })
}

// 新增抽奖记录
export function addLooteryrecord(data) {
  return request({
    url: '/member/looteryrecord',
    method: 'post',
    data: data
  })
}

// 修改抽奖记录
export function updateLooteryrecord(data) {
  return request({
    url: '/member/looteryrecord',
    method: 'put',
    data: data
  })
}

// 删除抽奖记录
export function delLooteryrecord(id) {
  return request({
    url: '/member/looteryrecord/' + id,
    method: 'delete'
  })
}
