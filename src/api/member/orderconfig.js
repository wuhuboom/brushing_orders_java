import request from '@/utils/request'

// 查询网站设置列表
export function listOrderconfig(query) {
  return request({
    url: '/member/orderconfig/list',
    method: 'get',
    params: query
  })
}

// 查询网站设置详细
export function getOrderconfig(id) {
  return request({
    url: '/member/orderconfig/' + id,
    method: 'get'
  })
}

// 新增网站设置
export function addOrderconfig(data) {
  return request({
    url: '/member/orderconfig',
    method: 'post',
    data: data
  })
}

// 修改网站设置
export function updateOrderconfig(data) {
  return request({
    url: '/member/orderconfig',
    method: 'put',
    data: data
  })
}

// 删除网站设置
export function delOrderconfig(id) {
  return request({
    url: '/member/orderconfig/' + id,
    method: 'delete'
  })
}

function configurationImageRequest(action, file, richTextMode) {
  const data = new FormData()
  data.append('file', file)
  data.append('richTextMode', richTextMode)
  return request({
    url: `/member/orderconfig/images/${action}`,
    method: 'post',
    data,
    timeout: 180000,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
  })
}

export function previewConfigurationImages(file, richTextMode = 'append') {
  return configurationImageRequest('preview', file, richTextMode)
}

export function importConfigurationImages(file, richTextMode = 'append') {
  return configurationImageRequest('import', file, richTextMode)
}
