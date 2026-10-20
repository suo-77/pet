package com.itheima.mq.stream;

import com.itheima.mq.consumer.StreamBizHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis Stream 生产者：XADD 追加消息（Redis 按持久化策略落盘）
 * 业务侧只依赖本类发送消息，与消费者完全解耦
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MQProducer {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发送消息到指定 Stream
     *
     * @param streamKey 目标流（见 MQConstant，如 mq:stream:sms）
     * @param payload   消息负载（业务字段 + 可选的消息ID做幂等键）
     */
    public void send(String streamKey, Map<String, String> payload) {
        stringRedisTemplate.opsForStream().add(streamKey, payload);
        log.info("发送Stream消息: stream={}, payload={}", streamKey, payload);
    }

    /**
     * 便捷重载：发送到业务处理器对应的流
     */
    public void send(StreamBizHandler handler, Map<String, String> payload) {
        send(handler.streamKey(), payload);
    }
}
