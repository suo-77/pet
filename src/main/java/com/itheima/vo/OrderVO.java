package com.itheima.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象
 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long goodsId;
    /** 宠物名称（联表） */
    private String petName;
    private Integer goodsNum;
    /** 单价快照 */
    private BigDecimal goodsPrice;
    private BigDecimal totalAmount;
    /** 优惠抵扣金额 */
    private BigDecimal discountAmount;
    /** 订单状态：0待付款 1已付款 2已取消 */
    private Integer status;
    /** 订单类型：0普通 1秒杀 */
    private Integer orderType;
    private LocalDateTime createTime;
}
