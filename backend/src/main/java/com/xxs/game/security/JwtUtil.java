package com.xxs.game.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：孩子端 Token 与 Admin Token 通过 audience 分离（需求文档 6.3）
 */
@Component
public class JwtUtil {

    /** 孩子端 Token 受众 */
    public static final String AUD_CHILD = "child";
    /** 管理端 Token 受众 */
    public static final String AUD_ADMIN = "admin";

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${xxs.jwt.secret}") String secret,
                   @Value("${xxs.jwt.expire-hours:12}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    /** 生成 Token；subject 为对应主体 id */
    public String create(Long subjectId, String audience) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(subjectId))
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 Token（含受众校验）
     *
     * @return 主体 id；Token 无效或受众不符返回 null
     */
    public Long parseSubjectId(String token, String expectedAudience) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** 有效期（秒），返回给前端便于自动登出 */
    public long getExpireSeconds() {
        return expireMillis / 1000;
    }
}