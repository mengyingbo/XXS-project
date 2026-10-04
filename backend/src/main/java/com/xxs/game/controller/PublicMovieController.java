package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.entity.Movie;
import com.xxs.game.service.AdminMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 电影片库公开接口（需求文档 07 F-H5-15）
 * 供 H5 端免登录获取已上架电影列表，路径 /api/movie 不被鉴权拦截器覆盖
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicMovieController {

    private final AdminMovieService adminMovieService;

    /** 获取已上架电影列表（按 sort_order, id 升序） */
    @GetMapping("/movie")
    public Result<List<Movie>> listMovies() {
        return Result.ok(adminMovieService.listEnabledMovies());
    }
}
