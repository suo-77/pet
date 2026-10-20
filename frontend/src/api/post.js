import request from '../utils/request'

/** 社区帖子相关接口 */
export const createPostApi = (data) => request.post('/post', data)
export const postPageApi = (params) => request.get('/post/page', { params })
export const myPostApi = (params) => request.get('/post/my', { params })
export const likePostApi = (id) => request.post(`/post/${id}/like`)
export const unlikePostApi = (id) => request.post(`/post/${id}/unlike`)
export const commentPostApi = (id, data) => request.post(`/post/${id}/comment`, data)
export const commentsApi = (id, params) => request.get(`/post/${id}/comments`, { params })
export const postLikesApi = (id) => request.get(`/post/${id}/likes`)
export const deletePostApi = (id) => request.delete(`/post/${id}`)
