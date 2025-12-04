import request from '@/utils/request'

// 查询抽奖配置列表
export function listLotteryconfig(query) {
  return request({
    url: '/member/lotteryconfig/list',
    method: 'get',
    params: query
  })
}

// 查询抽奖配置详细
export function getLotteryconfig(id) {
  return request({
    url: '/member/lotteryconfig/' + id,
    method: 'get'
  })
}

// 新增抽奖配置
export function addLotteryconfig(data) {
  return request({
    url: '/member/lotteryconfig',
    method: 'post',
    data: data
  })
}

// 修改抽奖配置
export function updateLotteryconfig(data) {
  return request({
    url: '/member/lotteryconfig',
    method: 'put',
    data: data
  })
}

// 删除抽奖配置
export function delLotteryconfig(id) {
  return request({
    url: '/member/lotteryconfig/' + id,
    method: 'delete'
  })
}
