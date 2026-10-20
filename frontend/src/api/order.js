import request from '../utils/request'

/** 模拟订单接口 */
export const buyGoodsApi = (data) => request.post('/order/buy', data)
export const myOrdersApi = (params) => request.get('/order/my', { params })
export const cancelOrderApi = (id) => request.post(`/order/${id}/cancel`)
