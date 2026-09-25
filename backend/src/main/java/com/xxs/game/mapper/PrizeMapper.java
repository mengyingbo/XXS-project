package com.xxs.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxs.game.entity.Prize;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PrizeMapper extends BaseMapper<Prize> {

    /** 扣库存：带库存校验，返回 0 表示库存不足（并发安全） */
    @Update("UPDATE prize SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int deductStock(@Param("id") Long id);

    /** 拒绝兑换时退回库存 */
    @Update("UPDATE prize SET stock = stock + 1 WHERE id = #{id}")
    int restoreStock(@Param("id") Long id);
}