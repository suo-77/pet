package com.itheima.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券&秒杀券实体，对应表 coupon
 */
@Data
public class Coupon {

    private Long id;
    private Long shopId;
    private String couponName;
    /** 类型：0普通券 1秒杀券 */
    private Integer category;
    /** 面额 */
    private BigDecimal money;
    private Integer totalStock;
    private Integer remainStock;
    /** 每人领取上限 */
    private Integer perLimit;
    /** 开始时间（秒杀倒计时起点） */
    private LocalDateTime startTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;
}
