package com.itheima.common.util;

import cn.hutool.dfa.WordTree;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 敏感词过滤工具：帖子、评论、评价、私信提交前校验
 * 命中敏感词直接拒绝提交并提示"内容不合法"
 */
@Component
public class SensitiveWordUtil {

    @Value("${pet.sensitive.words}")
    private String words;

    private final WordTree wordTree = new WordTree();

    @PostConstruct
    public void init() {
        List<String> wordList = cn.hutool.core.util.StrUtil.split(words, ',');
        wordTree.addWords(wordList);
    }

    /**
     * 是否命中敏感词
     */
    public boolean containsSensitive(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        return !wordTree.matchAll(content, -1, false, false).isEmpty();
    }
}
