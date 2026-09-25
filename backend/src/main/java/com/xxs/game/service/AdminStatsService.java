package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.entity.AnswerRecord;
import com.xxs.game.entity.GameSession;
import com.xxs.game.entity.Lesson;
import com.xxs.game.entity.Level;
import com.xxs.game.entity.Question;
import com.xxs.game.entity.Unit;
import com.xxs.game.mapper.AnswerRecordMapper;
import com.xxs.game.mapper.ChildMapper;
import com.xxs.game.mapper.GameSessionMapper;
import com.xxs.game.mapper.LessonMapper;
import com.xxs.game.mapper.LevelMapper;
import com.xxs.game.mapper.QuestionMapper;
import com.xxs.game.mapper.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端仪表盘与统计报表（需求文档 6.2 F-AD-02、F-AD-10）
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final ChildMapper childMapper;

    private final UnitMapper unitMapper;

    private final LessonMapper lessonMapper;

    private final LevelMapper levelMapper;

    private final QuestionMapper questionMapper;

    private final GameSessionMapper gameSessionMapper;

    private final AnswerRecordMapper answerRecordMapper;

    private final AdminPrizeService adminPrizeService;

    /** 仪表盘 */
    public Map<String, Object> dashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("childCount", childMapper.selectCount(Wrappers.lambdaQuery()));
        data.put("enabledChildCount", childMapper.selectCount(
                Wrappers.<com.xxs.game.entity.Child>lambdaQuery().eq(com.xxs.game.entity.Child::getEnabled, true)));
        data.put("todayActiveChildren", answerRecordMapper.countActiveChildrenSince(todayStart));
        data.put("todayAnswers", answerRecordMapper.selectCount(Wrappers.<AnswerRecord>lambdaQuery()
                .ge(AnswerRecord::getCreatedAt, todayStart)));
        data.put("todaySessions", gameSessionMapper.selectCount(Wrappers.<GameSession>lambdaQuery()
                .ge(GameSession::getCreatedAt, todayStart)));
        data.put("totalIssuedPoints", childMapper.sumTotalEarned());
        data.put("totalAvailablePoints", childMapper.sumTotalPoints());
        data.put("redeemCounts", adminPrizeService.countByStatus());
        data.put("pendingRedeemCount", adminPrizeService.countByStatus().get("PENDING"));

        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("unitCount", unitMapper.selectCount(Wrappers.<Unit>lambdaQuery()));
        catalog.put("lessonCount", lessonMapper.selectCount(Wrappers.<Lesson>lambdaQuery()));
        catalog.put("levelCount", levelMapper.selectCount(Wrappers.<Level>lambdaQuery()));
        catalog.put("questionCount", questionMapper.selectCount(Wrappers.<Question>lambdaQuery()));
        data.put("catalog", catalog);
        return data;
    }

    /**
     * 统计报表：整体 + 按孩子 / 按课文 / 按知识点
     *
     * @param childId 为空表示全部孩子
     * @param days    统计最近天数，为空或 <=0 表示全部时间
     */
    public Map<String, Object> stats(Long childId, Integer days) {
        LocalDateTime since = days == null || days <= 0 ? null : LocalDate.now().minusDays(days - 1L).atStartOfDay();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("childId", childId);
        data.put("days", days);
        data.put("since", since);
        data.put("byChild", withAccuracy(answerRecordMapper.statsByChild(since)));
        data.put("byLesson", withAccuracy(answerRecordMapper.statsByLesson(childId)));
        data.put("byKnowledgePoint", withAccuracy(answerRecordMapper.statsByKnowledgePoint(childId)));
        data.put("weakKnowledgePoints", weakPoints(answerRecordMapper.statsByKnowledgePoint(childId)));
        return data;
    }

    /** 给统计行补上 accuracy(%) 字段 */
    private List<Map<String, Object>> withAccuracy(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            long total = toLong(row.get("total"));
            long correct = toLong(row.get("correct"));
            BigDecimal accuracy = total == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
            item.put("total", total);
            item.put("correct", correct);
            item.put("accuracy", accuracy);
            result.add(item);
        }
        return result;
    }

    /** 薄弱知识点：作答量 >= 3 且正确率最低的前 5 个 */
    private List<Map<String, Object>> weakPoints(List<Map<String, Object>> rows) {
        return withAccuracy(rows).stream()
                .filter(row -> toLong(row.get("total")) >= 3)
                .sorted((a, b) -> ((BigDecimal) a.get("accuracy")).compareTo((BigDecimal) b.get("accuracy")))
                .limit(5)
                .toList();
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}