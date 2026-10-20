package com.itheima.mapper;

import com.itheima.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 社区帖子 Mapper
 */
@Mapper
public interface CommunityPostMapper {

    /** 发布帖子，回填自增ID */
    int insert(CommunityPost post);

    /** 按ID查询（逻辑未删除） */
    CommunityPost findById(@Param("id") Long id);

    /** 信息流分页（联表作者昵称头像，按发布时间倒序） */
    List<CommunityPost> selectFeedPage(@Param("offset") long offset, @Param("pageSize") int pageSize);

    /** 信息流总数 */
    long countFeed();

    /** 我发布的帖子分页 */
    List<CommunityPost> selectMyPage(@Param("userId") Long userId,
                                     @Param("offset") long offset,
                                     @Param("pageSize") int pageSize);

    /** 我发布的帖子总数 */
    long countMy(@Param("userId") Long userId);

    /** 逻辑删除帖子 */
    int deleteById(@Param("id") Long id);

    /** 评论数自增 */
    int incrCommentCount(@Param("id") Long id);

    /** 回写点赞数（点赞消费者按 Redis Set 大小最终一致回写） */
    int updateLikeCount(@Param("id") Long id, @Param("likeCount") long likeCount);
}
