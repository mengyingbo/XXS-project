package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.common.BizException;
import com.xxs.game.entity.Child;
import com.xxs.game.entity.PointLog;
import com.xxs.game.mapper.ChildMapper;
import com.xxs.game.mapper.PointLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 积分变更统一出口：改 child 余额 + 写 point_log 流水
 */
@Service
@RequiredArgsConstructor
public class PointService {

    /** 业务类型：答题得分 */
    public static final String BIZ_ANSWER = "ANSWER";
    /** 通关奖励 */
    public static final String BIZ_PASS = "PASS";
    /** 三星奖励 */
    public static final String BIZ_THREE_STAR = "THREE_STAR";
    /** 连对加成 */
    public static final String BIZ_COMBO = "COMBO";
    /** 兑换扣分 */
    public static final String BIZ_REDEEM = "REDEEM";
    /** 拒绝兑换退回 */
    public static final String BIZ_REJECT_REFUND = "REJECT_REFUND";
    /** 家长手动调分 */
    public static final String BIZ_ADMIN_ADJUST = "ADMIN_ADJUST";

    private final ChildMapper childMapper;

    private final PointLogMapper pointLogMapper;

    /**
     * 加分
     *
     * @param earnedDelta 计入「累计获得积分」的部分（家长手动加分为 0）
     * @return 变更后余额
     */
    @Transactional
    public int add(Long childId, int delta, int earnedDelta, String bizType, Long refId, String remark) {
        if (delta != 0) {
            childMapper.addPoints(childId, delta, earnedDelta);
        } else if (earnedDelta != 0) {
            childMapper.addPoints(childId, 0, earnedDelta);
        }
        Child child = childMapper.selectById(childId);
        if (child == null) {
            throw BizException.notFound("孩子档案不存在");
        }
        writeLog(childId, delta, child.getTotalPoints(), bizType, refId, remark);
        return child.getTotalPoints();
    }

    /**
     * 扣分（带余额校验，防止并发下扣成负数）
     *
     * @return 变更后余额
     */
    @Transactional
    public int deduct(Long childId, int cost, String bizType, Long refId, String remark) {
        int rows = childMapper.deductPoints(childId, cost);
        if (rows == 0) {
            throw BizException.conflict("积分不足");
        }
        Child child = childMapper.selectById(childId);
        writeLog(childId, -cost, child.getTotalPoints(), bizType, refId, remark);
        return child.getTotalPoints();
    }

    private void writeLog(Long childId, int delta, int balanceAfter, String bizType, Long refId, String remark) {
        PointLog log = new PointLog();
        log.setChildId(childId);
        log.setChangeAmount(delta);
        log.setBalanceAfter(balanceAfter);
        log.setBizType(bizType);
        log.setRefId(refId);
        log.setRemark(remark == null ? "" : remark);
        pointLogMapper.insert(log);
    }

    /** 最近积分流水 */
    public List<PointLog> recent(Long childId, int limit) {
        return pointLogMapper.selectList(Wrappers.<PointLog>lambdaQuery()
                .eq(PointLog::getChildId, childId)
                .orderByDesc(PointLog::getId)
                .last("LIMIT " + limit));
    }
}