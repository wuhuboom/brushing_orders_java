import request from '@/utils/request'
import { apiPathParam } from '@/utils/apiPath'

export const listCustomers = (params) => request({ url: '/websites/customers/list', method: 'get', params })
export const getCustomer = (id) => request({ url: `/websites/customers/${apiPathParam(id)}`, method: 'get' })
export const addCustomer = (data) => request({ url: '/websites/customers', method: 'post', data })
export const deleteCustomer = (ids) => request({ url: `/websites/customers/${apiPathParam(ids)}`, method: 'delete' })
