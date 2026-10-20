package com.itheima.vo;

import com.itheima.entity.User;
import lombok.Data;

/**
 * 用户脱敏视图对象：不返回密码
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String intro;
    /** 注册手机号（本人视角可见） */
    private String phone;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setIntro(user.getIntro());
        vo.setPhone(user.getPhone());
        return vo;
    }
}
