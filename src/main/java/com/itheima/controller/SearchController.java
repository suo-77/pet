package com.itheima.controller;

import com.itheima.common.result.Result;
import com.itheima.service.SearchService;
import com.itheima.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页搜索接口：一次关键字检索 店铺 / 宠物商品 / 用户
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /** 聚合搜索（公开） */
    @GetMapping
    public Result<SearchResultVO> search(@RequestParam String keyword) {
        return Result.success(searchService.search(keyword));
    }
}
