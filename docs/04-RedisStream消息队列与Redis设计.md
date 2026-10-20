# 04 Redis Stream 消息队列与 Redis 设计

> 变更说明：按用户要求**不再使用 RabbitMQ**，消息队列改用 **Redis Stream** 实现，部署在虚拟机 Redis（192.168.100.128 / db1）。

## 一、为什么 Redis Stream 能承担 MQ 职责
| 能力 | Redis Stream 实现 |
|------|------------------|
| 异步解耦 | 业务接口 XADD 投递消息后立即返回，消费者异步处理 |
| 流量削峰 | 秒杀/下单请求先写 Stream，消费者按自身节奏处理，保护 MySQL |
| 至少一次投递 | 消费组（XGROUP/XREADGROUP）+ 手动 ACK（XACK） |
| 消息不丢失 | PEL 待确认列表保存未 ACK 消息；Redis 开启 RDB/AOF 持久化落盘 |
| 消息重试 | 调度器定时扫描 PEL，XCLAIM 抢回空闲超时消息重新处理 |
| 避免重复消费 | 业务幂等：唯一键 Upsert（点赞）、order_no 唯一索引（订单）、messageId 去重（私信） |
| 死信处理 | 重试超限（3 次）→ XACK 移出 PEL → 写入死信流 `{streamKey}:dead` 记录负载供排查补偿 |
| 消息积压 | Stream 天然可积压；可临时扩消费者（多消费者同组竞争），消费后用 XDEL/XTRIM 清理已处理消息 |

## 二、Stream 键 / 消费组 / 死信流规划（全部在 db1）

| 业务 | Stream 键 | 消费组 | 死信流 |
|------|-----------|--------|--------|
| 短信验证码 | `mq:stream:sms` | `sms-group` | `mq:stream:sms:dead` |
| 帖子点赞 | `mq:stream:like` | `like-group` | `mq:stream:like:dead` |
| 订单创建 | `mq:stream:order` | `order-group` | `mq:stream:order:dead` |
| 私信通知 | `mq:stream:chat` | `chat-group` | `mq:stream:chat:dead` |
| 图片压缩 | `mq:stream:img` | `img-group` | `mq:stream:img:dead` |

## 三、六大业务场景映射

| 场景 | 生产者 | 消费者动作 | 削峰/异步收益 |
|------|--------|-----------|--------------|
| 短信验证码 | 获取验证码接口（先存 Redis 验证码再投递） | 异步模拟发送短信 | 主接口不阻塞 |
| 帖子点赞 | 点赞接口（先操作 Redis 计数/集合再投递） | 异步 UPSERT 点赞关系到 MySQL | 点赞 QPS 高防打库 |
| 订单创建 | 下单/秒杀接口（Redis 预扣库存后投递） | 异步建单 + 库存最终一致性校验 | 秒杀削峰 |
| 私信通知 | WebSocket 发送消息时投递 | 异步未读通知处理 | 推送与通知解耦 |
| 图片压缩 | 统一上传接口保存原图后投递 | 异步生成压缩图 | 上传快速返回 |
| 死信兜底 | 重试调度器 | 死信流保留原始负载 | 消息不丢失可补偿 |

## 四、可靠性保障机制（代码实现对应）
1. **手动 ACK**：`RedisStreamConfig` 使用 `container.receive(...)`（非自动 ACK），处理成功才 `XACK`
2. **PEL 重试**：`StreamRetryScheduler` 每 30s 扫描 PEL，空闲超 60s 未 ACK 用 `XCLAIM` 抢回重试
3. **最大重试 3 次**：投递次数 ≥3 仍失败 → 死信流 + 告警日志
4. **幂等**：点赞 UPSERT、订单 order_no 唯一索引、私信 messageId 去重
5. **初始化消息**：空流先 XADD `{"init":"1"}` 建流再建组，消费者识别并跳过
6. **落盘持久化**：虚拟机 Redis 需开启 RDB 或 AOF（建议 AOF everysec），见 README 部署说明
7. **多实例注意**：消费者名当前固定 `consumer-1`，多实例部署需改为随机/主机名

## 五、Redis 键设计总表（db1）
| Key | 类型 | 说明 |
|-----|------|------|
| `login:token:{token}` | String | 存 userId，TTL 与 token 有效期一致 |
| `sms:code:{phone}` | String | 短信验证码，TTL 5min |
| `sms:limit:{phone}` | String | 频率限制计数器，TTL 60s |
| `post:like:count:{postId}` | String | 帖子点赞计数 |
| `post:like:user:{postId}` | Set | 已点赞用户集合 |
| `seckill:stock:{couponId}` | String | 秒杀预扣库存 |
| `seckill:user:{couponId}` | Set | 秒杀限领去重 |
| `cart:{userId}` | Hash | 购物车 {goodsId: num} |
| `follow:following:{userId}` | Set | 我关注的用户 |
| `follow:fan:{userId}` | Set | 我的粉丝 |
| `goods:fav:{userId}` | Set | 商品收藏 |
| `mq:stream:*` | Stream | 五大业务消息流（见上表） |
| `mq:stream:*:dead` | Stream | 死信流 |

## 六、配置文件关键片段（application-dev.yml）
```yaml
spring:
  data:
    redis:
      host: 192.168.100.128   # 虚拟机 Redis，禁止写 localhost
      port: 6379
      password: "按需填写"
      database: 1             # 使用 db1
```
> 已移除 spring-boot-starter-amqp 依赖与 RabbitMQ 全部配置。

## 七、Dev/Prod 环境
- dev（application-dev.yml）：MySQL 本机 root/123456；Redis 虚拟机 192.168.100.128 db1；SQL 日志 DEBUG
- prod（application-prod.yml）：连接信息走环境变量注入；日志收敛为 WARN/INFO
