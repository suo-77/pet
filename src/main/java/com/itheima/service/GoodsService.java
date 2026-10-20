package com.itheima.service;

import com.itheima.common.result.PageResult;
import com.itheima.dto.EvaluateCreateDTO;
import com.itheima.dto.GoodsSaveDTO;
import com.itheima.vo.EvaluateVO;
import com.itheima.vo.GoodsVO;

/**
 * 宠物商品服务：详情 / 评价 / 收藏（Redis ZSet）/ 店主商品管理
 */
public interface GoodsService {

    /** 商品详情（viewerId 可为空，未登录 fav=false） */
    GoodsVO detail(Long viewerId, Long goodsId);

    /** 店铺下商品分页（公开，仅上架） */
    PageResult<GoodsVO> pageByShop(Long shopId, int page, int pageSize);

    /** 评价分页（公开） */
    PageResult<EvaluateVO> evaluates(Long goodsId, int page, int pageSize);

    /** 发表评价（登录，敏感词+XSS校验） */
    void evaluate(Long userId, Long goodsId, EvaluateCreateDTO dto);

    /** 收藏商品（幂等） */
    void fav(Long userId, Long goodsId);

    /** 取消收藏（幂等） */
    void unfav(Long userId, Long goodsId);

    /** 我的收藏分页（按收藏时间倒序） */
    PageResult<GoodsVO> myFavs(Long userId, int page, int pageSize);

    /** 店主商品分页（含下架商品） */
    PageResult<GoodsVO> myGoodsPage(Long userId, int page, int pageSize);

    /** 店主新增商品（默认上架） */
    GoodsVO createGoods(Long userId, GoodsSaveDTO dto);

    /** 店主编辑商品 */
    void updateGoods(Long userId, Long goodsId, GoodsSaveDTO dto);

    /** 店主上架/下架商品 */
    void updateGoodsStatus(Long userId, Long goodsId, Integer status);

    /** 店主删除商品（逻辑删除） */
    void deleteGoods(Long userId, Long goodsId);
}
