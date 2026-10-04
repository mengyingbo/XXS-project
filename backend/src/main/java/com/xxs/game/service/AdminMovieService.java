package com.xxs.game.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxs.game.common.BizException;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Movie;
import com.xxs.game.mapper.MovieMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端电影片库管理（需求文档 07 F-AD-12，v3.3 优化迭代）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMovieService {

    private static final String FILES_PREFIX = "/api/files/";

    private final MovieMapper movieMapper;

    @Value("${xxs.upload.dir:./uploads}")
    private String uploadDir;

    // ---------------- 管理端 CRUD ----------------

    public Map<String, Object> listMovies(int page, int size, String keyword, Boolean enabled) {
        IPage<Movie> result = movieMapper.selectPage(new Page<>(page, size),
                Wrappers.<Movie>lambdaQuery()
                        .eq(enabled != null, Movie::getEnabled, enabled)
                        // v3.3：关键词模糊匹配电影名/类型/主题
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Movie::getName, keyword)
                                .or().like(Movie::getType, keyword)
                                .or().like(Movie::getTheme, keyword))
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
        String name = req.name().trim();
        checkNameUnique(name, null);
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
        checkNameUnique(req.name().trim(), id);
        applyMovie(movie, req);
        movieMapper.updateById(movie);
        return movie;
    }

    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) {
            throw BizException.notFound("电影不存在");
        }
        movieMapper.deleteById(id);
        // v3.3：删除本地上传的封面文件（无其他电影引用时）
        deleteCoverFileIfUnused(movie.getCover());
    }

    /**
     * v3.3：上移/下移排序。按 sort_order, id 全序取出，与相邻项交换位置，
     * 然后按新位置规范化重写全部 sort_order（0,1,2…），消除并列/空值问题。
     */
    @Transactional
    public void moveMovie(Long id, String dir) {
        boolean up = "up".equals(dir);
        if (!up && !"down".equals(dir)) {
            throw BizException.badRequest("dir 仅支持 up/down");
        }
        List<Movie> all = movieMapper.selectList(Wrappers.<Movie>lambdaQuery()
                .orderByAsc(Movie::getSortOrder)
                .orderByAsc(Movie::getId));
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(id)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            throw BizException.notFound("电影不存在");
        }
        int target = up ? idx - 1 : idx + 1;
        if (target < 0 || target >= all.size()) {
            return; // 已在边界，无需移动
        }
        Movie tmp = all.get(idx);
        all.set(idx, all.get(target));
        all.set(target, tmp);
        for (int i = 0; i < all.size(); i++) {
            Movie m = all.get(i);
            if (m.getSortOrder() == null || m.getSortOrder() != i) {
                Movie upd = new Movie();
                upd.setId(m.getId());
                upd.setSortOrder(i);
                movieMapper.updateById(upd);
            }
        }
    }

    private void checkNameUnique(String name, Long excludeId) {
        Long count = movieMapper.selectCount(Wrappers.<Movie>lambdaQuery()
                .eq(Movie::getName, name)
                .ne(excludeId != null, Movie::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.badRequest("已存在同名电影「" + name + "」，请勿重复添加");
        }
    }

    private void deleteCoverFileIfUnused(String cover) {
        if (cover == null || !cover.startsWith(FILES_PREFIX)) {
            return;
        }
        // 仍被其他电影引用则不删
        Long used = movieMapper.selectCount(Wrappers.<Movie>lambdaQuery()
                .eq(Movie::getCover, cover));
        if (used != null && used > 0) {
            return;
        }
        try {
            String fileName = cover.substring(FILES_PREFIX.length());
            if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
                return;
            }
            Path file = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName);
            if (Files.isRegularFile(file)) {
                Files.delete(file);
                log.info("已清理电影封面文件: {}", file);
            }
        } catch (Exception e) {
            log.warn("清理电影封面文件失败: {}", e.getMessage());
        }
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
