import request from '@/utils/request'
import { apiPathParam } from '@/utils/apiPath'

const permissionBase = '/system/permission'
const fileBase = '/system/file'

export const listStrategies = params => request({ url: `${permissionBase}/strategies/list`, method: 'get', params })
export const getStrategy = id => request({ url: `${permissionBase}/strategies/${apiPathParam(id)}`, method: 'get' })
export const addStrategy = data => request({ url: `${permissionBase}/strategies`, method: 'post', data })
export const updateStrategy = data => request({ url: `${permissionBase}/strategies`, method: 'put', data })
export const deleteStrategies = ids => request({ url: `${permissionBase}/strategies/${apiPathParam(ids)}`, method: 'delete' })

export const listGroups = params => request({ url: `${permissionBase}/groups/list`, method: 'get', params })
export const getGroup = id => request({ url: `${permissionBase}/groups/${apiPathParam(id)}`, method: 'get' })
export const addGroup = data => request({ url: `${permissionBase}/groups`, method: 'post', data })
export const updateGroup = data => request({ url: `${permissionBase}/groups`, method: 'put', data })
export const deleteGroups = ids => request({ url: `${permissionBase}/groups/${apiPathParam(ids)}`, method: 'delete' })

export const getUserGroups = id => request({ url: `${permissionBase}/users/${apiPathParam(id)}/groups`, method: 'get' })
export const saveUserGroups = (id, data) => request({ url: `${permissionBase}/users/${apiPathParam(id)}/groups`, method: 'put', data })
export const getPostRoles = id => request({ url: `${permissionBase}/posts/${apiPathParam(id)}/roles`, method: 'get' })
export const savePostRoles = (id, data) => request({ url: `${permissionBase}/posts/${apiPathParam(id)}/roles`, method: 'put', data })
export const getRoleAlignment = id => request({ url: `${permissionBase}/roles/${apiPathParam(id)}/alignment`, method: 'get' })
export const saveRoleAlignment = (id, data) => request({ url: `${permissionBase}/roles/${apiPathParam(id)}/alignment`, method: 'put', data })

export const listFiles = params => request({ url: `${fileBase}/files/list`, method: 'get', params })
export const getFile = id => request({ url: `${fileBase}/files/${apiPathParam(id)}`, method: 'get' })
export const deleteFiles = ids => request({ url: `${fileBase}/files/${apiPathParam(ids)}`, method: 'delete' })
export const listFileReferences = params => request({ url: `${fileBase}/file-references/list`, method: 'get', params })
export const getFileReference = id => request({ url: `${fileBase}/file-references/${apiPathParam(id)}`, method: 'get' })
export const addFileReference = data => request({ url: `${fileBase}/file-references`, method: 'post', data })
export const deleteFileReferences = ids => request({ url: `${fileBase}/file-references/${apiPathParam(ids)}`, method: 'delete' })
