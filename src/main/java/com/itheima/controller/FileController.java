package com.itheima.controller;

import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.Result;
import com.itheima.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 统一文件上传接口：头像 / 社区图片 / 聊天图片 / 评价图片 全部走此入口
 * 前端拿到返回 URL 后随业务表单提交；上传后由消费者异步压缩
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** 上传图片，返回 {url}；@RequestParam 兼容浏览器 FormData 与各类客户端 multipart */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        String url = fileStorageService.upload(file);
        return Result.success(Map.of("url", url));
    }
}
