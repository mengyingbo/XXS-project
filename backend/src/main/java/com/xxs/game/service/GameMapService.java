package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.entity.Lesson;
import com.xxs.game.entity.Level;
import com.xxs.game.entity.Progress;
import com.xxs.game.entity.Unit;
import com.xxs.game.mapper.LessonMapper;
import com.xxs.game.mapper.LevelMapper;
import com.xxs.game.mapper.ProgressMapper;
import com.xxs.game.mapper.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 闯关地图：单元 → 课文 → 关卡 三级视图，并计算解锁状态（需求文档第 5 章）
 *
 * 解锁规则：线性推进。按 单元 → 课文 → 关卡 的全局顺序，
 * 第一个关卡默认解锁，其余关卡需「前一关已通关」才解锁。
 */
@Service
@RequiredArgsConstructor
public class GameMapService {

    /** 未解锁 */
    public static final String STATUS_LOCKED = "LOCKED";
    /** 已解锁 */
    public static final String STATUS_UNLOCKED = "UNLOCKED";
    /** 已通关 */
    public static final String STATUS_PASSED = "PASSED";

    /** 科目：语文（默认） */
    public static final String SUBJECT_CHINESE = "chinese";
    /** 科目：数学（v2.0） */
    public static final String SUBJECT_MATH = "math";
    /** 科目：英语（v2.6） */
    public static final String SUBJECT_ENGLISH = "english";

    /** 科目归一化：math→math，english→english，其余（含空）→chinese */
    public static String normalizeSubject(String subject) {
        if (SUBJECT_MATH.equalsIgnoreCase(subject)) {
            return SUBJECT_MATH;
        }
        if (SUBJECT_ENGLISH.equalsIgnoreCase(subject)) {
            return SUBJECT_ENGLISH;
        }
        return SUBJECT_CHINESE;
    }

    private final UnitMapper unitMapper;

    private final LessonMapper lessonMapper;

    private final LevelMapper levelMapper;

    private final ProgressMapper progressMapper;

