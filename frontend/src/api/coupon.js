import request from '../utils/request'

/** 优惠券接口：领取/秒杀/我的券/下单选券/店主建券 */
export const receiveCouponApi = (id) => request.post(`/coupon/${id}/receive`)
export const seckillCouponApi = (id) => request.post(`/coupon/${id}/seckill`)
export const myCouponsApi = (params) => request.get('/coupon/my', { params })
export const usableCouponsApi = () => request.get('/coupon/usable')
export const createCouponApi = (data) => request.post('/coupon/create', data)
export const deleteCouponApi = (id) => request.delete(`/coupon/${id}`)
