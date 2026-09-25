package com.xxs.game.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动初始化：默认管理员 admin / admin123（BCrypt 入库，首次登录强制改密）
 * 仅在 admin_user 表为空时写入；表不存在等异常只告警，不阻断服务启动。
 */
@Component
@RequiredArgsConstructor
public class DataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrap.class);

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final JdbcTemplate jdbcTemplate;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_user", Integer.class);
            if (count != null && count > 0) {
                log.info("管理员账号已存在（{} 个），跳过初始化", count);
                return;
            }
            jdbcTemplate.update(
                    "INSERT INTO admin_user (username, password_hash, nickname, must_change_pwd) VALUES (?, ?, ?, ?)",
                    DEFAULT_USERNAME,
                    passwordEncoder.encode(DEFAULT_PASSWORD),
                    "家长",
                    1);
            log.info("已初始化默认管理员：{} / {}（首次登录需修改密码）", DEFAULT_USERNAME, DEFAULT_PASSWORD);
        } catch (Exception e) {
            log.warn("初始化默认管理员失败（请确认已执行 sql/01_schema.sql）：{}", e.getMessage());
        }
    }
}