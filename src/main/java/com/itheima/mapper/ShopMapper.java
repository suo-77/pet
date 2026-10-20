package com.itheima.mapper;

import com.itheima.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 店铺 Mapper
 */
@Mapper
public interface ShopMapper {

    /** 店铺分页（按名称模糊搜索） */
    List<Shop> selectPage(@Param("keyword") String keyword,
                          @Param("offset") long offset,
                          @Param("pageSize") int pageSize);

    /** 店铺总数 */
    long countPage(@Param("keyword") String keyword);

    /** 按ID查询（逻辑未删除） */
    Shop findById(@Param("id") Long id);

    /** 开通店铺（一人一店，重复触发唯一键冲突） */
    int insert(Shop shop);

    /** 店主编辑店铺信息 */
    int updateInfo(Shop shop);

    /** 查询某用户的店铺（一人一店） */
    Shop findByOwner(@Param("ownerId") Long ownerId);
}
