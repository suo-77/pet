package com.itheima.controller;

import com.itheima.common.context.UserContext;
import com.itheima.common.result.PageResult;
import com.itheima.common.result.Result;
import com.itheima.dto.PostCommentDTO;
import com.itheima.dto.PostCreateDTO;
import com.itheima.service.PostService;
import com.itheima.vo.PostCommentVO;
import com.itheima.vo.PostLikeUserVO;
import com.itheima.vo.PostVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 社区帖子接口：发布 / 信息流 / 点赞 / 评论 / 删除 / 我的动态
 */
@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /** 发布帖子（登录） */
    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody PostCreateDTO dto) {
        Long postId = postService.create(UserContext.getUserIdRequired(), dto);
        return Result.success(Map.of("postId", postId));
    }

    /** 信息流分页（公开，未登录 liked=false）；传 userId 时查询指定用户的动态（他人主页使用） */
    @GetMapping("/page")
    public Result<PageResult<PostVO>> page(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(postService.pageFeed(UserContext.getUserId(), userId, page, pageSize));
    }

    /** 我发布的动态分页（登录，个人中心使用） */
    @GetMapping("/my")
    public Result<PageResult<PostVO>> my(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(postService.myPosts(UserContext.getUserIdRequired(), page, pageSize));
    }

    /** 点赞（Redis 计数 + 异步落库） */
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        postService.like(UserContext.getUserIdRequired(), id);
        return Result.success();
    }

    /** 取消点赞 */
    @PostMapping("/{id}/unlike")
    public Result<Void> unlike(@PathVariable Long id) {
        postService.unlike(UserContext.getUserIdRequired(), id);
        return Result.success();
    }

    /** 帖子点赞用户列表（谁赞了，公开） */
    @GetMapping("/{id}/likes")
    public Result<List<PostLikeUserVO>> likes(@PathVariable Long id) {
        return Result.success(postService.likeUsers(id));
    }

    /** 发表评论（登录） */
    @PostMapping("/{id}/comment")
    public Result<Void> comment(@PathVariable Long id, @Valid @RequestBody PostCommentDTO dto) {
        postService.comment(UserContext.getUserIdRequired(), id, dto);
        return Result.success();
    }

    /** 评论分页（公开） */
    @GetMapping("/{id}/comments")
    public Result<PageResult<PostCommentVO>> comments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(postService.comments(id, page, pageSize));
    }

    /** 删除帖子（仅作者本人） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.delete(UserContext.getUserIdRequired(), id);
        return Result.success();
    }
}
