package com.xxs.game.service;

import com.xxs.game.mapper.AnswerRecordMapper;
import com.xxs.game.mapper.GameSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日额度：答题数量上限 + 游玩时长上限（需求文档 3.3 约束）
 * v2.0：语文 / 数学两科各一套额度，分别计算、分别拦截
 */
@Service
@RequiredArgsConstructor
public class DailyQuotaService {

    private final AnswerRecordMapper answerRecordMapper;

    private final GameSessionMapper gameSessionMapper;

    private final ConfigService configService;

    /** 指定孩子、指定科目的今日额度情况（v2.0 每科各一套） */
    public DailyUsage usage(Long childId, String subject) {
        String sub = GameMapService.normalizeSubject(subject);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long answered = answerRecordMapper.countAnsweredSinceBySubject(childId, todayStart, sub);
        long usedMillis = gameSessionMapper.sumDurationSinceBySubject(childId, todayStart, sub);

        int questionLimit = configService.getInt(ConfigService.DAILY_QUESTION_LIMIT);
        int minuteLimit = configService.getInt(ConfigService.DAILY_MINUTE_LIMIT);
        long usedMinutes = usedMillis / 60_000L;

        return new DailyUsage(
                (int) answered,
                usedMinutes,
                questionLimit,
                minuteLimit,
                (int) Math.max(0, questionLimit - answered),
                (int) Math.max(0, minuteLimit - usedMinutes));
    }

    /**
     * 是否已达到上限（题量或时长任一达到即不可开始新关卡）
     */
    public boolean isExhausted(DailyUsage usage) {
        return usage.questionLimitReached() || usage.minuteLimitReached();
    }

    /** 今日额度情况 */
    public record DailyUsage(int answeredToday,
                             long usedMinutesToday,
                             int questionLimit,
                             int minuteLimit,
                             int remainingQuestions,
                             int remainingMinutes) {

        public boolean questionLimitReached() {
            return remainingQuestions <= 0;
        }

        public boolean minuteLimitReached() {
            return remainingMinutes <= 0;
        }
    }
}