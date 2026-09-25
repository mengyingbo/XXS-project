package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 奖品 */
@Data
@TableName("prize")
public class Prize {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String image;

    private Integer pointsCost;

    private Integer stock;

    private Boolean enabled;

    private String description;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}