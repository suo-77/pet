package com.itheima.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.PageResult;
import com.itheima.dto.BuyGoodsDTO;
import com.itheima.entity.Coupon;
import com.itheima.entity.Orders;
import com.itheima.entity.PetGoods;
import com.itheima.entity.UserCoupon;
import com.itheima.mapper.CouponMapper;
import com.itheima.mapper.OrdersMapper;
import com.itheima.mapper.PetGoodsMapper;
import com.itheima.mapper.UserCouponMapper;
import com.itheima.mq.constant.MQConstant;
import com.itheima.mq.stream.MQProducer;
import com.itheima.service.OrderService;
import com.itheima.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 模拟订单服务实现：
 * 1. 直接购买：预校验商品与库存、优惠券归属后发 Stream，消费者异步生成订单（削峰）
 * 2. 生成订单（消费者）：乐观锁扣库存 + 优惠券乐观锁核销抵扣 + 订单落库，独立事务
 * 3. 取消订单：仅本人且已付款，恢复库存并退回优惠券
 * 4. 模拟支付：下单即 status=1 已付款，不对接真实支付
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 订单状态：已付款（模拟支付完成） */
    private static final int STATUS_PAID = 1;
    /** 订单状态：已取消 */
    private static final int STATUS_CANCELLED = 2;
    /** 用户券状态：未使用 */
    private static final int UC_UNUSED = 0;
    /** 订单类型：0普通 1秒杀 */
    private static final int TYPE_NORMAL = 0;

    private final OrdersMapper ordersMapper;
    private final PetGoodsMapper goodsMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponMapper couponMapper;
    private final MQProducer mqProducer;

    @Override
    public void buy(Long userId, BuyGoodsDTO dto) {
        PetGoods goods = goodsMapper.findById(dto.getGoodsId());
        if (goods == null) {
            throw new BusinessException("商品不存在");
        }
        if (goods.getStatus() != 0) {
            throw new BusinessException("商品已下架");
        }
        if (goods.getStock() < dto.getNum()) {
            throw new BusinessException("商品库存不足");
        }
        // 优惠券归属与可用性预校验（消费端还会做乐观锁核销兜底）
        checkUsableCoupon(userId, dto.getUserCouponId(), goods.getShopId());
        Map<String, String> payload = new java.util.LinkedHashMap<>();
        payload.put("action", "buy");
        payload.put("userId", String.valueOf(userId));
        payload.put("goodsId", String.valueOf(dto.getGoodsId()));
        payload.put("num", String.valueOf(dto.getNum()));
        if (dto.getUserCouponId() != null) {
            payload.put("userCouponId", String.valueOf(dto.getUserCouponId()));
        }
        mqProducer.send(MQConstant.ORDER_STREAM, payload);
        log.info("直接购买请求已投递: userId={}, goodsId={}, num={}, coupon={}",
                userId, dto.getGoodsId(), dto.getNum(), dto.getUserCouponId());
    }

    @Override
    public PageResult<OrderVO> myOrders(Long userId, int page, int pageSize) {
        int size = Math.min(pageSize, 50);
        long offset = (long) (page - 1) * size;
        return PageResult.of(ordersMapper.countMy(userId),
                ordersMapper.selectMyPage(userId, offset, size));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSingleOrder(Long userId, Long goodsId, int num, int orderType, Long userCouponId) {
        PetGoods goods = goodsMapper.findById(goodsId);
        // 商品失效：业务失败，调用方记日志后正常确认（不重试）
        if (goods == null || goods.getStatus() != 0) {
            log.warn("订单生成失败，商品不存在或已下架: goodsId={}", goodsId);
            return null;
        }
        // 乐观锁扣库存：0 行 = 库存不足（最终一致性校验，防超卖）
        int rows = goodsMapper.deductStock(goodsId, num);
        if (rows == 0) {
            log.warn("订单生成失败，库存不足: goodsId={}, num={}", goodsId, num);
            return null;
        }
        BigDecimal total = goods.getPrice().multiply(BigDecimal.valueOf(num));
        // 优惠券核销与抵扣：乐观锁（status=0 才核销），失败不阻断下单（按原价）
        // 仅当券属于该商品所属店铺时才抵扣，跨店券一律不生效
        BigDecimal discount = BigDecimal.ZERO;
        Long couponId = null;
        if (userCouponId != null) {
            UserCoupon uc = userCouponMapper.findByUserAndId(userId, userCouponId);
            if (uc != null && uc.getStatus() == UC_UNUSED) {
                Coupon coupon = couponMapper.findById(uc.getCouponId());
                if (coupon != null && goods.getShopId().equals(coupon.getShopId())) {
                    int used = userCouponMapper.markUsed(userId, uc.getCouponId());
                    if (used > 0) {
                        discount = coupon.getMoney();
                        // 抵扣不超过订单金额
                        if (discount.compareTo(total) > 0) {
                            discount = total;
                        }
                        couponId = uc.getCouponId();
                    }
                }
            }
        }
        Orders order = new Orders();
        order.setOrderNo("P" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4));
        order.setUserId(userId);
        order.setGoodsId(goodsId);
        order.setCouponId(couponId);
        order.setDiscountAmount(discount);
        order.setGoodsNum(num);
        order.setGoodsPrice(goods.getPrice());
        order.setTotalAmount(total.subtract(discount));
        order.setStatus(STATUS_PAID);
        order.setOrderType(orderType);
        ordersMapper.insert(order);
        log.info("订单生成成功: orderNo={}, userId={}, goodsId={}, num={}, discount={}",
                order.getOrderNo(), userId, goodsId, num, discount);
        return order.getOrderNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        Orders order = ordersMapper.findById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 权限校验：只能取消自己的订单
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != STATUS_PAID) {
            throw new BusinessException("当前订单状态不可取消");
        }
        // 恢复库存
        goodsMapper.restoreStock(order.getGoodsId(), order.getGoodsNum());
        // 退回优惠券（若使用过）
        if (order.getCouponId() != null) {
            userCouponMapper.restoreUsed(userId, order.getCouponId());
        }
        ordersMapper.updateStatus(orderId, STATUS_CANCELLED);
        log.info("订单已取消: orderNo={}, userId={}, 已恢复库存与优惠券", order.getOrderNo(), userId);
    }

    /** 校验用户优惠券可用（存在、未使用、未过期、且属于目标商品店铺），未传券直接通过 */
    private void checkUsableCoupon(Long userId, Long userCouponId, Long goodsShopId) {
        if (userCouponId == null) {
            return;
        }
        UserCoupon uc = userCouponMapper.findByUserAndId(userId, userCouponId);
        if (uc == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (uc.getStatus() != UC_UNUSED) {
            throw new BusinessException("优惠券已使用");
        }
        var coupon = couponMapper.findById(uc.getCouponId());
        if (coupon == null || !LocalDateTime.now().isBefore(coupon.getExpireTime())) {
            throw new BusinessException("优惠券已过期");
        }
        if (goodsShopId != null && !goodsShopId.equals(coupon.getShopId())) {
            throw new BusinessException("该优惠券仅限对应店铺的商品使用");
        }
    }
}
