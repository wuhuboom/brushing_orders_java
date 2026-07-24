import request from '@/utils/request'
import { apiPathParam } from '@/utils/apiPath'

export const listActivities = (params) => request({ url: '/activities/activities/list', method: 'get', params })
export const getActivity = (id) => request({ url: `/activities/activities/${apiPathParam(id)}`, method: 'get' })
export const addActivity = (data) => request({ url: '/activities/activities', method: 'post', data })
export const updateActivity = (data) => request({ url: '/activities/activities', method: 'put', data })
export const deleteActivity = (ids) => request({ url: `/activities/activities/${apiPathParam(ids)}`, method: 'delete' })

export const listPrizes = (params) => request({ url: '/activities/prizes/list', method: 'get', params })
export const getPrize = (id) => request({ url: `/activities/prizes/${apiPathParam(id)}`, method: 'get' })
export const addPrize = (data) => request({ url: '/activities/prizes', method: 'post', data })
export const updatePrize = (data) => request({ url: '/activities/prizes', method: 'put', data })
export const deletePrize = (ids) => request({ url: `/activities/prizes/${apiPathParam(ids)}`, method: 'delete' })

export const listActivityAccounts = (params) => request({ url: '/activities/accounts/list', method: 'get', params })
export const adjustActivityTimes = (userId, data) => request({ url: `/activities/accounts/${apiPathParam(userId)}/adjust`, method: 'put', data })
export const listAccountPrizes = (userId, params) => request({ url: `/activities/accounts/${apiPathParam(userId)}/prizes/list`, method: 'get', params })
export const getAccountPrize = (id) => request({ url: `/activities/account-prizes/${apiPathParam(id)}`, method: 'get' })
export const addAccountPrize = (userId, data) => request({ url: `/activities/accounts/${apiPathParam(userId)}/prizes`, method: 'post', data })
export const updateAccountPrize = (userId, data) => request({ url: `/activities/accounts/${apiPathParam(userId)}/prizes`, method: 'put', data })
export const deleteAccountPrize = (ids) => request({ url: `/activities/account-prizes/${apiPathParam(ids)}`, method: 'delete' })

export const listPartners = (params) => request({ url: '/activities/partners/list', method: 'get', params })
export const updatePartnerHidden = (isHidden, ids) => request({ url: `/activities/partners/hidden/${apiPathParam(isHidden)}/${apiPathParam(ids)}`, method: 'put' })
