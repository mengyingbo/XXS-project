package com.xxs.game.common;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一响应体：{code, message, data}
 * code = 0 表示成功，非 0 表示失败
 */
@Getter
@Setter
public class Result<T> {

    /** 0 成功；非 0 失败 */
    private int code;

    private String message;

    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}