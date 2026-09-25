package com.xxs.game.common;

import lombok.Getter;

/**
 * 业务异常：由 GlobalExceptionHandler 统一转成 Result 响应
 */
@Getter
public class BizException extends RuntimeException {

    /** 业务错误码：400 参数/业务错误，401 未登录，403 无权限，404 不存在，409 冲突 */
    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(400, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(401, message);
    }

    public static BizException notFound(String message) {
        return new BizException(404, message);
    }

    public static BizException conflict(String message) {
        return new BizException(409, message);
    }
}