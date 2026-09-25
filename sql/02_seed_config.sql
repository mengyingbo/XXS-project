-- =============================================================
-- 小学随堂知识闯关游戏 —— 系统配置初始化（阶段 1）
-- 依据：docs/需求文档.md 第 7 章「配置项默认值」
-- 用法：mysql -uroot -p < 02_seed_config.sql
-- 说明：可重复执行（已存在的配置项不覆盖，便于家长后台改过的值保留）
--
-- 关于管理员账号：
--   默认管理员 admin / admin123 的密码需 BCrypt 加密后入库，
--   由后端首次启动时的 DataBootstrap 自动写入（不在此 SQL 中硬编码哈希），
--   并置 must_change_pwd=1，首次登录强制改密。
-- =============================================================

USE `xxs_game`;

INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
  ('questions_per_level',       '5',    '每关题目数'),
  ('daily_question_limit',      '20',   '每日答题数量上限（题/天）'),
  ('daily_minute_limit',        '20',   '每日游玩时长上限（分钟/天）'),
  ('points_per_correct',        '10',   '每答对 1 题得分'),
  ('combo_size',                '3',    '连对加成的连对题数'),
  ('combo_bonus',               '5',    '连对 combo_size 题额外加分'),
  ('combo_enabled',             'true', '是否开启连对加成'),
  ('points_per_level_pass',     '20',   '每通关 1 个关卡额外奖励'),
  ('points_per_three_star',     '30',   '三星通关额外奖励'),
  ('star3_rate',                '90',   '三星正确率阈值(%)'),
  ('star2_rate',                '70',   '二星正确率阈值(%)'),
  ('star1_rate',                '60',   '一星正确率阈值(%)'),
  ('show_analysis_immediately', 'true', '答题后是否立即显示解析')
ON DUPLICATE KEY UPDATE `config_key` = `config_key`;