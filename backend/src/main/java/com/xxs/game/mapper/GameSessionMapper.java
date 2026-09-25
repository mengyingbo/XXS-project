package com.xxs.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxs.game.entity.GameSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface GameSessionMapper extends BaseMapper<GameSession> {

    /** 某孩子某时间点之后的累计游玩时长（毫秒），用于每日时长限额 */
    @Select("SELECT COALESCE(SUM(duration_ms), 0) FROM game_session WHERE child_id = #{childId} AND created_at >= #{start}")
    long sumDurationSince(@Param("childId") Long childId, @Param("start") LocalDateTime start);
}