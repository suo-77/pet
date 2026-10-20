package com.itheima.service;

import com.itheima.common.result.PageResult;
import com.itheima.dto.CouponSaveDTO;
import com.itheima.vo.UserCouponVO;

import java.util.List;

/**
 * 优惠券服务：领取普通券 / 秒杀抢券（Redis预扣+异步发放）/ 我的优惠券 / 下单用券 / 店主建券
 */
public interface CouponService {

    /** 领取普通优惠券（乐观锁扣库存，防超卖） */
    void receive(Long userId, Long couponId);

    /** 秒杀抢券：Redis 预扣库存 + 去重，Stream 异步发放，立即返回 */
    void seckill(Long userId, Long couponId);

    /**
     * 我的优惠券分页
     * @param status null全部；0未使用；1已使用；2已过期
     */
    PageResult<UserCouponVO> myCoupons(Long userId, Integer status, int page, int pageSize);

    /**
     * 发放秒杀券（消费者调用，独立事务）：
     * DB 库存最终一致性校验 + 扣库存 + 写领取记录
     * @return 是否发放成功
     */
    boolean grantSeckillCoupon(Long userId, Long couponId);

    /** 可用优惠券列表（未使用且未过期，下单选择使用） */
    List<UserCouponVO> usableCoupons(Long userId);

    /** 校验并核销优惠券（下单消费端调用，乐观锁：status=0 才置 1） */
    void markUsed(Long userId, Long userCouponId);

    /** 取消订单退回优惠券（已使用 → 未使用） */
    void restoreUsed(Long userId, Long userCouponId);

    /** 店主创建优惠券/秒杀券（秒杀券创建后同步预热 Redis 库存） */
    void create(Long userId, CouponSaveDTO dto);

    /** 店主删除优惠券（仅本店，逻辑删除；秒杀券同步清理 Redis 预扣数据） */
    void delete(Long userId, Long couponId);
}
