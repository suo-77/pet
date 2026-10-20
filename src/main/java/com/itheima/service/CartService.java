package com.itheima.service;

import com.itheima.common.result.PageResult;
import com.itheima.dto.CartItemDTO;
import com.itheima.dto.CheckoutDTO;
import com.itheima.vo.CartItemVO;

import java.util.List;

/**
 * 购物车服务（数据主存 Redis Hash：cart:{userId} -> Hash<goodsId, num>）
 */
public interface CartService {

    /** 加入购物车 */
    void add(Long userId, CartItemDTO dto);

    /** 修改数量 */
    void update(Long userId, CartItemDTO dto);

    /** 移除单个商品 */
    void remove(Long userId, Long goodsId);

    /** 购物车列表 */
    List<CartItemVO> list(Long userId);

    /** 结算下单：全车或指定商品，Stream 异步生成订单 */
    void checkout(Long userId, CheckoutDTO dto);
}
