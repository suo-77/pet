package com.itheima.mapper;

import com.itheima.entity.ChatSession;
import com.itheima.vo.ChatSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话 Mapper
 */
@Mapper
public interface ChatSessionMapper {

    /** 查询两人会话（a=较小ID, b=较大ID，含已删除） */
    ChatSession findByPair(@Param("userAId") Long userAId, @Param("userBId") Long userBId);

    /** 新增会话（唯一键冲突由业务层处理） */
    int insert(ChatSession session);

    /** 更新最后消息预览与时间 */
    int updateLast(@Param("id") Long id,
                   @Param("lastMessage") String lastMessage,
                   @Param("lastTime") LocalDateTime lastTime);

    /** 我的会话分页（联表对方信息 + 未读数子查询，按最后消息时间倒序） */
    List<ChatSessionVO> selectMySessions(@Param("userId") Long userId,
                                         @Param("offset") long offset,
                                         @Param("pageSize") int pageSize);

    /** 我的会话总数 */
    long countMySessions(@Param("userId") Long userId);
}
