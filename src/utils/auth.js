import Cookies from 'js-cookie'

// Keep the local admin session isolated from other applications served on
// 127.0.0.1. Cookies are shared across ports, so the generic Admin-Token key
// allowed another project's login to silently replace this project's token.
const TokenKey = 'Order-Admin-Token'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}
