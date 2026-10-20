package com.itheima.service.impl;

import cn.hutool.core.util.StrUtil;
import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.PageResult;
import com.itheima.common.util.ImageJsonUtil;
import com.itheima.common.util.SensitiveWordUtil;
import com.itheima.common.util.XssUtil;
import com.itheima.dto.EvaluateCreateDTO;
import com.itheima.dto.GoodsSaveDTO;
import com.itheima.entity.GoodsEvaluate;
import com.itheima.entity.PetGoods;
import com.itheima.entity.Shop;
import com.itheima.mapper.GoodsEvaluateMapper;
import com.itheima.mapper.PetGoodsMapper;
import com.itheima.mapper.ShopMapper;
import com.itheima.service.GoodsService;
import com.itheima.vo.EvaluateVO;
import com.itheima.vo.GoodsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 宠物商品服务实现：
 * 1. 收藏使用 Redis ZSet：goods:fav:{userId}，score=收藏时间戳，支持按时间倒序分页
 * 2. 收藏/取消幂等：ZADD 重复收藏、ZREM 未收藏均直接返回成功
 * 3. 评价：敏感词拦截 + XSS 清洗 + 图片URL本服务路径校验
 * 4. 店主商品管理：先校验一人一店归属，再执行增删改（敏感词+XSS+图片路径校验）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    private static final int MAX_PAGE_SIZE = 50;
    /** 商品状态：上架 */
    private static final int STATUS_ON = 0;
    /** 商品状态：下架 */
    private static final int STATUS_OFF = 1;
    private static final int MAX_IMAGES = 6;

    private final PetGoodsMapper goodsMapper;
    private final GoodsEvaluateMapper evaluateMapper;
    private final ShopMapper shopMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SensitiveWordUtil sensitiveWordUtil;

    @Override
    public GoodsVO detail(Long viewerId, Long goodsId) {
        PetGoods goods = checkGoodsExists(goodsId);
        GoodsVO vo = GoodsVO.from(goods, ImageJsonUtil.parse(goods.getImages()));
        vo.setFav(isFav(viewerId, goodsId));
        return vo;
    }

    @Override
    public PageResult<GoodsVO> pageByShop(Long shopId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        List<PetGoods> goodsList = goodsMapper.selectByShop(shopId, offset, size);
        List<GoodsVO> list = goodsList.stream()
                .map(g -> GoodsVO.from(g, ImageJsonUtil.parse(g.getImages())))
                .toList();
        return PageResult.of(goodsMapper.countByShop(shopId), list);
    }

    @Override
    public PageResult<EvaluateVO> evaluates(Long goodsId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        List<EvaluateVO> list = evaluateMapper.selectPageByGoods(goodsId, offset, size)
                .stream()
                .map(e -> EvaluateVO.from(e, ImageJsonUtil.parse(e.getImages())))
                .toList();
        return PageResult.of(evaluateMapper.countByGoods(goodsId), list);
    }

    @Override
    public void evaluate(Long userId, Long goodsId, EvaluateCreateDTO dto) {
        checkGoodsExists(goodsId);
        if (sensitiveWordUtil.containsSensitive(dto.getContent())) {
            throw new BusinessException("内容不合法，请修改后重新评价");
        }
        List<String> images = dto.getImages() == null ? List.of() : dto.getImages();
        for (String img : images) {
            if (StrUtil.isBlank(img) || !img.startsWith("/images/")) {
                throw new BusinessException("图片参数不合法");
            }
        }
        GoodsEvaluate evaluate = new GoodsEvaluate();
        evaluate.setGoodsId(goodsId);
        evaluate.setUserId(userId);
        evaluate.setContent(XssUtil.clean(dto.getContent()));
        evaluate.setImages(ImageJsonUtil.toJson(images));
        evaluateMapper.insert(evaluate);
        log.info("评价成功: goodsId={}, userId={}", goodsId, userId);
    }

    @Override
    public void fav(Long userId, Long goodsId) {
        checkGoodsExists(goodsId);
        // ZADD 幂等：重复收藏覆盖 score（视为更新收藏时间），不报错
        stringRedisTemplate.opsForZSet().add(RedisKeyConstant.GOODS_FAV + userId,
                String.valueOf(goodsId), System.currentTimeMillis());
        log.info("收藏商品: userId={}, goodsId={}", userId, goodsId);
    }

    @Override
    public void unfav(Long userId, Long goodsId) {
        checkGoodsExists(goodsId);
        stringRedisTemplate.opsForZSet().remove(RedisKeyConstant.GOODS_FAV + userId,
                String.valueOf(goodsId));
        log.info("取消收藏: userId={}, goodsId={}", userId, goodsId);
    }

    @Override
    public PageResult<GoodsVO> myFavs(Long userId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        String key = RedisKeyConstant.GOODS_FAV + userId;
        // 总数与当页收藏ID（按收藏时间倒序）
        Long total = stringRedisTemplate.opsForZSet().zCard(key);
        Set<String> ids = stringRedisTemplate.opsForZSet()
                .reverseRange(key, offset, offset + size - 1);
        if (ids == null || ids.isEmpty()) {
            return PageResult.of(total == null ? 0 : total, List.of());
        }
        // 按 ZSet 返回顺序（时间倒序）组装，保证分页顺序稳定
        List<Long> goodsIds = ids.stream().map(Long::valueOf).toList();
        List<PetGoods> goodsList = goodsMapper.selectByIds(goodsIds);
        List<GoodsVO> list = new ArrayList<>();
        for (Long id : goodsIds) {
            goodsList.stream()
                    .filter(g -> g.getId().equals(id))
                    .findFirst()
                    .ifPresent(g -> list.add(GoodsVO.from(g, ImageJsonUtil.parse(g.getImages()))));
        }
        return PageResult.of(total == null ? 0 : total, list);
    }

    /** 当前用户是否已收藏（未登录返回 false） */
    private boolean isFav(Long viewerId, Long goodsId) {
        if (viewerId == null) {
            return false;
        }
        return stringRedisTemplate.opsForZSet()
                .score(RedisKeyConstant.GOODS_FAV + viewerId, String.valueOf(goodsId)) != null;
    }

    /** 商品存在性校验 */
    private PetGoods checkGoodsExists(Long goodsId) {
        PetGoods goods = goodsMapper.findById(goodsId);
        if (goods == null) {
            throw new BusinessException("商品不存在");
        }
        return goods;
    }

    /* ================= 店主商品管理 ================= */

    @Override
    public PageResult<GoodsVO> myGoodsPage(Long userId, int page, int pageSize) {
        Shop shop = checkOwnerShop(userId);
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        List<PetGoods> goodsList = goodsMapper.selectByShopOwner(shop.getId(), offset, size);
        List<GoodsVO> list = goodsList.stream()
                .map(g -> GoodsVO.from(g, ImageJsonUtil.parse(g.getImages())))
                .toList();
        return PageResult.of(goodsMapper.countByShopOwner(shop.getId()), list);
    }

    @Override
    public GoodsVO createGoods(Long userId, GoodsSaveDTO dto) {
        Shop shop = checkOwnerShop(userId);
        PetGoods goods = new PetGoods();
        applySaveDTO(goods, dto);
        goods.setShopId(shop.getId());
        goodsMapper.insert(goods);
        log.info("店主新增商品: shopId={}, goodsId={}, petName={}", shop.getId(), goods.getId(), goods.getPetName());
        return GoodsVO.from(goodsMapper.findById(goods.getId()), ImageJsonUtil.parse(goods.getImages()));
    }

    @Override
    public void updateGoods(Long userId, Long goodsId, GoodsSaveDTO dto) {
        Shop shop = checkOwnerShop(userId);
        PetGoods goods = checkOwnerGoods(shop, goodsId);
        applySaveDTO(goods, dto);
        // 不允许通过编辑修改店铺归属
        goodsMapper.update(goods);
        log.info("店主编辑商品: goodsId={}, userId={}", goodsId, userId);
    }

    @Override
    public void updateGoodsStatus(Long userId, Long goodsId, Integer status) {
        Shop shop = checkOwnerShop(userId);
        checkOwnerGoods(shop, goodsId);
        if (status == null || (status != STATUS_ON && status != STATUS_OFF)) {
            throw new BusinessException("商品状态不合法");
        }
        goodsMapper.updateStatus(goodsId, status);
        log.info("店主{}商品: goodsId={}, userId={}", status == STATUS_ON ? "上架" : "下架", goodsId, userId);
    }

    @Override
    public void deleteGoods(Long userId, Long goodsId) {
        Shop shop = checkOwnerShop(userId);
        checkOwnerGoods(shop, goodsId);
        goodsMapper.deleteById(goodsId);
        log.info("店主删除商品: goodsId={}, userId={}", goodsId, userId);
    }

    /** 组装商品保存字段：XSS 清洗 + 敏感词 + 图片路径校验 */
    private void applySaveDTO(PetGoods goods, GoodsSaveDTO dto) {
        String petName = XssUtil.clean(dto.getPetName());
        if (sensitiveWordUtil.containsSensitive(petName)) {
            throw new BusinessException("宠物名称包含不合法内容");
        }
        goods.setPetName(petName);
        goods.setBreed(StrUtil.isBlank(dto.getBreed()) ? null : XssUtil.clean(dto.getBreed()));
        goods.setAge(StrUtil.isBlank(dto.getAge()) ? null : XssUtil.clean(dto.getAge()));
        goods.setPrice(dto.getPrice());
        goods.setDescription(StrUtil.isBlank(dto.getDescription()) ? null : XssUtil.clean(dto.getDescription()));
        goods.setStock(dto.getStock());
        goods.setImages(checkGoodsImages(dto.getImages()));
    }

    /** 校验商品图片：最多6张，且必须为本服务上传路径；null=不修改，空列表=清空 */
    private String checkGoodsImages(List<String> images) {
        if (images == null) {
            return null;
        }
        if (images.isEmpty()) {
            return "[]";
        }
        if (images.size() > MAX_IMAGES) {
            throw new BusinessException("商品图片最多" + MAX_IMAGES + "张");
        }
        for (String img : images) {
            if (StrUtil.isBlank(img) || !img.startsWith("/images/")) {
                throw new BusinessException("图片参数不合法");
            }
        }
        return ImageJsonUtil.toJson(images);
    }

    /** 校验当前用户已开店，返回其店铺 */
    private Shop checkOwnerShop(Long userId) {
        Shop shop = shopMapper.findByOwner(userId);
        if (shop == null) {
            throw new BusinessException("您还未开通店铺，请先开店");
        }
        return shop;
    }

    /** 校验商品属于该店（防越权操作他人商品） */
    private PetGoods checkOwnerGoods(Shop shop, Long goodsId) {
        PetGoods goods = goodsMapper.findById(goodsId);
        if (goods == null) {
            throw new BusinessException("商品不存在");
        }
        if (!goods.getShopId().equals(shop.getId())) {
            throw new BusinessException("无权操作该商品");
        }
        return goods;
    }
}
