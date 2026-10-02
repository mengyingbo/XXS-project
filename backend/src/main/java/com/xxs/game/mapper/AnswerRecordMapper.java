package com.xxs.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxs.game.entity.AnswerRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AnswerRecordMapper extends BaseMapper<AnswerRecord> {

    /**
     * 错题本（2026-09-26 规则调整）：只要曾答错即收录，不因重做答对而移除；
     * mastered = 最近一次作答是否正确（1 已掌握 / 0 未掌握）；
     * 排序：未掌握在前，同组内按最近一次答错的记录倒序；上限 limit 条。
     */
    @Select("""
            SELECT q.id            AS questionId,
                   q.stem          AS stem,
                   q.type          AS type,
                   q.options       AS options,
                   q.answer        AS answer,
                   q.analysis      AS analysis,
                   q.knowledge_point AS knowledgePoint,
                   q.difficulty    AS difficulty,
                   l.title         AS lessonTitle,
                   u.title         AS unitTitle,
                   u.subject       AS subject,
                   COUNT(*)        AS wrongCount,
                   (SELECT a3.is_correct FROM answer_record a3
                     WHERE a3.child_id = #{childId} AND a3.question_id = q.id
                     ORDER BY a3.id DESC LIMIT 1) AS latestCorrect,
                   (SELECT MAX(a4.id) FROM answer_record a4
                     WHERE a4.child_id = #{childId} AND a4.question_id = q.id AND a4.is_correct = 0) AS lastWrongId
              FROM answer_record a
              JOIN question q ON q.id = a.question_id
              JOIN lesson   l ON l.id = q.lesson_id
              JOIN unit     u ON u.id = l.unit_id
             WHERE a.child_id = #{childId}
               AND a.is_correct = 0
             GROUP BY q.id, q.stem, q.type, q.options, q.answer, q.analysis,
                      q.knowledge_point, q.difficulty, l.title, u.title, u.subject
             ORDER BY latestCorrect ASC, lastWrongId DESC
             LIMIT #{limit}
            """)
    List<Map<String, Object>> selectWrongQuestions(@Param("childId") Long childId, @Param("limit") int limit);

    /** 孩子有作答记录的自然日（去重，用于计算连续学习天数 streakDays） */
    @Select("SELECT DISTINCT DATE(created_at) FROM answer_record WHERE child_id = #{childId}")
    List<LocalDate> selectDistinctAnswerDays(@Param("childId") Long childId);

    /** 某孩子某时间之后、指定科目的已答题数（v2.0 每日限额按科目分别计算） */
    @Select("""
            SELECT COUNT(*)
              FROM answer_record a
              JOIN question q ON q.id = a.question_id
              JOIN lesson   l ON l.id = q.lesson_id
              JOIN unit     u ON u.id = l.unit_id
             WHERE a.child_id = #{childId}
               AND a.created_at >= #{start}
               AND u.subject = #{subject}
            """)
    long countAnsweredSinceBySubject(@Param("childId") Long childId,
                                     @Param("start") LocalDateTime start,
                                     @Param("subject") String subject);

    /** 指定时间以来出现过答题行为的孩子数（今日活跃） */
    @Select("SELECT COUNT(DISTINCT child_id) FROM answer_record WHERE created_at >= #{start}")
    long countActiveChildrenSince(@Param("start") LocalDateTime start);

    /** 按孩子统计作答量与正确率（since 为空则统计全部） */
    @Select("""
            <script>
            SELECT c.id              AS childId,
                   c.nickname        AS nickname,
                   COUNT(a.id)       AS total,
                   COALESCE(SUM(a.is_correct), 0) AS correct
              FROM child c
              LEFT JOIN answer_record a ON a.child_id = c.id
                   <if test="since != null"> AND a.created_at &gt;= #{since} </if>
             GROUP BY c.id, c.nickname
             ORDER BY c.id
            </script>
            """)
    List<Map<String, Object>> statsByChild(@Param("since") LocalDateTime since);

    /** 按知识点统计正确率（childId 为空则统计全部孩子） */
    @Select("""
            <script>
            SELECT q.knowledge_point AS knowledgePoint,
                   COUNT(*)          AS total,
                   SUM(a.is_correct) AS correct
              FROM answer_record a
              JOIN question q ON q.id = a.question_id
             WHERE q.knowledge_point &lt;&gt; ''
               <if test="childId != null"> AND a.child_id = #{childId} </if>
             GROUP BY q.knowledge_point
             ORDER BY total DESC
            </script>
            """)
    List<Map<String, Object>> statsByKnowledgePoint(@Param("childId") Long childId);

    /** 按课文统计正确率（childId 为空则统计全部孩子） */
    @Select("""
            <script>
            SELECT l.id            AS lessonId,
                   l.title         AS lessonTitle,
                   u.title         AS unitTitle,
                   COUNT(*)        AS total,
                   SUM(a.is_correct) AS correct
              FROM answer_record a
              JOIN question q ON q.id = a.question_id
              JOIN lesson   l ON l.id = q.lesson_id
              JOIN unit     u ON u.id = l.unit_id
             WHERE 1 = 1
               <if test="childId != null"> AND a.child_id = #{childId} </if>
             GROUP BY l.id, l.title, u.title
             ORDER BY total DESC
            </script>
            """)
    List<Map<String, Object>> statsByLesson(@Param("childId") Long childId);
}