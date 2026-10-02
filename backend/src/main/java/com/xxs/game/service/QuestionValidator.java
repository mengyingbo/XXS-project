package com.xxs.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 题目字段校验：后台新增/修改题目与批量导入共用
 *
 * 约定（与 sql/01_schema.sql 第 6 节一致）：
 * SINGLE 选项数组 + 答案下标；JUDGE 答案布尔；BLANK 答案文本或文本数组；ORDER 待排序项数组 + 下标排列
 */
@Component
@RequiredArgsConstructor
public class QuestionValidator {

    private static final Set<String> TYPES = Set.of("SINGLE", "JUDGE", "BLANK", "ORDER", "HAND");

    private final QuestionJudge questionJudge;

    /** 校验题目，返回错误信息列表（空列表表示通过） */
    public List<String> validate(String type, String stem, String options, String answer) {
        List<String> errors = new ArrayList<>();
        if (stem == null || stem.isBlank()) {
            errors.add("题干不能为空");
        }
        if (type == null || type.isBlank()) {
            errors.add("题型不能为空");
            return errors;
        }
        String t = type.trim().toUpperCase();
        if (!TYPES.contains(t)) {
            errors.add("题型只能是 SINGLE / JUDGE / BLANK / ORDER / HAND");
            return errors;
        }

        JsonNode optionsNode = questionJudge.read(options);
        JsonNode answerNode = questionJudge.read(answer);
        if (answerNode == null || answerNode.isNull()) {
            errors.add("答案不能为空且必须是合法 JSON");
            return errors;
        }

        switch (t) {
            case "SINGLE" -> {
                if (optionsNode == null || !optionsNode.isArray() || optionsNode.size() < 2) {
                    errors.add("单选题选项需为至少 2 项的 JSON 数组");
                }
                if (!answerNode.canConvertToInt()) {
                    errors.add("单选题答案应为选项下标（数字）");
                } else if (optionsNode != null && optionsNode.isArray()
                        && (answerNode.asInt() < 0 || answerNode.asInt() >= optionsNode.size())) {
                    errors.add("单选题答案下标超出选项范围");
                }
            }
            case "JUDGE" -> {
                if (!answerNode.isBoolean()) {
                    errors.add("判断题答案应为 true 或 false");
                }
            }
            case "BLANK" -> {
                if (answerNode.isArray()) {
                    if (answerNode.isEmpty()) {
                        errors.add("填空题可接受答案数组不能为空");
                    }
                } else if (!answerNode.isValueNode()) {
                    errors.add("填空题答案应为文本或文本数组");
                }
            }
            case "HAND" -> {
                if (answerNode.isArray()) {
                    if (answerNode.isEmpty()) {
                        errors.add("词语手写题可接受答案数组不能为空");
                    }
                } else if (!answerNode.isValueNode()) {
                    errors.add("词语手写题答案应为文本或文本数组");
                }
            }
            case "ORDER" -> {
                boolean optionsOk = optionsNode != null && optionsNode.isArray() && optionsNode.size() >= 2;
                if (!optionsOk) {
                    errors.add("排序题待排序项需为至少 2 项的 JSON 数组");
                }
                if (!answerNode.isArray()) {
                    errors.add("排序题答案应为下标数组");
                    break;
                }
                if (optionsOk && answerNode.size() != optionsNode.size()) {
                    errors.add("排序题答案个数应与待排序项个数一致");
                    break;
                }
                Set<Integer> seen = new HashSet<>();
                for (JsonNode node : answerNode) {
                    if (!node.canConvertToInt()) {
                        errors.add("排序题答案必须都是下标数字");
                        break;
                    }
                    int value = node.asInt();
                    if (value < 0 || value >= answerNode.size() || !seen.add(value)) {
                        errors.add("排序题答案必须是 0..N-1 的一个排列");
                        break;
                    }
                }
            }
            default -> {
                // 已在上方拦截
            }
        }
        return errors;
    }

    /** 空字符串归一化为 null（JSON 列可为空） */
    public String nullIfBlank(String json) {
        return json == null || json.isBlank() ? null : json.trim();
    }
}