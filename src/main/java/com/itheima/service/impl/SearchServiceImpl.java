package com.itheima.service.impl;

import cn.hutool.core.util.StrUtil;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.util.XssUtil;
import com.itheima.mapper.PetGoodsMapper;
import com.itheima.mapper.ShopMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.service.SearchService;
import com.itheima.vo.GoodsVO;
import com.itheima.vo.SearchResultVO;
import com.itheima.vo.ShopVO;
import com.itheima.vo.UserCardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页聚合搜索实现：一次关键字同时检索 店铺 / 宠物商品 / 用户，各返回最多5条
 */
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    /** 每类结果上限 */
    private static final int LIMIT = 5;

    private final ShopMapper shopMapper;
    private final PetGoodsMapper goodsMapper;
    private final UserMapper userMapper;

    @Override
    public SearchResultVO search(String keyword) {
        String cleaned = XssUtil.clean(StrUtil.trimToEmpty(keyword));
        if (cleaned.isEmpty()) {
            throw new BusinessException("请输入搜索关键字");
        }
        SearchResultVO result = new SearchResultVO();
        result.setShops(shopMapper.selectPage(cleaned, 0, LIMIT)
                .stream().map(ShopVO::from).toList());
        result.setGoods(goodsMapper.searchByName(cleaned, LIMIT)
                .stream().map(g -> GoodsVO.from(g, com.itheima.common.util.ImageJsonUtil.parse(g.getImages())))
                .toList());
        List<UserCardVO> users = userMapper.searchByNickname(cleaned, LIMIT);
        result.setUsers(users);
        return result;
    }
}
