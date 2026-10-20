package com.itheima.dto;

import lombok.Data;

import java.util.List;

/**
 * 购物车结算请求：goodsIds 为空表示全车结算
 */
@Data
public class CheckoutDTO {

    /** 结算的商品ID列表，空=全车 */
    private List<Long> goodsIds;

    /** 使用的用户优惠券ID（可选，作用于结算金额最高的一单） */
    private Long userCouponId;
}
