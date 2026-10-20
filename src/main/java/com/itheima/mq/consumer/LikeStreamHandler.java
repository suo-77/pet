package com.itheima.mq.consumer;

import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.entity.PostLike;
import com.itheima.mapper.CommunityPostMapper;
import com.itheima.mapper.PostLikeMapper;
import com.itheima.mq.constant.MQConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 帖子点赞消费者：异步把点赞关系持久化写入 MySQL，缓解数据库压力
 * payload: {postId, userId, action} action=like 点赞 / unlike 取消
 * 幂等设计：
 * 1. 点赞关系按唯一键 UPSERT（新增冲突即恢复 deleted 标记），重复消费无副作用
 * 2. 回写计数 = Redis 点赞 Set 的大小（SCARD），最终一致；依赖 Redis 持久化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeStreamHandler implements StreamBizHandler {

    private static final String LIKE_ACTION = "like";

    private final PostLikeMapper likeMapper;
    private final CommunityPostMapper postMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String streamKey() {
        return MQConstant.LIKE_STREAM;
    }

    @Override
    public String group() {
        return MQConstant.LIKE_GROUP;
    }

    @Override
    public void handle(Map<String, String> payload) {
        long postId = Long.parseLong(payload.get("postId"));
        long userId = Long.parseLong(payload.get("userId"));
        String action = payload.getOrDefault("action", LIKE_ACTION);

        // UPSERT 点赞关系
        PostLike like = likeMapper.findByPair(postId, userId);
        if (like == null) {
            PostLike insert = new PostLike();
            insert.setPostId(postId);
            insert.setUserId(userId);
            try {
                likeMapper.insert(insert);
            } catch (DuplicateKeyException e) {
                // 并发插入冲突：读取已有记录继续处理
                like = likeMapper.findByPair(postId, userId);
            }
        }
        if (like != null) {
            int targetDeleted = LIKE_ACTION.equals(action) ? 0 : 1;
            if (like.getDeleted() != targetDeleted) {
                likeMapper.updateDeleted(like.getId(), targetDeleted);
            }
        }
        // 回写帖子点赞数 = Redis Set 大小（最终一致性）
        Long count = stringRedisTemplate.opsForSet().size(RedisKeyConstant.POST_LIKE_USER + postId);
        postMapper.updateLikeCount(postId, count == null ? 0 : count);
        log.info("点赞关系落库完成: postId={}, userId={}, action={}, likeCount={}",
                postId, userId, action, count);
    }
}
