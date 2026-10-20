package com.itheima.service;

import com.itheima.common.result.PageResult;
import com.itheima.dto.PostCommentDTO;
import com.itheima.dto.PostCreateDTO;
import com.itheima.vo.PostCommentVO;
import com.itheima.vo.PostLikeUserVO;
import com.itheima.vo.PostVO;

import java.util.List;

/**
 * 社区帖子服务：发布 / 信息流 / 点赞(Redis+异步落库) / 评论 / 删除 / 我的动态
 */
public interface PostService {

    /** 发布帖子（敏感词+XSS校验），返回新帖ID */
    Long create(Long userId, PostCreateDTO dto);

    /** 信息流分页（viewerId 可为空，未登录时 liked=false）；userId 非空时查询该用户动态 */
    PageResult<PostVO> pageFeed(Long viewerId, Long userId, int page, int pageSize);

    /** 我发布的动态分页 */
    PageResult<PostVO> myPosts(Long userId, int page, int pageSize);

    /** 点赞：Redis 计数/Set 先行，Stream 异步落库 */
    void like(Long userId, Long postId);

    /** 取消点赞 */
    void unlike(Long userId, Long postId);

    /** 帖子点赞用户列表（谁赞了）：Redis 实时优先，DB 回退 */
    List<PostLikeUserVO> likeUsers(Long postId);

    /** 发表评论（敏感词+XSS校验） */
    void comment(Long userId, Long postId, PostCommentDTO dto);

    /** 评论分页（时间正序） */
    PageResult<PostCommentVO> comments(Long postId, int page, int pageSize);

    /** 删除帖子：仅作者本人可删 */
    void delete(Long userId, Long postId);
}
