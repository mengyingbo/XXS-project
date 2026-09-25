package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 逐题作答记录（错题本数据来源） */
@Data
@TableName("answer_record")
public class AnswerRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long childId;

    private Long questionId;

    /** 孩子作答内容（JSON 字符串） */
    private String userAnswer;

    @TableField("is_correct")
    private Boolean isCorrect;

    private Long durationMs;

    private LocalDateTime createdAt;
}