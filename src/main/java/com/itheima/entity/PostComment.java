package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子评论实体，对应表 post_comment
 */
@Data
public class PostComment {

    private Long id;
    private Long postId;
    private Long userId;
    /** 父评论ID，0为一级评论 */
    private Long parentId;
    /** 评论内容(XSS/敏感词过滤后) */
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;

    /** 联表附加：评论者昵称 */
    private String nickname;
    /** 联表附加：评论者头像 */
    private String avatar;
}
