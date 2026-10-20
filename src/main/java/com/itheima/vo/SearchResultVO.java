package com.itheima.vo;

import lombok.Data;

import java.util.List;

/**
 * 首页聚合搜索结果：店铺 / 宠物商品 / 用户 三类各返回若干条
 */
@Data
public class SearchResultVO {

    private List<ShopVO> shops;
    private List<GoodsVO> goods;
    private List<UserCardVO> users;
}
