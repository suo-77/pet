package com.itheima.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表视图对象（含对方信息与未读数）
 */
@Data
public class ChatSessionVO {

    private Long sessionId;
    /** 对方用户ID */
    private Long peerId;
    private String peerNickname;
    private String peerAvatar;
    /** 最后一条消息预览 */
    private String lastMessage;
    private LocalDateTime lastTime;
    /** 未读消息数（我接收且未读） */
    private Long unreadCount;
}
