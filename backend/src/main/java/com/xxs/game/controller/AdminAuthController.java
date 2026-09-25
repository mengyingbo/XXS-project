package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.security.AuthContext;
import com.xxs.game.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端登录与改密（需求文档 8.2）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    /** 登录（免登录） */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AdminDtos.LoginReq req) {
        return Result.ok(adminAuthService.login(req));
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody AdminDtos.ChangePwdReq req) {
        adminAuthService.changePassword(AuthContext.require(), req);
        return Result.ok();
    }
}