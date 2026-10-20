package com.itheima.common.constant;

/**
 * Redis Key 统一规划（全部使用 db1）
 * 命名规范：业务:含义:主键
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {
    }

    /** 登录 token：login:token:{token} -> userId，TTL 与 token 有效期一致 */
    public static final String LOGIN_TOKEN = "login:token:";

    /** 短信验证码：sms:code:{phone} -> 6位验证码，TTL 5分钟 */
    public static final String SMS_CODE = "sms:code:";

    /** 短信频率限制：sms:limit:{phone}，TTL 60秒，防频繁获取验证码 */
    public static final String SMS_LIMIT = "sms:limit:";

    /** 帖子点赞计数：post:like:count:{postId} -> 点赞数 */
    public static final String POST_LIKE_COUNT = "post:like:count:";

    /** 帖子点赞用户集合：post:like:user:{postId} -> Set<userId>，判断是否已点赞 */
    public static final String POST_LIKE_USER = "post:like:user:";

    /** 秒杀券预扣库存：seckill:stock:{couponId} -> 剩余库存 */
    public static final String SECKILL_STOCK = "seckill:stock:";

    /** 秒杀限领去重：seckill:user:{couponId} -> Set<userId> */
    public static final String SECKILL_USER = "seckill:user:";

    /** 购物车：cart:{userId} -> Hash<goodsId, num> */
    public static final String CART = "cart:";

    /** 我关注的用户集合：follow:following:{userId} -> Set<followUserId> */
    public static final String FOLLOW_FOLLOWING = "follow:following:";

    /** 我的粉丝集合：follow:fan:{userId} -> Set<fanUserId> */
    public static final String FOLLOW_FAN = "follow:fan:";

    /** 商品收藏：goods:fav:{userId} -> ZSet<goodsId, 收藏时间戳>，支持按时间倒序分页 */
    public static final String GOODS_FAV = "goods:fav:";
}
