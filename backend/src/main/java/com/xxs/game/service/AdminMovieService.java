package com.xxs.game.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxs.game.common.BizException;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Movie;
import com.xxs.game.mapper.MovieMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端电影片库管理（需求文档 07 F-AD-12）
 */
@Service
@RequiredArgsConstructor
public class AdminMovieService {

    private final MovieMapper movieMapper;

    // ---------------- 管理端 CRUD ----------------

    public Map<String, Object> listMovies(int page, int size, String keyword, Boolean enabled) {
        IPage<Movie> result = movieMapper.selectPage(new Page<>(page, size),
                Wrappers.<Movie>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), Movie::getName, keyword)
                        .eq(enabled != null, Movie::getEnabled, enabled)
                        .orderByAsc(Movie::getSortOrder)
                        .orderByAsc(Movie::getId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        data.put("records", result.getRecords());
        return data;
    }

    @Transactional
    public Movie createMovie(AdminDtos.MovieSaveReq req) {
        Movie movie = new Movie();
        applyMovie(movie, req);
        movieMapper.insert(movie);
        return movie;
    }

    @Transactional
    public Movie updateMovie(Long id, AdminDtos.MovieSaveReq req) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) {
            throw BizException.notFound("电影不存在");
        }
        applyMovie(movie, req);
        movieMapper.updateById(movie);
        return movie;
    }

    @Transactional
    public void deleteMovie(Long id) {
        if (movieMapper.selectById(id) == null) {
            throw BizException.notFound("电影不存在");
        }
        movieMapper.deleteById(id);
    }

    private void applyMovie(Movie movie, AdminDtos.MovieSaveReq req) {
        movie.setNo(req.no() == null ? 0 : req.no());
        movie.setName(req.name().trim());
        movie.setType(req.type() == null ? "" : req.type().trim());
        movie.setDuration(req.duration() == null ? 0 : req.duration());
        movie.setRating(req.rating() == null ? BigDecimal.ZERO : req.rating());
        movie.setTheme(req.theme() == null ? "" : req.theme().trim());
        movie.setNote(req.note() == null ? "" : req.note().trim());
        movie.setCover(req.cover() == null ? "" : req.cover().trim());
        if (req.watched() != null) {
            movie.setWatched(req.watched());
        } else if (movie.getId() == null) {
            movie.setWatched(false);
        }
        if (req.enabled() != null) {
            movie.setEnabled(req.enabled());
        } else if (movie.getId() == null) {
            movie.setEnabled(true);
        }
        movie.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    }

    // ---------------- 公开接口（H5 端） ----------------

    /** 获取已上架电影列表（按 sort_order, id 升序） */
    public List<Movie> listEnabledMovies() {
        return movieMapper.selectList(Wrappers.<Movie>lambdaQuery()
                .eq(Movie::getEnabled, true)
                .orderByAsc(Movie::getSortOrder)
                .orderByAsc(Movie::getId));
    }
}
