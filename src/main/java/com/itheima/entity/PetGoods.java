package com.itheima.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 宠物商品实体，对应表 pet_goods
 * shopName 为联表查询的店铺附加字段（非本表列）
 */
@Data
public class PetGoods {

    private Long id;
    /** 所属店铺ID */
    private Long shopId;
    private String petName;
    private String breed;
    private String age;
    private BigDecimal price;
    private String description;
    /** 多张图片URL(JSON数组字符串) */
    private String images;
    private Integer stock;
    /** 状态：0上架 1下架 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;

    /** 联表附加：店铺名称 */
    private String shopName;
}
