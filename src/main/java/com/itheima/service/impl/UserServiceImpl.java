package com.itheima.service.impl;

import cn.hutool.core.util.StrUtil;
import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.PageResult;
import com.itheima.common.util.XssUtil;
import com.itheima.dto.UpdateProfileDTO;
import com.itheima.entity.Follow;
import com.itheima.entity.User;
import com.itheima.mapper.FollowMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.service.UserService;
import com.itheima.vo.UserCardVO;
import com.itheima.vo.UserHomeVO;
import com.itheima.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现：
 * 1. 关注关系双写：MySQL 持久化（真相源）+ Redis Set 缓存（快速判断是否已关注）
 * 2. 是否已关注优先查 Redis，未命中回查 MySQL 并回填，防 Redis 数据丢失后误判
 * 3. 计数直接查 MySQL（有索引，轻量），避免双源不一致
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 分页上限保护 */
    private static final int MAX_PAGE_SIZE = 50;

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public UserVO updateProfile(Long userId, UpdateProfileDTO dto) {
        // 昵称、简介做 XSS 清洗后入库
        userMapper.updateProfile(userId,
                XssUtil.clean(dto.getNickname()),
                XssUtil.clean(dto.getIntro()),
                dto.getAvatar());
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserVO.from(user);
    }

    @Override
    public UserHomeVO home(Long viewerId, Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserHomeVO vo = UserHomeVO.from(user);
        vo.setFollowingCount(followMapper.countFollowing(userId));
        vo.setFanCount(followMapper.countFan(userId));
        vo.setFollowed(isFollowing(viewerId, userId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long userId, Long targetId) {
        if (userId.equals(targetId)) {
            throw new BusinessException("不能关注自己");
        }
        checkUserExists(targetId);
        Follow follow = followMapper.findByPair(userId, targetId);
        if (follow == null) {
            // 首次关注：并发下唯一键冲突则视为已存在
            Follow insert = new Follow();
            insert.setUserId(userId);
            insert.setFollowUserId(targetId);
            try {
                followMapper.insert(insert);
            } catch (DuplicateKeyException e) {
                follow = followMapper.findByPair(userId, targetId);
            }
        }
        if (follow != null) {
            if (follow.getDeleted() == 0) {
                throw new BusinessException("已关注该用户");
            }
            // 曾经取关过，恢复关注关系
            followMapper.updateDeleted(follow.getId(), 0);
        }
        // 同步维护 Redis 缓存集合
        stringRedisTemplate.opsForSet()
                .add(RedisKeyConstant.FOLLOW_FOLLOWING + userId, String.valueOf(targetId));
        stringRedisTemplate.opsForSet()
                .add(RedisKeyConstant.FOLLOW_FAN + targetId, String.valueOf(userId));
        log.info("关注成功: userId={}, targetId={}", userId, targetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long userId, Long targetId) {
        if (userId.equals(targetId)) {
            throw new BusinessException("不能取关自己");
        }
        Follow follow = followMapper.findByPair(userId, targetId);
        if (follow == null || follow.getDeleted() == 1) {
            throw new BusinessException("尚未关注该用户");
        }
        followMapper.updateDeleted(follow.getId(), 1);
        // 同步清理 Redis 缓存集合
        stringRedisTemplate.opsForSet()
                .remove(RedisKeyConstant.FOLLOW_FOLLOWING + userId, String.valueOf(targetId));
        stringRedisTemplate.opsForSet()
                .remove(RedisKeyConstant.FOLLOW_FAN + targetId, String.valueOf(userId));
        log.info("取消关注: userId={}, targetId={}", userId, targetId);
    }

    @Override
    public boolean isFollowing(Long viewerId, Long targetId) {
        if (viewerId == null) {
            return false;
        }
        String key = RedisKeyConstant.FOLLOW_FOLLOWING + viewerId;
        Boolean cached = stringRedisTemplate.opsForSet().isMember(key, String.valueOf(targetId));
        if (Boolean.TRUE.equals(cached)) {
            return true;
        }
        // Redis 未命中：回查 MySQL 并回填缓存（防 Redis 重启丢数据）
        Follow follow = followMapper.findByPair(viewerId, targetId);
        if (follow != null && follow.getDeleted() == 0) {
            stringRedisTemplate.opsForSet().add(key, String.valueOf(targetId));
            stringRedisTemplate.opsForSet()
                    .add(RedisKeyConstant.FOLLOW_FAN + targetId, String.valueOf(viewerId));
            return true;
        }
        return false;
    }

    @Override
    public PageResult<UserCardVO> followingList(Long userId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        List<UserCardVO> list = followMapper.selectFollowingPage(userId, offset, size);
        long total = followMapper.countFollowing(userId);
        return PageResult.of(total, list);
    }

    @Override
    public PageResult<UserCardVO> fanList(Long userId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        List<UserCardVO> list = followMapper.selectFanPage(userId, offset, size);
        long total = followMapper.countFan(userId);
        return PageResult.of(total, list);
    }

    @Override
    public PageResult<UserCardVO> recommendList(Long userId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        List<UserCardVO> list = followMapper.selectRecommendPage(userId, offset, size);
        long total = followMapper.countRecommend(userId);
        return PageResult.of(total, list);
    }

    /** 目标用户存在性校验 */
    private void checkUserExists(Long userId) {
        if (userMapper.findById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
    }
}
