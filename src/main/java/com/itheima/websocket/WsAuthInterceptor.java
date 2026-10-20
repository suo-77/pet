package com.itheima.websocket;

import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器：连接地址 /ws/chat?token=xxx
 * WS 无法携带自定义请求头，改为从 URL 查询参数取 token；
 * 校验 JWT 签名 + Redis 登录态，通过后把 userId 放入 attributes 供 Handler 使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 解析查询参数 token
        String query = request.getURI().getQuery();
        String token = null;
        if (query != null) {
            for (String pair : query.split("&")) {
                if (pair.startsWith("token=")) {
                    token = pair.substring(6);
                    break;
                }
            }
        }
        if (token == null || token.isBlank()) {
            log.warn("WS握手拒绝：缺少token");
            return false;
        }
        try {
            Long userId = jwtUtil.parseToken(token);
            // Redis 登录态二次校验
            String cached = stringRedisTemplate.opsForValue().get(RedisKeyConstant.LOGIN_TOKEN + token);
            if (cached == null || !cached.equals(String.valueOf(userId))) {
                log.warn("WS握手拒绝：token已失效, userId={}", userId);
                return false;
            }
            attributes.put("userId", userId);
            return true;
        } catch (Exception e) {
            log.warn("WS握手拒绝：token解析失败");
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}
