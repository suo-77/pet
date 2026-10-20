package com.itheima.vo;

import com.itheima.entity.PetGoods;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 宠物商品视图对象
 */
@Data
public class GoodsVO {

    private Long id;
    private Long shopId;
    /** 店铺名称（联表） */
    private String shopName;
    private String petName;
    private String breed;
    private String age;
    private BigDecimal price;
    private String description;
    /** 多张图片URL列表 */
    private List<String> images;
    private Integer stock;
    /** 状态：0上架 1下架（店主管理端使用） */
    private Integer status;
    /** 当前用户是否已收藏（未登录恒为 false） */
    private Boolean fav;

    public static GoodsVO from(PetGoods goods, List<String> images) {
        GoodsVO vo = new GoodsVO();
        vo.setId(goods.getId());
        vo.setShopId(goods.getShopId());
        vo.setShopName(goods.getShopName());
        vo.setPetName(goods.getPetName());
        vo.setBreed(goods.getBreed());
        vo.setAge(goods.getAge());
        vo.setPrice(goods.getPrice());
        vo.setDescription(goods.getDescription());
        vo.setImages(images);
        vo.setStock(goods.getStock());
        vo.setStatus(goods.getStatus());
        return vo;
    }
}
