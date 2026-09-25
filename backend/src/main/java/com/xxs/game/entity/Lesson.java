package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 课文 / 语文园地 */
@Data
@TableName("lesson")
public class Lesson {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long unitId;

    /** 课文序号（语文园地记为 0） */
    private Integer lessonNo;

    private String title;

    /** TEXT 课文 / GARDEN 语文园地 */
    private String lessonType;

    @TableField("is_skim")
    private Boolean isSkim;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}