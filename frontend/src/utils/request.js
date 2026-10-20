import axios from 'axios'
import { addToast } from './toast'

/**
 * axios 统一封装：
 * 1. 请求头自动携带 Authorization token
 * 2. 响应拆包 {code,msg,data}：成功直接返回 data
 * 3. 业务失败统一 Toast 提示并 reject
 * 4. 401 清理本地状态并跳转登录页
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一成功码为 0（ResultCode.SUCCESS）
    if (body.code === 0) {
      return body.data
    }
    // token 失效：清理本地并跳登录
    if (body.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(body.msg || '登录已失效'))
    }
    addToast(body.msg || '请求失败', 'error')
    return Promise.reject(new Error(body.msg || '请求失败'))
  },
  (error) => {
    // HTTP 层错误（网络断开/超时/500等）
    if (error.response && error.response.status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }
    addToast(error.response?.data?.msg || '网络异常，请稍后重试', 'error')
    return Promise.reject(error)
  }
)

/** 401 统一处理：清状态跳登录（保留回跳地址） */
function handleUnauthorized() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.href = '/login?redirect=' + redirect
}

export default service
