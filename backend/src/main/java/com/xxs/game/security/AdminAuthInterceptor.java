package com.xxs.game.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 管理端鉴权：仅接受 audience=admin 的 Token
 */
@Component
public class AdminAuthInterceptor extends AbstractAuthInterceptor {

    public AdminAuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(jwtUtil, objectMapper);
    }

    @Override
    protected String audience() {
        return JwtUtil.AUD_ADMIN;
    }

    @Override
    protected String rejectMessage() {
        return "登录已过期，请重新登录后台";
    }
}