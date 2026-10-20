package com.itheima.mapper;

import com.itheima.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 帖子点赞关系 Mapper
 */
@Mapper
public interface PostLikeMapper {

    /** 查询点赞记录（含已取消） */
    PostLike findByPair(@Param("postId") Long postId, @Param("userId") Long userId);

    /** 新增点赞记录（唯一键冲突由业务层处理） */
    int insert(PostLike like);

    /** 恢复/取消点赞：更新逻辑删除标记 */
    int updateDeleted(@Param("id") Long id, @Param("deleted") Integer deleted);

    /** 帖子有效点赞列表（Redis 数据被清理后的 DB 回退来源） */
    List<PostLike> selectByPost(@Param("postId") Long postId);
}
