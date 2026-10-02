package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 单元 */
@Data
@TableName("unit")
public class Unit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 科目：chinese语文 / math数学（v2.0）/ english英语（v2.6） */
    private String subject;

    /** 单元序号 1~8 */
    private Integer unitNo;

    private String title;

    private String description;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}