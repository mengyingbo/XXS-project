package com.xxs.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 孩子端请求体
 */
public final class ChildDtos {

    private ChildDtos() {
    }

    /** 新建孩子档案 */
    public record RegisterReq(
            @NotBlank(message = "昵称不能为空")
            @Size(min = 1, max = 10, message = "昵称长度需在 1~10 个字符之间")
            String nickname,

            @Size(max = 255, message = "头像标识过长")
            String avatar,

            @NotBlank(message = "PIN 不能为空")
            @Pattern(regexp = "\\d{4}", message = "PIN 必须是 4 位数字")
            String pin) {
    }

    /** PIN 登录 */
    public record LoginReq(
            @NotNull(message = "请选择孩子档案")
            Long childId,

            @NotBlank(message = "请输入 PIN")
            @Pattern(regexp = "\\d{4}", message = "PIN 必须是 4 位数字")
            String pin) {
    }

    /** 开始一关 */
    public record StartReq(
            @NotNull(message = "缺少关卡 id")
            Long levelId) {
    }

    /** 单题作答 */
    public record AnswerItem(
            @NotNull(message = "缺少题目 id")
            Long questionId,

            /** 作答内容（JSON 字符串），为空表示跳过（记为错） */
            String userAnswer,

            Long durationMs) {
    }

    /** 单题对答案（多邻国式"检查"：只读判题，不写库、不计数、不发分） */
    public record CheckReq(
            @NotNull(message = "缺少关卡 id")
            Long levelId,

            @NotNull(message = "缺少题目 id")
            Long questionId,

            /** 作答内容（JSON 字符串，口径与 SubmitReq.AnswerItem 一致；为空表示未答） */
            String userAnswer) {
    }

    /** 提交整关作答 */
    public record SubmitReq(
            @NotNull(message = "缺少关卡 id")
            Long levelId,

            Long durationMs,

            @NotEmpty(message = "作答列表不能为空")
            List<AnswerItem> answers) {
    }

    /** 提交兑换申请 */
    public record RedeemReq(
            @NotNull(message = "请选择奖品")
            Long prizeId) {
    }
}