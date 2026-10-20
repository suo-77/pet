package com.itheima.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回：token + 用户基本信息
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private UserVO user;
}
