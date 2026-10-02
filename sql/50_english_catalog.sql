-- =============================================================
-- 阶段 13（v2.6）—— 英语科目上线：英语校目录
-- 教材依据：docs/需求文档-英语题库.md 3.2 / 3.4 / 3.5
--   义务教育教科书·英语·四年级上册（人民教育出版社·PEP三年级起点，2024年新课标修订版）
--   目录（2026-10-01）、词汇表 110 词条与常用表达（2026-10-02）已按实体书照片核对
-- 前置：11~18 语文题库、30~33 数学题库、40/41 词语题库已导入
--   （语文 unit 1~8 / lesson 1~34 / level 1~102；数学 unit 9~16 / lesson 35~45 / level 103~153；词语 level 200~220）
-- id 约定：英语单元 id 17~23；英语课文 id 46~52；英语关卡 id 300~332
--   单元 17 Unit 1 Helping at home           课文 46，关卡 300~304（5关 30题）
--   单元 18 Unit 2 My friends                课文 47，关卡 305~309（5关 28题）
--   单元 19 Unit 3 Places we live in         课文 48，关卡 310~314（5关 30题）
--   单元 20 Unit 4 Helping in the community  课文 49，关卡 315~319（5关 28题）
--   单元 21 Unit 5 The weather and us        课文 50，关卡 320~325（6关 34题）
--   单元 22 Unit 6 Changing for the seasons  课文 51，关卡 326~330（5关 32题）
--   单元 23 Revision Let's help!             课文 52，关卡 331~332（2关 12题）
-- 用法：mysql --default-character-set=utf8mb4 -uroot -p xxs_game < 50_english_catalog.sql
-- 幂等性：固定 id 插入，重复导入前需先删除既有英语数据（见文件尾部注释）
-- =============================================================

SET NAMES utf8mb4;
USE `xxs_game`;

-- -------------------------------------------------------------
-- 1. 英语单元（unit id 17~23，subject='english'，unit_no 独立编号）
-- -------------------------------------------------------------
INSERT INTO `unit` (`id`, `subject`, `unit_no`, `title`, `description`, `sort_order`) VALUES
(17, 'english', 1, 'Unit 1', 'Helping at home：职业与家务，What''s your mother''s job? / We can do some chores.（p2）', 1),
(18, 'english', 2, 'Unit 2', 'My friends：朋友的外貌性格与爱好，Who''s your best friend?（p14）', 2),
(19, 'english', 3, 'Unit 3', 'Places we live in：社区场所与 there be 句型（p26）', 3),
(20, 'english', 4, 'Unit 4', 'Helping in the community：社区职业与现在进行时（p38）', 4),
(21, 'english', 5, 'Unit 5', 'The weather and us：天气词汇与问答 What''s the weather like...?（p50）', 5),
(22, 'english', 6, 'Unit 6', 'Changing for the seasons：服装与季节，Whose...? / Which season...?（p62）', 6),
(23, 'english', 7, 'Revision', 'Let''s help! 全书综合复习（p74）', 7);

-- -------------------------------------------------------------
-- 2. 英语课文（lesson id 46~52；Revision 为 PRACTICE 类型）
-- -------------------------------------------------------------
INSERT INTO `lesson` (`id`, `unit_id`, `lesson_no`, `title`, `lesson_type`, `is_skim`, `sort_order`) VALUES
(46, 17, 1, 'Helping at home',           'TEXT',     0, 1),
(47, 18, 1, 'My friends',                'TEXT',     0, 1),
(48, 19, 1, 'Places we live in',         'TEXT',     0, 1),
(49, 20, 1, 'Helping in the community',  'TEXT',     0, 1),
(50, 21, 1, 'The weather and us',        'TEXT',     0, 1),
(51, 22, 1, 'Changing for the seasons',  'TEXT',     0, 1),
(52, 23, 1, 'Let''s help!',              'PRACTICE', 0, 1);

-- -------------------------------------------------------------
-- 3. 英语关卡（level id 300~332；按知识点分组命名，共 33 关 194 题）
-- -------------------------------------------------------------
INSERT INTO `level` (`id`, `lesson_id`, `level_no`, `name`, `question_count`, `sort_order`) VALUES
-- Unit 1 Helping at home（课文 46，5 关 30 题）
(300, 46, 1, '职业与身份',   6, 1),
(301, 46, 2, '家务劳动',     6, 2),
(302, 46, 3, '核心句型',     6, 3),
(303, 46, 4, '语法与运用',   6, 4),
(304, 46, 5, '拼写挑战',     6, 5),
-- Unit 2 My friends（课文 47，5 关 28 题）
(305, 47, 1, '外貌与性格',   6, 1),
(306, 47, 2, '爱好与活动',   6, 2),
(307, 47, 3, '朋友对话',     6, 3),
(308, 47, 4, '语法与运用',   5, 4),
(309, 47, 5, '拼写挑战',     5, 5),
-- Unit 3 Places we live in（课文 48，5 关 30 题）
(310, 48, 1, '场所词汇（一）', 6, 1),
(311, 48, 2, '场所词汇（二）', 6, 2),
(312, 48, 3, 'There be 句型', 6, 3),
(313, 48, 4, '问答与运用',   6, 4),
(314, 48, 5, '拼写挑战',     6, 5),
-- Unit 4 Helping in the community（课文 49，5 关 28 题）
(315, 49, 1, '社区职业',     6, 1),
(316, 49, 2, '工作与帮助',   6, 2),
(317, 49, 3, '句型与对话',   6, 3),
(318, 49, 4, '现在进行时',   5, 4),
(319, 49, 5, '拼写挑战',     5, 5),
-- Unit 5 The weather and us（课文 50，6 关 34 题）
(320, 50, 1, '天气词汇（一）', 6, 1),
(321, 50, 2, '天气词汇（二）', 6, 2),
(322, 50, 3, '天气问答',     6, 3),
(323, 50, 4, '活动与安排',   6, 4),
(324, 50, 5, '四季与活动',   5, 5),
(325, 50, 6, '拼写挑战',     5, 6),
-- Unit 6 Changing for the seasons（课文 51，5 关 32 题）
(326, 51, 1, '服装词汇',     7, 1),
(327, 51, 2, '季节词汇',     7, 2),
(328, 51, 3, 'Whose 与 Which', 6, 3),
(329, 51, 4, '季节对话',     6, 4),
(330, 51, 5, '拼写挑战',     6, 5),
-- Revision Let's help!（课文 52，2 关 12 题）
(331, 52, 1, '综合复习（一）', 6, 1),
(332, 52, 2, '综合复习（二）', 6, 2);

-- -------------------------------------------------------------
-- 重复导入前的清理语句（如需重跑本脚本，先执行以下 DELETE）
-- -------------------------------------------------------------
-- DELETE FROM `question` WHERE `level_id` BETWEEN 300 AND 332;
-- DELETE FROM `level`    WHERE `id`      BETWEEN 300 AND 332;
-- DELETE FROM `lesson`   WHERE `id`      BETWEEN 46 AND 52;
-- DELETE FROM `unit`     WHERE `id`      BETWEEN 17 AND 23;
-- （注意：删除 unit 前需确认无 progress/game_session 等关联数据残留）
