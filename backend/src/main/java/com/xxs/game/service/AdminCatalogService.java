package com.xxs.game.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxs.game.common.BizException;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.AnswerRecord;
import com.xxs.game.entity.Lesson;
import com.xxs.game.entity.Level;
import com.xxs.game.entity.Progress;
import com.xxs.game.entity.Question;
import com.xxs.game.entity.Unit;
import com.xxs.game.mapper.AnswerRecordMapper;
import com.xxs.game.mapper.LessonMapper;
import com.xxs.game.mapper.LevelMapper;
import com.xxs.game.mapper.ProgressMapper;
import com.xxs.game.mapper.QuestionMapper;
import com.xxs.game.mapper.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端基础数据维护：单元 / 课文 / 关卡 / 题目（需求文档 6.2 F-AD-04、F-AD-06）
 */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final UnitMapper unitMapper;

    private final LessonMapper lessonMapper;

    private final LevelMapper levelMapper;

    private final QuestionMapper questionMapper;

    private final ProgressMapper progressMapper;

    private final AnswerRecordMapper answerRecordMapper;

    private final QuestionValidator questionValidator;

    // ---------------- 单元 ----------------

    public List<Unit> listUnits() {
        return unitMapper.selectList(Wrappers.<Unit>lambdaQuery()
                .orderByAsc(Unit::getSortOrder).orderByAsc(Unit::getUnitNo));
    }

    @Transactional
    public Unit createUnit(AdminDtos.UnitSaveReq req) {
        Unit unit = new Unit();
        unit.setSubject(normalizeSubject(req.subject()));
        unit.setUnitNo(req.unitNo());
        unit.setTitle(req.title().trim());
        unit.setDescription(req.description() == null ? "" : req.description().trim());
        unit.setSortOrder(req.sortOrder() == null ? req.unitNo() : req.sortOrder());
        unitMapper.insert(unit);
        return unit;
    }

    @Transactional
    public Unit updateUnit(Long id, AdminDtos.UnitSaveReq req) {
        Unit unit = unitMapper.selectById(id);
        if (unit == null) {
            throw BizException.notFound("单元不存在");
        }
        unit.setSubject(normalizeSubject(req.subject()));
        unit.setUnitNo(req.unitNo());
        unit.setTitle(req.title().trim());
        unit.setDescription(req.description() == null ? "" : req.description().trim());
        if (req.sortOrder() != null) {
            unit.setSortOrder(req.sortOrder());
        }
        unitMapper.updateById(unit);
        return unit;
    }

    /** 科目归一化（v2.6）：只允许 chinese / math / english */
    private String normalizeSubject(String subject) {
        String sub = subject == null || subject.isBlank() ? GameMapService.SUBJECT_CHINESE : subject.trim().toLowerCase();
        if (!GameMapService.SUBJECT_CHINESE.equals(sub)
                && !GameMapService.SUBJECT_MATH.equals(sub)
                && !GameMapService.SUBJECT_ENGLISH.equals(sub)) {
            throw BizException.badRequest("科目只能是 chinese、math 或 english");
        }
        return sub;
    }

    @Transactional
    public void deleteUnit(Long id) {
        if (unitMapper.selectById(id) == null) {
            throw BizException.notFound("单元不存在");
        }
        if (lessonMapper.selectCount(Wrappers.<Lesson>lambdaQuery().eq(Lesson::getUnitId, id)) > 0) {
            throw BizException.conflict("该单元下还有课文，请先删除课文");
        }
        unitMapper.deleteById(id);
    }

    // ---------------- 课文 ----------------

    public List<Lesson> listLessons(Long unitId) {
        return lessonMapper.selectList(Wrappers.<Lesson>lambdaQuery()
                .eq(unitId != null, Lesson::getUnitId, unitId)
                .orderByAsc(Lesson::getSortOrder).orderByAsc(Lesson::getLessonNo));
    }

    @Transactional
    public Lesson createLesson(AdminDtos.LessonSaveReq req) {
        if (unitMapper.selectById(req.unitId()) == null) {
            throw BizException.badRequest("所属单元不存在");
        }
        Lesson lesson = new Lesson();
        lesson.setUnitId(req.unitId());
        lesson.setLessonNo(req.lessonNo());
        lesson.setTitle(req.title().trim());
        lesson.setLessonType(req.lessonType() == null ? "TEXT" : req.lessonType());
        lesson.setIsSkim(req.isSkim() != null && req.isSkim());
        lesson.setSortOrder(req.sortOrder() == null ? req.lessonNo() : req.sortOrder());
        lessonMapper.insert(lesson);
        return lesson;
    }

    @Transactional
    public Lesson updateLesson(Long id, AdminDtos.LessonSaveReq req) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw BizException.notFound("课文不存在");
        }
        if (unitMapper.selectById(req.unitId()) == null) {
            throw BizException.badRequest("所属单元不存在");
        }
        lesson.setUnitId(req.unitId());
        lesson.setLessonNo(req.lessonNo());
        lesson.setTitle(req.title().trim());
        if (req.lessonType() != null) {
            lesson.setLessonType(req.lessonType());
        }
        if (req.isSkim() != null) {
            lesson.setIsSkim(req.isSkim());
        }
        if (req.sortOrder() != null) {
            lesson.setSortOrder(req.sortOrder());
        }
        lessonMapper.updateById(lesson);
        return lesson;
    }

    @Transactional
    public void deleteLesson(Long id) {
        if (lessonMapper.selectById(id) == null) {
            throw BizException.notFound("课文不存在");
        }
        if (levelMapper.selectCount(Wrappers.<Level>lambdaQuery().eq(Level::getLessonId, id)) > 0) {
            throw BizException.conflict("该课文下还有关卡，请先删除关卡");
        }
        if (questionMapper.selectCount(Wrappers.<Question>lambdaQuery().eq(Question::getLessonId, id)) > 0) {
            throw BizException.conflict("该课文下还有题目，请先删除题目");
        }
        lessonMapper.deleteById(id);
    }

    // ---------------- 关卡 ----------------

    public List<Level> listLevels(Long lessonId) {
        return levelMapper.selectList(Wrappers.<Level>lambdaQuery()
                .eq(lessonId != null, Level::getLessonId, lessonId)
                .orderByAsc(Level::getSortOrder).orderByAsc(Level::getLevelNo));
    }

    @Transactional
    public Level createLevel(AdminDtos.LevelSaveReq req) {
        if (lessonMapper.selectById(req.lessonId()) == null) {
            throw BizException.badRequest("所属课文不存在");
        }
        Level level = new Level();
        level.setLessonId(req.lessonId());
        level.setLevelNo(req.levelNo());
        level.setName(req.name() == null ? "" : req.name().trim());
        level.setQuestionCount(req.questionCount() == null || req.questionCount() <= 0 ? 5 : req.questionCount());
        level.setSortOrder(req.sortOrder() == null ? req.levelNo() : req.sortOrder());
        levelMapper.insert(level);
        return level;
    }

    @Transactional
    public Level updateLevel(Long id, AdminDtos.LevelSaveReq req) {
        Level level = levelMapper.selectById(id);
        if (level == null) {
            throw BizException.notFound("关卡不存在");
        }
        if (lessonMapper.selectById(req.lessonId()) == null) {
            throw BizException.badRequest("所属课文不存在");
        }
        level.setLessonId(req.lessonId());
        level.setLevelNo(req.levelNo());
        level.setName(req.name() == null ? "" : req.name().trim());
        if (req.questionCount() != null && req.questionCount() > 0) {
            level.setQuestionCount(req.questionCount());
        }
        if (req.sortOrder() != null) {
            level.setSortOrder(req.sortOrder());
        }
        levelMapper.updateById(level);
        return level;
    }

    @Transactional
    public void deleteLevel(Long id) {
        if (levelMapper.selectById(id) == null) {
            throw BizException.notFound("关卡不存在");
        }
        if (questionMapper.selectCount(Wrappers.<Question>lambdaQuery().eq(Question::getLevelId, id)) > 0) {
            throw BizException.conflict("该关卡下还有题目，请先移出或删除题目");
        }
        if (progressMapper.selectCount(Wrappers.<Progress>lambdaQuery().eq(Progress::getLevelId, id)) > 0) {
            throw BizException.conflict("已有孩子的闯关进度关联该关卡，无法删除");
        }
        levelMapper.deleteById(id);
    }

    // ---------------- 题目 ----------------

    public Map<String, Object> listQuestions(int page, int size, Long lessonId, Long levelId,
                                             String type, String keyword, String subject) {
        // v2.0：按科目筛选（科目挂在 unit 上，先解析出该科目的课文 id 集合）
        List<Long> subjectLessonIds = null;
        if (subject != null && !subject.isBlank()) {
            String sub = GameMapService.normalizeSubject(subject);
            List<Long> unitIds = unitMapper.selectList(Wrappers.<Unit>lambdaQuery()
                            .eq(Unit::getSubject, sub)).stream().map(Unit::getId).toList();
            subjectLessonIds = unitIds.isEmpty() ? List.of(-1L)
                    : lessonMapper.selectList(Wrappers.<Lesson>lambdaQuery()
                            .in(Lesson::getUnitId, unitIds)).stream().map(Lesson::getId).toList();
        }

        IPage<Question> result = questionMapper.selectPage(new Page<>(page, size),
                Wrappers.<Question>lambdaQuery()
                        .eq(lessonId != null, Question::getLessonId, lessonId)
                        .eq(levelId != null, Question::getLevelId, levelId)
                        .eq(type != null && !type.isBlank(), Question::getType, type)
                        .in(subjectLessonIds != null, Question::getLessonId, subjectLessonIds)
                        .like(keyword != null && !keyword.isBlank(), Question::getStem, keyword)
                        .orderByAsc(Question::getLessonId)
                        .orderByAsc(Question::getSortOrder)
                        .orderByAsc(Question::getId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        data.put("records", result.getRecords());
        return data;
    }

    public Question getQuestion(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw BizException.notFound("题目不存在");
        }
        return question;
    }

    @Transactional
    public Question createQuestion(AdminDtos.QuestionSaveReq req) {
        validateQuestionRefs(req);
        List<String> errors = questionValidator.validate(req.type(), req.stem(), req.options(), req.answer());
        if (!errors.isEmpty()) {
            throw BizException.badRequest(String.join("；", errors));
        }
        Question question = new Question();
        applyQuestion(question, req);
        questionMapper.insert(question);
        return question;
    }

    @Transactional
    public Question updateQuestion(Long id, AdminDtos.QuestionSaveReq req) {
        Question question = getQuestion(id);
        validateQuestionRefs(req);
        List<String> errors = questionValidator.validate(req.type(), req.stem(), req.options(), req.answer());
        if (!errors.isEmpty()) {
            throw BizException.badRequest(String.join("；", errors));
        }
        applyQuestion(question, req);
        questionMapper.updateById(question);
        return question;
    }

    @Transactional
    public void deleteQuestion(Long id) {
        getQuestion(id);
        if (answerRecordMapper.selectCount(Wrappers.<AnswerRecord>lambdaQuery()
                .eq(AnswerRecord::getQuestionId, id)) > 0) {
            throw BizException.conflict("该题目已有孩子的作答记录，无法删除（可将它移出关卡）");
        }
        questionMapper.deleteById(id);
    }

    private void validateQuestionRefs(AdminDtos.QuestionSaveReq req) {
        if (lessonMapper.selectById(req.lessonId()) == null) {
            throw BizException.badRequest("所属课文不存在");
        }
        if (req.levelId() != null) {
            Level level = levelMapper.selectById(req.levelId());
            if (level == null) {
                throw BizException.badRequest("所属关卡不存在");
            }
            if (!level.getLessonId().equals(req.lessonId())) {
                throw BizException.badRequest("所属关卡与课文不匹配");
            }
        }
    }

    private void applyQuestion(Question question, AdminDtos.QuestionSaveReq req) {
        question.setLessonId(req.lessonId());
        question.setLevelId(req.levelId());
        question.setType(req.type().trim().toUpperCase());
        question.setStem(req.stem().trim());
        question.setOptions(questionValidator.nullIfBlank(req.options()));
        question.setAnswer(req.answer().trim());
        question.setAnalysis(req.analysis() == null ? "" : req.analysis().trim());
        question.setKnowledgePoint(req.knowledgePoint() == null ? "" : req.knowledgePoint().trim());
        question.setDifficulty(req.difficulty() == null ? 1 : Math.min(3, Math.max(1, req.difficulty())));
        if (req.sortOrder() != null) {
            question.setSortOrder(req.sortOrder());
        }
    }
}