package com.itheima.service;

import com.itheima.common.result.PageResult;
import com.itheima.dto.UpdateProfileDTO;
import com.itheima.vo.UserCardVO;
import com.itheima.vo.UserHomeVO;
import com.itheima.vo.UserVO;

/**
 * 用户服务：资料编辑 / 用户主页 / 关注关系（Redis 缓存 + MySQL 持久化）/ 列表 / 推荐
 */
public interface UserService {

    /** 编辑个人资料（昵称/简介/头像），返回最新信息 */
    UserVO updateProfile(Long userId, UpdateProfileDTO dto);

    /** 用户主页：计数 + 查看者关注状态（viewerId 可为空，未登录） */
    UserHomeVO home(Long viewerId, Long userId);

    /** 关注用户（不能关注自己，幂等校验） */
    void follow(Long userId, Long targetId);

    /** 取消关注 */
    void unfollow(Long userId, Long targetId);

    /** 是否已关注（viewerId 为空返回 false；Redis 优先，未命中回源 MySQL 回填） */
    boolean isFollowing(Long viewerId, Long targetId);

    /** 我关注的人（分页）；userId 为空时默认当前登录用户 */
    PageResult<UserCardVO> followingList(Long userId, int page, int pageSize);

    /** 我的粉丝（分页）；userId 为空时默认当前登录用户 */
    PageResult<UserCardVO> fanList(Long userId, int page, int pageSize);

    /** 推荐用户：排除自己与已关注 */
    PageResult<UserCardVO> recommendList(Long userId, int page, int pageSize);
}
