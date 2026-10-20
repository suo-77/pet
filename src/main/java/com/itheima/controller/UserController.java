package com.itheima.controller;

import com.itheima.common.context.UserContext;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.PageResult;
import com.itheima.common.result.Result;
import com.itheima.dto.UpdateProfileDTO;
import com.itheima.service.UserService;
import com.itheima.vo.UserCardVO;
import com.itheima.vo.UserHomeVO;
import com.itheima.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：资料编辑 / 用户主页 / 关注关系 / 关注粉丝列表 / 推荐用户
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 编辑个人资料（昵称/简介/头像） */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success(userService.updateProfile(UserContext.getUserIdRequired(), dto));
    }

    /** 用户主页：计数 + 查看者关注状态（公开可访问） */
    @GetMapping("/home/{userId}")
    public Result<UserHomeVO> home(@PathVariable Long userId) {
        return Result.success(userService.home(UserContext.getUserId(), userId));
    }

    /** 关注用户 */
    @PostMapping("/follow/{targetId}")
    public Result<Void> follow(@PathVariable Long targetId) {
        userService.follow(UserContext.getUserIdRequired(), targetId);
        return Result.success();
    }

    /** 取消关注 */
    @PostMapping("/unfollow/{targetId}")
    public Result<Void> unfollow(@PathVariable Long targetId) {
        userService.unfollow(UserContext.getUserIdRequired(), targetId);
        return Result.success();
    }

    /** 我关注的人（分页）；不传 userId 时查看自己的 */
    @GetMapping("/following")
    public Result<PageResult<UserCardVO>> following(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(userService.followingList(resolveUserId(userId), page, pageSize));
    }

    /** 我的粉丝（分页）；不传 userId 时查看自己的 */
    @GetMapping("/fans")
    public Result<PageResult<UserCardVO>> fans(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(userService.fanList(resolveUserId(userId), page, pageSize));
    }

    /** 推荐用户（排除自己与已关注） */
    @GetMapping("/recommend")
    public Result<PageResult<UserCardVO>> recommend(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(userService.recommendList(UserContext.getUserIdRequired(), page, pageSize));
    }

    /**
     * 解析目标用户ID：未传参数时默认当前登录用户；接口本身放行给未登录访问，因此需自行校验
     */
    private Long resolveUserId(Long userId) {
        if (userId != null) {
            return userId;
        }
        Long me = UserContext.getUserId();
        if (me == null) {
            throw new BusinessException(401, "请先登录");
        }
        return me;
    }
}
