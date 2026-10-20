package com.itheima.service;

import com.itheima.common.result.PageResult;
import com.itheima.dto.BuyGoodsDTO;
import com.itheima.vo.OrderVO;

/**
 * 模拟订单服务：直接购买（Stream 异步生成）/ 我的订单 / 用券 / 取消
 */
public interface OrderService {

    /** 直接购买：预校验（含优惠券归属校验）后发 Stream，消费者异步生成订单 */
    void buy(Long userId, BuyGoodsDTO dto);

    /** 我的订单分页 */
    PageResult<OrderVO> myOrders(Long userId, int page, int pageSize);

    /**
     * 生成单商品订单（消费者调用，独立事务）：乐观锁扣库存 + 优惠券核销抵扣 + 写订单
     * @param userCouponId 用户优惠券记录ID（可为空=不使用券）
     * @return 订单号，失败返回 null
     */
    String createSingleOrder(Long userId, Long goodsId, int num, int orderType, Long userCouponId);

    /** 取消订单（仅本人、已付款状态）：恢复库存 + 退回优惠券 */
    void cancel(Long userId, Long orderId);
}
