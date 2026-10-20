package com.itheima.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 开通店铺请求（一人一店）
 */
@Data
public class ShopOpenDTO {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 50, message = "店铺名称最长50字")
    private String shopName;

    @Size(max = 200, message = "店铺简介最长200字")
    private String shopDesc;

    /** 店铺封面URL（可选，通过统一上传接口获取） */
    private String cover;
}
