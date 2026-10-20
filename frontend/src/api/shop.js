import request from '../utils/request'

/** 店铺与优惠券展示相关接口 */
export const shopPageApi = (params) => request.get('/shop/page', { params })
export const shopDetailApi = (id) => request.get(`/shop/${id}`)
export const shopCouponsApi = (id) => request.get(`/shop/${id}/coupons`)

/** 店主开店与店铺管理 */
export const myShopApi = () => request.get('/shop/my')
export const openShopApi = (data) => request.post('/shop/open', data)
export const updateShopApi = (data) => request.put('/shop/my', data)
