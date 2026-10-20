package com.itheima.mq.consumer;

import com.itheima.mq.constant.MQConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 短信验证码消费者：异步发送短信（模拟）
 * payload: {phone, code}
 * 生产者：/api/auth/sendSmsCode 接口先写 Redis 验证码，再投递本消息，主接口快速返回
 */
@Slf4j
@Component
public class SmsStreamHandler implements StreamBizHandler {

    @Override
    public String streamKey() {
        return MQConstant.SMS_STREAM;
    }

    @Override
    public String group() {
        return MQConstant.SMS_GROUP;
    }

    @Override
    public void handle(Map<String, String> payload) {
        String phone = payload.get("phone");
        String code = payload.get("code");
        // 演示项目：打印模拟短信发送；对接真实短信服务商时替换此处调用
        log.info("【模拟短信】发送验证码短信 -> 手机号: {}, 验证码: {}", phone, code);
    }
}
