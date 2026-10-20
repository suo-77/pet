package com.itheima.controller;

import com.itheima.common.context.UserContext;
import com.itheima.common.result.PageResult;
import com.itheima.common.result.Result;
import com.itheima.dto.ChatShareDTO;
import com.itheima.service.ChatService;
import com.itheima.vo.ChatMessageVO;
import com.itheima.vo.ChatSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 私信聊天 HTTP 接口（实时收发走 WebSocket /ws/chat）
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** 我的会话列表分页（对方信息+最后消息+未读红点数） */
    @GetMapping("/sessions")
    public Result<PageResult<ChatSessionVO>> sessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(chatService.sessions(UserContext.getUserIdRequired(), page, pageSize));
    }

    /** 与某人的聊天记录分页（进入聊天窗加载历史） */
    @GetMapping("/messages/{peerUserId}")
    public Result<PageResult<ChatMessageVO>> messages(
            @PathVariable Long peerUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(
                chatService.messages(UserContext.getUserIdRequired(), peerUserId, page, pageSize));
    }

    /** 标记与某人的会话已读（进入聊天窗时调用） */
    @PostMapping("/read/{peerUserId}")
    public Result<Void> markRead(@PathVariable Long peerUserId) {
        chatService.markRead(UserContext.getUserIdRequired(), peerUserId);
        return Result.success();
    }

    /** 我的未读消息总数（红点角标） */
    @GetMapping("/unread-total")
    public Result<Map<String, Long>> unreadTotal() {
        return Result.success(Map.of("unread", chatService.unreadTotal(UserContext.getUserIdRequired())));
    }

    /** 分享动态给好友私信（生成分享卡片消息） */
    @PostMapping("/share")
    public Result<ChatMessageVO> share(@Valid @RequestBody ChatShareDTO dto) {
        return Result.success(
                chatService.sendShare(UserContext.getUserIdRequired(), dto.getToUserId(), dto.getPostId()));
    }
}
