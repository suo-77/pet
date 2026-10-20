package com.itheima.service.impl;

import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.PageResult;
import com.itheima.common.util.XssUtil;
import com.itheima.dto.CouponSaveDTO;
import com.itheima.entity.Coupon;
import com.itheima.entity.Shop;
import com.itheima.entity.UserCoupon;
import com.itheima.mapper.CouponMapper;
import com.itheima.mapper.ShopMapper;
import com.itheima.mapper.UserCouponMapper;
import com.itheima.mq.constant.MQConstant;
import com.itheima.mq.stream.MQProducer;
import com.itheima.service.CouponService;
import com.itheima.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 优惠券服务实现：
 * 1. 普通券领取：DB 乐观锁扣库存（remain_stock > 0 才扣）+ 唯一键防重复，同事务保证一致
 * 2. 秒杀抢券：Redis 预扣库存（DECR，负数回补）+ Set 限领去重 -> Stream 异步发放，削峰
 * 3. 发放（消费者）：独立事务，幂等（已领取直接跳过），失败回补 Redis 库存与去重集合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final int CATEGORY_NORMAL = 0;
    private static final int CATEGORY_SECKILL = 1;

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final ShopMapper shopMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MQProducer mqProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long userId, Long couponId) {
        Coupon coupon = checkCoupon(couponId, CATEGORY_NORMAL);
        // 限领校验（唯一键兜底：重复领取触发 DuplicateKey 回滚）
        long received = userCouponMapper.countByUserAndCoupon(userId, couponId);
        if (received >= coupon.getPerLimit()) {
            throw new BusinessException("已达该券领取上限");
        }
        // 乐观锁扣库存：0 行 = 已抢完
        int rows = couponMapper.deductStock(couponId);
        if (rows == 0) {
            throw new BusinessException("手慢了，优惠券已抢完");
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        try {
            userCouponMapper.insert(userCoupon);
        } catch (DuplicateKeyException e) {
            // 并发重复领取：抛出后整个事务回滚（含已扣库存）
            throw new BusinessException("已达该券领取上限");
        }
        log.info("领取优惠券成功: userId={}, couponId={}", userId, couponId);
    }

    @Override
    public void seckill(Long userId, Long couponId) {
        Coupon coupon = checkCoupon(couponId, CATEGORY_SECKILL);
        // 秒杀时间窗校验
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            throw new BusinessException("秒杀尚未开始");
        }
        if (!now.isBefore(coupon.getExpireTime())) {
            throw new BusinessException("秒杀已结束");
        }
        String userKey = RedisKeyConstant.SECKILL_USER + couponId;
        String stockKey = RedisKeyConstant.SECKILL_STOCK + couponId;
        // 限领去重：已抢购直接拒绝
        Long added = stringRedisTemplate.opsForSet().add(userKey, String.valueOf(userId));
        if (added == null || added == 0) {
            throw new BusinessException("您已抢购过该秒杀券");
        }
        // Redis 预扣库存：负数说明已抢光，回补并拒绝
        Long stock = stringRedisTemplate.opsForValue().decrement(stockKey);
        if (stock == null || stock < 0) {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.opsForSet().remove(userKey, String.valueOf(userId));
            throw new BusinessException("手慢了，已被抢光");
        }
        // 投递异步发放消息，立即返回（削峰）
        mqProducer.send(MQConstant.ORDER_STREAM,
                Map.of("action", "seckill", "userId", String.valueOf(userId),
                        "couponId", String.valueOf(couponId)));
        log.info("秒杀预扣成功，异步发放中: userId={}, couponId={}, 剩余={}", userId, couponId, stock);
    }

    @Override
    public PageResult<UserCouponVO> myCoupons(Long userId, Integer status, int page, int pageSize) {
        int size = Math.min(pageSize, 50);
        long offset = (long) (page - 1) * size;
        // SQL 按三态过滤（已过期=未使用且 expire_time <= now）
        PageResult<UserCouponVO> result = PageResult.of(
                userCouponMapper.countMy(userId, status),
                userCouponMapper.selectMyPage(userId, status, offset, size));
        // 展示层状态：未使用但已过期的标记为已过期
        if (status == null) {
            LocalDateTime now = LocalDateTime.now();
            result.getList().forEach(vo -> {
                if (vo.getStatus() == 0 && vo.getExpireTime() != null
                        && !now.isBefore(vo.getExpireTime())) {
                    vo.setStatus(2);
                }
            });
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean grantSeckillCoupon(Long userId, Long couponId) {
        // 幂等：已领取直接跳过（消息重复消费安全）
        if (userCouponMapper.countByUserAndCoupon(userId, couponId) > 0) {
            log.info("秒杀券已发放，跳过重复消息: userId={}, couponId={}", userId, couponId);
            return true;
        }
        Coupon coupon = couponMapper.findById(couponId);
        if (coupon == null) {
            log.warn("秒杀券不存在: couponId={}", couponId);
            restoreRedis(couponId, userId);
            return false;
        }
        // DB 库存最终一致性校验：乐观锁扣减
        int rows = couponMapper.deductStock(couponId);
        if (rows == 0) {
            log.warn("秒杀券DB库存不足，回补Redis: userId={}, couponId={}", userId, couponId);
            restoreRedis(couponId, userId);
            return false;
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        try {
            userCouponMapper.insert(userCoupon);
        } catch (DuplicateKeyException e) {
            // 极小概率并发：回滚扣减并视为已发放
            couponMapper.restoreStock(couponId);
            log.info("秒杀券并发发放冲突，已回补: userId={}, couponId={}", userId, couponId);
            return true;
        }
        log.info("秒杀券发放成功: userId={}, couponId={}", userId, couponId);
        return true;
    }

    /** 校验券存在性与类型 */
    private Coupon checkCoupon(Long couponId, int expectedCategory) {
        Coupon coupon = couponMapper.findById(couponId);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (coupon.getCategory() != expectedCategory) {
            throw new BusinessException(expectedCategory == CATEGORY_SECKILL
                    ? "该券不是秒杀券" : "该券为秒杀券，请走秒杀入口");
        }
        return coupon;
    }

    /* ================= 下单用券 / 店主建券 ================= */

    @Override
    public List<UserCouponVO> usableCoupons(Long userId) {
        // status=0：未使用且未过期（SQL 三态过滤），取前50张足够下单选择
        return userCouponMapper.selectMyPage(userId, 0, 0, 50);
    }

    @Override
    public void markUsed(Long userId, Long userCouponId) {
        UserCoupon uc = checkUserCoupon(userId, userCouponId);
        int rows = userCouponMapper.markUsed(userId, uc.getCouponId());
        if (rows == 0) {
            throw new BusinessException("优惠券已使用或已失效");
        }
    }

    @Override
    public void restoreUsed(Long userId, Long userCouponId) {
        UserCoupon uc = checkUserCoupon(userId, userCouponId);
        userCouponMapper.restoreUsed(userId, uc.getCouponId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Long userId, CouponSaveDTO dto) {
        // 仅店主可为自己的店铺建券
        Shop shop = shopMapper.findByOwner(userId);
        if (shop == null) {
            throw new BusinessException("您还未开通店铺，请先开店");
        }
        if (!shop.getId().equals(dto.getShopId())) {
            throw new BusinessException("无权为该店铺创建优惠券");
        }
        // 时间校验：过期时间必须晚于当前，秒杀券开始时间需早于过期时间
        if (!LocalDateTime.now().isBefore(dto.getExpireTime())) {
            throw new BusinessException("过期时间必须晚于当前时间");
        }
        if (dto.getCategory() == CATEGORY_SECKILL && dto.getStartTime() == null) {
            throw new BusinessException("秒杀券必须设置开始时间");
        }
        if (dto.getStartTime() != null && !dto.getStartTime().isBefore(dto.getExpireTime())) {
            throw new BusinessException("开始时间必须早于过期时间");
        }
        if (dto.getMoney().compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new BusinessException("券面额不合法");
        }
        Coupon coupon = new Coupon();
        coupon.setShopId(dto.getShopId());
        coupon.setCouponName(XssUtil.clean(dto.getCouponName()));
        coupon.setCategory(dto.getCategory());
        coupon.setMoney(dto.getMoney());
        coupon.setTotalStock(dto.getTotalStock());
        coupon.setPerLimit(dto.getPerLimit());
        coupon.setStartTime(dto.getStartTime());
        coupon.setExpireTime(dto.getExpireTime());
        couponMapper.insert(coupon);
        // 秒杀券预热：Redis 预扣库存初始化，清空历史去重集合
        if (dto.getCategory() == CATEGORY_SECKILL) {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstant.SECKILL_STOCK + coupon.getId(), String.valueOf(dto.getTotalStock()));
            stringRedisTemplate.delete(RedisKeyConstant.SECKILL_USER + coupon.getId());
        }
        log.info("店主创建优惠券成功: shopId={}, couponId={}, category={}",
                dto.getShopId(), coupon.getId(), dto.getCategory());
    }

    /** 校验用户持券记录存在 */
    private UserCoupon checkUserCoupon(Long userId, Long userCouponId) {
        UserCoupon uc = userCouponMapper.findByUserAndId(userId, userCouponId);
        if (uc == null) {
            throw new BusinessException("优惠券不存在");
        }
        return uc;
    }

    /* ================= 店主删除优惠券 ================= */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long couponId) {
        Coupon coupon = couponMapper.findById(couponId);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        // 仅能删除自己店铺的券
        Shop shop = shopMapper.findByOwner(userId);
        if (shop == null || !shop.getId().equals(coupon.getShopId())) {
            throw new BusinessException("无权删除该优惠券");
        }
        couponMapper.logicDelete(couponId);
        // 秒杀券同步清理 Redis 预扣库存与去重集合，避免残留脏数据
        if (coupon.getCategory() == CATEGORY_SECKILL) {
            stringRedisTemplate.delete(RedisKeyConstant.SECKILL_STOCK + couponId);
            stringRedisTemplate.delete(RedisKeyConstant.SECKILL_USER + couponId);
        }
        log.info("店主删除优惠券: userId={}, couponId={}, category={}", userId, couponId, coupon.getCategory());
    }

    /** 秒杀失败回补 Redis 预扣的库存与去重集合 */
    private void restoreRedis(Long couponId, Long userId) {
        stringRedisTemplate.opsForValue().increment(RedisKeyConstant.SECKILL_STOCK + couponId);
        stringRedisTemplate.opsForSet().remove(RedisKeyConstant.SECKILL_USER + couponId,
                String.valueOf(userId));
    }
}
