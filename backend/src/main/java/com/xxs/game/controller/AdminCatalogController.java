package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Lesson;
import com.xxs.game.entity.Level;
import com.xxs.game.entity.Unit;
import com.xxs.game.service.AdminCatalogService;
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

import java.util.List;

/**
 * 管理端单元 / 课文 / 关卡维护（需求文档 8.2）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService adminCatalogService;

    // ---------------- 单元 ----------------

    @GetMapping("/unit")
    public Result<List<Unit>> listUnits() {
        return Result.ok(adminCatalogService.listUnits());
    }

    @PostMapping("/unit")
    public Result<Unit> createUnit(@Valid @RequestBody AdminDtos.UnitSaveReq req) {
        return Result.ok(adminCatalogService.createUnit(req));
    }

    @PutMapping("/unit/{id}")
    public Result<Unit> updateUnit(@PathVariable Long id, @Valid @RequestBody AdminDtos.UnitSaveReq req) {
        return Result.ok(adminCatalogService.updateUnit(id, req));
    }

    @DeleteMapping("/unit/{id}")
    public Result<Void> deleteUnit(@PathVariable Long id) {
        adminCatalogService.deleteUnit(id);
        return Result.ok();
    }

    // ---------------- 课文 ----------------

    @GetMapping("/lesson")
    public Result<List<Lesson>> listLessons(@RequestParam(required = false) Long unitId) {
        return Result.ok(adminCatalogService.listLessons(unitId));
    }

    @PostMapping("/lesson")
    public Result<Lesson> createLesson(@Valid @RequestBody AdminDtos.LessonSaveReq req) {
        return Result.ok(adminCatalogService.createLesson(req));
    }

    @PutMapping("/lesson/{id}")
    public Result<Lesson> updateLesson(@PathVariable Long id, @Valid @RequestBody AdminDtos.LessonSaveReq req) {
        return Result.ok(adminCatalogService.updateLesson(id, req));
    }

    @DeleteMapping("/lesson/{id}")
    public Result<Void> deleteLesson(@PathVariable Long id) {
        adminCatalogService.deleteLesson(id);
        return Result.ok();
    }

    // ---------------- 关卡 ----------------

    @GetMapping("/level")
    public Result<List<Level>> listLevels(@RequestParam(required = false) Long lessonId) {
        return Result.ok(adminCatalogService.listLevels(lessonId));
    }

    @PostMapping("/level")
    public Result<Level> createLevel(@Valid @RequestBody AdminDtos.LevelSaveReq req) {
        return Result.ok(adminCatalogService.createLevel(req));
    }

    @PutMapping("/level/{id}")
    public Result<Level> updateLevel(@PathVariable Long id, @Valid @RequestBody AdminDtos.LevelSaveReq req) {
        return Result.ok(adminCatalogService.updateLevel(id, req));
    }

    @DeleteMapping("/level/{id}")
    public Result<Void> deleteLevel(@PathVariable Long id) {
        adminCatalogService.deleteLevel(id);
        return Result.ok();
    }
}