package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端仪表盘与统计报表（需求文档 8.2 /dashboard、/stats）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(adminStatsService.dashboard());
    }

    /**
     * 统计报表
     *
     * @param childId 为空表示全部孩子
     * @param days    最近天数，为空或 0 表示全部时间
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(required = false) Long childId,
                                             @RequestParam(required = false) Integer days) {
        return Result.ok(adminStatsService.stats(childId, days));
    }
}