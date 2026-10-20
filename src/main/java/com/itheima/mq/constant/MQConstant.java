package com.itheima.mq.constant;

/**
 * Redis Stream 消息队列常量（替代 RabbitMQ，部署在虚拟机 Redis db1）
 * 每个业务一条 Stream + 一个消费组；消费失败的消息留在 PEL(待确认列表)，
 * 由重试调度器 XCLAIM 抢回重试，超过最大重试次数写入死信流：{streamKey}:dead
 */
public final class MQConstant {

    private MQConstant() {
    }

    /** 短信验证码 */
    public static final String SMS_STREAM = "mq:stream:sms";
    public static final String SMS_GROUP = "sms-group";

    /** 帖子点赞 */
    public static final String LIKE_STREAM = "mq:stream:like";
    public static final String LIKE_GROUP = "like-group";

    /** 订单创建（普通下单 + 秒杀下单） */
    public static final String ORDER_STREAM = "mq:stream:order";
    public static final String ORDER_GROUP = "order-group";

    /** 私信通知（未读消息异步处理） */
    public static final String CHAT_STREAM = "mq:stream:chat";
    public static final String CHAT_GROUP = "chat-group";

    /** 图片压缩 */
    public static final String IMG_STREAM = "mq:stream:img";
    public static final String IMG_GROUP = "img-group";

    /** 死信流后缀：完整键 = streamKey + DEAD_SUFFIX */
    public static final String DEAD_SUFFIX = ":dead";

    /** 最大重试次数：超过后进入死信流 */
    public static final int MAX_RETRY = 3;

    /** 消息投递后空闲超过该时长视为消费失败，可被重试调度器抢回 */
    public static final long CLAIM_MIN_IDLE_MS = 60_000;

    /** 重试调度器使用的消费者名 */
    public static final String RETRY_CONSUMER = "retry-consumer";
}
