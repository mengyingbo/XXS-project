package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端规则配置（需求文档 8.2 /config）
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final ConfigService configService;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(configService.listForAdmin());
    }

    /** 保存配置：入参为 {配置键: 配置值} */
    @PutMapping
    public Result<Void> update(@RequestBody Map<String, String> values) {
        configService.update(values);
        return Result.ok();
    }
}