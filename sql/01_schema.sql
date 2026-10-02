-- =============================================================
-- 小学随堂知识闯关游戏 —— 数据库结构（阶段 1）
-- 依据：docs/需求文档.md 第 7 章
-- 目标库：xxs_game   字符集：utf8mb4   引擎：InnoDB
-- 用法：mysql -uroot -p < 01_schema.sql
-- 注意：本脚本会 DROP 并重建全部表，仅在初始化/重置时执行
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `xxs_game`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `xxs_game`;

-- -------------------------------------------------------------
-- 1. admin_user 管理员
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(50)     NOT NULL                COMMENT '登录账号',
  `password_hash`   VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 密码哈希',
  `nickname`        VARCHAR(50)     NOT NULL DEFAULT '家长'  COMMENT '昵称',
  `must_change_pwd` TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否强制改密：0否 1是',
  `last_login_at`   DATETIME        NULL                    COMMENT '最后登录时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员';

-- -------------------------------------------------------------
-- 2. child 孩子档案
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `child`;
CREATE TABLE `child` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `nickname`      VARCHAR(30)     NOT NULL                COMMENT '昵称',
  `avatar`        VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '头像标识/图片地址',
  `pin_hash`      VARCHAR(100)    NOT NULL                COMMENT '4 位 PIN 的 BCrypt 哈希',
  `total_points`  INT             NOT NULL DEFAULT 0      COMMENT '当前可用积分',
  `total_earned`  INT             NOT NULL DEFAULT 0      COMMENT '累计获得积分（不因兑换减少）',
  `pin_fail_count` TINYINT        NOT NULL DEFAULT 0      COMMENT 'PIN 连续错误次数',
  `locked_until`  DATETIME        NULL                    COMMENT 'PIN 锁定截止时间（错 5 次锁 1 分钟）',
  `enabled`       TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '是否启用：0停用 1启用',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_child_enabled` (`enabled`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '孩子档案';

-- -------------------------------------------------------------
-- 3. unit 单元
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `unit`;
CREATE TABLE `unit` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject`     VARCHAR(10)     NOT NULL DEFAULT 'chinese' COMMENT '科目：chinese语文 / math数学（v2.0）',
  `unit_no`     TINYINT         NOT NULL                COMMENT '单元序号 1~8',
  `title`       VARCHAR(50)     NOT NULL                COMMENT '单元标题',
  `description` VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '单元说明',
  `sort_order`  INT             NOT NULL DEFAULT 0      COMMENT '排序值',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_unit_no` (`subject`, `unit_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '单元';

-- -------------------------------------------------------------
-- 4. lesson 课文 / 语文园地
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `lesson`;
CREATE TABLE `lesson` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `unit_id`     BIGINT UNSIGNED NOT NULL                COMMENT '所属单元 id',
  `lesson_no`   TINYINT         NOT NULL                COMMENT '课文序号（语文园地记为 0）',
  `title`       VARCHAR(50)     NOT NULL                COMMENT '课文标题',
  `lesson_type` VARCHAR(20)     NOT NULL DEFAULT 'TEXT' COMMENT '类型：TEXT课文 / GARDEN语文园地 / PRACTICE综合实践 / FUN数学好玩（v2.0）',
  `is_skim`     TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否略读课文（目录带 *）：0否 1是',
  `sort_order`  INT             NOT NULL DEFAULT 0      COMMENT '排序值',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_lesson_unit` (`unit_id`, `sort_order`),
  CONSTRAINT `fk_lesson_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课文/语文园地';

-- -------------------------------------------------------------
-- 5. level 关卡
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `level`;
CREATE TABLE `level` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lesson_id`      BIGINT UNSIGNED NOT NULL                COMMENT '所属课文 id',
  `level_no`       TINYINT         NOT NULL                COMMENT '关卡序号，从 1 开始',
  `name`           VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '关卡名称，空则前端显示“第N关”',
  `question_count` INT             NOT NULL DEFAULT 5      COMMENT '本关题目数',
  `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '排序值',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_lesson_no` (`lesson_id`, `level_no`),
  CONSTRAINT `fk_level_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '关卡';

