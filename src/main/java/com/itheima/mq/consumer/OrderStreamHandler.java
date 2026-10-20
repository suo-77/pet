package com.itheima.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.entity.Coupon;
import com.itheima.entity.PetGoods;
import com.itheima.entity.UserCoupon;
import com.itheima.mapper.CouponMapper;
import com.itheima.mapper.PetGoodsMapper;
import com.itheima.mapper.UserCouponMapper;
import com.itheima.mq.constant.MQConstant;
import com.itheima.service.CouponService;
import com.itheima.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 订单消费者：处理 mq:stream:order 消息，按 action 分发
 * 1. seckill：秒杀券发放（幂等 + DB库存最终一致性校验，失败回补 Redis）
 * 2. buy：直接购买下单（乐观锁扣库存生成订单）
 * 3. cart：购物车结算下单（逐商品生成订单，成功后清理购物车对应条目）
 * 业务失败（商品下架/库存不足）记日志后正常 ACK，不做无意义重试；
 * 系统异常抛出走 PEL 重试，超过次数进死信流
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStreamHandler implements StreamBizHandler {

    private final CouponService couponService;
    private final OrderService orderService;
    private final PetGoodsMapper goodsMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String streamKey() {
        return MQConstant.ORDER_STREAM;
    }

    @Override
    public String group() {
        return MQConstant.ORDER_GROUP;
    }

    @Override
    public void handle(Map<String, String> payload) {
        long userId = Long.parseLong(payload.get("userId"));
        String action = payload.getOrDefault("action", "buy");
        switch (action) {
            case "seckill" -> grantSeckill(userId, payload);
            case "cart" -> checkoutCart(userId, payload);
            default -> buyNow(userId, payload);
        }
    }

    /** 秒杀券异步发放 */
    private void grantSeckill(long userId, Map<String, String> payload) {
        long couponId = Long.parseLong(payload.get("couponId"));
        couponService.grantSeckillCoupon(userId, couponId);
    }

    /** 直接购买异步下单（可带优惠券） */
    private void buyNow(long userId, Map<String, String> payload) {
        long goodsId = Long.parseLong(payload.get("goodsId"));
        int num = Integer.parseInt(payload.getOrDefault("num", "1"));
        Long userCouponId = parseNullableLong(payload.get("userCouponId"));
        orderService.createSingleOrder(userId, goodsId, num, 0, userCouponId);
    }

    /**
     * 购物车结算：逐商品按数量下单，成功后清理对应购物车条目
     * 优惠券作用于金额最高的一单（一张券只抵扣一次）
     */
    private void checkoutCart(long userId, Map<String, String> payload) {
        String goodsIdsStr = payload.get("goodsIds");
        if (StrUtil.isBlank(goodsIdsStr)) {
            return;
        }
        Long userCouponId = parseNullableLong(payload.get("userCouponId"));
        List<Long> goodsIds = java.util.Arrays.stream(goodsIdsStr.split(","))
                .map(String::trim).map(Long::parseLong).toList();
        // 数量与 goodsIds 一一对应（缺省按 1 件兼容旧消息）
        List<Integer> nums = parseNums(payload.get("nums"), goodsIds.size());
        // 找出优惠券所属店铺中金额最高的商品（单价×数量），用券只作用于该店铺订单
        // 券必须属于对应店铺，跨店商品不参与选券，保证"券只用于所属店铺的商品"
        Long couponGoodsId = null;
        if (userCouponId != null && !goodsIds.isEmpty()) {
            Long couponShopId = couponShopIdOf(userId, userCouponId);
            java.math.BigDecimal max = null;
            for (int i = 0; i < goodsIds.size(); i++) {
                PetGoods g = goodsMapper.findById(goodsIds.get(i));
                if (g == null || couponShopId == null || !couponShopId.equals(g.getShopId())) {
                    continue;
                }
                java.math.BigDecimal amount = g.getPrice().multiply(java.math.BigDecimal.valueOf(nums.get(i)));
                if (max == null || amount.compareTo(max) > 0) {
                    max = amount;
                    couponGoodsId = goodsIds.get(i);
                }
            }
        }
        for (int i = 0; i < goodsIds.size(); i++) {
            Long goodsId = goodsIds.get(i);
            Long usedCoupon = goodsId.equals(couponGoodsId) ? userCouponId : null;
            String orderNo = orderService.createSingleOrder(userId, goodsId, nums.get(i), 0, usedCoupon);
            if (orderNo != null) {
                // 下单成功移除购物车该商品
                stringRedisTemplate.opsForHash()
                        .delete(RedisKeyConstant.CART + userId, String.valueOf(goodsId));
            } else {
                // 业务失败（下架/库存不足）：保留购物车条目供用户调整
                log.warn("购物车商品下单失败，保留条目: userId={}, goodsId={}", userId, goodsId);
            }
        }
    }

    /** 解析数量列表：与 goodsIds 对齐，异常/缺失时按 1 件兜底 */
    private List<Integer> parseNums(String numsStr, int size) {
        Integer[] nums = new Integer[size];
        java.util.Arrays.fill(nums, 1);
        if (StrUtil.isNotBlank(numsStr)) {
            String[] parts = numsStr.split(",");
            for (int i = 0; i < Math.min(parts.length, size); i++) {
                try {
                    nums[i] = Math.max(1, Integer.parseInt(parts[i].trim()));
                } catch (NumberFormatException ignored) {
                    // 非法值按 1 件处理
                }
            }
        }
        return java.util.Arrays.asList(nums);
    }

    /** 解析可空 Long 参数 */
    private Long parseNullableLong(String value) {
        return StrUtil.isBlank(value) ? null : Long.valueOf(value);
    }

    /** 根据用户持券记录解析券所属店铺ID；券不存在/已使用返回 null（不参与用券） */
    private Long couponShopIdOf(long userId, Long userCouponId) {
        if (userCouponId == null) {
            return null;
        }
        UserCoupon uc = userCouponMapper.findByUserAndId(userId, userCouponId);
        if (uc == null) {
            return null;
        }
        Coupon coupon = couponMapper.findById(uc.getCouponId());
        return coupon == null ? null : coupon.getShopId();
    }
}
