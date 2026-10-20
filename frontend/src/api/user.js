import request from '../utils/request'

/** 用户与关注相关接口 */
export const updateProfileApi = (data) => request.put('/user/profile', data)
export const userHomeApi = (userId) => request.get(`/user/home/${userId}`)
export const followApi = (targetId) => request.post(`/user/follow/${targetId}`)
export const unfollowApi = (targetId) => request.post(`/user/unfollow/${targetId}`)
export const followingApi = (params) => request.get('/user/following', { params })
export const fansApi = (params) => request.get('/user/fans', { params })
export const recommendApi = (params) => request.get('/user/recommend', { params })
