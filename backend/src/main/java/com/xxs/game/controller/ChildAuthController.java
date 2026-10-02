package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.dto.ChildDtos;
import com.xxs.game.security.AuthContext;
import com.xxs.game.service.ChildAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 孩子端：档案 / 登录 / 个人信息（需求文档 8.1）
 */
@RestController
@RequestMapping("/api/child")
@RequiredArgsConstructor
public class ChildAuthController {

    private final ChildAuthService childAuthService;

    /** 档案列表（免登录） */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(childAuthService.list());
    }

    /** 新建档案（免登录） */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody ChildDtos.RegisterReq req) {
        return Result.ok(childAuthService.register(req));
    }

    /** PIN 登录（免登录） */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody ChildDtos.LoginReq req) {
        return Result.ok(childAuthService.login(req));
    }

    /** 当前孩子信息 + 今日额度（v2.0：subject=chinese|math，额度按科目分别计算） */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(@RequestParam(required = false) String subject) {
        return Result.ok(childAuthService.profile(AuthContext.require(), subject));
    }
}