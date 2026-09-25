package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.entity.AnswerRecord;
import com.xxs.game.mapper.AnswerRecordMapper;
import com.xxs.game.mapper.GameSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日额度：答题数量上限 + 游玩时长上限（需求文档 3.3 约束）
 */
@Service
@RequiredArgsConstructor
public class DailyQuotaService {

    private final AnswerRecordMapper answerRecordMapper;

    private final GameSessionMapper gameSessionMapper;

    private final ConfigService configService;

    public DailyUsage usage(Long childId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long answered = answerRecordMapper.selectCount(Wrappers.<AnswerRecord>lambdaQuery()
                .eq(AnswerRecord::getChildId, childId)
                .ge(AnswerRecord::getCreatedAt, todayStart));
        long usedMillis = gameSessionMapper.sumDurationSince(childId, todayStart);

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