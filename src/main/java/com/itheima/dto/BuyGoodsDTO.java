package com.itheima.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 直接购买请求（模拟下单）
 */
@Data
public class BuyGoodsDTO {

    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 99, message = "单件商品最多99件")
    private Integer num;

    /** 使用的用户优惠券ID（可选，不传=不使用优惠券） */
    private Long userCouponId;
}
