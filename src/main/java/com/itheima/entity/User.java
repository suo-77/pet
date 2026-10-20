package com.itheima.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 user
 */
@Data
public class User {

    private Long id;
    /** 账号/登录名 */
    private String username;
    /** 密码(加盐MD5) */
    private String password;
    /** 手机号 */
    private String phone;
    /** 昵称 */
    private String nickname;
    /** 头像URL */
    private String avatar;
    /** 个人简介 */
    private String intro;
    /** 角色：0普通用户 1管理员 */
    private Integer role;
    /** 状态：0正常 1禁用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0否 1是 */
    private Integer deleted;
}
