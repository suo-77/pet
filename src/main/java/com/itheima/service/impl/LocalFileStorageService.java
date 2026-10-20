package com.itheima.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.itheima.common.exception.BusinessException;
import com.itheima.mq.constant.MQConstant;
import com.itheima.mq.stream.MQProducer;
import com.itheima.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 本地磁盘存储实现（pet.upload.oss-type=local）：
 * 1. 校验：非空、大小、格式白名单
 * 2. 存储：D:/MYsuo/study/images/{yyyyMM}/{uuid}.{ext}
 * 3. 上传完成后投递 img.compress Stream 消息，消费者异步压缩图片
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final MQProducer mqProducer;

    @Value("${pet.upload.dir}")
    private String uploadDir;

    @Value("${pet.upload.allow-types}")
    private String allowTypes;

    @Value("${pet.upload.max-size}")
    private long maxSize;

    @Override
    public String upload(MultipartFile file) {
        // 基础校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("图片大小不能超过10MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = StrUtil.isBlank(originalName) ? ""
                : StrUtil.subAfter(originalName, ".", true).toLowerCase();
        List<String> allowList = StrUtil.split(allowTypes, ',');
        if (!allowList.contains(ext)) {
            throw new BusinessException("仅支持 " + allowTypes + " 格式的图片");
        }
        try {
            // 按 年月 子目录存储，UUID 文件名防止冲突与路径穿越
            String monthDir = DateUtil.format(new Date(), "yyyyMM");
            Path dir = Paths.get(uploadDir, monthDir);
            Files.createDirectories(dir);
            String fileName = IdUtil.simpleUUID() + "." + ext;
            Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());

            // 投递异步压缩消息（压缩失败不影响原图可用）
            String relativePath = monthDir + "/" + fileName;
            String url = "/images/" + relativePath;
            mqProducer.send(MQConstant.IMG_STREAM, Map.of("fileName", relativePath, "url", url));
            log.info("图片上传成功: {}", url);
            return url;
        } catch (IOException e) {
            log.error("图片上传失败: ", e);
            throw new BusinessException("上传失败，请稍后重试");
        }
    }
}
