package com.itheima.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 帖子评论请求
 */
@Data
public class PostCommentDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论最长500字")
    private String content;

    /** 父评论ID，0为一级评论 */
    private Long parentId;
}