    /** 闯关地图（孩子端，按科目；v2.0 两科进度互相独立） */
    public Map<String, Object> map(Long childId, String subject) {
        String sub = normalizeSubject(subject);
        List<Unit> units = orderedUnits(sub);
        List<Lesson> lessons = orderedLessons(sub);
        List<Level> levels = orderedLevels(sub);
        Map<Long, Progress> progressMap = progressMap(childId, levels);
        Map<Long, String> statusMap = resolveStatus(levels, progressMap);

        Map<Long, List<Lesson>> lessonsByUnit = lessons.stream()
                .collect(Collectors.groupingBy(Lesson::getUnitId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<Level>> levelsByLesson = levels.stream()
                .collect(Collectors.groupingBy(Level::getLessonId, LinkedHashMap::new, Collectors.toList()));

        int totalStars = 0;
        int passedLevels = 0;
        Long currentLevelId = null;

        List<Map<String, Object>> unitNodes = new ArrayList<>();
        for (Unit unit : units) {
            List<Lesson> unitLessons = lessonsByUnit.getOrDefault(unit.getId(), List.of());
            List<Map<String, Object>> lessonNodes = new ArrayList<>();
            int unitLevelCount = 0;
            int unitPassedCount = 0;
            int unitStars = 0;

            for (Lesson lesson : unitLessons) {
                List<Level> lessonLevels = levelsByLesson.getOrDefault(lesson.getId(), List.of());
                List<Map<String, Object>> levelNodes = new ArrayList<>();
                boolean lessonCompleted = !lessonLevels.isEmpty();

                for (Level level : lessonLevels) {
                    String status = statusMap.getOrDefault(level.getId(), STATUS_LOCKED);
                    Progress progress = progressMap.get(level.getId());
                    int stars = progress == null ? 0 : progress.getStars();

                    Map<String, Object> levelNode = new LinkedHashMap<>();
                    levelNode.put("id", level.getId());
                    levelNode.put("levelNo", level.getLevelNo());
                    levelNode.put("name", level.getName() == null || level.getName().isBlank()
                            ? "第" + level.getLevelNo() + "关" : level.getName());
                    levelNode.put("questionCount", level.getQuestionCount());
                    levelNode.put("status", status);
                    levelNode.put("locked", STATUS_LOCKED.equals(status));
                    levelNode.put("stars", stars);
                    levelNode.put("bestAccuracy", progress == null ? null : progress.getBestAccuracy());
                    levelNode.put("attemptCount", progress == null ? 0 : progress.getAttemptCount());
                    levelNodes.add(levelNode);

                    unitLevelCount++;
                    totalStars += stars;
                    unitStars += stars;
                    if (STATUS_PASSED.equals(status)) {
                        unitPassedCount++;
                        passedLevels++;
                    } else {
                        lessonCompleted = false;
                        if (currentLevelId == null && STATUS_UNLOCKED.equals(status)) {
                            currentLevelId = level.getId();
                        }
                    }
                }

                boolean lessonLocked = lessonLevels.isEmpty()
                        || lessonLevels.stream().allMatch(l -> STATUS_LOCKED.equals(statusMap.get(l.getId())));

                Map<String, Object> lessonNode = new LinkedHashMap<>();
                lessonNode.put("id", lesson.getId());
                lessonNode.put("lessonNo", lesson.getLessonNo());
                lessonNode.put("title", lesson.getTitle());
                lessonNode.put("lessonType", lesson.getLessonType());
                lessonNode.put("isSkim", lesson.getIsSkim());
                lessonNode.put("locked", lessonLocked);
                lessonNode.put("completed", lessonCompleted);
                lessonNode.put("levelCount", lessonLevels.size());
                lessonNode.put("levels", levelNodes);
                lessonNodes.add(lessonNode);
            }

            boolean unitLocked = lessonNodes.isEmpty()
                    || lessonNodes.stream().allMatch(node -> Boolean.TRUE.equals(node.get("locked")));

            Map<String, Object> unitNode = new LinkedHashMap<>();
            unitNode.put("id", unit.getId());
            unitNode.put("unitNo", unit.getUnitNo());
            unitNode.put("title", unit.getTitle());
            unitNode.put("description", unit.getDescription());
            unitNode.put("locked", unitLocked);
            unitNode.put("completed", unitLevelCount > 0 && unitPassedCount == unitLevelCount);
            unitNode.put("levelCount", unitLevelCount);
            unitNode.put("passedCount", unitPassedCount);
            unitNode.put("stars", unitStars);
            unitNode.put("lessons", lessonNodes);
            unitNodes.add(unitNode);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentLevelId", currentLevelId);
        result.put("totalLevels", levels.size());
        result.put("passedLevels", passedLevels);
        result.put("totalStars", totalStars);
        result.put("maxStars", levels.size() * 3);
        result.put("units", unitNodes);
        return result;
    }

    /** 指定关卡当前状态（按该关所在科目的关卡链计算） */
    public String statusOf(Long childId, Long levelId) {
        String subject = subjectOfLevel(levelId);
        List<Level> levels = orderedLevels(subject);
        Map<Long, Progress> progressMap = progressMap(childId, levels);
        return resolveStatus(levels, progressMap).getOrDefault(levelId, STATUS_LOCKED);
    }

    /** 指定关卡的下一个关卡 id（同科目内；已是该科最后一关返回 null） */
    public Long nextLevelId(Long levelId) {
        String subject = subjectOfLevel(levelId);
        List<Level> levels = orderedLevels(subject);
        for (int i = 0; i < levels.size(); i++) {
            if (Objects.equals(levels.get(i).getId(), levelId)) {
                return i + 1 < levels.size() ? levels.get(i + 1).getId() : null;
            }
        }
        return null;
    }

    /** 某关卡所属科目（level → lesson → unit.subject；查不到时归为语文） */
    public String subjectOfLevel(Long levelId) {
        Level level = levelMapper.selectById(levelId);
        if (level == null) {
            return SUBJECT_CHINESE;
        }
        Lesson lesson = lessonMapper.selectById(level.getLessonId());
        if (lesson == null) {
            return SUBJECT_CHINESE;
        }
        Unit unit = unitMapper.selectById(lesson.getUnitId());
        return unit == null || unit.getSubject() == null
                ? SUBJECT_CHINESE : normalizeSubject(unit.getSubject());
    }

    /** 某科目全局有序的关卡列表 */
    public List<Level> orderedLevels(String subject) {
        List<Lesson> lessons = orderedLessons(normalizeSubject(subject));
        if (lessons.isEmpty()) {
            return List.of();
        }
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        List<Level> levels = levelMapper.selectList(Wrappers.<Level>lambdaQuery()
                .in(Level::getLessonId, lessonIds)
                .orderByAsc(Level::getSortOrder).orderByAsc(Level::getLevelNo).orderByAsc(Level::getId));
        Map<Long, Integer> lessonIndex = new LinkedHashMap<>();
        for (int i = 0; i < lessons.size(); i++) {
            lessonIndex.put(lessons.get(i).getId(), i);
        }
        levels.sort(Comparator
                .comparingInt((Level l) -> lessonIndex.getOrDefault(l.getLessonId(), Integer.MAX_VALUE))
                .thenComparingInt(l -> l.getSortOrder() == null ? 0 : l.getSortOrder())
                .thenComparingInt(l -> l.getLevelNo() == null ? 0 : l.getLevelNo()));
        return levels;
    }

    public List<Unit> orderedUnits(String subject) {
        return unitMapper.selectList(Wrappers.<Unit>lambdaQuery()
                .eq(Unit::getSubject, normalizeSubject(subject))
                .orderByAsc(Unit::getSortOrder).orderByAsc(Unit::getUnitNo));
    }

    public List<Lesson> orderedLessons(String subject) {
        List<Unit> units = orderedUnits(subject);
        if (units.isEmpty()) {
            return List.of();
        }
        List<Long> unitIds = units.stream().map(Unit::getId).toList();
        List<Lesson> lessons = lessonMapper.selectList(Wrappers.<Lesson>lambdaQuery()
                .in(Lesson::getUnitId, unitIds)
                .orderByAsc(Lesson::getSortOrder).orderByAsc(Lesson::getLessonNo).orderByAsc(Lesson::getId));
        Map<Long, Integer> unitIndex = new LinkedHashMap<>();
        for (int i = 0; i < units.size(); i++) {
            unitIndex.put(units.get(i).getId(), i);
        }
        lessons.sort(Comparator
                .comparingInt((Lesson l) -> unitIndex.getOrDefault(l.getUnitId(), Integer.MAX_VALUE))
                .thenComparingInt(l -> l.getSortOrder() == null ? 0 : l.getSortOrder())
                .thenComparingInt(l -> l.getLessonNo() == null ? 0 : l.getLessonNo()));
        return lessons;
    }

    /** 按全局顺序计算每个关卡的状态 */
    private Map<Long, String> resolveStatus(List<Level> levels, Map<Long, Progress> progressMap) {
        Map<Long, String> statusMap = new LinkedHashMap<>();
        boolean prevPassed = true;
        for (Level level : levels) {
            Progress progress = progressMap.get(level.getId());
            boolean passed = progress != null && STATUS_PASSED.equals(progress.getStatus());
            String status;
            if (passed) {
                status = STATUS_PASSED;
            } else if (prevPassed) {
                status = STATUS_UNLOCKED;
            } else {
                status = STATUS_LOCKED;
            }
            statusMap.put(level.getId(), status);
            prevPassed = passed;
        }
        return statusMap;
    }

    private Map<Long, Progress> progressMap(Long childId, List<Level> levels) {
        if (childId == null || levels.isEmpty()) {
            return Map.of();
        }
        List<Long> levelIds = levels.stream().map(Level::getId).toList();
        return progressMapper.selectList(Wrappers.<Progress>lambdaQuery()
                        .eq(Progress::getChildId, childId)
                        .in(Progress::getLevelId, levelIds))
                .stream()
                .collect(Collectors.toMap(Progress::getLevelId, Function.identity(), (a, b) -> a));
    }
}