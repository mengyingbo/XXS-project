package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 兑换订单 */
@Data
@TableName("redeem_order")
public class RedeemOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long childId;

    private Long prizeId;

    /** 奖品名称快照 */
    private String prizeName;

    /** 消耗积分快照 */
    private Integer pointsCost;

    /** PENDING 待审核 / APPROVED 已通过 / REJECTED 已拒绝 / DELIVERED 已发放 */
    private String status;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;
}