package com.itheima.mapper;

import com.itheima.entity.User;
import com.itheima.vo.UserCardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper {

    /** 新增用户，回填自增ID */
    int insert(User user);

    /** 按账号查询（逻辑未删除） */
    User findByUsername(@Param("username") String username);

    /** 按手机号查询（逻辑未删除） */
    User findByPhone(@Param("phone") String phone);

    /** 按ID查询（逻辑未删除） */
    User findById(@Param("id") Long id);

    /** 编辑个人资料（昵称/简介/头像） */
    int updateProfile(@Param("id") Long id,
                      @Param("nickname") String nickname,
                      @Param("intro") String intro,
                      @Param("avatar") String avatar);

    /** 按昵称模糊搜索（首页搜索使用，返回脱敏卡片） */
    List<UserCardVO> searchByNickname(@Param("keyword") String keyword,
                                      @Param("limit") int limit);

    /** 按ID集合批量查询（点赞用户列表使用） */
    List<User> selectByIds(@Param("ids") List<Long> ids);
}
