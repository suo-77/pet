package com.itheima.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模拟订单实体，对应表 orders
 */
@Data
public class Orders {

    private Long id;
    /** 订单号（时间戳+随机数，唯一） */
    private String orderNo;
    private Long userId;
    private Long goodsId;
    /** 使用的优惠券ID */
    private Long couponId;
    /** 优惠抵扣金额 */
    private BigDecimal discountAmount;
    /** 商品数量 */
    private Integer goodsNum;
    /** 下单时商品单价快照 */
    private BigDecimal goodsPrice;
    /** 实付总金额 */
    private BigDecimal totalAmount;
    /** 订单状态：0待付款 1已付款 2已取消（模拟支付：下单即已付款） */
    private Integer status;
    /** 订单类型：0普通 1秒杀 */
    private Integer orderType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;
}
