package com.itheima.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 店主新增/编辑宠物商品请求
 */
@Data
public class GoodsSaveDTO {

    @NotBlank(message = "宠物名称不能为空")
    @Size(max = 30, message = "宠物名称最长30字")
    private String petName;

    @Size(max = 30, message = "品种最长30字")
    private String breed;

    @Size(max = 20, message = "年龄描述最长20字")
    private String age;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @Size(max = 500, message = "商品描述最长500字")
    private String description;

    /** 商品图片URL列表（/images/ 开头，最多6张） */
    private List<String> images;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    @Max(value = 9999, message = "库存最多9999")
    private Integer stock;
}
