package com.xxs.game.service;

import com.xxs.game.common.BizException;
import com.xxs.game.dto.AdminDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 豆瓣电影信息检索（需求文档 08 F-AD-13）
 * 1. 搜索：GET 豆瓣搜索页 → 正则解析候选列表
 * 2. 详情：GET 电影详情页 → 如遇反爬挑战则解 SHA-512 工作量证明 → 解析类型/片长/评分/简介/海报
 * 3. 海报下载：下载到 uploads 目录，返回 /api/files/xxx.jpg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoubanMovieService {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .cookieHandler(new CookieManager())
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${xxs.upload.dir:./uploads}")
    private String uploadDir;

    // ======================== 搜索 ========================

    /**
     * 搜索豆瓣电影，返回候选列表
     */
    public List<AdminDtos.DoubanCandidate> search(String name) {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String url = "https://www.douban.com/search?cat=1002&q=" + encoded;

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", UA)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String html = resp.body();
            return parseSearchResults(html);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("豆瓣搜索失败: {}", e.getMessage());
            throw BizException.badRequest("豆瓣搜索失败，请稍后重试或手动填写");
        }
    }

    /**
     * 解析豆瓣搜索结果页 HTML
     */
    private List<AdminDtos.DoubanCandidate> parseSearchResults(String html) {
        List<AdminDtos.DoubanCandidate> list = new ArrayList<>();

        // 每个 <div class="result"> 到下一个 </div>\n\n  为一条结果
        // 提取关键信息：海报、标题、评分、subject URL、演员年份、简介
        Pattern resultPattern = Pattern.compile(
                "<div class=\"result\">(.*?)</div>\\s*</div>\\s*</div>",
                Pattern.DOTALL);
        Matcher m = resultPattern.matcher(html);

        while (m.find() && list.size() < 10) {
            String block = m.group(1);

            // 海报
            String poster = extractGroup(block,
                    "<img src=\"(https://img\\d+\\.doubanio\\.com/[^\"]+)\"");
            if (poster == null) continue;

            // subject URL（从 link2 跳转链接中提取真实 URL）
            String link2 = extractGroup(block,
                    "href=\"https://www\\.douban\\.com/link2/\\?url=([^\"&]+)");
            String subjectUrl = null;
            if (link2 != null) {
                subjectUrl = java.net.URLDecoder.decode(link2, StandardCharsets.UTF_8);
            }
            if (subjectUrl == null || !subjectUrl.contains("movie.douban.com/subject/")) continue;

            // 标题（在 <a> 标签内，紧跟 <span>[电影]</span>）
            String title = extractGroup(block,
                    "<span>\\[电影\\]</span>\\s*&nbsp;\\s*<a[^>]*>([^<]+)</a>");
            if (title != null) title = title.trim();

            // 评分
            String rating = extractGroup(block,
                    "class=\"rating_nums\">([^<]+)<");
            if (rating != null) rating = rating.trim();

            // 演员/年份信息
            String cast = extractGroup(block,
                    "class=\"subject-cast\">([^<]+)</span>");
            if (cast != null) cast = cast.trim();

            // 解析年份和导演
            String year = "";
            String director = "";
            if (cast != null && !cast.isEmpty()) {
                // 格式：原名:xxx / 导演 / 演员 / 年份
                String[] parts = cast.split(" / ");
                if (parts.length > 0) {
                    // 最后一个通常是年份
                    String last = parts[parts.length - 1].trim();
                    if (last.matches("\\d{4}")) year = last;
                    // 第二个通常是导演（如果原名存在则从 index 1 开始，否则从 index 0）
                    int dirIdx = cast.contains("原名:") ? 1 : 0;
                    if (parts.length > dirIdx) director = parts[dirIdx].trim();
                }
            }

            // 简介
            String summary = extractGroup(block, "<p>(.*?)</p>", Pattern.DOTALL);
            if (summary != null) summary = summary.trim();

            list.add(new AdminDtos.DoubanCandidate(
                    title != null ? title : "未知",
                    year,
                    rating,
                    downloadPoster(poster),  // 下载到本地，避免豆瓣外链防盗链
                    subjectUrl,
                    director,
                    summary
            ) );
        }

        return list;
    }

    // ======================== 详情 ========================

    /**
     * 获取电影详情（含反爬破解 + 海报下载）
     */
    public AdminDtos.DoubanMovieInfo fetchDetail(String subjectUrl) {
        try {
            String html = fetchWithChallenge(subjectUrl);
            return parseDetailPage(html, subjectUrl);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("豆瓣详情获取失败: {}", e.getMessage());
            throw BizException.badRequest("豆瓣详情获取失败，请手动填写");
        }
    }

    /**
     * 获取详情页 HTML，如遇反爬挑战则解 SHA-512 工作量证明
     */
    private String fetchWithChallenge(String targetUrl) throws Exception {
        // 第一次请求（HttpClient 会自动跟随 302 重定向）
        // 豆瓣详情页会 302 到 sec.douban.com/c?r=... → 返回挑战页
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", "https://www.douban.com/")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String html = resp.body();
        // 记录最终 URL（重定向后可能在 sec.douban.com）
        String finalUrl = resp.uri().toString();

        // 检查是否是挑战页
        if (html.contains("tok") && html.contains("cha") && html.contains("sha512")) {
            log.info("豆瓣反爬挑战（来自 {}），开始解 SHA-512 工作量证明...", finalUrl);
            html = solveChallengeAndFetch(finalUrl, html);
        }

        return html;
    }

    /**
     * 解 SHA-512 工作量证明并重新获取页面
     * @param challengePageUrl 挑战页的 URL（重定向后的实际 URL，如 sec.douban.com/c?r=...）
     */
    private String solveChallengeAndFetch(String challengePageUrl, String challengeHtml) throws Exception {
        // 提取 tok 和 cha
        String tok = extractGroup(challengeHtml, "id=\"tok\"[^>]*value=\"([^\"]+)\"");
        String cha = extractGroup(challengeHtml, "id=\"cha\"[^>]*value=\"([^\"]+)\"");
        String action = extractGroup(challengeHtml, "action=\"([^\"]+)\"");
        String red = extractGroup(challengeHtml, "id=\"red\"[^>]*value=\"([^\"]+)\"");
        if (tok == null || cha == null) {
            throw new RuntimeException("无法解析挑战参数");
        }
        if (red == null) red = challengePageUrl;

        // 解工作量证明：找 nonce 使得 SHA-512(cha + nonce) 以 "0000" 开头
        String sol = solveProofOfWork(cha, 4);
        log.info("挑战已解决, sol={}", sol);

        // 构造 POST URL：基于挑战页的实际 URL 拼接 action
        // action 是相对路径如 "/c"，挑战页可能在 sec.douban.com
        String postUrl;
        if (action != null && action.startsWith("http")) {
            postUrl = action;
        } else if (action != null) {
            // 从挑战页 URL 提取 origin（如 https://sec.douban.com）
            URI challengeUri = URI.create(challengePageUrl);
            String origin = challengeUri.getScheme() + "://" + challengeUri.getHost();
            postUrl = origin + action;
        } else {
            postUrl = challengePageUrl;
        }
        log.info("POST 挑战解到: {}", postUrl);

        // POST 表单数据
        String formBody = "tok=" + URLEncoder.encode(tok, StandardCharsets.UTF_8)
                + "&cha=" + URLEncoder.encode(cha, StandardCharsets.UTF_8)
                + "&sol=" + sol
                + "&red=" + URLEncoder.encode(red, StandardCharsets.UTF_8);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(postUrl))
                .header("User-Agent", UA)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", challengePageUrl)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> resp = HTTP_CLIENT.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return resp.body();
    }

    /**
     * SHA-512 工作量证明：找 nonce 使 hash 前 difficulty 位为 '0'
     */
    private String solveProofOfWork(String cha, int difficulty) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            String target = "0".repeat(difficulty);
            for (int nonce = 1; nonce <= 10_000_000; nonce++) {
                String input = cha + nonce;
                byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder();
                for (byte b : hash) {
                    hex.append(String.format("%02x", b));
                }
                if (hex.substring(0, difficulty).equals(target)) {
                    return String.valueOf(nonce);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("SHA-512 计算失败", e);
        }
        throw new RuntimeException("工作量证明未能在合理范围内求解");
    }

    /**
     * 解析详情页 HTML
     */
    private AdminDtos.DoubanMovieInfo parseDetailPage(String html, String subjectUrl) {
        // 标题：豆瓣详情页 <h1> 内可能有 <span> 标签
        // 优先从 <title> 提取（格式：放牛班的春天 (豆瓣)）
        String name = extractGroup(html, "<title>\\s*([^<]+?)\\s*\\(豆瓣\\)");
        if (name == null) {
            // 备选：从 h1 提取纯文本（去除嵌套标签后的内容）
            String h1Content = extractGroup(html, "<h1[^>]*>(.*?)</h1>", Pattern.DOTALL);
            if (h1Content != null) {
                name = h1Content.replaceAll("<[^>]+>", "").trim();
            }
        }
        if (name == null) name = "";

        // 评分：<strong class="ll rating_num" property="v:average">9.3</strong>
        String ratingStr = extractGroup(html, "class=\"ll rating_num\"[^>]*>([^<]+)<");
        double rating = 0.0;
        if (ratingStr != null) {
            try { rating = Double.parseDouble(ratingStr.trim()); } catch (Exception ignored) {}
        }

        // 类型：<span property="v:genre">剧情</span> <span property="v:genre">音乐</span>
        List<String> genres = new ArrayList<>();
        Pattern genrePattern = Pattern.compile("property=\"v:genre\">([^<]+)<");
        Matcher gm = genrePattern.matcher(html);
        while (gm.find()) genres.add(gm.group(1).trim());
        String type = String.join("/", genres);

        // 片长：<span property="v:runtime" content="97"></span> 或文本"片长: 97分钟"
        String durationStr = extractGroup(html, "property=\"v:runtime\"[^>]*content=\"(\\d+)\"");
        if (durationStr == null) {
            durationStr = extractGroup(html, "片长[^\\d]*(\\d+)\\s*分钟");
        }
        int duration = 0;
        if (durationStr != null) {
            try { duration = Integer.parseInt(durationStr); } catch (Exception ignored) {}
        }

        // 简介：<span property="v:summary">...</span>
        String summary = extractGroup(html, "property=\"v:summary\">(.*?)</span>", Pattern.DOTALL);
        if (summary != null) {
            summary = summary.replaceAll("<[^>]+>", "").trim();
        }
        if (summary == null || summary.isEmpty()) summary = "";

        // 海报：<img src="https://img3.doubanio.com/view/photo/l_ratio_poster/..." />
        String posterUrl = extractGroup(html,
                "id=\"mainpic\".*?<img[^>]*src=\"(https://img\\d+\\.doubanio\\.com/[^\"]+)\"",
                Pattern.DOTALL);
        if (posterUrl == null) {
            posterUrl = extractGroup(html,
                    "<img[^>]*src=\"(https://img\\d+\\.doubanio\\.com/view/photo/[^\"]+poster[^\"]*)\"");
        }

        // 下载海报到 uploads
        String coverPath = "";
        if (posterUrl != null && !posterUrl.isEmpty()) {
            coverPath = downloadPoster(posterUrl);
        }

        // 主题：豆瓣没有"推荐主题"概念，用类型(genres)预填，家长可编辑
        String theme = type;

        return new AdminDtos.DoubanMovieInfo(
                name, type, duration, rating, theme, summary, coverPath
        );
    }

    /**
     * 下载海报到 uploads 目录
     */
    private String downloadPoster(String posterUrl) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(posterUrl))
                    .header("User-Agent", UA)
                    .header("Referer", "https://movie.douban.com/")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<byte[]> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
            byte[] data = resp.body();

            // 从 Content-Type 或 URL 推断扩展名
            String contentType = resp.headers().firstValue("Content-Type").orElse("");
            String ext = "jpg";
            if (contentType.contains("png")) ext = "png";
            else if (contentType.contains("webp")) ext = "webp";
            else if (posterUrl.endsWith(".png")) ext = "png";
            else if (posterUrl.endsWith(".webp")) ext = "webp";

            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), data);

            log.info("海报已下载: {} -> {}", posterUrl, fileName);
            return "/api/files/" + fileName;
        } catch (Exception e) {
            log.warn("海报下载失败: {}", e.getMessage());
            return "";
        }
    }

    // ======================== 工具方法 ========================

    private String extractGroup(String text, String regex) {
        return extractGroup(text, regex, 0);
    }

    private String extractGroup(String text, String regex, int flags) {
        Pattern p = flags != 0 ? Pattern.compile(regex, flags) : Pattern.compile(regex);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }
}
