package com.itheima.mq.consumer;

import java.util.Map;

/**
 * Redis Stream 业务处理器接口：
 * 每个业务实现该接口，由 RedisStreamConfig 统一注册监听（手动 ACK），
 * 由 StreamRetryScheduler 统一做 PEL 抢回重试与死信流转
 */
public interface StreamBizHandler {

    /** 监听的 Stream 键 */
    String streamKey();

    /** 消费组名 */
    String group();

    /**
     * 业务处理逻辑；抛出异常视为处理失败：
     * 消息不 ACK，保留在 PEL 中，由重试调度器接管
     */
    void handle(Map<String, String> payload) throws Exception;
}
