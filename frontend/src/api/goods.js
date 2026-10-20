import request from '../utils/request'

/** 宠物商品相关接口：详情/评价/收藏 */
export const goodsDetailApi = (id) => request.get(`/goods/${id}`)
export const goodsByShopApi = (shopId, params) => request.get(`/goods/shop/${shopId}`, { params })
export const evaluatesApi = (id, params) => request.get(`/goods/${id}/evaluates`, { params })
export const evaluateApi = (id, data) => request.post(`/goods/${id}/evaluate`, data)
export const favApi = (id) => request.post(`/goods/${id}/fav`)
export const unfavApi = (id) => request.post(`/goods/${id}/unfav`)
export const myFavsApi = (params) => request.get('/goods/fav/list', { params })

/** 店主商品管理接口 */
export const myGoodsPageApi = (params) => request.get('/goods/my/page', { params })
export const createGoodsApi = (data) => request.post('/goods/my', data)
export const updateGoodsApi = (id, data) => request.put(`/goods/my/${id}`, data)
export const updateGoodsStatusApi = (id, status) => request.put(`/goods/my/${id}/status`, null, { params: { status } })
export const deleteGoodsApi = (id) => request.delete(`/goods/my/${id}`)
