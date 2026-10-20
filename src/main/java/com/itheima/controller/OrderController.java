package com.itheima.controller;

import com.itheima.common.context.UserContext;
import com.itheima.common.result.PageResult;
import com.itheima.common.result.Result;
import com.itheima.dto.BuyGoodsDTO;
import com.itheima.service.OrderService;
import com.itheima.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟订单接口：直接购买（异步生成）/ 我的订单
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 直接购买（模拟支付，异步生成订单） */
    @PostMapping("/buy")
    public Result<Void> buy(@Valid @RequestBody BuyGoodsDTO dto) {
        orderService.buy(UserContext.getUserIdRequired(), dto);
        return Result.success();
    }

    /** 我的订单分页 */
    @GetMapping("/my")
    public Result<PageResult<OrderVO>> my(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.myOrders(UserContext.getUserIdRequired(), page, pageSize));
    }

    /** 取消订单（仅本人已付款订单）：恢复库存 + 退回优惠券 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContext.getUserIdRequired(), id);
        return Result.success();
    }
}
