package com.itheima.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车条目视图对象（Redis 购物车 + DB 商品信息组装）
 */
@Data
public class CartItemVO {

    private Long goodsId;
    private String petName;
    private BigDecimal price;
    /** 商品首图 */
    private String image;
    /** 购物车数量 */
    private Integer num;
    /** 商品当前库存 */
    private Integer stock;
}
