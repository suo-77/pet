package com.itheima.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 开店/编辑店铺请求（一人一店）
 */
@Data
public class ShopSaveDTO {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 30, message = "店铺名称最长30字")
    private String shopName;

    @Size(max = 200, message = "店铺简介最长200字")
    private String shopDesc;

    /** 店铺封面图URL（/images/ 开头，可选） */
    @Size(max = 255, message = "封面地址不合法")
    private String cover;
}
