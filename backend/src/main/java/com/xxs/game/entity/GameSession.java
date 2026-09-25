package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 答题局记录 */
@Data
@TableName("game_session")
public class GameSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long childId;

    private Long levelId;

    private Integer totalCount;

    private Integer correctCount;

    /** 正确率(%) */
    private BigDecimal accuracy;

    private Integer stars;

    private Integer pointsGained;

    private Long durationMs;

    private LocalDateTime createdAt;
}