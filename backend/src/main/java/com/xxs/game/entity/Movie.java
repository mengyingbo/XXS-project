package com.xxs.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 电影片库 */
@Data
@TableName("movie")
public class Movie {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 显示序号（排序用） */
    private Integer no;

    private String name;

    /** 类型（如"成长/音乐"） */
    private String type;

    /** 片长（分钟） */
    private Integer duration;

    /** 豆瓣评分 */
    private BigDecimal rating;

    /** 推荐主题（如"包容与赏识"） */
    private String theme;

    /** 适龄看点 */
    private String note;

    /** 封面图URL */
    private String cover;

    /** 默认观看状态（0未看/1已看），H5首次访问的初始值 */
    private Boolean watched;

    /** 是否上架（0下架/1上架） */
    private Boolean enabled;

    /** 排序权重（升序） */
    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
