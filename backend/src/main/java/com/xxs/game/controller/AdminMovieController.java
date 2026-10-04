package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Movie;
import com.xxs.game.service.AdminMovieService;
import com.xxs.game.service.DoubanMovieService;
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
import java.util.Map;

/**
 * 管理端电影片库管理（需求文档 07 F-AD-12）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMovieController {

    private final AdminMovieService adminMovieService;

    private final DoubanMovieService doubanMovieService;

    @GetMapping("/movie")
    public Result<Map<String, Object>> listMovies(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Boolean enabled) {
        return Result.ok(adminMovieService.listMovies(page, size, keyword, enabled));
    }

    @PostMapping("/movie")
    public Result<Movie> createMovie(@Valid @RequestBody AdminDtos.MovieSaveReq req) {
        return Result.ok(adminMovieService.createMovie(req));
    }

    @PutMapping("/movie/{id}")
    public Result<Movie> updateMovie(@PathVariable Long id, @Valid @RequestBody AdminDtos.MovieSaveReq req) {
        return Result.ok(adminMovieService.updateMovie(id, req));
    }

    @PutMapping("/movie/{id}/move")
    public Result<Void> moveMovie(@PathVariable Long id, @RequestParam String dir) {
        adminMovieService.moveMovie(id, dir);
        return Result.ok();
    }

    @DeleteMapping("/movie/{id}")
    public Result<Void> deleteMovie(@PathVariable Long id) {
        adminMovieService.deleteMovie(id);
        return Result.ok();
    }

    // ---------------- 豆瓣检索 ----------------

    @GetMapping("/movie/search")
    public Result<List<AdminDtos.DoubanCandidate>> searchMovies(@RequestParam String name) {
        return Result.ok(doubanMovieService.search(name));
    }

    @GetMapping("/movie/fetch")
    public Result<AdminDtos.DoubanMovieInfo> fetchMovieDetail(@RequestParam String url) {
        return Result.ok(doubanMovieService.fetchDetail(url));
    }
}
