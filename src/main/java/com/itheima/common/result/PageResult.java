package com.itheima.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页返回结构：{total, list}
 * 分页接口统一参数 page、pageSize，返回 total 总条数
 */
@Data
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /** 总条数 */
    private Long total;
    /** 当前页数据 */
    private List<T> list;

    /** 工厂方法：total 使用基本类型 long，兼容 int/long/Long 各类入参，避免编译器差异问题 */
    public static <T> PageResult<T> of(long total, List<T> list) {
        return new PageResult<>(total, list);
    }
}
