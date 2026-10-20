package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话实体，对应表 chat_session（user_a_id 存两者中较小的用户ID）
 */
@Data
public class ChatSession {

    private Long id;
    private Long userAId;
    private Long userBId;
    /** 最后一条消息预览 */
    private String lastMessage;
    private LocalDateTime lastTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;
}
