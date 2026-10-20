package com.itheima.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.itheima.mq.constant.MQConstant;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 图片压缩消费者：上传完成后异步压缩，降低磁盘占用与前端加载流量
 * 规则：
 * 1. 宽度超过 1280px 的图片等比缩至 1280
 * 2. 以 0.85 质量重新编码（jpg/webp 明显变小）
 * 3. gif 为保持动图不处理；文件已不存在直接跳过
 * 处理失败抛出异常 -> 不 ACK -> 重试调度器接管，超限进死信流
 */
@Slf4j
@Component
public class ImgStreamHandler implements StreamBizHandler {

    /** 压缩最大宽度 */
    private static final int MAX_WIDTH = 1280;
    /** 压缩质量 */
    private static final double QUALITY = 0.85;

    @Value("${pet.upload.dir}")
    private String uploadDir;

    @Override
    public String streamKey() {
        return MQConstant.IMG_STREAM;
    }

    @Override
    public String group() {
        return MQConstant.IMG_GROUP;
    }

    @Override
    public void handle(Map<String, String> payload) throws Exception {
        String fileName = payload.get("fileName");
        if (StrUtil.isBlank(fileName)) {
            log.warn("图片压缩消息缺少fileName: {}", payload);
            return;
        }
        Path path = Paths.get(uploadDir, fileName).normalize();
        // 目录穿越防护：必须仍位于上传根目录内
        if (!path.startsWith(Paths.get(uploadDir).normalize())) {
            log.warn("非法文件路径，跳过压缩: {}", fileName);
            return;
        }
        if (!Files.exists(path)) {
            log.info("图片文件不存在，跳过压缩: {}", fileName);
            return;
        }
        String ext = StrUtil.subAfter(fileName, ".", true).toLowerCase();
        if ("gif".equals(ext)) {
            log.info("gif动图跳过压缩: {}", fileName);
            return;
        }
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            log.info("非有效图片，跳过压缩: {}", fileName);
            return;
        }
        long before = Files.size(path);
        File targetFile = path.toFile();
        Thumbnails.Builder<File> builder = Thumbnails.of(targetFile);
        if (image.getWidth() > MAX_WIDTH) {
            // 等比缩放到最大宽度
            builder.width(MAX_WIDTH);
        } else {
            // 尺寸不变，仅重新编码压缩
            builder.scale(1.0);
        }
        builder.outputQuality(QUALITY).toFile(targetFile);
        long after = Files.size(path);
        log.info("图片压缩完成: {} -> {} ({}KB -> {}KB)", fileName, image.getWidth() + "px",
                before / 1024, after / 1024);
    }
}
