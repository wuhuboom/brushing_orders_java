import request from '@/utils/request'

// 查询网站设置列表
export function listSiteconfig(query) {
  return request({
    url: '/set/siteconfig/list',
    method: 'get',
    params: query
  })
}

// 查询网站设置详细
export function getSiteconfig(id) {
  return request({
    url: '/set/siteconfig/' + id,
    method: 'get'
  })
}

// 新增网站设置
export function addSiteconfig(data) {
  return request({
    url: '/set/siteconfig',
    method: 'post',
    data: data
  })
}

// 修改网站设置
export function updateSiteconfig(data) {
  return request({
    url: '/set/siteconfig',
    method: 'put',
    data: data
  })
}

// 删除网站设置
export function delSiteconfig(id) {
  return request({
    url: '/set/siteconfig/' + id,
    method: 'delete'
  })
}
