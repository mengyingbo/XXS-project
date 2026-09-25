package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目
 * type：SINGLE单选 / JUDGE判断 / BLANK填空 / ORDER排序
 * options / answer 以 JSON 字符串存取，约定见 sql/01_schema.sql 第 6 节
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    /** 所属关卡 id，可为空 */
    private Long levelId;

    private String type;

    private String stem;

    /** JSON 字符串 */
    private String options;

    /** JSON 字符串 */
    private String answer;

    private String analysis;

    private String knowledgePoint;

    /** 难度 1~3 */
    private Integer difficulty;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}