import request from '../utils/request'

/** 账号认证相关接口 */
export const sendSmsCodeApi = (phone) => request.post('/auth/sendSmsCode', { phone })
export const registerApi = (data) => request.post('/auth/register', data)
export const loginApi = (data) => request.post('/auth/login', data)
export const loginBySmsApi = (data) => request.post('/auth/loginBySms', data)
export const getInfoApi = () => request.get('/auth/info')
export const logoutApi = () => request.post('/auth/logout')
