package com.itheima.config;

import com.itheima.mq.consumer.StreamBizHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.List;

/**
 * Redis Stream 消息队列配置（单独配置类）：
 * 1. 启动时为每个业务流创建消费组（流不存在时先写入初始化消息创建流）
 * 2. 注册消费者监听（XREADGROUP），处理成功手动 ACK；
 *    处理失败不 ACK，消息保留在 PEL 待确认列表，由 StreamRetryScheduler 定时抢回重试
 * 3. 消息可靠性：消费组至少一次投递 + 手动 ACK + PEL 重试 + 死信流兜底；
 *    落盘可靠性依赖虚拟机 Redis 的 RDB/AOF 持久化配置
 */
@Slf4j
@Configuration
@EnableScheduling
public class RedisStreamConfig {

    /** 当前实例消费者名：单实例部署固定；多实例部署需改为 随机值/主机名 */
    private static final String CONSUMER_NAME = "consumer-1";

    private final StringRedisTemplate stringRedisTemplate;
    private final List<StreamBizHandler> handlers;

    public RedisStreamConfig(StringRedisTemplate stringRedisTemplate, List<StreamBizHandler> handlers) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.handlers = handlers;
    }

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer(
            RedisConnectionFactory connectionFactory) {
        // 容器选项：阻塞轮询 2 秒，每次最多拉取 10 条
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .batchSize(10)
                        .build();
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        // 每个业务：创建消费组 + 注册监听（手动 ACK）
        for (StreamBizHandler handler : handlers) {
            createGroup(handler.streamKey(), handler.group());
            container.receive(
                    Consumer.from(handler.group(), CONSUMER_NAME),
                    StreamOffset.create(handler.streamKey(), ReadOffset.lastConsumed()),
                    record -> processMessage(handler, record));
        }
        container.start();
        return container;
    }

    /**
     * 创建消费组；流不存在时先写入初始化消息把流建出来再创建（组从 0 开始读）
     */
    private void createGroup(String streamKey, String group) {
        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), group);
            log.info("创建Stream消费组成功: stream={}, group={}", streamKey, group);
        } catch (Exception e) {
            // BUSYGROUP：组已存在，正常场景
            log.info("创建Stream消费组跳过: stream={}, group={}, 原因: {}", streamKey, group, e.getMessage());
        }
    }

    /**
     * 消费处理：成功手动 ACK；失败不 ACK，留在 PEL 由重试调度器接管
     */
    private void processMessage(StreamBizHandler handler, MapRecord<String, String, String> record) {
        try {
            // 初始化消息（仅用于创建空流）直接 ACK 跳过
            if ("1".equals(record.getValue().get("init"))) {
                ack(handler, record.getId().getValue());
                return;
            }
            log.info("收到Stream消息: stream={}, id={}, payload={}", handler.streamKey(), record.getId(), record.getValue());
            handler.handle(record.getValue());
            ack(handler, record.getId().getValue());
            log.info("消息处理完成并ACK: stream={}, id={}", handler.streamKey(), record.getId());
        } catch (Exception e) {
            log.warn("消息处理失败，保留PEL等待重试: stream={}, id={}, 原因: {}",
                    handler.streamKey(), record.getId(), e.getMessage());
        }
    }

    /** 手动 ACK */
    private void ack(StreamBizHandler handler, String recordId) {
        stringRedisTemplate.opsForStream().acknowledge(handler.streamKey(), handler.group(), recordId);
    }
}
