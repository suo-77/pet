package com.itheima.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑个人资料请求：昵称 / 简介 / 头像
 */
@Data
public class UpdateProfileDTO {

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 20, message = "昵称长度为1-20位")
    private String nickname;

    @Size(max = 100, message = "个人简介最长100字")
    private String intro;

    /** 新头像URL（先上传获取） */
    private String avatar;
}
