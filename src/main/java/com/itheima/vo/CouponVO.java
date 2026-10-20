package com.itheima.vo;

import com.itheima.entity.Coupon;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券视图对象（店铺详情页展示）
 */
@Data
public class CouponVO {

    private Long id;
    private Long shopId;
    private String couponName;
    /** 类型：0普通券 1秒杀券 */
    private Integer category;
    /** 面额 */
    private BigDecimal money;
    private Integer totalStock;
    private Integer remainStock;
    /** 每人领取上限 */
    private Integer perLimit;
    /** 开始时间（秒杀券倒计时使用） */
    private LocalDateTime startTime;
    private LocalDateTime expireTime;

    public static CouponVO from(Coupon coupon) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setShopId(coupon.getShopId());
        vo.setCouponName(coupon.getCouponName());
        vo.setCategory(coupon.getCategory());
        vo.setMoney(coupon.getMoney());
        vo.setTotalStock(coupon.getTotalStock());
        vo.setRemainStock(coupon.getRemainStock());
        vo.setPerLimit(coupon.getPerLimit());
        vo.setStartTime(coupon.getStartTime());
        vo.setExpireTime(coupon.getExpireTime());
        return vo;
    }
}
