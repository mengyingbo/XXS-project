package com.xxs.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 判题器：按题型比对答案（题目答案与孩子作答均为 JSON 字符串）
 *
 * <pre>
 * SINGLE 单选：answer = 选项下标(数字)；userAnswer = 选项下标(数字)
 * JUDGE  判断：answer = true/false；userAnswer = true/false
 * BLANK  填空：answer = 可接受答案数组；userAnswer = 文本
 * HAND   词语手写：answer = 可接受答案数组；userAnswer = 识别出的文本（判分同 BLANK）
 * ORDER  排序：answer = 正确顺序对应的原下标数组；userAnswer = 下标数组
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class QuestionJudge {

    private static final int NOT_FOUND_ANSWER = -1;
    private static final int NOT_FOUND_USER = -2;

    private final ObjectMapper objectMapper;

    /** 判题：作答缺失（跳过）一律记为错误 */
    public boolean judge(String type, String answerJson, String userAnswerJson) {
        if (userAnswerJson == null || userAnswerJson.isBlank()) {
            return false;
        }
        JsonNode answer = read(answerJson);
        JsonNode user = read(userAnswerJson);
        if (answer == null || user == null || answer.isNull() || user.isNull()) {
            return false;
        }
        String t = type == null ? "" : type.trim().toUpperCase();
        return switch (t) {
            case "SINGLE" -> answer.asInt(NOT_FOUND_ANSWER) == user.asInt(NOT_FOUND_USER);
            case "JUDGE" -> answer.asBoolean() == user.asBoolean();
            case "BLANK" -> judgeBlank(answer, user);
            case "HAND" -> judgeBlank(answer, user);
            case "ORDER" -> judgeOrder(answer, user);
            default -> false;
        };
    }

    private boolean judgeBlank(JsonNode answer, JsonNode user) {
        List<String> accepted = new ArrayList<>();
        if (answer.isArray()) {
            answer.forEach(node -> accepted.add(node.asText()));
        } else {
            accepted.add(answer.asText());
        }
        String input = user.isValueNode() ? user.asText() : user.toString();
        String normalizedInput = normalize(input);
        return accepted.stream().anyMatch(item -> normalize(item).equals(normalizedInput));
    }

    private boolean judgeOrder(JsonNode answer, JsonNode user) {
        if (!answer.isArray() || !user.isArray() || answer.size() != user.size()) {
            return false;
        }
        for (int i = 0; i < answer.size(); i++) {
            if (answer.get(i).asInt(NOT_FOUND_ANSWER) != user.get(i).asInt(NOT_FOUND_USER)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 填空答案归一化：全角转半角 + 去空白 + 去标点符号 + 转小写，
     * 避免孩子多打一个逗号或句号就被判错
     */
    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c == '\u3000') {
                sb.append(' ');
            } else if (c >= '\uFF01' && c <= '\uFF5E') {
                sb.append((char) (c - 0xFEE0));
            } else {
                sb.append(c);
            }
        }
        return sb.toString().replaceAll("[\\p{P}\\p{S}\\s]", "").toLowerCase();
    }

    /** 宽松解析 JSON：解析失败返回 null */
    public JsonNode read(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}