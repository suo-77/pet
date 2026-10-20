package com.itheima.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息视图对象（WS 推送与聊天记录分页共用）
 */
@Data
public class ChatMessageVO {

    private Long id;
    private Long sessionId;
    private Long fromUserId;
    private Long toUserId;
    /** 文本内容 */
    private String content;
    /** 图片URL */
    private String imageUrl;
    /** 分享的帖子ID（分享消息时非空） */
    private Long postId;
    /** 消息类型：0文本 1图片 2分享动态 */
    private Integer msgType;
    /** 是否已读：0未读 1已读 */
    private Integer isRead;
    private LocalDateTime sendTime;
}
