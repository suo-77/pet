package com.itheima.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.PageResult;
import com.itheima.common.util.SensitiveWordUtil;
import com.itheima.common.util.XssUtil;
import com.itheima.entity.ChatMessage;
import com.itheima.entity.ChatSession;
import com.itheima.entity.CommunityPost;
import com.itheima.entity.User;
import com.itheima.mapper.ChatMessageMapper;
import com.itheima.mapper.ChatSessionMapper;
import com.itheima.mapper.CommunityPostMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.mq.constant.MQConstant;
import com.itheima.mq.stream.MQProducer;
import com.itheima.service.ChatService;
import com.itheima.vo.ChatMessageVO;
import com.itheima.vo.ChatSessionVO;
import com.itheima.websocket.WsSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 私信聊天服务实现：
 * 1. 发送：文本做敏感词/XSS/长度校验，图片校验本服务上传路径；消息落库（未读）+ 会话预览更新
 * 2. 推送：接收方在线实时 WS 推送；发送方同步推送回显；离线则仅落库（红点在会话列表体现）
 * 3. 通知：投递 chat Stream，消费者异步处理未读通知，不阻塞消息主链路
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MSG_TYPE_TEXT = 0;
    private static final int MSG_TYPE_IMAGE = 1;
    private static final int MSG_TYPE_SHARE = 2;
    private static final int MAX_PAGE_SIZE = 50;

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final CommunityPostMapper postMapper;
    private final WsSessionManager wsSessionManager;
    private final MQProducer mqProducer;
    private final SensitiveWordUtil sensitiveWordUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO send(Long fromUserId, Long toUserId, int msgType, String content, String imageUrl) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException("不能给自己发私信");
        }
        // 对方存在性校验
        User peer = userMapper.findById(toUserId);
        if (peer == null) {
            throw new BusinessException("对方用户不存在");
        }
        // 内容校验
        if (msgType == MSG_TYPE_TEXT) {
            if (StrUtil.isBlank(content)) {
                throw new BusinessException("消息内容不能为空");
            }
            if (content.length() > 500) {
                throw new BusinessException("消息最长500字");
            }
            if (sensitiveWordUtil.containsSensitive(content)) {
                throw new BusinessException("内容不合法，请修改后重新发送");
            }
            content = XssUtil.clean(content);
        } else if (msgType == MSG_TYPE_IMAGE) {
            if (StrUtil.isBlank(imageUrl) || !imageUrl.startsWith("/images/")) {
                throw new BusinessException("图片消息参数不合法");
            }
        } else {
            throw new BusinessException("不支持的消息类型");
        }
        // 获取或创建会话
        ChatSession session = getOrCreateSession(fromUserId, toUserId);
        // 消息落库（未读）
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(msgType == MSG_TYPE_TEXT ? content : null);
        message.setImageUrl(msgType == MSG_TYPE_IMAGE ? imageUrl : null);
        message.setMsgType(msgType);
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
        // 会话预览更新
        String preview = msgType == MSG_TYPE_IMAGE ? "[图片]" : StrUtil.maxLength(content, 100);
        sessionMapper.updateLast(session.getId(), preview, message.getSendTime());
        // 组装 VO
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(session.getId());
        vo.setFromUserId(fromUserId);
        vo.setToUserId(toUserId);
        vo.setContent(message.getContent());
        vo.setImageUrl(message.getImageUrl());
        vo.setMsgType(msgType);
        vo.setIsRead(0);
        vo.setSendTime(message.getSendTime());
        // WS 双向推送（事务提交前的本地推送可接受，消息已在DB）
        wsSessionManager.sendTo(fromUserId, pushBody("chat", vo));
        wsSessionManager.sendTo(toUserId, pushBody("chat", vo));
        // Stream 异步未读通知（不阻塞主链路）
        mqProducer.send(MQConstant.CHAT_STREAM,
                Map.of("fromUserId", String.valueOf(fromUserId),
                        "toUserId", String.valueOf(toUserId),
                        "messageId", String.valueOf(message.getId())));
        log.info("私信已发送: from={}, to={}, msgId={}, type={}", fromUserId, toUserId, message.getId(), msgType);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendShare(Long fromUserId, Long toUserId, Long postId) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException("不能给自己发私信");
        }
        User peer = userMapper.findById(toUserId);
        if (peer == null) {
            throw new BusinessException("对方用户不存在");
        }
        CommunityPost post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("分享的动态不存在或已删除");
        }
        // 会话预览与卡片内容
        String content = StrUtil.sub(post.getContent(), 0, 100);
        String firstImage = null;
        if (StrUtil.isNotBlank(post.getImages())) {
            try {
                firstImage = JSONUtil.parseArray(post.getImages()).getStr(0, null);
            } catch (Exception e) {
                log.warn("解析分享动态图片失败: postId={}", postId, e);
            }
        }
        // 获取或创建会话
        ChatSession session = getOrCreateSession(fromUserId, toUserId);
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setImageUrl(firstImage);
        message.setPostId(postId);
        message.setMsgType(MSG_TYPE_SHARE);
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
        sessionMapper.updateLast(session.getId(), "[分享动态]", message.getSendTime());
        // 组装 VO
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(session.getId());
        vo.setFromUserId(fromUserId);
        vo.setToUserId(toUserId);
        vo.setContent(message.getContent());
        vo.setImageUrl(message.getImageUrl());
        vo.setPostId(postId);
        vo.setMsgType(MSG_TYPE_SHARE);
        vo.setIsRead(0);
        vo.setSendTime(message.getSendTime());
        // WS 双向推送 + Stream 异步未读通知
        wsSessionManager.sendTo(fromUserId, pushBody("chat", vo));
        wsSessionManager.sendTo(toUserId, pushBody("chat", vo));
        mqProducer.send(MQConstant.CHAT_STREAM,
                Map.of("fromUserId", String.valueOf(fromUserId),
                        "toUserId", String.valueOf(toUserId),
                        "messageId", String.valueOf(message.getId())));
        log.info("动态已分享: from={}, to={}, postId={}, msgId={}", fromUserId, toUserId, postId, message.getId());
        return vo;
    }

    @Override
    public PageResult<ChatSessionVO> sessions(Long userId, int page, int pageSize) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        return PageResult.of(sessionMapper.countMySessions(userId),
                sessionMapper.selectMySessions(userId, offset, size));
    }

    @Override
    public PageResult<ChatMessageVO> messages(Long userId, Long peerUserId, int page, int pageSize) {
        // 会话不存在说明从未聊过，返回空页
        ChatSession session = findPairSession(userId, peerUserId);
        if (session == null) {
            return PageResult.of(0, List.of());
        }
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        long offset = (long) (page - 1) * size;
        return PageResult.of(messageMapper.countBySession(session.getId()),
                messageMapper.selectPageBySession(session.getId(), offset, size));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long peerUserId) {
        ChatSession session = findPairSession(userId, peerUserId);
        if (session == null) {
            return;
        }
        messageMapper.markRead(session.getId(), userId);
        // 通知对方"我已读"（在线才推）
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "read");
        body.put("sessionId", session.getId());
        body.put("readerId", userId);
        wsSessionManager.sendTo(peerUserId, JSONUtil.toJsonStr(body));
    }

    @Override
    public long unreadTotal(Long userId) {
        return messageMapper.countUnread(userId);
    }

    /** 查询两人会话（内部换算 a/b 顺序） */
    private ChatSession findPairSession(Long user1, Long user2) {
        long a = Math.min(user1, user2);
        long b = Math.max(user1, user2);
        return sessionMapper.findByPair(a, b);
    }

    /** 获取或创建会话（唯一键冲突时重查，并发安全） */
    private ChatSession getOrCreateSession(Long user1, Long user2) {
        ChatSession session = findPairSession(user1, user2);
        if (session != null) {
            return session;
        }
        ChatSession insert = new ChatSession();
        insert.setUserAId(Math.min(user1, user2));
        insert.setUserBId(Math.max(user1, user2));
        try {
            sessionMapper.insert(insert);
            return insert;
        } catch (DuplicateKeyException e) {
            return findPairSession(user1, user2);
        }
    }

    /** 构造 WS 推送 JSON 体 */
    private String pushBody(String type, ChatMessageVO vo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("message", vo);
        return JSONUtil.toJsonStr(body);
    }
}
