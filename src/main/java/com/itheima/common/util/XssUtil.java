package com.itheima.common.util;

import cn.hutool.http.HtmlUtil;

/**
 * XSS 过滤工具：用户提交的文本内容（帖子、评论、评价、私信、资料）入库前统一清洗
 */
public final class XssUtil {

    private XssUtil() {
    }

    /** 清洗文本中的 XSS 风险内容（过滤 script/事件脚本等），保留正常文字 */
    public static String clean(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return HtmlUtil.filter(content);
    }
}
