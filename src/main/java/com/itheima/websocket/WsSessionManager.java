package com.itheima.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 在线会话管理器：userId -> WebSocketSession
 * 单实例部署使用内存 Map；集群部署可扩展为 Redis 记录节点路由
 */
@Slf4j
@Component
public class WsSessionManager {

    private final Map<Long, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();

    /** 连接建立时登记 */
    public void add(Long userId, WebSocketSession session) {
        // 同一用户重复连接（多标签页）：旧会话关闭，保留新会话
        WebSocketSession old = onlineSessions.put(userId, session);
        if (old != null && old.isOpen()) {
            try {
                old.close();
            } catch (IOException ignored) {
            }
        }
        log.info("WS连接建立: userId={}, 当前在线={}", userId, onlineSessions.size());
    }

    /** 连接关闭时移除（仅移除当前会话，防止误删新连接） */
    public void remove(Long userId, WebSocketSession session) {
        onlineSessions.remove(userId, session);
        log.info("WS连接关闭: userId={}, 当前在线={}", userId, onlineSessions.size());
    }

    public boolean isOnline(Long userId) {
        WebSocketSession session = onlineSessions.get(userId);
        return session != null && session.isOpen();
    }

    /** 向指定用户推送文本消息（离线静默跳过；发送加锁防并发写） */
    public void sendTo(Long userId, String json) {
        WebSocketSession session = onlineSessions.get(userId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("WS消息推送失败: userId={}", userId, e);
        }
    }
}
