package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户领取优惠券记录实体，对应表 user_coupon
 */
@Data
public class UserCoupon {

    private Long id;
    private Long userId;
    private Long couponId;
    /** 使用状态：0未使用 1已使用 */
    private Integer status;
    private LocalDateTime receiveTime;
    private LocalDateTime createTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;
}
