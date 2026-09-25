package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.common.BizException;
import com.xxs.game.dto.ChildDtos;
import com.xxs.game.entity.Child;
import com.xxs.game.mapper.ChildMapper;
import com.xxs.game.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 孩子端：档案列表 / 注册 / PIN 登录 / 个人信息（需求文档 6.1 F-H5-01、F-H5-02、F-H5-10）
 */
@Service
@RequiredArgsConstructor
public class ChildAuthService {

    /** PIN 连续错误达到该次数后锁定 */
    private static final int PIN_MAX_FAIL = 5;

    /** 锁定时长（分钟） */
    private static final int PIN_LOCK_MINUTES = 1;

    private final ChildMapper childMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final DailyQuotaService dailyQuotaService;

    private final ConfigService configService;

    /** 档案列表（仅 id / 昵称 / 头像） */
    public List<Map<String, Object>> list() {
        List<Child> children = childMapper.selectList(Wrappers.<Child>lambdaQuery()
                .eq(Child::getEnabled, true)
                .orderByAsc(Child::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Child child : children) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", child.getId());
            node.put("nickname", child.getNickname());
            node.put("avatar", child.getAvatar());
            result.add(node);
        }
        return result;
    }

    /** 新建档案 */
    @Transactional
    public Map<String, Object> register(ChildDtos.RegisterReq req) {
        Child child = new Child();
        child.setNickname(req.nickname().trim());
        child.setAvatar(req.avatar() == null ? "" : req.avatar().trim());
        child.setPinHash(passwordEncoder.encode(req.pin()));
        child.setTotalPoints(0);
        child.setTotalEarned(0);
        child.setPinFailCount(0);
        child.setEnabled(true);
        childMapper.insert(child);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", child.getId());
        result.put("nickname", child.getNickname());
        result.put("avatar", child.getAvatar());
        return result;
    }

    /**
     * PIN 登录：错误 5 次锁定 1 分钟
     * 注意：本方法不能加 @Transactional —— 校验失败时要先把错误次数落库再抛异常，
     * 若处于事务中，抛异常会回滚掉错误次数，导致永远锁不上
     */
    public Map<String, Object> login(ChildDtos.LoginReq req) {
        Child child = childMapper.selectById(req.childId());
        if (child == null) {
            throw BizException.notFound("档案不存在，请重新选择");
        }
        if (!Boolean.TRUE.equals(child.getEnabled())) {
            throw new BizException(403, "该档案已被家长停用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (child.getLockedUntil() != null && child.getLockedUntil().isAfter(now)) {
            long seconds = java.time.Duration.between(now, child.getLockedUntil()).getSeconds() + 1;
            throw new BizException(403, "密码输错太多次啦，请等 " + seconds + " 秒后再试");
        }

        if (!passwordEncoder.matches(req.pin(), child.getPinHash())) {
            int failCount = (child.getPinFailCount() == null ? 0 : child.getPinFailCount()) + 1;
            Child update = new Child();
            update.setId(child.getId());
            if (failCount >= PIN_MAX_FAIL) {
                update.setPinFailCount(0);
                update.setLockedUntil(now.plusMinutes(PIN_LOCK_MINUTES));
            } else {
                update.setPinFailCount(failCount);
            }
            childMapper.updateById(update);
            if (failCount >= PIN_MAX_FAIL) {
                throw new BizException(403, "密码连续输错 " + PIN_MAX_FAIL + " 次，已锁定 " + PIN_LOCK_MINUTES + " 分钟");
            }
            throw new BizException(403, "密码不对哦，还可以再试 " + (PIN_MAX_FAIL - failCount) + " 次");
        }

        // 登录成功：清空错误次数与锁定（locked_until 需显式置 null，updateById 会忽略 null 字段）
        childMapper.update(null, Wrappers.<Child>lambdaUpdate()
                .eq(Child::getId, child.getId())
                .set(Child::getPinFailCount, 0)
                .set(Child::getLockedUntil, null));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtUtil.create(child.getId(), JwtUtil.AUD_CHILD));
        result.put("expiresIn", jwtUtil.getExpireSeconds());
        result.put("child", basicInfo(child));
        return result;
    }

    /** 当前孩子信息 + 今日已答题数/时长 + 剩余额度 */
    public Map<String, Object> profile(Long childId) {
        Child child = require(childId);
        DailyQuotaService.DailyUsage usage = dailyQuotaService.usage(childId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("child", basicInfo(child));
        result.put("points", child.getTotalPoints());
        result.put("totalEarned", child.getTotalEarned());

        Map<String, Object> today = new LinkedHashMap<>();
        today.put("answeredToday", usage.answeredToday());
        today.put("usedMinutesToday", usage.usedMinutesToday());
        today.put("questionLimit", usage.questionLimit());
        today.put("minuteLimit", usage.minuteLimit());
        today.put("remainingQuestions", usage.remainingQuestions());
        today.put("remainingMinutes", usage.remainingMinutes());
        today.put("limitReached", dailyQuotaService.isExhausted(usage));
        result.put("today", today);

        result.put("showAnalysisImmediately", configService.getBool(ConfigService.SHOW_ANALYSIS_IMMEDIATELY));
        return result;
    }

    /** 取孩子档案，不存在或停用则报错 */
    public Child require(Long childId) {
        Child child = childMapper.selectById(childId);
        if (child == null) {
            throw BizException.notFound("档案不存在");
        }
        return child;
    }

    private Map<String, Object> basicInfo(Child child) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", child.getId());
        node.put("nickname", child.getNickname());
        node.put("avatar", child.getAvatar());
        return node;
    }
}