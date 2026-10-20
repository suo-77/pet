package com.itheima.mapper;

import com.itheima.entity.GoodsEvaluate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品评价 Mapper
 */
@Mapper
public interface GoodsEvaluateMapper {

    /** 新增评价，回填自增ID */
    int insert(GoodsEvaluate evaluate);

    /** 评价分页（联表评价者昵称头像，按时间倒序） */
    List<GoodsEvaluate> selectPageByGoods(@Param("goodsId") Long goodsId,
                                          @Param("offset") long offset,
                                          @Param("pageSize") int pageSize);

    /** 商品评价总数 */
    long countByGoods(@Param("goodsId") Long goodsId);
}
