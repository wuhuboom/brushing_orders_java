import request from '@/utils/request'

// 查询全局配置（中英文内容）列表
export function listGlobalconfig(query) {
  return request({
    url: '/set/globalconfig/list',
    method: 'get',
    params: query
  })
}

// 查询全局配置（中英文内容）详细
export function getGlobalconfig(id) {
  return request({
    url: '/set/globalconfig/' + id,
    method: 'get'
  })
}

// 新增全局配置（中英文内容）
export function addGlobalconfig(data) {
  return request({
    url: '/set/globalconfig',
    method: 'post',
    data: data
  })
}

// 修改全局配置（中英文内容）
export function updateGlobalconfig(data) {
  return request({
    url: '/set/globalconfig',
    method: 'put',
    data: data
  })
}

// 删除全局配置（中英文内容）
export function delGlobalconfig(id) {
  return request({
    url: '/set/globalconfig/' + id,
    method: 'delete'
  })
}