-- -------------------------------------------------------------
-- 6. question 题目
--   type：SINGLE单选 / JUDGE判断 / BLANK填空 / ORDER排序
--   options：单选为 ["A选项","B选项",...]；判断/填空为 null；排序为待排序项数组
--   answer ：单选为选项下标；判断为 true/false；填空为可接受答案数组；排序为正确顺序下标数组
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lesson_id`       BIGINT UNSIGNED NOT NULL                COMMENT '所属课文 id',
  `level_id`        BIGINT UNSIGNED NULL                    COMMENT '所属关卡 id（可为空，表示未入关）',
  `type`            VARCHAR(10)     NOT NULL                COMMENT '题型：SINGLE/JUDGE/BLANK/ORDER',
  `stem`            VARCHAR(500)    NOT NULL                COMMENT '题干',
  `options`         JSON            NULL                    COMMENT '选项（JSON）',
  `answer`          JSON            NOT NULL                COMMENT '正确答案（JSON）',
  `analysis`        VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '解析，答错时展示',
  `knowledge_point` VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '知识点标签',
  `difficulty`      TINYINT         NOT NULL DEFAULT 1      COMMENT '难度 1~3',
  `sort_order`      INT             NOT NULL DEFAULT 0      COMMENT '排序值',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_question_lesson` (`lesson_id`, `type`),
  KEY `idx_question_level` (`level_id`),
  CONSTRAINT `fk_question_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`id`),
  CONSTRAINT `fk_question_level` FOREIGN KEY (`level_id`) REFERENCES `level` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '题目';

-- -------------------------------------------------------------
-- 7. progress 闯关进度
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `progress`;
CREATE TABLE `progress` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `child_id`        BIGINT UNSIGNED NOT NULL                COMMENT '孩子 id',
  `level_id`        BIGINT UNSIGNED NOT NULL                COMMENT '关卡 id',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'LOCKED' COMMENT '状态：LOCKED未解锁/UNLOCKED已解锁/PASSED已通关',
  `stars`           TINYINT         NOT NULL DEFAULT 0      COMMENT '最好星级 0~3',
  `best_accuracy`   DECIMAL(5,2)    NOT NULL DEFAULT 0.00   COMMENT '最好正确率(%)',
  `best_score`      INT             NOT NULL DEFAULT 0      COMMENT '最高得分',
  `attempt_count`   INT             NOT NULL DEFAULT 0      COMMENT '尝试次数',
  `first_passed_at` DATETIME        NULL                    COMMENT '首次通关时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_progress_child_level` (`child_id`, `level_id`),
  KEY `idx_progress_child_status` (`child_id`, `status`),
  CONSTRAINT `fk_progress_child` FOREIGN KEY (`child_id`) REFERENCES `child` (`id`),
  CONSTRAINT `fk_progress_level` FOREIGN KEY (`level_id`) REFERENCES `level` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '闯关进度';

-- -------------------------------------------------------------
-- 8. game_session 答题局记录
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `game_session`;
CREATE TABLE `game_session` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `child_id`      BIGINT UNSIGNED NOT NULL                COMMENT '孩子 id',
  `level_id`      BIGINT UNSIGNED NOT NULL                COMMENT '关卡 id',
  `total_count`   INT             NOT NULL DEFAULT 0      COMMENT '本关题目总数',
  `correct_count` INT             NOT NULL DEFAULT 0      COMMENT '答对题数',
  `accuracy`      DECIMAL(5,2)    NOT NULL DEFAULT 0.00   COMMENT '正确率(%)',
  `stars`         TINYINT         NOT NULL DEFAULT 0      COMMENT '本次星级 0~3',
  `points_gained` INT             NOT NULL DEFAULT 0      COMMENT '本次获得积分',
  `duration_ms`   BIGINT          NOT NULL DEFAULT 0      COMMENT '作答总耗时(毫秒)',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_child_time` (`child_id`, `created_at`),
  CONSTRAINT `fk_session_child` FOREIGN KEY (`child_id`) REFERENCES `child` (`id`),
  CONSTRAINT `fk_session_level` FOREIGN KEY (`level_id`) REFERENCES `level` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '答题局记录';

-- -------------------------------------------------------------
-- 9. answer_record 逐题作答记录（错题本数据来源）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `answer_record`;
CREATE TABLE `answer_record` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`  BIGINT UNSIGNED NOT NULL                COMMENT '所属答题局 id',
  `child_id`    BIGINT UNSIGNED NOT NULL                COMMENT '孩子 id',
  `question_id` BIGINT UNSIGNED NOT NULL                COMMENT '题目 id',
  `user_answer` JSON            NULL                    COMMENT '孩子作答内容（JSON）',
  `is_correct`  TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否正确：0否 1是',
  `duration_ms` BIGINT          NOT NULL DEFAULT 0      COMMENT '本题耗时(毫秒)',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_answer_session` (`session_id`),
  KEY `idx_answer_child_correct` (`child_id`, `is_correct`),
  KEY `idx_answer_question` (`question_id`),
  CONSTRAINT `fk_answer_session` FOREIGN KEY (`session_id`) REFERENCES `game_session` (`id`),
  CONSTRAINT `fk_answer_child` FOREIGN KEY (`child_id`) REFERENCES `child` (`id`),
  CONSTRAINT `fk_answer_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '逐题作答记录';

