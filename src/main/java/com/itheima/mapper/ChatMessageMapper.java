package com.itheima.mapper;

import com.itheima.entity.ChatMessage;
import com.itheima.vo.ChatMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper {

    /** 新增消息，回填自增ID */
    int insert(ChatMessage message);

    /** 会话聊天记录分页（时间倒序，前端展示时翻转为正序） */
    List<ChatMessageVO> selectPageBySession(@Param("sessionId") Long sessionId,
                                            @Param("offset") long offset,
                                            @Param("pageSize") int pageSize);

    /** 会话消息总数 */
    long countBySession(@Param("sessionId") Long sessionId);

    /** 标记会话中我接收的消息为已读 */
    int markRead(@Param("sessionId") Long sessionId, @Param("toUserId") Long toUserId);

    /** 用户全部未读消息总数 */
    long countUnread(@Param("userId") Long userId);
}
