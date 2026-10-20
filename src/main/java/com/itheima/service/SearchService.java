package com.itheima.service;

import com.itheima.vo.SearchResultVO;

/**
 * 首页聚合搜索：店铺名称 / 宠物名称 / 用户昵称
 */
public interface SearchService {

    /** 三合一搜索，每类最多返回 5 条 */
    SearchResultVO search(String keyword);
}
