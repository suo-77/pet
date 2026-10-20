package com.itheima.service;

import com.itheima.common.result.PageResult;
import com.itheima.vo.ChatMessageVO;
import com.itheima.vo.ChatSessionVO;

/**
 * 私信聊天服务：发消息 / 会话列表 / 聊天记录 / 标记已读
 */
public interface ChatService {

    /**
     * 发送私信：敏感词/XSS校验 -> 持久化 -> 更新会话 -> WS双向推送 -> Stream异步通知
     *
     * @param msgType 0文本 1图片
     */
    ChatMessageVO send(Long fromUserId, Long toUserId, int msgType, String content, String imageUrl);

    /** 分享动态给好友私信（msgType=2）：校验帖子存在，生成分享消息并推送 */
    ChatMessageVO sendShare(Long fromUserId, Long toUserId, Long postId);

    /** 我的会话分页（对方信息+最后消息+未读数，按最后消息时间倒序） */
    PageResult<ChatSessionVO> sessions(Long userId, int page, int pageSize);

    /** 与某人的聊天记录分页（时间倒序，前端翻转为正序展示） */
    PageResult<ChatMessageVO> messages(Long userId, Long peerUserId, int page, int pageSize);

    /** 标记与某人的会话为已读，并通知对方 */
    void markRead(Long userId, Long peerUserId);

    /** 我的未读消息总数 */
    long unreadTotal(Long userId);
}
