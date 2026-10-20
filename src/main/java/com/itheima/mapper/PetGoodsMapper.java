package com.itheima.mapper;

import com.itheima.entity.PetGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 宠物商品 Mapper
 */
@Mapper
public interface PetGoodsMapper {

    /** 店铺下商品分页 */
    List<PetGoods> selectByShop(@Param("shopId") Long shopId,
                                @Param("offset") long offset,
                                @Param("pageSize") int pageSize);

    /** 店铺下商品总数 */
    long countByShop(@Param("shopId") Long shopId);

    /** 按ID查询（联表店铺名称） */
    PetGoods findById(@Param("id") Long id);

    /** 按ID集合批量查询（我的收藏使用） */
    List<PetGoods> selectByIds(@Param("ids") List<Long> ids);

    /** 按宠物名称模糊搜索（首页搜索使用） */
    List<PetGoods> searchByName(@Param("keyword") String keyword, @Param("limit") int limit);

    /** 乐观锁扣减库存：stock >= num 才扣，返回受影响行数（0=库存不足） */
    int deductStock(@Param("id") Long id, @Param("num") int num);

    /** 恢复库存（取消订单） */
    int restoreStock(@Param("id") Long id, @Param("num") int num);

    /** 店主新增商品，回填自增ID */
    int insert(PetGoods goods);

    /** 店主编辑商品 */
    int update(PetGoods goods);

    /** 店主上架/下架商品 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 店主删除商品（逻辑删除） */
    int deleteById(@Param("id") Long id);

    /** 店主查看本店全部商品（含下架，管理端使用） */
    List<PetGoods> selectByShopOwner(@Param("shopId") Long shopId,
                                     @Param("offset") long offset,
                                     @Param("pageSize") int pageSize);

    /** 店主商品总数（含下架） */
    long countByShopOwner(@Param("shopId") Long shopId);
}
