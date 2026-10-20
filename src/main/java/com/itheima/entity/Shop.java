package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺实体，对应表 shop
 */
@Data
public class Shop {

    private Long id;
    private String shopName;
    /** 店主用户ID（开店人，一人一店） */
    private Long ownerId;
    private String shopDesc;
    /** 店铺封面URL */
    private String cover;
    /** 店铺logoURL */
    private String logo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;
}
