package com.xxs.game.controller;

import com.xxs.game.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：验证服务存活 + 数据库连通
 * GET /api/health
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("app", "xxs-game");
        data.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            String dbVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                    Integer.class);

            Map<String, Object> db = new LinkedHashMap<>();
            db.put("connected", true);
            db.put("version", dbVersion);
            db.put("tables", tableCount);
            db.put("schema", jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));
            data.put("db", db);
            return Result.ok(data);
        } catch (Exception e) {
            Map<String, Object> db = new LinkedHashMap<>();
            db.put("connected", false);
            db.put("error", e.getMessage());
            data.put("db", db);
            return new Result<>(1, "数据库连接失败", data);
        }
    }
}