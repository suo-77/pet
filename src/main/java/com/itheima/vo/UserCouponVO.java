package com.itheima.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的优惠券视图对象
 * status：0未使用 1已使用 2已过期（已过期 = 未使用且券已过期，由服务层计算）
 */
@Data
public class UserCouponVO {

    private Long id;
    private Long couponId;
    private String couponName;
    /** 面额 */
    private BigDecimal money;
    /** 类型：0普通券 1秒杀券 */
    private Integer category;
    private Long shopId;
    private Integer status;
    private LocalDateTime receiveTime;
    private LocalDateTime expireTime;
}
