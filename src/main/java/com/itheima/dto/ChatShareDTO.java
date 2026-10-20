package com.itheima.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分享动态给好友私信请求体
 */
@Data
public class ChatShareDTO {

    /** 接收分享的好友用户ID */
    @NotNull(message = "请选择要分享的好友")
    private Long toUserId;

    /** 被分享的帖子ID */
    @NotNull(message = "分享的帖子不能为空")
    private Long postId;
}