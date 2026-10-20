package com.itheima.websocket;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.itheima.common.exception.BusinessException;
import com.itheima.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * 聊天 WebSocket 处理器（/ws/chat）：
 * 客户端上行：{toUserId, msgType, content, imageUrl} msgType 0文本 1图片
 * 服务端下行：{type:"connected"|"chat"|"read"|"error", ...}
 * 业务异常（敏感词/参数错误等）以 {type:"error", msg} 推回发送者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WsSessionManager wsSessionManager;
    private final ChatService chatService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        wsSessionManager.add(userId, session);
        // 连接成功回执
        wsSessionManager.sendTo(userId, "{\"type\":\"connected\",\"userId\":" + userId + "}");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = getUserId(session);
        try {
            JSONObject json = JSONUtil.parseObj(message.getPayload());
            long toUserId = json.getLong("toUserId");
            int msgType = json.getInt("msgType", 0);
            String content = json.getStr("content", "");
            String imageUrl = json.getStr("imageUrl", "");
            chatService.send(userId, toUserId, msgType, content, imageUrl);
        } catch (BusinessException e) {
            // 业务校验失败：错误提示推回发送者
            wsSessionManager.sendTo(userId,
                    JSONUtil.toJsonStr(Map.of("type", "error", "msg", e.getMessage())));
        } catch (Exception e) {
            log.error("WS消息处理异常: userId={}", userId, e);
            wsSessionManager.sendTo(userId,
                    JSONUtil.toJsonStr(Map.of("type", "error", "msg", "消息发送失败，请重试")));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        wsSessionManager.remove(getUserId(session), session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WS传输异常: userId={}", getUserId(session), exception);
    }

    /** 从握手 attributes 取出鉴权后的用户ID */
    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId == null) {
            throw new BusinessException("WebSocket 未鉴权");
        }
        return (Long) userId;
    }
}
