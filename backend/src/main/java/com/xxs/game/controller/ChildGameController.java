package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.dto.ChildDtos;
import com.xxs.game.security.AuthContext;
import com.xxs.game.service.GameMapService;
import com.xxs.game.service.GameSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 孩子端：闯关地图与答题（需求文档 8.1）
 */
@RestController
@RequestMapping("/api/child")
@RequiredArgsConstructor
public class ChildGameController {

    private final GameMapService gameMapService;

    private final GameSessionService gameSessionService;

    /** 单元 → 课文 → 关卡 树 + 解锁状态 + 星级 */
    @GetMapping("/map")
    public Result<Map<String, Object>> map() {
        return Result.ok(gameMapService.map(AuthContext.require()));
    }

    /** 开始一关（校验额度，返回不含答案的题目） */
    @PostMapping("/session/start")
    public Result<Map<String, Object>> start(@Valid @RequestBody ChildDtos.StartReq req) {
        return Result.ok(gameSessionService.start(AuthContext.require(), req.levelId()));
    }

    /** 提交整关答案，返回结算 */
    @PostMapping("/session/submit")
    public Result<Map<String, Object>> submit(@Valid @RequestBody ChildDtos.SubmitReq req) {
        return Result.ok(gameSessionService.submit(AuthContext.require(), req));
    }
}