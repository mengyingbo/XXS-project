package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.entity.SysConfig;
import com.xxs.game.mapper.SysConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置读写（对应 sys_config 表，默认值见需求文档第 7 章）
 * 读多写少：内存缓存，后台修改后立即刷新
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    public static final String QUESTIONS_PER_LEVEL = "questions_per_level";
    public static final String DAILY_QUESTION_LIMIT = "daily_question_limit";
    public static final String DAILY_MINUTE_LIMIT = "daily_minute_limit";
    public static final String POINTS_PER_CORRECT = "points_per_correct";
    public static final String COMBO_SIZE = "combo_size";
    public static final String COMBO_BONUS = "combo_bonus";
    public static final String COMBO_ENABLED = "combo_enabled";
    public static final String POINTS_PER_LEVEL_PASS = "points_per_level_pass";
    public static final String POINTS_PER_THREE_STAR = "points_per_three_star";
    public static final String STAR3_RATE = "star3_rate";
    public static final String STAR2_RATE = "star2_rate";
    public static final String STAR1_RATE = "star1_rate";
    public static final String SHOW_ANALYSIS_IMMEDIATELY = "show_analysis_immediately";

    /** 配置项顺序与默认值（也是后台配置页的展示顺序） */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(QUESTIONS_PER_LEVEL, "5");
        DEFAULTS.put(DAILY_QUESTION_LIMIT, "20");
        DEFAULTS.put(DAILY_MINUTE_LIMIT, "20");
        DEFAULTS.put(POINTS_PER_CORRECT, "10");
        DEFAULTS.put(COMBO_SIZE, "3");
        DEFAULTS.put(COMBO_BONUS, "5");
        DEFAULTS.put(COMBO_ENABLED, "true");
        DEFAULTS.put(POINTS_PER_LEVEL_PASS, "20");
        DEFAULTS.put(POINTS_PER_THREE_STAR, "30");
        DEFAULTS.put(STAR3_RATE, "90");
        DEFAULTS.put(STAR2_RATE, "70");
        DEFAULTS.put(STAR1_RATE, "60");
        DEFAULTS.put(SHOW_ANALYSIS_IMMEDIATELY, "true");
    }

    private final SysConfigMapper sysConfigMapper;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    /** 从数据库重新加载配置（表不存在等异常时退回默认值，不阻断启动） */
    public void refresh() {
        Map<String, String> loaded = new ConcurrentHashMap<>();
        try {
            List<SysConfig> list = sysConfigMapper.selectList(null);
            for (SysConfig item : list) {
                if (item.getConfigKey() != null && item.getConfigValue() != null) {
                    loaded.put(item.getConfigKey(), item.getConfigValue());
                }
            }
        } catch (Exception e) {
            log.warn("读取 sys_config 失败，将使用默认配置：{}", e.getMessage());
        }
        cache.clear();
        cache.putAll(loaded);
    }

    public String getString(String key) {
        String value = cache.get(key);
        if (value == null || value.isBlank()) {
            return DEFAULTS.getOrDefault(key, "");
        }
        return value;
    }

    public int getInt(String key) {
        try {
            return Integer.parseInt(getString(key).trim());
        } catch (NumberFormatException e) {
            return Integer.parseInt(DEFAULTS.getOrDefault(key, "0"));
        }
    }

    public boolean getBool(String key) {
        return "true".equalsIgnoreCase(getString(key).trim());
    }

    /** 后台配置页展示：key / value / remark（不含隐藏项） */
    public List<Map<String, Object>> listForAdmin() {
        Map<String, String> remarks = new LinkedHashMap<>();
        try {
            for (SysConfig item : sysConfigMapper.selectList(null)) {
                remarks.put(item.getConfigKey(), item.getRemark());
            }
        } catch (Exception ignored) {
            // 忽略：仅用于展示备注
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        DEFAULTS.forEach((key, def) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("configKey", key);
            row.put("configValue", getString(key));
            row.put("defaultValue", def);
            row.put("remark", remarks.getOrDefault(key, ""));
            result.add(row);
        });
        return result;
    }

    /** 后台保存配置：仅接受已知配置项 */
    @Transactional
    public void update(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.forEach((key, value) -> {
            if (!DEFAULTS.containsKey(key) || value == null) {
                return;
            }
            String trimmed = value.trim();
            SysConfig exist = sysConfigMapper.selectOne(
                    Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, key));
            if (exist == null) {
                SysConfig insert = new SysConfig();
                insert.setConfigKey(key);
                insert.setConfigValue(trimmed);
                insert.setRemark("");
                sysConfigMapper.insert(insert);
            } else {
                exist.setConfigValue(trimmed);
                sysConfigMapper.updateById(exist);
            }
        });
        refresh();
    }

    /** 玩法规则快照（一次取全，避免结算过程中配置被改） */
    public GameRules rules() {
        return new GameRules(
                getInt(POINTS_PER_CORRECT),
                getInt(COMBO_SIZE),
                getInt(COMBO_BONUS),
                getBool(COMBO_ENABLED),
                getInt(POINTS_PER_LEVEL_PASS),
                getInt(POINTS_PER_THREE_STAR),
                getInt(STAR3_RATE),
                getInt(STAR2_RATE),
                getInt(STAR1_RATE));
    }

    /** 奖惩规则快照（需求文档 3.3） */
    public record GameRules(int pointsPerCorrect,
                            int comboSize,
                            int comboBonus,
                            boolean comboEnabled,
                            int pointsPerLevelPass,
                            int pointsPerThreeStar,
                            int star3Rate,
                            int star2Rate,
                            int star1Rate) {

        /** 按正确率评定星级 */
        public int starsOf(double accuracy) {
            if (accuracy >= star3Rate) {
                return 3;
            }
            if (accuracy >= star2Rate) {
                return 2;
            }
            if (accuracy >= star1Rate) {
                return 1;
            }
            return 0;
        }
    }
}