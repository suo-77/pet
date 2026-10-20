package com.itheima.service;

import com.itheima.common.result.PageResult;
import com.itheima.dto.ShopSaveDTO;
import com.itheima.vo.CouponVO;
import com.itheima.vo.ShopVO;

import java.util.List;

/**
 * 店铺服务：列表 / 详情 / 店铺有效优惠券 / 店主开店与管理
 */
public interface ShopService {

    /** 店铺分页（支持名称关键字） */
    PageResult<ShopVO> page(String keyword, int page, int pageSize);

    /** 店铺详情 */
    ShopVO detail(Long shopId);

    /** 店铺有效优惠券（普通券+秒杀券，未过期） */
    List<CouponVO> coupons(Long shopId);

    /** 我的店铺（未开店返回 null） */
    ShopVO myShop(Long userId);

    /** 开通店铺（一人一店，重复开店报错） */
    ShopVO openShop(Long userId, ShopSaveDTO dto);

    /** 店主编辑店铺信息（名称/简介/封面） */
    ShopVO updateMyShop(Long userId, ShopSaveDTO dto);
}
