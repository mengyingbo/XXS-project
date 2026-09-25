package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Lesson;
import com.xxs.game.entity.Level;
import com.xxs.game.entity.Question;
import com.xxs.game.mapper.LessonMapper;
import com.xxs.game.mapper.LevelMapper;
import com.xxs.game.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目批量导入（需求文档 6.2 F-AD-05）
 * 模板列：lessonId, levelId, type, stem, options, answer, analysis, knowledgePoint, difficulty
 * 前置：前端把 Excel/CSV 解析成 rows 再提交；dryRun=true 时仅校验并返回预览
 */
@Service
@RequiredArgsConstructor
public class QuestionImportService {

    /** CSV 模板表头 */
    private static final String TEMPLATE_HEADER =
            "lessonId,levelId,type,stem,options,answer,analysis,knowledgePoint,difficulty";

    private static final String TEMPLATE_BODY = """
            1,1,SINGLE,"下面哪个字的读音是正确的？","[""潮水(cháo)"",""潮水(zhāo)""]",0,"潮读 cháo，不读 zhāo","字音",1
            1,1,JUDGE,"《观潮》描写的是钱塘江大潮。",,true,"课文写的是钱塘江大潮的壮观景象","课文理解",1
            1,1,BLANK,"默写：一水护田将绿绕，______。",,"两山排闼送青来","出自《书湖阴先生壁》","古诗默写",2
            1,1,ORDER,"把下列诗句按原文顺序排列。","[""潮平两岸阔"",""风正一帆悬"",""海日生残夜""]","[0,1,2]","按诗句原顺序","古诗排序",2
            """;

    private final QuestionMapper questionMapper;

    private final LessonMapper lessonMapper;

    private final LevelMapper levelMapper;

    private final QuestionValidator questionValidator;

    /** 下载用 CSV 模板 */
    public String template() {
        return TEMPLATE_HEADER + "\n" + TEMPLATE_BODY;
    }

    /**
     * 批量导入：dryRun 为 true（默认）时只校验并返回预览；为 false 时把校验通过的行入库
     */
    @Transactional
    public Map<String, Object> importQuestions(AdminDtos.ImportReq req) {
        boolean dryRun = req.dryRun() == null || req.dryRun();
        List<Map<String, Object>> preview = new ArrayList<>();
        List<Question> pending = new ArrayList<>();
        int index = 0;

        for (AdminDtos.ImportRow row : req.rows()) {
            index++;
            List<String> errors = new ArrayList<>();
            Long lessonId = row.lessonId() != null ? row.lessonId() : req.defaultLessonId();
            Long levelId = row.levelId() != null ? row.levelId() : req.defaultLevelId();
            String type = row.type() == null ? "" : row.type().trim().toUpperCase();

            Lesson lesson = lessonId == null ? null : lessonMapper.selectById(lessonId);
            if (lesson == null) {
                errors.add("lessonId 无效或未指定");
            }
            if (levelId != null) {
                Level level = levelMapper.selectById(levelId);
                if (level == null) {
                    errors.add("levelId 无效");
                } else if (lesson != null && !level.getLessonId().equals(lessonId)) {
                    errors.add("levelId 与 lessonId 不匹配");
                }
            }
            errors.addAll(questionValidator.validate(type, row.stem(), row.options(), row.answer()));

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("index", index);
            node.put("lessonId", lessonId);
            node.put("levelId", levelId);
            node.put("type", type);
            node.put("stem", row.stem());
            node.put("errors", errors);
            node.put("ok", errors.isEmpty());
            preview.add(node);

            if (errors.isEmpty()) {
                Question question = new Question();
                question.setLessonId(lessonId);
                question.setLevelId(levelId);
                question.setType(type);
                question.setStem(row.stem().trim());
                question.setOptions(questionValidator.nullIfBlank(row.options()));
                question.setAnswer(row.answer().trim());
                question.setAnalysis(row.analysis() == null ? "" : row.analysis().trim());
                question.setKnowledgePoint(row.knowledgePoint() == null ? "" : row.knowledgePoint().trim());
                question.setDifficulty(row.difficulty() == null ? 1 : Math.min(3, Math.max(1, row.difficulty())));
                pending.add(question);
            }
        }

        int inserted = 0;
        if (!dryRun) {
            int sortOrder = nextSortOrder(pending);
            for (Question question : pending) {
                question.setSortOrder(sortOrder++);
                questionMapper.insert(question);
                inserted++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dryRun", dryRun);
        result.put("total", req.rows().size());
        result.put("validCount", pending.size());
        result.put("invalidCount", req.rows().size() - pending.size());
        result.put("inserted", inserted);
        result.put("rows", preview);
        return result;
    }

    /** 导入题目的 sort_order 从当前最大值继续，避免与已有题目交错 */
    private int nextSortOrder(List<Question> questions) {
        if (questions.isEmpty()) {
            return 0;
        }
        Long lessonId = questions.get(0).getLessonId();
        Question last = questionMapper.selectOne(Wrappers.<Question>lambdaQuery()
                .eq(Question::getLessonId, lessonId)
                .orderByDesc(Question::getSortOrder)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 0 : last.getSortOrder() + 1;
    }
}