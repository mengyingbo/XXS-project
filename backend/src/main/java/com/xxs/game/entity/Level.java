package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 关卡 */
@Data
@TableName("level")
public class Level {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    /** 关卡序号，从 1 开始 */
    private Integer levelNo;

    /** 关卡名称，空则前端显示「第N关」 */
    private String name;

    private Integer questionCount;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}