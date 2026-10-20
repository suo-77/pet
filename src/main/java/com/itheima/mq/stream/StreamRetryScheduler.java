package com.itheima.mq.stream;

import com.itheima.mq.constant.MQConstant;
import com.itheima.mq.consumer.StreamBizHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream 重试调度器：处理"消息丢失/重复消费/积压/超时"问题的兜底机制
 * 1. 每 30 秒扫描各业务流的 PEL（待确认列表）
 * 2. 空闲超过 60 秒未 ACK 的消息用 XCLAIM 抢回重试
 * 3. 投递次数达到 MAX_RETRY(3) 仍未成功：XACK 移出 PEL 并写入死信流 {streamKey}:dead，
 *    保留完整负载供人工排查与补偿
 * 4. 幂等由业务层保证（唯一键/业务ID去重），重试不会造成重复业务数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamRetryScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final List<StreamBizHandler> handlers;

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void retryPendingMessages() {
        for (StreamBizHandler handler : handlers) {
            try {
                scanAndRetry(handler);
            } catch (Exception e) {
                log.warn("Stream重试扫描异常: stream={}, 原因: {}", handler.streamKey(), e.getMessage());
            }
        }
    }

    /**
     * 扫描单个业务流的 PEL：抢回超时未 ACK 消息，重试或转入死信
     */
    private void scanAndRetry(StreamBizHandler handler) {
        PendingMessages pending = stringRedisTemplate.opsForStream()
                .pending(handler.streamKey(), handler.group(), Range.unbounded(), 100);
        if (pending == null || pending.isEmpty()) {
            return;
        }
        for (PendingMessage pm : pending) {
            // 投递后空闲未超过阈值，可能消费中，跳过
            if (pm.getElapsedTimeSinceLastDelivery().toMillis() < MQConstant.CLAIM_MIN_IDLE_MS) {
                continue;
            }
            // XCLAIM 抢回消息（投递次数 +1）
            List<MapRecord<String, Object, Object>> claimed = stringRedisTemplate.opsForStream()
                    .claim(handler.streamKey(), handler.group(), MQConstant.RETRY_CONSUMER,
                            Duration.ofMillis(MQConstant.CLAIM_MIN_IDLE_MS), pm.getId());
            for (MapRecord<String, Object, Object> record : claimed) {
                Map<String, String> payload = toStringMap(record.getValue());
                if (pm.getTotalDeliveryCount() >= MQConstant.MAX_RETRY) {
                    moveToDead(handler, record.getId().getValue(), payload);
                } else {
                    retryHandle(handler, record.getId().getValue(), payload, pm.getTotalDeliveryCount());
                }
            }
        }
    }

    /** 重试一次业务处理，成功则 ACK；失败保留 PEL 等待下轮 */
    private void retryHandle(StreamBizHandler handler, String recordId, Map<String, String> payload, long deliveryCount) {
        try {
            log.info("重试Stream消息: stream={}, id={}, 第{}次投递", handler.streamKey(), recordId, deliveryCount);
            handler.handle(payload);
            stringRedisTemplate.opsForStream().acknowledge(handler.streamKey(), handler.group(), recordId);
            log.info("重试成功并ACK: stream={}, id={}", handler.streamKey(), recordId);
        } catch (Exception e) {
            log.warn("重试仍失败，继续留在PEL: stream={}, id={}, 原因: {}",
                    handler.streamKey(), recordId, e.getMessage());
        }
    }

    /** 超过最大重试次数：写入死信流并 ACK 移出 PEL */
    private void moveToDead(StreamBizHandler handler, String recordId, Map<String, String> payload) {
        Map<String, String> deadPayload = new HashMap<>(payload);
        deadPayload.put("originStream", handler.streamKey());
        deadPayload.put("originRecordId", recordId);
        deadPayload.put("deadTime", LocalDateTime.now().toString());
        stringRedisTemplate.opsForStream().add(handler.streamKey() + MQConstant.DEAD_SUFFIX, deadPayload);
        stringRedisTemplate.opsForStream().acknowledge(handler.streamKey(), handler.group(), recordId);
        log.error("消息重试超限进入死信流: stream={}, id={}, dead={}, payload={}",
                handler.streamKey(), recordId, handler.streamKey() + MQConstant.DEAD_SUFFIX, deadPayload);
    }

    /** 记录负载转换为 Map<String,String> */
    private Map<String, String> toStringMap(Map<Object, Object> value) {
        Map<String, String> result = new HashMap<>();
        value.forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
        return result;
    }
}
