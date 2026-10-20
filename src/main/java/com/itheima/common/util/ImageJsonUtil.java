package com.itheima.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.List;

/**
 * 图片JSON工具：entity 的 images 字段（JSON数组字符串）与 List<String> 互转
 */
public final class ImageJsonUtil {

    private ImageJsonUtil() {
    }

    /** JSON数组字符串解析为图片URL列表，空值/异常时返回空列表 */
    public static List<String> parse(String imagesJson) {
        if (StrUtil.isBlank(imagesJson) || "[]".equals(imagesJson)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(imagesJson, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 图片URL列表序列化为 JSON 数组字符串 */
    public static String toJson(List<String> images) {
        if (images == null || images.isEmpty()) {
            return "[]";
        }
        return JSONUtil.toJsonStr(images);
    }
}
