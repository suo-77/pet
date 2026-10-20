package com.itheima.controller;

import com.itheima.common.context.UserContext;
import com.itheima.common.result.PageResult;
import com.itheima.common.result.Result;
import com.itheima.dto.CouponSaveDTO;
import com.itheima.service.CouponService;
import com.itheima.vo.UserCouponVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券接口：领取普通券 / 秒杀抢券（异步发放）/ 我的优惠券（三态）/ 下单选券 / 店主建券
 */
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /** 领取普通优惠券 */
    @PostMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        couponService.receive(UserContext.getUserIdRequired(), id);
        return Result.success();
    }

    /** 秒杀抢券：Redis 预扣库存后异步发放，立即返回 */
    @PostMapping("/{id}/seckill")
    public Result<Void> seckill(@PathVariable Long id) {
        couponService.seckill(UserContext.getUserIdRequired(), id);
        return Result.success();
    }

    /** 我的优惠券分页：status 不传=全部；0未使用 1已使用 2已过期 */
    @GetMapping("/my")
    public Result<PageResult<UserCouponVO>> my(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(couponService.myCoupons(UserContext.getUserIdRequired(), status, page, pageSize));
    }

    /** 下单可选优惠券（未使用且未过期） */
    @GetMapping("/usable")
    public Result<List<UserCouponVO>> usable() {
        return Result.success(couponService.usableCoupons(UserContext.getUserIdRequired()));
    }

    /** 店主创建优惠券/秒杀券（仅本店） */
    @PostMapping("/create")
    public Result<Void> create(@Valid @RequestBody CouponSaveDTO dto) {
        couponService.create(UserContext.getUserIdRequired(), dto);
        return Result.success();
    }

    /** 店主删除优惠券（仅本店，逻辑删除；秒杀券同步清理 Redis 预扣数据） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(UserContext.getUserIdRequired(), id);
        return Result.success();
    }
}
