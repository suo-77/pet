package com.itheima.vo;

import com.itheima.entity.PostComment;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子评论视图对象
 */
@Data
public class PostCommentVO {

    private Long id;
    private Long postId;
    private Long userId;
    /** 评论者昵称 */
    private String nickname;
    /** 评论者头像 */
    private String avatar;
    private String content;
    private LocalDateTime createTime;

    public static PostCommentVO from(PostComment comment) {
        PostCommentVO vo = new PostCommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setNickname(comment.getNickname());
        vo.setAvatar(comment.getAvatar());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
