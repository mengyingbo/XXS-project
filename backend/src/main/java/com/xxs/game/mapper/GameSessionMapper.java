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

    /** 某孩子某时间之后、指定科目的累计游玩时长（毫秒）（v2.0 每日限额按科目分别计算） */
    @Select("""
            SELECT COALESCE(SUM(s.duration_ms), 0)
              FROM game_session s
              JOIN level   l ON l.id = s.level_id
              JOIN lesson  le ON le.id = l.lesson_id
              JOIN unit    u ON u.id = le.unit_id
             WHERE s.child_id = #{childId}
               AND s.created_at >= #{start}
               AND u.subject = #{subject}
            """)
    long sumDurationSinceBySubject(@Param("childId") Long childId,
                                   @Param("start") LocalDateTime start,
                                   @Param("subject") String subject);
}