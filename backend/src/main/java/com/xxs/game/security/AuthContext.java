package com.xxs.game.security;

/**
 * 当前登录主体（孩子 id 或管理员 id）的线程上下文
 */
public final class AuthContext {

    private static final ThreadLocal<Long> CURRENT_ID = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(Long id) {
        CURRENT_ID.set(id);
    }

    public static Long get() {
        return CURRENT_ID.get();
    }

    public static Long require() {
        Long id = CURRENT_ID.get();
        if (id == null) {
            throw new IllegalStateException("未登录");
        }
        return id;
    }

    public static void clear() {
        CURRENT_ID.remove();
    }
}