package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxs.game.common.BizException;
import com.xxs.game.dto.ChildDtos;
import com.xxs.game.entity.AnswerRecord;
import com.xxs.game.entity.Child;
import com.xxs.game.entity.GameSession;
import com.xxs.game.entity.Lesson;
import com.xxs.game.entity.Level;
import com.xxs.game.entity.Progress;
import com.xxs.game.entity.Question;
import com.xxs.game.mapper.AnswerRecordMapper;
import com.xxs.game.mapper.GameSessionMapper;
import com.xxs.game.mapper.LessonMapper;
import com.xxs.game.mapper.LevelMapper;
import com.xxs.game.mapper.ProgressMapper;
import com.xxs.game.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 孩子端答题：开始一关 / 提交结算（需求文档 3.3 奖惩机制、6.1 F-H5-04~F-H5-06）
 */
@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final ChildAuthService childAuthService;

    private final GameMapService gameMapService;

    private final DailyQuotaService dailyQuotaService;

    private final ConfigService configService;

    private final QuestionJudge questionJudge;

    private final LevelMapper levelMapper;

    private final LessonMapper lessonMapper;

    private final QuestionMapper questionMapper;

    private final ProgressMapper progressMapper;

    private final GameSessionMapper gameSessionMapper;

    private final AnswerRecordMapper answerRecordMapper;

    private final PointService pointService;

    private final ObjectMapper objectMapper;

    /**
     * 开始一关：校验解锁状态与每日额度，返回不含答案的题目列表
     */
    public Map<String, Object> start(Long childId, Long levelId) {
        Child child = childAuthService.require(childId);
        if (!Boolean.TRUE.equals(child.getEnabled())) {
            throw new BizException(403, "该档案已被家长停用");
        }

        Level level = levelMapper.selectById(levelId);
        if (level == null) {
            throw BizException.notFound("关卡不存在");
        }
        if (GameMapService.STATUS_LOCKED.equals(gameMapService.statusOf(childId, levelId))) {
            throw new BizException(409, "这一关还没有解锁，先通过前面的关卡吧");
        }

        DailyQuotaService.DailyUsage usage = dailyQuotaService.usage(childId);
        if (usage.questionLimitReached()) {
            throw new BizException(409, "今天学得很棒，题目已经做完啦，明天再来吧！");
        }
        if (usage.minuteLimitReached()) {
            throw new BizException(409, "今天学得很棒，时长已经用完啦，明天再来吧！");
        }

        int limit = level.getQuestionCount() == null || level.getQuestionCount() <= 0
                ? configService.getInt(ConfigService.QUESTIONS_PER_LEVEL)
                : level.getQuestionCount();
        List<Question> questions = questionMapper.selectList(Wrappers.<Question>lambdaQuery()
                .eq(Question::getLevelId, levelId)
                .orderByAsc(Question::getSortOrder)
                .orderByAsc(Question::getId)
                .last("LIMIT " + limit));
        if (questions.isEmpty()) {
            throw new BizException(409, "本关还没有题目，请家长先在后台添加题目");
        }

        Lesson lesson = lessonMapper.selectById(level.getLessonId());

        List<Map<String, Object>> questionNodes = new ArrayList<>();
        for (Question question : questions) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", question.getId());
            node.put("type", question.getType());
            node.put("stem", question.getStem());
            node.put("options", questionJudge.read(question.getOptions()));
            node.put("difficulty", question.getDifficulty());
            node.put("knowledgePoint", question.getKnowledgePoint());
            questionNodes.add(node);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("levelId", level.getId());
        result.put("levelName", level.getName() == null || level.getName().isBlank()
                ? "第" + level.getLevelNo() + "关" : level.getName());
        result.put("lessonId", level.getLessonId());
        result.put("lessonTitle", lesson == null ? "" : lesson.getTitle());
        result.put("questionCount", questions.size());
        result.put("questions", questionNodes);
        result.put("showAnalysisImmediately", configService.getBool(ConfigService.SHOW_ANALYSIS_IMMEDIATELY));
        result.put("today", todayNode(usage));
        return result;
    }

    /**
     * 提交整关答案：结算对错、星级、积分，并更新进度
     */
    @Transactional
    public Map<String, Object> submit(Long childId, ChildDtos.SubmitReq req) {
        Child child = childAuthService.require(childId);
        if (!Boolean.TRUE.equals(child.getEnabled())) {
            throw new BizException(403, "该档案已被家长停用");
        }

        Level level = levelMapper.selectById(req.levelId());
        if (level == null) {
            throw BizException.notFound("关卡不存在");
        }
        if (GameMapService.STATUS_LOCKED.equals(gameMapService.statusOf(childId, req.levelId()))) {
            throw new BizException(409, "这一关还没有解锁");
        }

        List<Long> questionIds = req.answers().stream().map(ChildDtos.AnswerItem::questionId).toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        for (Long questionId : questionIds) {
            Question question = questionMap.get(questionId);
            if (question == null) {
                throw BizException.badRequest("题目不存在：" + questionId);
            }
            if (!req.levelId().equals(question.getLevelId())) {
                throw BizException.badRequest("题目不属于本关卡：" + questionId);
            }
        }

        ConfigService.GameRules rules = configService.rules();
        int correctCount = 0;
        int comboBonusTotal = 0;
        int streak = 0;
        long durationSum = 0L;

        List<Map<String, Object>> details = new ArrayList<>();
        List<AnswerRecord> records = new ArrayList<>();

        for (ChildDtos.AnswerItem item : req.answers()) {
            Question question = questionMap.get(item.questionId());
            String userAnswerJson = toJsonAnswer(item.userAnswer(), question.getType());
            boolean correct = questionJudge.judge(question.getType(), question.getAnswer(), userAnswerJson);
            if (correct) {
                correctCount++;
                streak++;
                if (rules.comboEnabled() && rules.comboSize() > 0 && streak % rules.comboSize() == 0) {
                    comboBonusTotal += rules.comboBonus();
                }
            } else {
                streak = 0;
            }
            long itemDuration = item.durationMs() == null || item.durationMs() < 0 ? 0L : item.durationMs();
            durationSum += itemDuration;

            AnswerRecord record = new AnswerRecord();
            record.setChildId(childId);
            record.setQuestionId(question.getId());
            record.setUserAnswer(userAnswerJson);
            record.setIsCorrect(correct);
            record.setDurationMs(itemDuration);
            records.add(record);

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("questionId", question.getId());
            detail.put("type", question.getType());
            detail.put("stem", question.getStem());
            detail.put("options", questionJudge.read(question.getOptions()));
            detail.put("userAnswer", questionJudge.read(userAnswerJson));
            detail.put("correctAnswer", questionJudge.read(question.getAnswer()));
            detail.put("isCorrect", correct);
            detail.put("analysis", question.getAnalysis());
            detail.put("knowledgePoint", question.getKnowledgePoint());
            details.add(detail);
        }

        int totalCount = req.answers().size();
        double accuracy = totalCount == 0 ? 0 : correctCount * 100.0 / totalCount;
        BigDecimal accuracyValue = BigDecimal.valueOf(accuracy).setScale(2, RoundingMode.HALF_UP);
        int stars = rules.starsOf(accuracy);
        boolean passed = stars >= 1;

        Progress progress = progressMapper.selectOne(Wrappers.<Progress>lambdaQuery()
                .eq(Progress::getChildId, childId)
                .eq(Progress::getLevelId, req.levelId()));
        boolean firstPass = passed && (progress == null || progress.getFirstPassedAt() == null);
        boolean firstThreeStar = stars >= 3 && (progress == null || progress.getStars() == null || progress.getStars() < 3);

        int correctPoints = correctCount * rules.pointsPerCorrect();
        int passBonus = firstPass ? rules.pointsPerLevelPass() : 0;
        int threeStarBonus = firstThreeStar ? rules.pointsPerThreeStar() : 0;
        int pointsGained = correctPoints + comboBonusTotal + passBonus + threeStarBonus;

        // 1. 答题局记录
        GameSession session = new GameSession();
        session.setChildId(childId);
        session.setLevelId(req.levelId());
        session.setTotalCount(totalCount);
        session.setCorrectCount(correctCount);
        session.setAccuracy(accuracyValue);
        session.setStars(stars);
        session.setPointsGained(pointsGained);
        session.setDurationMs(req.durationMs() != null && req.durationMs() > 0 ? req.durationMs() : durationSum);
        gameSessionMapper.insert(session);

        // 2. 逐题作答记录
        for (AnswerRecord record : records) {
            record.setSessionId(session.getId());
            answerRecordMapper.insert(record);
        }

        // 3. 进度更新（只有首次通关才写 first_passed_at）
        saveProgress(childId, req.levelId(), progress, stars, accuracyValue, pointsGained, passed);

        // 4. 积分与流水
        if (correctPoints > 0) {
            pointService.add(childId, correctPoints, correctPoints, PointService.BIZ_ANSWER, session.getId(),
                    "答对 " + correctCount + " 题");
        }
        if (comboBonusTotal > 0) {
            pointService.add(childId, comboBonusTotal, comboBonusTotal, PointService.BIZ_COMBO, session.getId(),
                    "连对 " + rules.comboSize() + " 题加成");
        }
        if (passBonus > 0) {
            pointService.add(childId, passBonus, passBonus, PointService.BIZ_PASS, req.levelId(), "首次通关奖励");
        }
        if (threeStarBonus > 0) {
            pointService.add(childId, threeStarBonus, threeStarBonus, PointService.BIZ_THREE_STAR, req.levelId(),
                    "三星通关奖励");
        }

        Child latest = childAuthService.require(childId);
        Long nextLevelId = passed ? gameMapService.nextLevelId(req.levelId()) : null;

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("correct", correctPoints);
        breakdown.put("combo", comboBonusTotal);
        breakdown.put("passBonus", passBonus);
        breakdown.put("threeStarBonus", threeStarBonus);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("levelId", req.levelId());
        result.put("totalCount", totalCount);
        result.put("correctCount", correctCount);
        result.put("accuracy", accuracyValue);
        result.put("stars", stars);
        result.put("passed", passed);
        result.put("firstPass", firstPass);
        result.put("pointsGained", pointsGained);
        result.put("pointsBreakdown", breakdown);
        result.put("points", latest.getTotalPoints());
        result.put("nextLevelId", nextLevelId);
        result.put("details", details);
        result.put("today", todayNode(dailyQuotaService.usage(childId)));
        return result;
    }

    private void saveProgress(Long childId, Long levelId, Progress progress, int stars,
                              BigDecimal accuracy, int score, boolean passed) {
        if (progress == null) {
            progress = new Progress();
            progress.setChildId(childId);
            progress.setLevelId(levelId);
            progress.setStars(stars);
            progress.setBestAccuracy(accuracy);
            progress.setBestScore(score);
            progress.setAttemptCount(1);
            progress.setStatus(passed ? GameMapService.STATUS_PASSED : GameMapService.STATUS_UNLOCKED);
            if (passed) {
                progress.setFirstPassedAt(java.time.LocalDateTime.now());
            }
            progressMapper.insert(progress);
            return;
        }

        Progress update = new Progress();
        update.setId(progress.getId());
        update.setStatus(passed || GameMapService.STATUS_PASSED.equals(progress.getStatus())
                ? GameMapService.STATUS_PASSED : GameMapService.STATUS_UNLOCKED);
        update.setStars(Math.max(progress.getStars() == null ? 0 : progress.getStars(), stars));
        update.setBestAccuracy(progress.getBestAccuracy() == null || accuracy.compareTo(progress.getBestAccuracy()) > 0
                ? accuracy : progress.getBestAccuracy());
        update.setBestScore(Math.max(progress.getBestScore() == null ? 0 : progress.getBestScore(), score));
        update.setAttemptCount((progress.getAttemptCount() == null ? 0 : progress.getAttemptCount()) + 1);
        if (passed && progress.getFirstPassedAt() == null) {
            update.setFirstPassedAt(java.time.LocalDateTime.now());
        }
        progressMapper.updateById(update);
    }

    /**
     * 把前端提交的作答统一转成合法 JSON 字符串（user_answer 是 JSON 列，必须合法）
     */
    private String toJsonAnswer(String raw, String type) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        JsonNode node = questionJudge.read(trimmed);
        if (node != null && !node.isNull()) {
            // 填空题的答案始终按文本比对，避免 "3" 被解析成数字
            if ("BLANK".equalsIgnoreCase(type) && !node.isTextual()) {
                return quote(node.asText());
            }
            return trimmed;
        }
        return quote(trimmed);
    }

    private String quote(String text) {
        try {
            return objectMapper.writeValueAsString(text);
        } catch (Exception e) {
            return "\"\"";
        }
    }

    private Map<String, Object> todayNode(DailyQuotaService.DailyUsage usage) {
        Map<String, Object> today = new LinkedHashMap<>();
        today.put("answeredToday", usage.answeredToday());
        today.put("usedMinutesToday", usage.usedMinutesToday());
        today.put("questionLimit", usage.questionLimit());
        today.put("minuteLimit", usage.minuteLimit());
        today.put("remainingQuestions", usage.remainingQuestions());
        today.put("remainingMinutes", usage.remainingMinutes());
        today.put("limitReached", dailyQuotaService.isExhausted(usage));
        return today;
    }
}