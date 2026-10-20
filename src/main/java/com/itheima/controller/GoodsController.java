package com.itheima.controller;

import com.itheima.common.context.UserContext;
import com.itheima.common.result.PageResult;
import com.itheima.common.result.Result;
import com.itheima.dto.EvaluateCreateDTO;
import com.itheima.dto.GoodsSaveDTO;
import com.itheima.service.GoodsService;
import com.itheima.vo.EvaluateVO;
import com.itheima.vo.GoodsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 宠物商品接口：详情 / 店铺商品分页 / 评价 / 收藏 / 店主商品管理
 */
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    /** 商品详情（公开，未登录 fav=false） */
    @GetMapping("/{id}")
    public Result<GoodsVO> detail(@PathVariable Long id) {
        return Result.success(goodsService.detail(UserContext.getUserId(), id));
    }

    /** 店铺下商品分页（公开，店铺详情页使用） */
    @GetMapping("/shop/{shopId}")
    public Result<PageResult<GoodsVO>> pageByShop(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(goodsService.pageByShop(shopId, page, pageSize));
    }

    /** 商品评价分页（公开） */
    @GetMapping("/{id}/evaluates")
    public Result<PageResult<EvaluateVO>> evaluates(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(goodsService.evaluates(id, page, pageSize));
    }

    /** 发表评价（登录） */
    @PostMapping("/{id}/evaluate")
    public Result<Void> evaluate(@PathVariable Long id, @Valid @RequestBody EvaluateCreateDTO dto) {
        goodsService.evaluate(UserContext.getUserIdRequired(), id, dto);
        return Result.success();
    }

    /** 收藏商品（登录） */
    @PostMapping("/{id}/fav")
    public Result<Void> fav(@PathVariable Long id) {
        goodsService.fav(UserContext.getUserIdRequired(), id);
        return Result.success();
    }

    /** 取消收藏（登录） */
    @PostMapping("/{id}/unfav")
    public Result<Void> unfav(@PathVariable Long id) {
        goodsService.unfav(UserContext.getUserIdRequired(), id);
        return Result.success();
    }

    /** 我的收藏分页（登录，按收藏时间倒序） */
    @GetMapping("/fav/list")
    public Result<PageResult<GoodsVO>> myFavs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(goodsService.myFavs(UserContext.getUserIdRequired(), page, pageSize));
    }

    /* ================= 店主商品管理 ================= */

    /** 我的店铺商品分页（含下架商品） */
    @GetMapping("/my/page")
    public Result<PageResult<GoodsVO>> myGoodsPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(goodsService.myGoodsPage(UserContext.getUserIdRequired(), page, pageSize));
    }

    /** 店主新增商品（默认上架） */
    @PostMapping("/my")
    public Result<GoodsVO> create(@Valid @RequestBody GoodsSaveDTO dto) {
        return Result.success(goodsService.createGoods(UserContext.getUserIdRequired(), dto));
    }

    /** 店主编辑商品 */
    @PutMapping("/my/{goodsId}")
    public Result<Void> update(@PathVariable Long goodsId, @Valid @RequestBody GoodsSaveDTO dto) {
        goodsService.updateGoods(UserContext.getUserIdRequired(), goodsId, dto);
        return Result.success();
    }

    /** 店主上架/下架商品：status 0上架 1下架 */
    @PutMapping("/my/{goodsId}/status")
    public Result<Void> updateStatus(@PathVariable Long goodsId, @RequestParam Integer status) {
        goodsService.updateGoodsStatus(UserContext.getUserIdRequired(), goodsId, status);
        return Result.success();
    }

    /** 店主删除商品（逻辑删除） */
    @DeleteMapping("/my/{goodsId}")
    public Result<Void> delete(@PathVariable Long goodsId) {
        goodsService.deleteGoods(UserContext.getUserIdRequired(), goodsId);
        return Result.success();
    }
}
