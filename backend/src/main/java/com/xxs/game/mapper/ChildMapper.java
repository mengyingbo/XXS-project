package com.xxs.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxs.game.entity.Child;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ChildMapper extends BaseMapper<Child> {

    /** 累计发放积分（所有孩子 total_earned 之和） */
    @Select("SELECT COALESCE(SUM(total_earned), 0) FROM child")
    long sumTotalEarned();

    /** 当前所有孩子的可用积分总额 */
    @Select("SELECT COALESCE(SUM(total_points), 0) FROM child")
    long sumTotalPoints();

    /**
     * 加积分：同时累加「当前可用积分」与「累计获得积分」
     * earnedDelta 为 0 时只加可用积分（如家长手动调分）
     */
    @Update("UPDATE child SET total_points = total_points + #{delta}, "
            + "total_earned = total_earned + #{earnedDelta} WHERE id = #{id}")
    int addPoints(@Param("id") Long id, @Param("delta") int delta, @Param("earnedDelta") int earnedDelta);

    /** 扣积分：带余额校验，返回 0 表示余额不足（并发安全） */
    @Update("UPDATE child SET total_points = total_points - #{cost} WHERE id = #{id} AND total_points >= #{cost}")
    int deductPoints(@Param("id") Long id, @Param("cost") int cost);
}