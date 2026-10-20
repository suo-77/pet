package com.itheima.mq.consumer;

import com.itheima.mq.constant.MQConstant;
import com.itheima.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 私信通知消费者：WebSocket 实时推送之外，异步处理未读消息通知
 * 当前实现：统计接收方未读总数并记录日志（模拟站内通知发送）；
 * 预留扩展：对接短信/邮件/APP推送等通知渠道，通知失败不影响聊天主链路
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamHandler implements StreamBizHandler {

    private final ChatService chatService;

    @Override
    public String streamKey() {
        return MQConstant.CHAT_STREAM;
    }

    @Override
    public String group() {
        return MQConstant.CHAT_GROUP;
    }

    @Override
    public void handle(Map<String, String> payload) {
        long fromUserId = Long.parseLong(payload.get("fromUserId"));
        long toUserId = Long.parseLong(payload.get("toUserId"));
        long messageId = Long.parseLong(payload.get("messageId"));
        // 未读总数统计（通知内容素材）
        long unreadTotal = chatService.unreadTotal(toUserId);
        // 模拟异步通知：此处仅记录，实际可扩展推送渠道
        log.info("私信未读通知处理完成: from={}, to={}, msgId={}, 累计未读={}",
                fromUserId, toUserId, messageId, unreadTotal);
    }
}
