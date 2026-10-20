package com.itheima.mapper;

import com.itheima.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 优惠券 Mapper（本模块只读展示；领取/秒杀在优惠券模块扩展）
 */
@Mapper
public interface CouponMapper {

    /** 店铺有效券列表（普通券+秒杀券，未过期） */
    List<Coupon> selectValidByShop(@Param("shopId") Long shopId);

    /** 全部有效秒杀券（库存预热使用） */
    List<Coupon> selectValidSeckill();

    /** 按ID查询（逻辑未删除） */
    Coupon findById(@Param("id") Long id);

    /** 乐观锁扣减库存：remain_stock > 0 才扣，返回受影响行数（0=已抢完） */
    int deductStock(@Param("id") Long id);

    /** 库存回补（秒杀失败/幂等回滚场景） */
    int restoreStock(@Param("id") Long id);

    /** 店主创建优惠券，回填自增ID */
    int insert(Coupon coupon);

    /** 店主删除优惠券（逻辑删除） */
    int logicDelete(@Param("id") Long id);
}
