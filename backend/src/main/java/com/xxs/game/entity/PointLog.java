package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水
 * bizType：ANSWER 答题 / PASS 通关 / THREE_STAR 三星 / COMBO 连对
 *          REDEEM 兑换 / REJECT_REFUND 拒绝退回 / ADMIN_ADJUST 家长调整
 */
@Data
@TableName("point_log")
public class PointLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long childId;

    /** 积分变动（正增负减） */
    private Integer changeAmount;

    private Integer balanceAfter;

    private String bizType;

    private Long refId;

    private String remark;

    private LocalDateTime createdAt;
}