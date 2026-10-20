package com.itheima.vo;

import lombok.Data;

/**
 * 用户卡片视图对象：关注/粉丝/推荐列表使用
 */
@Data
public class UserCardVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String intro;
}
