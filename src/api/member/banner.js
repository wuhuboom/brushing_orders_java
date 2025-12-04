import request from '@/utils/request'

// 查询横幅：用于存储横幅广告相关信息列表
export function listBanner(query) {
  return request({
    url: '/member/banner/list',
    method: 'get',
    params: query
  })
}

// 查询横幅：用于存储横幅广告相关信息详细
export function getBanner(id) {
  return request({
    url: '/member/banner/' + id,
    method: 'get'
  })
}

// 新增横幅：用于存储横幅广告相关信息
export function addBanner(data) {
  return request({
    url: '/member/banner',
    method: 'post',
    data: data
  })
}

// 修改横幅：用于存储横幅广告相关信息
export function updateBanner(data) {
  return request({
    url: '/member/banner',
    method: 'put',
    data: data
  })
}

// 删除横幅：用于存储横幅广告相关信息
export function delBanner(id) {
  return request({
    url: '/member/banner/' + id,
    method: 'delete'
  })
}
