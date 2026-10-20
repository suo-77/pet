package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评价实体，对应表 goods_evaluate
 * nickname / avatar 为联表查询的评论者附加字段（非本表列）
 */
@Data
public class GoodsEvaluate {

    private Long id;
    private Long goodsId;
    private Long userId;
    /** 评价内容(XSS/敏感词过滤后) */
    private String content;
    /** 评价图片URL(JSON数组字符串) */
    private String images;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;

    /** 联表附加：评价者昵称 */
    private String nickname;
    /** 联表附加：评价者头像 */
    private String avatar;
}
