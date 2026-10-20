package com.itheima.service.impl;

import cn.hutool.core.util.StrUtil;
import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.PageResult;
import com.itheima.common.result.ResultCode;
import com.itheima.common.util.ImageJsonUtil;
import com.itheima.common.util.SensitiveWordUtil;
import com.itheima.common.util.XssUtil;
import com.itheima.dto.PostCommentDTO;
import com.itheima.dto.PostCreateDTO;
import com.itheima.entity.CommunityPost;
import com.itheima.entity.PostComment;
import com.itheima.entity.User;
import com.itheima.mapper.CommunityPostMapper;
import com.itheima.mapper.PostCommentMapper;
import com.itheima.mapper.PostLikeMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.mq.constant.MQConstant;
import com.itheima.mq.stream.MQProducer;
import com.itheima.service.PostService;
import com.itheima.vo.PostCommentVO;
import com.itheima.vo.PostLikeUserVO;
import com.itheima.vo.PostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 社区帖子服务实现：
 * 1. 点赞：先操作 Redis（Set 记录点赞人 + String 计数），再投递 Stream 异步落库 MySQL，
 *    接口高频读写不触库；重复点赞/重复取消幂等处理，不重复发消息
 * 2. 内容安全：文字统一 XSS 清洗 + 敏感词拦截；图片URL限制本服务上传路径
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String LIKE_ACTION = "like";
    private static final String UNLIKE_ACTION = "unlike";

    private final CommunityPostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MQProducer mqProducer;
    private final SensitiveWordUtil sensitiveWordUtil;

    @Override
    public Long create(Long userId, PostCreateDTO dto) {
        String content = StrUtil.trimToEmpty(dto.getContent());
        List<String> images = dto.getImages() == null ? List.of() : dto.getImages();
        // 文字与图片至少其一
        if (content.isEmpty() && images.isEmpty()) {
            throw new BusinessException("发布内容不能为空");
        }
        // 敏感词拦截 + XSS 清洗
        if (sensitiveWordUtil.containsSensitive(content)) {
            throw new BusinessException("内容不合法，请修改后重新发布");
        }
        content = XssUtil.clean(content);
        // 图片URL必须是本服务上传路径，防外链注入
        for (String img : images) {
            if (StrUtil.isBlank(img) || !img.startsWith("/images/")) {
                throw new BusinessException("图片参数不合法");
            }
        }
        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setContent(content);
        post.setImages(ImageJsonUtil.toJson(images));
        postMapper.insert(post);
        log.info("发布帖子成功: postId={}, userId={}, 图片数={}", post.getId(), userId, images.size());
        return post.getId();
    }

    @Override
    public PageResult<PostVO> pageFeed(Long viewerId, Long userId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        // userId 非空时查询指定用户动态（他人主页），否则查询全站信息流
        List<CommunityPost> posts;
        long total;
        if (userId != null) {
            posts = postMapper.selectMyPage(userId, offset, size);
            total = postMapper.countMy(userId);
        } else {
            posts = postMapper.selectFeedPage(offset, size);
            total = postMapper.countFeed();
        }
        List<PostVO> list = new ArrayList<>();
        for (CommunityPost post : posts) {
            PostVO vo = PostVO.from(post, ImageJsonUtil.parse(post.getImages()));
            vo.setLiked(isLiked(viewerId, post.getId()));
            list.add(vo);
        }
        return PageResult.of(total, list);
    }

    @Override
    public PageResult<PostVO> myPosts(Long userId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        List<CommunityPost> posts = postMapper.selectMyPage(userId, offset, size);
        List<PostVO> list = new ArrayList<>();
        for (CommunityPost post : posts) {
            PostVO vo = PostVO.from(post, ImageJsonUtil.parse(post.getImages()));
            vo.setLiked(isLiked(userId, post.getId()));
            list.add(vo);
        }
        return PageResult.of(postMapper.countMy(userId), list);
    }

    @Override
    public void like(Long userId, Long postId) {
        checkPostExists(postId);
        String userKey = RedisKeyConstant.POST_LIKE_USER + postId;
        // Set 新增返回 1 表示首次点赞；重复点赞幂等返回
        Long added = stringRedisTemplate.opsForSet().add(userKey, String.valueOf(userId));
        if (added != null && added > 0) {
            stringRedisTemplate.opsForValue().increment(RedisKeyConstant.POST_LIKE_COUNT + postId);
            mqProducer.send(MQConstant.LIKE_STREAM,
                    Map.of("postId", String.valueOf(postId), "userId", String.valueOf(userId),
                            "action", LIKE_ACTION));
        }
    }

    @Override
    public void unlike(Long userId, Long postId) {
        checkPostExists(postId);
        String userKey = RedisKeyConstant.POST_LIKE_USER + postId;
        // Set 移除返回 1 表示本次有效取消；未点赞幂等返回
        Long removed = stringRedisTemplate.opsForSet().remove(userKey, String.valueOf(userId));
        if (removed != null && removed > 0) {
            Long count = stringRedisTemplate.opsForValue().decrement(RedisKeyConstant.POST_LIKE_COUNT + postId);
            // 计数兜底不为负
            if (count != null && count < 0) {
                stringRedisTemplate.opsForValue().set(RedisKeyConstant.POST_LIKE_COUNT + postId, "0");
            }
            mqProducer.send(MQConstant.LIKE_STREAM,
                    Map.of("postId", String.valueOf(postId), "userId", String.valueOf(userId),
                            "action", UNLIKE_ACTION));
        }
    }

    @Override
    public List<PostLikeUserVO> likeUsers(Long postId) {
        checkPostExists(postId);
        // 优先 Redis Set（实时，取消点赞立即生效）
        Set<String> members = stringRedisTemplate.opsForSet()
                .members(RedisKeyConstant.POST_LIKE_USER + postId);
        if (members != null && !members.isEmpty()) {
            List<Long> userIds = members.stream().map(Long::valueOf).toList();
            Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            return userIds.stream()
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .map(u -> {
                        PostLikeUserVO vo = new PostLikeUserVO();
                        vo.setUserId(u.getId());
                        vo.setNickname(u.getNickname());
                        vo.setAvatar(u.getAvatar());
                        return vo;
                    })
                    .toList();
        }
        // Redis 无数据（被清理/重启丢失）时回退 DB 落库记录
        return postLikeMapper.selectByPost(postId).stream()
                .map(pl -> {
                    User u = userMapper.findById(pl.getUserId());
                    if (u == null) {
                        return null;
                    }
                    PostLikeUserVO vo = new PostLikeUserVO();
                    vo.setUserId(u.getId());
                    vo.setNickname(u.getNickname());
                    vo.setAvatar(u.getAvatar());
                    vo.setCreateTime(pl.getCreateTime());
                    return vo;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void comment(Long userId, Long postId, PostCommentDTO dto) {
        checkPostExists(postId);
        if (sensitiveWordUtil.containsSensitive(dto.getContent())) {
            throw new BusinessException("内容不合法，请修改后重新评论");
        }
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        comment.setContent(XssUtil.clean(dto.getContent()));
        commentMapper.insert(comment);
        // 帖子评论计数 +1
        postMapper.incrCommentCount(postId);
    }

    @Override
    public PageResult<PostCommentVO> comments(Long postId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        List<PostComment> comments = commentMapper.selectPageByPost(postId, offset, size);
        List<PostCommentVO> list = comments.stream().map(PostCommentVO::from).toList();
        return PageResult.of(commentMapper.countByPost(postId), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long postId) {
        CommunityPost post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已删除");
        }
        // 权限校验：仅作者本人可删
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己发布的帖子");
        }
        postMapper.deleteById(postId);
        // 清理该帖 Redis 点赞数据
        stringRedisTemplate.delete(RedisKeyConstant.POST_LIKE_USER + postId);
        stringRedisTemplate.delete(RedisKeyConstant.POST_LIKE_COUNT + postId);
        log.info("删除帖子: postId={}, userId={}", postId, userId);
    }

    /** 当前用户是否已点赞（未登录返回 false） */
    private boolean isLiked(Long viewerId, Long postId) {
        if (viewerId == null) {
            return false;
        }
        Boolean member = stringRedisTemplate.opsForSet()
                .isMember(RedisKeyConstant.POST_LIKE_USER + postId, String.valueOf(viewerId));
        return Boolean.TRUE.equals(member);
    }

    /** 帖子存在性校验 */
    private void checkPostExists(Long postId) {
        if (postMapper.findById(postId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已删除");
        }
    }
}
