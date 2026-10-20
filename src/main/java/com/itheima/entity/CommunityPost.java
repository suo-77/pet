package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区帖子实体，对应表 community_post
 * nickname / avatar 为信息流联表查询的作者附加字段（非本表列）
 */
@Data
public class CommunityPost {

    private Long id;
    /** 作者ID */
    private Long userId;
    /** 文字内容 */
    private String content;
    /** 图片URL数组(JSON数组字符串) */
    private String images;
    /** 点赞数(Redis计数异步同步) */
    private Integer likeCount;
    /** 评论数 */
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;

    /** 联表附加：作者昵称 */
    private String nickname;
    /** 联表附加：作者头像 */
    private String avatar;
}
