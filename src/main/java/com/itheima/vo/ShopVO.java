package com.itheima.vo;

import com.itheima.entity.Shop;
import lombok.Data;

/**
 * 店铺视图对象
 */
@Data
public class ShopVO {

    private Long id;
    private String shopName;
    /** 店主用户ID（前端判断是否本人店铺，展示管理入口） */
    private Long ownerId;
    private String shopDesc;
    private String cover;
    private String logo;

    public static ShopVO from(Shop shop) {
        ShopVO vo = new ShopVO();
        vo.setId(shop.getId());
        vo.setShopName(shop.getShopName());
        vo.setOwnerId(shop.getOwnerId());
        vo.setShopDesc(shop.getShopDesc());
        vo.setCover(shop.getCover());
        vo.setLogo(shop.getLogo());
        return vo;
    }
}
