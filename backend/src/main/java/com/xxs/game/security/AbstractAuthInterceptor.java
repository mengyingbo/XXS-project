package com.xxs.game.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxs.game.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器基类：解析 Authorization: Bearer <token> 并写入 AuthContext
 */
@RequiredArgsConstructor
public abstract class AbstractAuthInterceptor implements HandlerInterceptor {

    protected final JwtUtil jwtUtil;

    protected final ObjectMapper objectMapper;

    /** 期望的 Token 受众 */
    protected abstract String audience();

    /** 未通过鉴权的提示语 */
    protected abstract String rejectMessage();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        Long id = token == null ? null : jwtUtil.parseSubjectId(token, audience());
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, rejectMessage())));
            return false;
        }
        AuthContext.set(id);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        String token = header.startsWith("Bearer ") ? header.substring(7) : header;
        token = token.trim();
        return token.isEmpty() ? null : token;
    }
}