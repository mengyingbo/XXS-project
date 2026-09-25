package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 闯关进度（孩子 × 关卡） */
@Data
@TableName("progress")
public class Progress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long childId;

    private Long levelId;

    /** LOCKED 未解锁 / UNLOCKED 已解锁 / PASSED 已通关 */
    private String status;

    /** 最好星级 0~3 */
    private Integer stars;

    /** 最好正确率(%) */
    private BigDecimal bestAccuracy;

    private Integer bestScore;

    private Integer attemptCount;

    private LocalDateTime firstPassedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}