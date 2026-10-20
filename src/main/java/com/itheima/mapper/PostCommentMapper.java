package com.itheima.mapper;

import com.itheima.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 帖子评论 Mapper
 */
@Mapper
public interface PostCommentMapper {

    /** 新增评论，回填自增ID */
    int insert(PostComment comment);

    /** 评论分页（联表评论者昵称头像，按时间正序） */
    List<PostComment> selectPageByPost(@Param("postId") Long postId,
                                       @Param("offset") long offset,
                                       @Param("pageSize") int pageSize);

    /** 帖子评论总数 */
    long countByPost(@Param("postId") Long postId);
}
