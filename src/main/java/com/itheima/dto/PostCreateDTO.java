package com.itheima.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布帖子请求：文字与图片至少其一（纯文字发布允许不传图片）
 */
@Data
public class PostCreateDTO {

    @Size(max = 1000, message = "帖子内容最长1000字")
    private String content;

    /** 图片URL列表（先调用统一上传接口获取，最多9张） */
    @Size(max = 9, message = "最多上传9张图片")
    private List<String> images;
}
