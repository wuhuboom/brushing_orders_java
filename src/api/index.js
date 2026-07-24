import request from '@/utils/request'


export function getStats() {
  return request({
    url: '/admin/dashboard/stats',
    method: 'get'
  })
}

export function getHeaderStats() {
  return request({
    url: '/admin/dashboard/header-stats',
    method: 'get'
  })
}
