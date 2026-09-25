package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Question;
import com.xxs.game.service.AdminCatalogService;
import com.xxs.game.service.QuestionImportService;
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
 * 管理端题库维护与批量导入（需求文档 8.2）
 */
@RestController
@RequestMapping("/api/admin/question")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final AdminCatalogService adminCatalogService;

    private final QuestionImportService questionImportService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(required = false) Long lessonId,
                                            @RequestParam(required = false) Long levelId,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(adminCatalogService.listQuestions(page, size, lessonId, levelId, type, keyword));
    }

    @GetMapping("/{id}")
    public Result<Question> get(@PathVariable Long id) {
        return Result.ok(adminCatalogService.getQuestion(id));
    }

    @PostMapping
    public Result<Question> create(@Valid @RequestBody AdminDtos.QuestionSaveReq req) {
        return Result.ok(adminCatalogService.createQuestion(req));
    }

    @PutMapping("/{id}")
    public Result<Question> update(@PathVariable Long id, @Valid @RequestBody AdminDtos.QuestionSaveReq req) {
        return Result.ok(adminCatalogService.updateQuestion(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminCatalogService.deleteQuestion(id);
        return Result.ok();
    }

    /** 批量导入：dryRun=true（默认）仅校验预览，false 则校验通过的行入库 */
    @PostMapping("/import")
    public Result<Map<String, Object>> importQuestions(@Valid @RequestBody AdminDtos.ImportReq req) {
        return Result.ok(questionImportService.importQuestions(req));
    }

    /** CSV 模板内容（供后台下载） */
    @GetMapping("/import/template")
    public Result<String> template() {
        return Result.ok(questionImportService.template());
    }
}