package com.itheima.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发表商品评价请求
 */
@Data
public class EvaluateCreateDTO {

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 500, message = "评价最长500字")
    private String content;

    /** 评价图片URL列表（先调用统一上传接口获取，最多9张） */
    @Size(max = 9, message = "最多上传9张图片")
    private List<String> images;
}
