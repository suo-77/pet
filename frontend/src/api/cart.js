import request from '../utils/request'

/** 购物车接口（数据主存 Redis） */
export const cartAddApi = (data) => request.post('/cart/add', data)
export const cartUpdateApi = (data) => request.put('/cart/update', data)
export const cartRemoveApi = (goodsId) => request.delete(`/cart/${goodsId}`)
export const cartListApi = () => request.get('/cart/list')
export const cartCheckoutApi = (data) => request.post('/cart/checkout', data)
