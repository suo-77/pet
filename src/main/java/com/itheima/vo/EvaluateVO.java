package com.itheima.vo;

import com.itheima.entity.GoodsEvaluate;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品评价视图对象
 */
@Data
public class EvaluateVO {

    private Long id;
    private Long goodsId;
    private Long userId;
    /** 评价者昵称 */
    private String nickname;
    /** 评价者头像 */
    private String avatar;
    private String content;
    /** 评价图片URL列表 */
    private List<String> images;
    private LocalDateTime createTime;

    public static EvaluateVO from(GoodsEvaluate evaluate, List<String> images) {
        EvaluateVO vo = new EvaluateVO();
        vo.setId(evaluate.getId());
        vo.setGoodsId(evaluate.getGoodsId());
        vo.setUserId(evaluate.getUserId());
        vo.setNickname(evaluate.getNickname());
        vo.setAvatar(evaluate.getAvatar());
        vo.setContent(evaluate.getContent());
        vo.setImages(images);
        vo.setCreateTime(evaluate.getCreateTime());
        return vo;
    }
}