-- -------------------------------------------------------------
-- 10. prize 奖品
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `prize`;
CREATE TABLE `prize` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50)     NOT NULL                COMMENT '奖品名称',
  `image`       VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '奖品图片地址',
  `points_cost` INT             NOT NULL DEFAULT 0      COMMENT '兑换所需积分',
  `stock`       INT             NOT NULL DEFAULT 0      COMMENT '库存数量',
  `enabled`     TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '是否上架：0下架 1上架',
  `description` VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '奖品说明',
  `sort_order`  INT             NOT NULL DEFAULT 0      COMMENT '排序值',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_prize_enabled` (`enabled`, `sort_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '奖品';

-- -------------------------------------------------------------
-- 11. redeem_order 兑换订单
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `redeem_order`;
CREATE TABLE `redeem_order` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `child_id`   BIGINT UNSIGNED NOT NULL                COMMENT '孩子 id',
  `prize_id`   BIGINT UNSIGNED NOT NULL                COMMENT '奖品 id',
  `prize_name` VARCHAR(50)     NOT NULL                COMMENT '奖品名称快照',
  `points_cost` INT            NOT NULL DEFAULT 0      COMMENT '消耗积分快照',
  `status`     VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待审核/APPROVED已通过/REJECTED已拒绝/DELIVERED已发放',
  `remark`     VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '备注（拒绝原因等）',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `handled_at` DATETIME        NULL                    COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_redeem_child` (`child_id`, `created_at`),
  KEY `idx_redeem_status` (`status`),
  CONSTRAINT `fk_redeem_child` FOREIGN KEY (`child_id`) REFERENCES `child` (`id`),
  CONSTRAINT `fk_redeem_prize` FOREIGN KEY (`prize_id`) REFERENCES `prize` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '兑换订单';

-- -------------------------------------------------------------
-- 12. point_log 积分流水
--   biz_type：ANSWER答题 / PASS通关 / THREE_STAR三星 / COMBO连对
--             REDEEM兑换 / REJECT_REFUND拒绝退回 / ADMIN_ADJUST家长调整
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `point_log`;
CREATE TABLE `point_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `child_id`      BIGINT UNSIGNED NOT NULL                COMMENT '孩子 id',
  `change_amount` INT             NOT NULL                COMMENT '积分变动（正增负减）',
  `balance_after` INT             NOT NULL                COMMENT '变动后余额',
  `biz_type`      VARCHAR(20)     NOT NULL                COMMENT '业务类型',
  `ref_id`        BIGINT UNSIGNED NULL                    COMMENT '关联业务 id（局次/订单等）',
  `remark`        VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '备注',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_point_log_child` (`child_id`, `created_at`),
  CONSTRAINT `fk_point_log_child` FOREIGN KEY (`child_id`) REFERENCES `child` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '积分流水';

-- -------------------------------------------------------------
-- 13. sys_config 系统配置
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`   VARCHAR(50)     NOT NULL                COMMENT '配置键',
  `config_value` VARCHAR(255)    NOT NULL                COMMENT '配置值',
  `remark`       VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '配置说明',
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统配置';

SET FOREIGN_KEY_CHECKS = 1;