package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Child;
import com.xxs.game.service.AdminChildService;
import com.xxs.game.service.AdminProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端孩子档案管理（需求文档 8.2：含重置 PIN、调分、重置进度）
 */
@RestController
@RequestMapping("/api/admin/child")
@RequiredArgsConstructor
public class AdminChildController {

    private final AdminChildService adminChildService;

    private final AdminProgressService adminProgressService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(adminChildService.list(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<Child> get(@PathVariable Long id) {
        return Result.ok(adminChildService.get(id));
    }

    @PostMapping
    public Result<Child> create(@Valid @RequestBody AdminDtos.ChildSaveReq req) {
        return Result.ok(adminChildService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Child> update(@PathVariable Long id, @Valid @RequestBody AdminDtos.ChildSaveReq req) {
        return Result.ok(adminChildService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminChildService.delete(id);
        return Result.ok();
    }

    /** 重置 PIN */
    @PutMapping("/{id}/pin")
    public Result<Void> resetPin(@PathVariable Long id, @Valid @RequestBody AdminDtos.ResetPinReq req) {
        adminChildService.resetPin(id, req.pin());
        return Result.ok();
    }

    /** 手动调整积分（写流水） */
    @PutMapping("/{id}/points")
    public Result<Map<String, Object>> adjustPoints(@PathVariable Long id,
                                                    @Valid @RequestBody AdminDtos.AdjustPointsReq req) {
        return Result.ok(adminChildService.adjustPoints(id, req));
    }

    /** 重置某关进度 */
    @DeleteMapping("/{id}/progress/{levelId}")
    public Result<Map<String, Object>> resetLevelProgress(@PathVariable Long id, @PathVariable Long levelId) {
        return Result.ok(adminProgressService.resetLevel(id, levelId));
    }

    /** 重置全部进度 */
    @DeleteMapping("/{id}/progress")
    public Result<Map<String, Object>> resetAllProgress(@PathVariable Long id) {
        return Result.ok(adminProgressService.resetAll(id));
    }
}