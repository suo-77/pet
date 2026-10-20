package com.itheima.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 店主编辑店铺信息请求
 */
@Data
public class ShopUpdateDTO {

    @Size(max = 50, message = "店铺名称最长50字")
    private String shopName;

    @Size(max = 200, message = "店铺简介最长200字")
    private String shopDesc;

    /** 店铺封面URL */
    private String cover;
}
