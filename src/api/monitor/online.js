import request from '@/utils/request'
import { apiPathParam } from '@/utils/apiPath'

// 查询在线用户列表
export function list(query) {
  return request({
    url: '/monitor/online/list',
    method: 'get',
    params: query
  })
}

// 强退用户
export function forceLogout(tokenId) {
  return request({
    url: '/monitor/online/' + apiPathParam(tokenId),
    method: 'delete'
  })
}
