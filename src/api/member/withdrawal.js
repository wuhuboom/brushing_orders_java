import axios from "axios";
import { getToken } from "@/utils/auth";
import request from '@/utils/request'

// 查询提现列表
export function listWithdrawal(query) {
  return request({
    url: '/member/withdrawal/list',
    method: 'get',
    params: query
  })
}

// 查询提现详细
export function getWithdrawal(id) {
  return request({
    url: '/member/withdrawal/' + id,
    method: 'get'
  })
}

// 新增提现
export function addWithdrawal(data) {
  return request({
    url: '/member/withdrawal',
    method: 'post',
    data: data
  })
}

// 修改提现备注/显示状态
export function updateWithdrawal(data) {
  return request({
    url: '/member/withdrawal',
    method: 'put',
    data
  })
}

// 审核提现
export function reviewWithdrawal(data) {
  return request({
    url: '/member/withdrawal/' + data.id + '/status',
    method: 'put',
    data: {
      status: data.status,
      remarks: data.remarks
    }
  })
}

// 查看用于编辑的完整付款账户（需要独立修改权限）
export function getSensitiveWithdrawalAccount(id) {
  return request({
    url: '/member/withdrawal/' + id + '/sensitive-account',
    method: 'get'
  })
}

// 修改提现地址
export function updateSensitiveWithdrawalAccount(id, data) {
  return request({
    url: '/member/withdrawal/' + id + '/sensitive-account',
    method: 'put',
    data
  })
}

// 删除提现
export function delWithdrawal(id) {
  return request({
    url: '/member/withdrawal/' + id,
    method: 'delete'
  })
}

// Background checks stay quiet on transport errors; the reminder displays its own status.
export async function getWithdrawalNotifications(signal) {
  const response = await axios.get('/member/withdrawal/notifications', {
    baseURL: window.APP_CONFIG.baseApiUrl,
    headers: { Authorization: `Bearer ${getToken()}` },
    timeout: 10000,
    signal,
  });
  if (Number(response.data?.code) !== 200) {
    const error = new Error('Withdrawal notification request failed');
    error.status = Number(response.data?.code) || response.status;
    throw error;
  }
  return response.data.data;
}
