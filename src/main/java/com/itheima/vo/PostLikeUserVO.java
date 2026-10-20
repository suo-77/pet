package com.itheima.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子点赞用户视图对象（谁赞了我）
 */
@Data
public class PostLikeUserVO {

    private Long userId;
    /** 点赞者昵称 */
    private String nickname;
    /** 点赞者头像 */
    private String avatar;
    /** 点赞时间（DB 回退来源才有，Redis 实时来源为 null） */
    private LocalDateTime createTime;
}
