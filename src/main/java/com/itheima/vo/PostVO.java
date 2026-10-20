package com.itheima.vo;

import com.itheima.entity.CommunityPost;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子视图对象：信息流/个人动态列表卡片
 */
@Data
public class PostVO {

    private Long id;
    private Long userId;
    /** 作者昵称 */
    private String nickname;
    /** 作者头像 */
    private String avatar;
    private String content;
    /** 图片URL列表 */
    private List<String> images;
    private Integer likeCount;
    private Integer commentCount;
    /** 当前用户是否已点赞（未登录恒为 false） */
    private Boolean liked;
    private LocalDateTime createTime;

    /** 实体转 VO：images 为 JSON 数组字符串，由服务层解析后设置 */
    public static PostVO from(CommunityPost post, List<String> images) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setNickname(post.getNickname());
        vo.setAvatar(post.getAvatar());
        vo.setContent(post.getContent());
        vo.setImages(images);
        vo.setLikeCount(post.getLikeCount() == null ? 0 : post.getLikeCount());
        vo.setCommentCount(post.getCommentCount() == null ? 0 : post.getCommentCount());
        vo.setCreateTime(post.getCreateTime());
        return vo;
    }
}
