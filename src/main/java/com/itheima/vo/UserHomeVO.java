package com.itheima.vo;

import com.itheima.entity.User;
import lombok.Data;

/**
 * 用户主页视图对象：粉丝数、关注数、当前用户是否已关注该用户
 */
@Data
public class UserHomeVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String intro;
    /** 关注数 */
    private Long followingCount;
    /** 粉丝数 */
    private Long fanCount;
    /** 查看者是否已关注该用户（未登录时恒为 false） */
    private Boolean followed;

    public static UserHomeVO from(User user) {
        UserHomeVO vo = new UserHomeVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setIntro(user.getIntro());
        return vo;
    }
}
