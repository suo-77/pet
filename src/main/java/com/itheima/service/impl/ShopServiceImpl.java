package com.itheima.service.impl;

import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.PageResult;
import com.itheima.common.util.XssUtil;
import com.itheima.dto.ShopSaveDTO;
import com.itheima.entity.Shop;
import com.itheima.mapper.CouponMapper;
import com.itheima.mapper.ShopMapper;
import com.itheima.service.ShopService;
import com.itheima.vo.CouponVO;
import com.itheima.vo.ShopVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 店铺服务实现：公开浏览 + 店主开店/管理（一人一店由 owner_id 唯一键兜底）
 */
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ShopMapper shopMapper;
    private final CouponMapper couponMapper;

    @Override
    public PageResult<ShopVO> page(String keyword, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        List<Shop> list = shopMapper.selectPage(keyword, offset, size);
        return PageResult.of(shopMapper.countPage(keyword),
                list.stream().map(ShopVO::from).toList());
    }

    @Override
    public ShopVO detail(Long shopId) {
        Shop shop = shopMapper.findById(shopId);
        if (shop == null) {
            throw new BusinessException("店铺不存在");
        }
        return ShopVO.from(shop);
    }

    @Override
    public List<CouponVO> coupons(Long shopId) {
        // 店铺存在性校验
        if (shopMapper.findById(shopId) == null) {
            throw new BusinessException("店铺不存在");
        }
        return couponMapper.selectValidByShop(shopId)
                .stream().map(CouponVO::from).toList();
    }

    @Override
    public ShopVO myShop(Long userId) {
        Shop shop = shopMapper.findByOwner(userId);
        return shop == null ? null : ShopVO.from(shop);
    }

    @Override
    public ShopVO openShop(Long userId, ShopSaveDTO dto) {
        if (shopMapper.findByOwner(userId) != null) {
            throw new BusinessException("您已开通店铺，一人仅能拥有一家小店");
        }
        Shop shop = new Shop();
        shop.setOwnerId(userId);
        shop.setShopName(XssUtil.clean(dto.getShopName()));
        shop.setShopDesc(dto.getShopDesc() == null ? null : XssUtil.clean(dto.getShopDesc()));
        shop.setCover(dto.getCover());
        try {
            shopMapper.insert(shop);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("您已开通店铺，一人仅能拥有一家小店");
        }
        return ShopVO.from(shop);
    }

    @Override
    public ShopVO updateMyShop(Long userId, ShopSaveDTO dto) {
        Shop shop = checkOwner(userId, null);
        shop.setShopName(XssUtil.clean(dto.getShopName()));
        shop.setShopDesc(dto.getShopDesc() == null ? null : XssUtil.clean(dto.getShopDesc()));
        if (dto.getCover() != null && !dto.getCover().isBlank()) {
            shop.setCover(dto.getCover());
        }
        shopMapper.updateInfo(shop);
        return ShopVO.from(shopMapper.findById(shop.getId()));
    }

    /** 校验当前用户是店铺店主（shopId 为空时取其唯一店铺） */
    private Shop checkOwner(Long userId, Long shopId) {
        Shop shop = shopId == null ? shopMapper.findByOwner(userId) : shopMapper.findById(shopId);
        if (shop == null) {
            throw new BusinessException("店铺不存在，请先开店");
        }
        if (!shop.getOwnerId().equals(userId)) {
            throw new BusinessException("无权管理该店铺");
        }
        return shop;
    }
}
