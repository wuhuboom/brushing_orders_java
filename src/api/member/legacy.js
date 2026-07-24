import request from '@/utils/request'
import { apiPathParam } from '@/utils/apiPath'

const baseUrl = '/member/legacy'

export function listLegacy(resource, query) {
  return request({
    url: `${baseUrl}/${apiPathParam(resource)}/list`,
    method: 'get',
    params: query
  })
}

export function getLegacy(resource, id) {
  return request({
    url: `${baseUrl}/${apiPathParam(resource)}/${apiPathParam(id)}`,
    method: 'get'
  })
}

export function addLegacy(resource, data) {
  return request({
    url: `${baseUrl}/${apiPathParam(resource)}`,
    method: 'post',
    data
  })
}

export function updateLegacy(resource, data) {
  return request({
    url: `${baseUrl}/${apiPathParam(resource)}`,
    method: 'put',
    data
  })
}

export function delLegacy(resource, id) {
  return request({
    url: `${baseUrl}/${apiPathParam(resource)}/${apiPathParam(id)}`,
    method: 'delete'
  })
}

export function updateLegacyHidden(resource, isHidden, ids) {
  return request({
    url: `${baseUrl}/${apiPathParam(resource)}/hidden/${apiPathParam(isHidden)}/${apiPathParam(ids)}`,
    method: 'put'
  })
}
