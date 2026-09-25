package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.dto.ChildDtos;
import com.xxs.game.security.AuthContext;
import com.xxs.game.service.ChildPrizeService;
import com.xxs.game.service.ChildRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 孩子端：积分商城 / 我的 / 错题本（需求文档 8.1）
 */
@RestController
@RequestMapping("/api/child")
@RequiredArgsConstructor
public class ChildCenterController {

    private final ChildPrizeService childPrizeService;

    private final ChildRecordService childRecordService;

    /** 奖品列表 */
    @GetMapping("/prize/list")
    public Result<Map<String, Object>> prizeList() {
        return Result.ok(childPrizeService.list(AuthContext.require()));
    }

    /** 提交兑换申请 */
    @PostMapping("/prize/redeem")
    public Result<Map<String, Object>> redeem(@Valid @RequestBody ChildDtos.RedeemReq req) {
        return Result.ok(childPrizeService.redeem(AuthContext.require(), req.prizeId()));
    }

    /** 兑换记录 + 积分流水 + 答题统计 */
    @GetMapping("/records")
    public Result<Map<String, Object>> records() {
        return Result.ok(childRecordService.records(AuthContext.require()));
    }

    /** 错题本 */
    @GetMapping("/wrong-questions")
    public Result<List<Map<String, Object>>> wrongQuestions() {
        return Result.ok(childRecordService.wrongQuestions(AuthContext.require()));
    }
}