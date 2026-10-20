package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子点赞关系实体，对应表 post_like
 */
@Data
public class PostLike {

    private Long id;
    private Long postId;
    private Long userId;
    private LocalDateTime createTime;
    /** 逻辑删除：0点赞中 1已取消 */
    private Integer deleted;
}
