package com.itheima.controller;

import com.itheima.common.context.UserContext;
import com.itheima.common.result.PageResult;
import com.itheima.common.result.Result;
import com.itheima.dto.ShopSaveDTO;
import com.itheima.service.ShopService;
import com.itheima.vo.CouponVO;
import com.itheima.vo.ShopVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 店铺接口：公开浏览 + 店主开店/店铺管理
 */
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    /** 店铺分页（支持名称关键字） */
    @GetMapping("/page")
    public Result<PageResult<ShopVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(shopService.page(keyword, page, pageSize));
    }

    /** 店铺详情 */
    @GetMapping("/{id}")
    public Result<ShopVO> detail(@PathVariable Long id) {
        return Result.success(shopService.detail(id));
    }

    /** 店铺有效优惠券（普通券+秒杀券） */
    @GetMapping("/{id}/coupons")
    public Result<List<CouponVO>> coupons(@PathVariable Long id) {
        return Result.success(shopService.coupons(id));
    }

    /** 我的店铺（未开店返回 data=null） */
    @GetMapping("/my")
    public Result<ShopVO> my() {
        return Result.success(shopService.myShop(UserContext.getUserIdRequired()));
    }

    /** 开通店铺（一人一店） */
    @PostMapping("/open")
    public Result<ShopVO> open(@Valid @RequestBody ShopSaveDTO dto) {
        return Result.success(shopService.openShop(UserContext.getUserIdRequired(), dto));
    }

    /** 店主编辑店铺信息 */
    @PutMapping("/my")
    public Result<ShopVO> update(@Valid @RequestBody ShopSaveDTO dto) {
        return Result.success(shopService.updateMyShop(UserContext.getUserIdRequired(), dto));
    }
}
