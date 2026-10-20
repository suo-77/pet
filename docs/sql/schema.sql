-- =============================================================
-- 宠物购买社交社区全栈系统 - 数据库初始化脚本
-- 字符集 utf8mb4 / 排序规则 utf8mb4_general_ci / 引擎 InnoDB
-- 执行方式：mysql -uroot -p < schema.sql
-- =============================================================
CREATE DATABASE IF NOT EXISTS pet_community DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE pet_community;

-- -------------------------------------------------------------
-- 1. user 用户表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '账号/登录名',
    `password`    VARCHAR(64)  NOT NULL COMMENT '密码(加盐MD5)',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `intro`       VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0普通用户 1管理员',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_nickname` (`nickname`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '用户表';

-- -------------------------------------------------------------
-- 2. community_post 社区帖子表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
    `user_id`       BIGINT   NOT NULL COMMENT '作者ID',
    `content`       TEXT COMMENT '文字内容',
    `images`        TEXT COMMENT '图片URL数组(JSON数组字符串)',
    `like_count`    INT      NOT NULL DEFAULT 0 COMMENT '点赞数(Redis计数异步同步)',
    `comment_count` INT      NOT NULL DEFAULT 0 COMMENT '评论数',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '社区帖子表';

-- -------------------------------------------------------------
-- 3. post_like 帖子点赞关系表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `post_like`;
CREATE TABLE `post_like` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id`     BIGINT   NOT NULL COMMENT '帖子ID',
    `user_id`     BIGINT   NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0点赞中 1已取消',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
    CONSTRAINT `fk_like_post` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
    CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '帖子点赞关系表';

-- -------------------------------------------------------------
-- 4. post_comment 帖子评论表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `post_comment`;
CREATE TABLE `post_comment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `post_id`     BIGINT       NOT NULL COMMENT '帖子ID',
    `user_id`     BIGINT       NOT NULL COMMENT '评论用户ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父评论ID，0为一级评论',
    `content`     VARCHAR(500) NOT NULL COMMENT '评论内容(XSS/敏感词过滤后)',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_post_create` (`post_id`, `create_time`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '帖子评论表';

-- -------------------------------------------------------------
-- 5. shop 店铺表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `shop`;
CREATE TABLE `shop` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
    `shop_name`   VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `owner_id`    BIGINT       DEFAULT NULL COMMENT '店主用户ID（开店人）',
    `shop_desc`   VARCHAR(500) DEFAULT NULL COMMENT '店铺简介',
    `cover`       VARCHAR(255) DEFAULT NULL COMMENT '店铺封面URL',
    `logo`        VARCHAR(255) DEFAULT NULL COMMENT '店铺logoURL',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_shop_name` (`shop_name`),
    UNIQUE KEY `uk_owner` (`owner_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '店铺表';

-- -------------------------------------------------------------
-- 6. pet_goods 宠物商品表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `pet_goods`;
CREATE TABLE `pet_goods` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `shop_id`     BIGINT        NOT NULL COMMENT '所属店铺ID',
    `pet_name`    VARCHAR(100)  NOT NULL COMMENT '宠物名称',
    `breed`       VARCHAR(50)   DEFAULT NULL COMMENT '品种',
    `age`         VARCHAR(20)   DEFAULT NULL COMMENT '年龄(如3个月)',
    `price`       DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格',
    `description` TEXT COMMENT '宠物描述',
    `images`      TEXT COMMENT '多张图片URL(JSON数组字符串)',
    `stock`       INT           NOT NULL DEFAULT 0 COMMENT '库存',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0上架 1下架',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_pet_name` (`pet_name`),
    CONSTRAINT `fk_goods_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '宠物商品表';

-- -------------------------------------------------------------
-- 7. goods_evaluate 商品评价表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `goods_evaluate`;
CREATE TABLE `goods_evaluate` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `goods_id`    BIGINT       NOT NULL COMMENT '商品ID',
    `user_id`     BIGINT       NOT NULL COMMENT '评价用户ID',
    `content`     VARCHAR(500) NOT NULL COMMENT '评价内容',
    `images`      TEXT COMMENT '评价图片URL(JSON数组字符串)',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_goods_create` (`goods_id`, `create_time`),
    CONSTRAINT `fk_eval_goods` FOREIGN KEY (`goods_id`) REFERENCES `pet_goods` (`id`),
    CONSTRAINT `fk_eval_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '商品评价表';

-- -------------------------------------------------------------
-- 8. coupon 优惠券&秒杀券表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    `shop_id`      BIGINT        NOT NULL COMMENT '所属店铺ID',
    `coupon_name`  VARCHAR(100)  NOT NULL COMMENT '券名称',
    `category`     TINYINT       NOT NULL DEFAULT 0 COMMENT '类型：0普通券 1秒杀券',
    `money`        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '面额',
    `total_stock`  INT           NOT NULL DEFAULT 0 COMMENT '总库存',
    `remain_stock` INT           NOT NULL DEFAULT 0 COMMENT '剩余库存',
    `per_limit`    INT           NOT NULL DEFAULT 1 COMMENT '每人领取上限',
    `start_time`   DATETIME      DEFAULT NULL COMMENT '开始时间(秒杀倒计时起点)',
    `expire_time`  DATETIME      NOT NULL COMMENT '过期时间',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_category_expire` (`category`, `expire_time`),
    CONSTRAINT `fk_coupon_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '优惠券&秒杀券表';

-- -------------------------------------------------------------
-- 9. user_coupon 用户领取优惠券中间表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT   NOT NULL COMMENT '用户ID',
    `coupon_id`    BIGINT   NOT NULL COMMENT '优惠券ID',
    `status`       TINYINT  NOT NULL DEFAULT 0 COMMENT '使用状态：0未使用 1已使用 2已过期',
    `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`      TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_uc_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_uc_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '用户领取优惠券中间表';

-- -------------------------------------------------------------
-- 10. follow 用户关注关系表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT   NOT NULL COMMENT '关注者ID',
    `follow_user_id` BIGINT   NOT NULL COMMENT '被关注者ID',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0关注中 1已取关',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`),
    KEY `idx_follow_user` (`follow_user_id`),
    CONSTRAINT `fk_follow_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_follow_target` FOREIGN KEY (`follow_user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '用户关注关系表';

-- -------------------------------------------------------------
-- 11. chat_session 聊天会话表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_a_id`    BIGINT       NOT NULL COMMENT '会话方A(两者中较小的用户ID)',
    `user_b_id`    BIGINT       NOT NULL COMMENT '会话方B(两者中较大的用户ID)',
    `last_message` VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息预览',
    `last_time`    DATETIME     DEFAULT NULL COMMENT '最后消息时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_pair` (`user_a_id`, `user_b_id`),
    KEY `idx_last_time` (`last_time`),
    CONSTRAINT `fk_session_a` FOREIGN KEY (`user_a_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_session_b` FOREIGN KEY (`user_b_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '聊天会话表';

-- -------------------------------------------------------------
-- 12. chat_message 聊天消息表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id`   BIGINT       NOT NULL COMMENT '会话ID',
    `from_user_id` BIGINT       NOT NULL COMMENT '发送者ID',
    `to_user_id`   BIGINT       NOT NULL COMMENT '接收者ID',
    `content`      TEXT COMMENT '文本内容(文本消息时非空)',
    `image_url`    VARCHAR(255) DEFAULT NULL COMMENT '图片URL(图片消息时非空)',
    `post_id`      BIGINT       DEFAULT NULL COMMENT '分享的帖子ID(分享消息时非空)',
    `msg_type`     TINYINT      NOT NULL DEFAULT 0 COMMENT '消息类型：0文本 1图片 2分享动态',
    `is_read`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `send_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_session_send` (`session_id`, `send_time`),
    KEY `idx_to_read` (`to_user_id`, `is_read`),
    CONSTRAINT `fk_msg_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '聊天消息表';

-- -------------------------------------------------------------
-- 13. cart 购物车表（主数据在 Redis：cart:{userId}，本表做落库兜底）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `goods_id`    BIGINT   NOT NULL COMMENT '商品ID',
    `num`         INT      NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_cart_goods` FOREIGN KEY (`goods_id`) REFERENCES `pet_goods` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '购物车表';

-- -------------------------------------------------------------
-- 14. orders 模拟订单表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`     VARCHAR(64)   NOT NULL COMMENT '订单号(时间戳+随机数)',
    `user_id`      BIGINT        NOT NULL COMMENT '下单用户ID',
    `goods_id`     BIGINT        NOT NULL COMMENT '商品ID',
    `coupon_id`    BIGINT        DEFAULT NULL COMMENT '使用的优惠券ID',
    `goods_num`    INT           NOT NULL DEFAULT 1 COMMENT '商品数量',
    `goods_price`  DECIMAL(10,2) NOT NULL COMMENT '下单时商品单价快照',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '实付总金额(已扣优惠)',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠抵扣金额',
    `status`       TINYINT       NOT NULL DEFAULT 0 COMMENT '订单状态：0待付款 1已付款 2已取消',
    `order_type`   TINYINT       NOT NULL DEFAULT 0 COMMENT '订单类型：0普通 1秒杀',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_order_goods` FOREIGN KEY (`goods_id`) REFERENCES `pet_goods` (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '模拟订单表';
