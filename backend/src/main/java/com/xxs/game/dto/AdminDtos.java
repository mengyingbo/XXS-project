package com.xxs.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端请求体
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    // ---------------- 登录 / 密码 ----------------

    public record LoginReq(
            @NotBlank(message = "账号不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record ChangePwdReq(
            @NotBlank(message = "原密码不能为空") String oldPassword,

            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 30, message = "新密码长度需在 6~30 位之间")
            String newPassword) {
    }

    // ---------------- 孩子管理 ----------------

    public record ChildSaveReq(
            @NotBlank(message = "昵称不能为空")
            @Size(min = 1, max = 10, message = "昵称长度需在 1~10 个字符之间")
            String nickname,

            @Size(max = 255, message = "头像标识过长") String avatar,

            /** 仅新增时必填 */
            @Pattern(regexp = "\\d{4}", message = "PIN 必须是 4 位数字") String pin,

            Boolean enabled) {
    }

    public record ResetPinReq(
            @NotBlank(message = "PIN 不能为空")
            @Pattern(regexp = "\\d{4}", message = "PIN 必须是 4 位数字")
            String pin) {
    }

    public record AdjustPointsReq(
            @NotNull(message = "请填写积分变动值") Integer delta,
            @Size(max = 255, message = "备注过长") String remark) {
    }

    // ---------------- 单元 / 课文 / 关卡 ----------------

    public record UnitSaveReq(
            String subject,
            @NotNull(message = "请填写单元序号") Integer unitNo,
            @NotBlank(message = "请填写单元标题") @Size(max = 50, message = "标题过长") String title,
            @Size(max = 255, message = "说明过长") String description,
            Integer sortOrder) {
    }

    public record LessonSaveReq(
            @NotNull(message = "请选择所属单元") Long unitId,
            @NotNull(message = "请填写课序号") Integer lessonNo,
            @NotBlank(message = "请填写课文标题") @Size(max = 50, message = "标题过长") String title,
            @Pattern(regexp = "TEXT|GARDEN", message = "类型只能是 TEXT 或 GARDEN") String lessonType,
            Boolean isSkim,
            Integer sortOrder) {
    }

    public record LevelSaveReq(
            @NotNull(message = "请选择所属课文") Long lessonId,
            @NotNull(message = "请填写关卡序号") Integer levelNo,
            @Size(max = 50, message = "关卡名过长") String name,
            Integer questionCount,
            Integer sortOrder) {
    }

    // ---------------- 题目 ----------------

    public record QuestionSaveReq(
            @NotNull(message = "请选择所属课文") Long lessonId,
            Long levelId,
            @NotBlank(message = "题型不能为空")
            @Pattern(regexp = "SINGLE|JUDGE|BLANK|ORDER", message = "题型只能是 SINGLE/JUDGE/BLANK/ORDER")
            String type,
            @NotBlank(message = "题干不能为空") @Size(max = 500, message = "题干过长") String stem,
            /** JSON 字符串，单选为选项数组、排序为待排序项数组 */
            String options,
            @NotBlank(message = "答案不能为空") String answer,
            @Size(max = 500, message = "解析过长") String analysis,
            @Size(max = 50, message = "知识点过长") String knowledgePoint,
            Integer difficulty,
            Integer sortOrder) {
    }

    /** 批量导入的一行（对应模板列） */
    public record ImportRow(
            Long lessonId,
            Long levelId,
            String type,
            String stem,
            String options,
            String answer,
            String analysis,
            String knowledgePoint,
            Integer difficulty) {
    }

    public record ImportReq(
            /** true 仅校验并返回预览，false 校验通过后入库 */
            Boolean dryRun,
            /** 默认课文：行内未指定 lessonId 时使用 */
            Long defaultLessonId,
            /** 默认关卡：行内未指定 levelId 时使用 */
            Long defaultLevelId,
            @NotNull(message = "导入内容不能为空") List<ImportRow> rows) {
    }

    // ---------------- 奖品 / 兑换 ----------------

    public record PrizeSaveReq(
            @NotBlank(message = "奖品名称不能为空") @Size(max = 50, message = "名称过长") String name,
            @Size(max = 255, message = "图片地址过长") String image,
            @NotNull(message = "请填写所需积分") Integer pointsCost,
            @NotNull(message = "请填写库存") Integer stock,
            Boolean enabled,
            @Size(max = 255, message = "说明过长") String description,
            Integer sortOrder) {
    }

    public record RedeemHandleReq(
            @NotBlank(message = "缺少操作类型")
            @Pattern(regexp = "APPROVE|REJECT|DELIVER", message = "操作类型只能是 APPROVE/REJECT/DELIVER")
            String action,
            @Size(max = 255, message = "备注过长") String remark) {
    }

    // ---------------- 电影片库 ----------------

    public record MovieSaveReq(
            Integer no,
            @NotBlank(message = "电影名称不能为空")
            @Size(max = 100, message = "名称过长") String name,
            @Size(max = 50, message = "类型过长") String type,
            Integer duration,
            BigDecimal rating,
            @Size(max = 100, message = "主题过长") String theme,
            @Size(max = 500, message = "看点过长") String note,
            @Size(max = 255, message = "封面地址过长") String cover,
            Boolean watched,
            Boolean enabled,
            Integer sortOrder) {
    }
}