package com.itheima.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储统一抽象（扩展点）：
 * 本期默认实现 LocalFileStorageService（本地磁盘 D:/MYsuo/study/images）
 * 后续对接阿里云/腾讯云 OSS 时，新增实现类并通过 yml pet.upload.oss-type 切换即可，
 * 上传调用方（头像/社区图片/聊天图片/评价图片）无需任何改动
 */
public interface FileStorageService {

    /**
     * 上传图片
     *
     * @param file 前端上传的图片文件
     * @return 图片访问 URL（本地实现返回 /images/...）
     */
    String upload(MultipartFile file);
}
