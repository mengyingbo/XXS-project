package com.xxs.game.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 孩子端鉴权：仅接受 audience=child 的 Token
 */
@Component
public class ChildAuthInterceptor extends AbstractAuthInterceptor {

    public ChildAuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(jwtUtil, objectMapper);
    }

    @Override
    protected String audience() {
        return JwtUtil.AUD_CHILD;
    }

    @Override
    protected String rejectMessage() {
        return "登录已过期，请重新选择档案并输入密码";
    }
}