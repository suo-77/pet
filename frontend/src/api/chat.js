import request from '../utils/request'

/** 私信聊天 HTTP 接口（实时收发走 WebSocket） */
export const sessionsApi = (params) => request.get('/chat/sessions', { params })
export const messagesApi = (peerUserId, params) => request.get(`/chat/messages/${peerUserId}`, { params })
export const markReadApi = (peerUserId) => request.post(`/chat/read/${peerUserId}`)
export const unreadTotalApi = () => request.get('/chat/unread-total')
export const sharePostApi = (postId, toUserId) => request.post('/chat/share', { toUserId, postId })
