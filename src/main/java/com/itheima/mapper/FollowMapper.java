package com.itheima.mapper;

import com.itheima.entity.Follow;
import com.itheima.vo.UserCardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 关注关系 Mapper
 */
@Mapper
public interface FollowMapper {

    /** 查询两个用户之间的关注记录（含已取关） */
    Follow findByPair(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    /** 新增关注关系（唯一键冲突由业务层处理） */
    int insert(Follow follow);

    /** 恢复/取消关注：更新逻辑删除标记 */
    int updateDeleted(@Param("id") Long id, @Param("deleted") Integer deleted);

    /** 我关注的人数 */
    long countFollowing(@Param("userId") Long userId);

    /** 我的粉丝数 */
    long countFan(@Param("userId") Long userId);

    /** 分页：我关注的人（含其资料卡片） */
    List<UserCardVO> selectFollowingPage(@Param("userId") Long userId,
                                         @Param("offset") long offset,
                                         @Param("pageSize") int pageSize);

    /** 分页：我的粉丝（含其资料卡片） */
    List<UserCardVO> selectFanPage(@Param("userId") Long userId,
                                   @Param("offset") long offset,
                                   @Param("pageSize") int pageSize);

    /** 推荐用户总数：排除自己与已关注用户 */
    long countRecommend(@Param("excludeUserId") Long excludeUserId);

    /** 分页：推荐用户 */
    List<UserCardVO> selectRecommendPage(@Param("excludeUserId") Long excludeUserId,
                                         @Param("offset") long offset,
                                         @Param("pageSize") int pageSize);
}
