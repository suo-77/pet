package com.itheima.config;

import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.entity.Coupon;
import com.itheima.mapper.CouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秒杀库存预热：
 * 应用启动时把有效秒杀券的 DB 剩余库存写入 Redis（SETNX，仅在 key 不存在时写入），
 * 不覆盖运行中的预扣进度；key 缺失时 DECR 会得到负数（视为抢光），预热保证存在
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponStockInit implements ApplicationRunner {

    private final CouponMapper couponMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            warmUp();
        } catch (Exception e) {
            // Redis/MySQL 暂不可达不阻断启动；秒杀前需保证依赖可用（可重启应用重新预热）
            log.warn("秒杀库存预热失败（依赖不可达？），启动继续: {}", e.getMessage());
        }
    }

    /** 执行预热：有效秒杀券 DB 库存写入 Redis（仅 key 不存在时） */
    private void warmUp() {
        // 查询有效秒杀券：category=1、未删除、未过期
        List<Coupon> seckillCoupons = couponMapper.selectValidSeckill();
        int warmed = 0;
        for (Coupon coupon : seckillCoupons) {
            String key = RedisKeyConstant.SECKILL_STOCK + coupon.getId();
            Boolean first = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, String.valueOf(coupon.getRemainStock()));
            if (Boolean.TRUE.equals(first)) {
                warmed++;
            }
        }
        log.info("秒杀库存预热完成: 有效秒杀券 {} 张，本次初始化 {} 张", seckillCoupons.size(), warmed);
    }
}
