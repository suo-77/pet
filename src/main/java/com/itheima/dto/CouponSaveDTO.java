package com.itheima.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店主创建优惠券/秒杀券请求
 */
@Data
public class CouponSaveDTO {

    /** 所属店铺ID */
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @NotBlank(message = "券名称不能为空")
    @Size(max = 50, message = "券名称最长50字")
    private String couponName;

    /** 类型：0普通券 1秒杀券 */
    @NotNull(message = "券类型不能为空")
    @Min(value = 0, message = "券类型不合法")
    @Max(value = 1, message = "券类型不合法")
    private Integer category;

    @NotNull(message = "面额不能为空")
    @DecimalMin(value = "0.01", message = "面额必须大于0")
    private BigDecimal money;

    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少1张")
    @Max(value = 10000, message = "库存最多10000张")
    private Integer totalStock;

    /** 每人领取上限 */
    @NotNull(message = "领取上限不能为空")
    @Min(value = 1, message = "领取上限至少1")
    @Max(value = 10, message = "领取上限最多10")
    private Integer perLimit;

    /** 开始时间（秒杀券必填，用于倒计时） */
    private LocalDateTime startTime;

    /** 过期时间 */
    @NotNull(message = "过期时间不能为空")
    private LocalDateTime expireTime;
}
