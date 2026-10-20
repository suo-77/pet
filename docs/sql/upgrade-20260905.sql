-- =============================================================
-- 升级脚本 2026-09-05：优惠券使用 / 取消订单 / 店主开店
-- 说明：若某列已存在会报 Duplicate column，跳过对应语句继续执行即可
-- =============================================================
USE pet_community;

-- 1. 订单表：优惠抵扣金额（orders.coupon_id 已存在）
ALTER TABLE `orders`
    ADD COLUMN `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠抵扣金额' AFTER `total_amount`;

-- 2. 店铺表：店主用户ID（每个用户可开自己的小店，唯一索引保证一人一店）
ALTER TABLE `shop`
    ADD COLUMN `owner_id` BIGINT DEFAULT NULL COMMENT '店主用户ID（开店人）' AFTER `shop_name`,
    ADD UNIQUE INDEX `uk_owner` (`owner_id`);

-- 3. 旧演示店铺绑定店主（使店主可编辑自己的店铺；一人一店，miu 未绑定可演示开通）
UPDATE `shop` SET `owner_id` = 1 WHERE `id` = 1 AND `owner_id` IS NULL;
UPDATE `shop` SET `owner_id` = 2 WHERE `id` = 2 AND `owner_id` IS NULL;
UPDATE `shop` SET `owner_id` = 3 WHERE `id` = 3 AND `owner_id` IS NULL;

-- 4. 私信分享动态：聊天消息增加 post_id（分享消息类型 msg_type=2 时使用）
ALTER TABLE `chat_message`
    ADD COLUMN `post_id` BIGINT DEFAULT NULL COMMENT '分享的帖子ID(分享消息时非空)' AFTER `image_url`;
UPDATE `chat_message` SET `msg_type` = 0 WHERE `msg_type` >= 2;
