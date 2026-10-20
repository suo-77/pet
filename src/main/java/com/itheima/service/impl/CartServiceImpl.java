package com.itheima.service.impl;

import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.util.ImageJsonUtil;
import com.itheima.dto.CartItemDTO;
import com.itheima.dto.CheckoutDTO;
import com.itheima.entity.PetGoods;
import com.itheima.mapper.PetGoodsMapper;
import com.itheima.mq.constant.MQConstant;
import com.itheima.mq.stream.MQProducer;
import com.itheima.service.CartService;
import com.itheima.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车服务实现（数据主存 Redis Hash：cart:{userId} -> Hash<goodsId, num>）
 * 1. 加购/改量校验商品在售与库存上限，避免结算时才暴露问题
 * 2. 列表按 Hash 顺序组装商品快照信息（价格/库存取实时值），失效商品自动清理
 * 3. 结算仅做预校验后投递 order Stream，由消费者异步逐商品下单（削峰）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    /** 商品状态：上架 */
    private static final int GOODS_ON_SALE = 0;

    private final StringRedisTemplate stringRedisTemplate;
    private final PetGoodsMapper petGoodsMapper;
    private final MQProducer mqProducer;

    @Override
    public void add(Long userId, CartItemDTO dto) {
        PetGoods goods = getOnSaleGoods(dto.getGoodsId());
        String goodsKey = String.valueOf(dto.getGoodsId());
        Object cur = stringRedisTemplate.opsForHash().get(cartKey(userId), goodsKey);
        int total = (cur == null ? 0 : Integer.parseInt(cur.toString())) + dto.getNum();
        if (total > goods.getStock()) {
            throw new BusinessException("超出商品库存，当前最多可加购 " + goods.getStock() + " 件");
        }
        stringRedisTemplate.opsForHash().put(cartKey(userId), goodsKey, String.valueOf(total));
        log.info("加入购物车: userId={}, goodsId={}, num={}, 累计={}", userId, dto.getGoodsId(), dto.getNum(), total);
    }

    @Override
    public void update(Long userId, CartItemDTO dto) {
        PetGoods goods = getOnSaleGoods(dto.getGoodsId());
        if (dto.getNum() > goods.getStock()) {
            throw new BusinessException("超出商品库存，当前最多可购买 " + goods.getStock() + " 件");
        }
        stringRedisTemplate.opsForHash().put(cartKey(userId), String.valueOf(dto.getGoodsId()), String.valueOf(dto.getNum()));
    }

    @Override
    public void remove(Long userId, Long goodsId) {
        stringRedisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(goodsId));
    }

    @Override
    public List<CartItemVO> list(Long userId) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(cartKey(userId));
        if (entries.isEmpty()) {
            return List.of();
        }
        // Hash 键为 goodsId，保持插入顺序组装
        Map<Long, Integer> goodsNumMap = new LinkedHashMap<>();
        entries.forEach((k, v) -> goodsNumMap.put(Long.parseLong(k.toString()), Integer.parseInt(v.toString())));
        List<PetGoods> goodsList = petGoodsMapper.selectByIds(new ArrayList<>(goodsNumMap.keySet()));
        List<CartItemVO> result = new ArrayList<>();
        for (PetGoods goods : goodsList) {
            Integer num = goodsNumMap.get(goods.getId());
            if (num == null) {
                continue;
            }
            // 已下架/删除商品同步清理出购物车，避免僵尸条目
            if (goods.getDeleted() == 1 || goods.getStatus() != GOODS_ON_SALE) {
                remove(userId, goods.getId());
                continue;
            }
            CartItemVO vo = new CartItemVO();
            vo.setGoodsId(goods.getId());
            vo.setPetName(goods.getPetName());
            vo.setPrice(goods.getPrice());
            List<String> images = ImageJsonUtil.parse(goods.getImages());
            vo.setImage(images.isEmpty() ? null : images.get(0));
            vo.setNum(num);
            vo.setStock(goods.getStock());
            result.add(vo);
        }
        return result;
    }

    @Override
    public void checkout(Long userId, CheckoutDTO dto) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(cartKey(userId));
        if (entries.isEmpty()) {
            throw new BusinessException("购物车为空，先去挑选心仪的宠物吧");
        }
        List<Long> goodsIds = dto == null || dto.getGoodsIds() == null || dto.getGoodsIds().isEmpty()
                ? entries.keySet().stream().map(k -> Long.parseLong(k.toString())).collect(Collectors.toList())
                : dto.getGoodsIds();
        // 结算预校验：商品在售且购物车数量不超库存，快速反馈给用户
        for (Long goodsId : goodsIds) {
            Object num = entries.get(String.valueOf(goodsId));
            if (num == null) {
                throw new BusinessException("商品不在购物车中");
            }
            PetGoods goods = getOnSaleGoods(goodsId);
            if (Integer.parseInt(num.toString()) > goods.getStock()) {
                throw new BusinessException("《" + goods.getPetName() + "》库存不足，仅剩 " + goods.getStock() + " 件");
            }
        }
        // 投递 order Stream，消费者逐商品异步下单（削峰，避免结算高峰打爆数据库）
        // 优惠券作用于金额最高的一单，由消费者决定核销时机；nums 与 goodsIds 顺序一一对应
        java.util.Map<String, String> payload = new LinkedHashMap<>();
        payload.put("action", "cart");
        payload.put("userId", String.valueOf(userId));
        payload.put("goodsIds", goodsIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        payload.put("nums", goodsIds.stream()
                .map(gid -> entries.get(String.valueOf(gid)).toString())
                .collect(Collectors.joining(",")));
        if (dto != null && dto.getUserCouponId() != null) {
            payload.put("userCouponId", String.valueOf(dto.getUserCouponId()));
        }
        mqProducer.send(MQConstant.ORDER_STREAM, payload);
        log.info("购物车结算已投递: userId={}, goodsIds={}", userId, goodsIds);
    }

    /** 查询在售商品，不存在/下架统一业务异常 */
    private PetGoods getOnSaleGoods(Long goodsId) {
        PetGoods goods = petGoodsMapper.findById(goodsId);
        if (goods == null || goods.getDeleted() == 1 || goods.getStatus() != GOODS_ON_SALE) {
            throw new BusinessException("商品不存在或已下架");
        }
        return goods;
    }

    /** 购物车 Redis 键 */
    private String cartKey(Long userId) {
        return RedisKeyConstant.CART + userId;
    }
}
