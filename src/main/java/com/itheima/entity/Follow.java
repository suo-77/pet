package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系实体，对应表 follow
 */
@Data
public class Follow {

    private Long id;
    /** 关注者ID */
    private Long userId;
    /** 被关注者ID */
    private Long followUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0关注中 1已取关 */
    private Integer deleted;
}
