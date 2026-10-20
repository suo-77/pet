package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体，对应表 chat_message
 */
@Data
public class ChatMessage {

    private Long id;
    private Long sessionId;
    private Long fromUserId;
    private Long toUserId;
    /** 文本内容（文本消息时非空） */
    private String content;
    /** 图片URL（图片消息时非空） */
    private String imageUrl;
    /** 分享的帖子ID（分享消息时非空） */
    private Long postId;
    /** 消息类型：0文本 1图片 2分享动态 */
    private Integer msgType;
    /** 是否已读：0未读 1已读 */
    private Integer isRead;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;
}
