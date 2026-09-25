package com.xxs.game.config;

import com.xxs.game.security.AdminAuthInterceptor;
import com.xxs.game.security.ChildAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 配置：注册鉴权拦截器、上传文件的静态访问映射
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ChildAuthInterceptor childAuthInterceptor;

    private final AdminAuthInterceptor adminAuthInterceptor;

    @Value("${xxs.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 孩子端：档案列表/注册/登录免登录，其余需 Child Token
        registry.addInterceptor(childAuthInterceptor)
                .addPathPatterns("/api/child/**")
                .excludePathPatterns(
                        "/api/child/list",
                        "/api/child/register",
                        "/api/child/login");

        // 管理端：登录免登录，其余需 Admin Token
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
            // 目录创建失败时仅影响上传与图片访问，不阻断启动
        }
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/api/files/**").addResourceLocations(location);
    }
}