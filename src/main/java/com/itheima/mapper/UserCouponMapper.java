package com.itheima.mapper;

import com.itheima.entity.UserCoupon;
import com.itheima.vo.UserCouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户优惠券 Mapper
 */
@Mapper
public interface UserCouponMapper {

    /** 新增领取记录（唯一键冲突由业务层处理） */
    int insert(UserCoupon userCoupon);

    /** 用户对某张券的已领取数量（幂等/限领校验） */
    long countByUserAndCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 我的优惠券分页（联表券信息）
     * status：null 全部；0 未使用(未过期)；1 已使用；2 已过期(status=0 且过期)
     */
    List<UserCouponVO> selectMyPage(@Param("userId") Long userId,
                                    @Param("status") Integer status,
                                    @Param("offset") long offset,
                                    @Param("pageSize") int pageSize);

    /** 我的优惠券总数（过滤条件同上） */
    long countMy(@Param("userId") Long userId, @Param("status") Integer status);

    /** 查询用户持有券记录 */
    UserCoupon findByUserAndId(@Param("userId") Long userId, @Param("id") Long id);

    /** 使用优惠券（乐观锁：status=0 未使用才置1，0行=已使用/不存在） */
    int markUsed(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /** 取消订单退回优惠券（已使用→未使用） */
    int restoreUsed(@Param("userId") Long userId, @Param("couponId") Long couponId);
}
