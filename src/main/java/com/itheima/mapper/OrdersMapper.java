package com.itheima.mapper;

import com.itheima.entity.Orders;
import com.itheima.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模拟订单 Mapper
 */
@Mapper
public interface OrdersMapper {

    /** 新增订单，回填自增ID */
    int insert(Orders orders);

    /** 我的订单分页（联表商品名称） */
    List<OrderVO> selectMyPage(@Param("userId") Long userId,
                               @Param("offset") long offset,
                               @Param("pageSize") int pageSize);

    /** 我的订单总数 */
    long countMy(@Param("userId") Long userId);

    /** 按ID查询订单（本人校验用） */
    Orders findById(@Param("id") Long id);

    /** 更新订单状态（取消订单） */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
