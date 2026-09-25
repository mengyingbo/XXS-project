package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.entity.AnswerRecord;
import com.xxs.game.entity.Child;
import com.xxs.game.entity.PointLog;
import com.xxs.game.entity.Progress;
import com.xxs.game.entity.RedeemOrder;
import com.xxs.game.mapper.AnswerRecordMapper;
import com.xxs.game.mapper.GameSessionMapper;
import com.xxs.game.mapper.ProgressMapper;
import com.xxs.game.mapper.RedeemOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 孩子端「我的」：答题统计 + 兑换记录 + 积分流水 + 错题本
 * （需求文档 6.1 F-H5-09，8.1 /records、/wrong-questions）
 */
@Service
@RequiredArgsConstructor
public class ChildRecordService {

    private static final int RECENT_LIMIT = 50;

    private static final int WRONG_LIMIT = 100;

    private final ChildAuthService childAuthService;

    private final AnswerRecordMapper answerRecordMapper;

    private final GameSessionMapper gameSessionMapper;

    private final ProgressMapper progressMapper;

    private final RedeemOrderMapper redeemOrderMapper;

    private final PointService pointService;

    /** 统计 + 兑换记录 + 积分流水 */
    public Map<String, Object> records(Long childId) {
        Child child = childAuthService.require(childId);

        long answered = answerRecordMapper.selectCount(Wrappers.<AnswerRecord>lambdaQuery()
                .eq(AnswerRecord::getChildId, childId));
        long correct = answerRecordMapper.selectCount(Wrappers.<AnswerRecord>lambdaQuery()
                .eq(AnswerRecord::getChildId, childId)
                .eq(AnswerRecord::getIsCorrect, true));
        long sessions = gameSessionMapper.selectCount(Wrappers.<com.xxs.game.entity.GameSession>lambdaQuery()
                .eq(com.xxs.game.entity.GameSession::getChildId, childId));

        List<Progress> progressList = progressMapper.selectList(Wrappers.<Progress>lambdaQuery()
                .eq(Progress::getChildId, childId));
        int totalStars = progressList.stream().mapToInt(p -> p.getStars() == null ? 0 : p.getStars()).sum();
        long passedLevels = progressList.stream()
                .filter(p -> GameMapService.STATUS_PASSED.equals(p.getStatus())).count();

        BigDecimal accuracy = answered == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(correct * 100.0 / answered).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("points", child.getTotalPoints());
        statistics.put("totalEarned", child.getTotalEarned());
        statistics.put("answeredTotal", answered);
        statistics.put("correctTotal", correct);
        statistics.put("accuracy", accuracy);
        statistics.put("sessionTotal", sessions);
        statistics.put("passedLevels", passedLevels);
        statistics.put("totalStars", totalStars);

        List<RedeemOrder> orders = redeemOrderMapper.selectList(Wrappers.<RedeemOrder>lambdaQuery()
                .eq(RedeemOrder::getChildId, childId)
                .orderByDesc(RedeemOrder::getId)
                .last("LIMIT " + RECENT_LIMIT));
        List<Map<String, Object>> orderNodes = new ArrayList<>();
        for (RedeemOrder order : orders) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", order.getId());
            node.put("prizeName", order.getPrizeName());
            node.put("pointsCost", order.getPointsCost());
            node.put("status", order.getStatus());
            node.put("remark", order.getRemark());
            node.put("createdAt", order.getCreatedAt());
            node.put("handledAt", order.getHandledAt());
            orderNodes.add(node);
        }

        List<Map<String, Object>> logNodes = new ArrayList<>();
        for (PointLog log : pointService.recent(childId, RECENT_LIMIT)) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", log.getId());
            node.put("changeAmount", log.getChangeAmount());
            node.put("balanceAfter", log.getBalanceAfter());
            node.put("bizType", log.getBizType());
            node.put("remark", log.getRemark());
            node.put("createdAt", log.getCreatedAt());
            logNodes.add(node);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("child", Map.of("id", child.getId(),
                "nickname", child.getNickname(),
                "avatar", child.getAvatar()));
        result.put("statistics", statistics);
        result.put("redeemOrders", orderNodes);
        result.put("pointLogs", logNodes);
        return result;
    }

    /** 错题本：当前仍未掌握（最近一次作答为错误）的题目 */
    public List<Map<String, Object>> wrongQuestions(Long childId) {
        childAuthService.require(childId);
        return answerRecordMapper.selectWrongQuestions(childId, WRONG_LIMIT);
    }
}