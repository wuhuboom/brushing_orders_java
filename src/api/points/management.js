import request from '@/utils/request'
import { apiPathParam } from '@/utils/apiPath'

export const listGifts = (params) => request({ url: '/points/gifts/list', method: 'get', params })
export const getGift = (id) => request({ url: `/points/gifts/${apiPathParam(id)}`, method: 'get' })
export const addGift = (data) => request({ url: '/points/gifts', method: 'post', data })
export const updateGift = (data) => request({ url: '/points/gifts', method: 'put', data })
export const deleteGift = (ids) => request({ url: `/points/gifts/${apiPathParam(ids)}`, method: 'delete' })
export const adjustGift = (id, data) => request({ url: `/points/gifts/${apiPathParam(id)}/adjust`, method: 'put', data })

export const listPointsAccounts = (params) => request({ url: '/points/accounts/list', method: 'get', params })
export const adjustPoints = (userId, data) => request({ url: `/points/accounts/${apiPathParam(userId)}/adjust`, method: 'put', data })
export const listPointsFlows = (params) => request({ url: '/points/flows/list', method: 'get', params })

export const listGiftOrders = (params) => request({ url: '/points/gift-orders/list', method: 'get', params })
export const getGiftOrder = (id) => request({ url: `/points/gift-orders/${apiPathParam(id)}`, method: 'get' })
export const updateGiftOrder = (data) => request({ url: '/points/gift-orders', method: 'put', data })
export const shipGiftOrder = (id) => request({ url: `/points/gift-orders/${apiPathParam(id)}/ship`, method: 'put' })
export const receiveGiftOrder = (id) => request({ url: `/points/gift-orders/${apiPathParam(id)}/receive`, method: 'put' })
export const cancelGiftOrder = (id) => request({ url: `/points/gift-orders/${apiPathParam(id)}/cancel`, method: 'put' })
