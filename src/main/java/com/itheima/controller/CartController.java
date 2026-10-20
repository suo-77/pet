package com.itheima.controller;

import com.itheima.common.context.UserContext;
import com.itheima.common.result.Result;
import com.itheima.dto.CartItemDTO;
import com.itheima.dto.CheckoutDTO;
import com.itheima.service.CartService;
import com.itheima.vo.CartItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口（数据主存 Redis：cart:{userId}）
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** 加入购物车 */
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody CartItemDTO dto) {
        cartService.add(UserContext.getUserIdRequired(), dto);
        return Result.success();
    }

    /** 修改数量 */
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody CartItemDTO dto) {
        cartService.update(UserContext.getUserIdRequired(), dto);
        return Result.success();
    }

    /** 移除单个商品 */
    @DeleteMapping("/{goodsId}")
    public Result<Void> remove(@PathVariable Long goodsId) {
        cartService.remove(UserContext.getUserIdRequired(), goodsId);
        return Result.success();
    }

    /** 购物车列表 */
    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        return Result.success(cartService.list(UserContext.getUserIdRequired()));
    }

    /** 结算下单（goodsIds 空=全车，异步生成订单） */
    @PostMapping("/checkout")
    public Result<Void> checkout(@RequestBody CheckoutDTO dto) {
        cartService.checkout(UserContext.getUserIdRequired(), dto);
        return Result.success();
    }
}
