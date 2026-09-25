package com.xxs.game.controller;

import com.xxs.game.common.BizException;
import com.xxs.game.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 管理端文件上传（奖品图片，需求文档 6.2 F-AD-07）
 * 类型白名单 + 随机文件名，文件通过 /api/files/** 访问
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUploadController {

    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp");

    private static final long MAX_SIZE = 5 * 1024 * 1024L;

    @Value("${xxs.upload.dir:./uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BizException.badRequest("文件不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw BizException.badRequest("只支持 png / jpg / jpeg / gif / webp 图片");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName).toFile());
        } catch (IOException e) {
            throw new BizException(500, "文件保存失败：" + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", fileName);
        result.put("url", "/api/files/" + fileName);
        return Result.ok(result);
    }
}