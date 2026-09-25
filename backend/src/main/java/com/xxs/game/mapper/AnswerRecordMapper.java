package com.xxs.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxs.game.entity.AnswerRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AnswerRecordMapper extends BaseMapper<AnswerRecord> {

    /**
     * 错题本：取每道题「最近一次作答为错误」的题目（即孩子当前仍未掌握的题）
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
                   a.user_answer   AS userAnswer,
                   a.created_at    AS answeredAt,
                   (SELECT COUNT(*) FROM answer_record a2
                     WHERE a2.child_id = a.child_id AND a2.question_id = a.question_id AND a2.is_correct = 0) AS wrongCount
              FROM answer_record a
              JOIN question q ON q.id = a.question_id
              JOIN lesson   l ON l.id = q.lesson_id
              JOIN unit     u ON u.id = l.unit_id
             WHERE a.child_id = #{childId}
               AND a.is_correct = 0
               AND a.id = (SELECT MAX(a3.id) FROM answer_record a3
                            WHERE a3.child_id = a.child_id AND a3.question_id = a.question_id)
             ORDER BY a.id DESC
             LIMIT #{limit}
            """)
    List<Map<String, Object>> selectWrongQuestions(@Param("childId") Long childId, @Param("limit") int limit);

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