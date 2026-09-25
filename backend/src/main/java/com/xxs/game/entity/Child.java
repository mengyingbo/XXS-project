package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 孩子档案 */
@Data
@TableName("child")
public class Child {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nickname;

    private String avatar;

    private String pinHash;

    /** 当前可用积分 */
    private Integer totalPoints;

    /** 累计获得积分（不因兑换减少） */
    private Integer totalEarned;

    /** PIN 连续错误次数 */
    private Integer pinFailCount;

    /** PIN 锁定截止时间 */
    private LocalDateTime lockedUntil;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}