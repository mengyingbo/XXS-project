-- =============================================================
-- 清空全部业务数据（保留 sys_config），用于重置到干净状态
-- 用途：
--   1. 阶段 2 接口自测（scripts/api-smoke-test.ps1）前后清理测试数据
--   2. 阶段 3 正式题库导入前清场
-- 注意：会清空 admin_user，服务下次启动时 DataBootstrap 会重建
--       默认管理员 admin / admin123（must_change_pwd=1）
-- 用法：mysql -h127.0.0.1 -uxxs -p xxs_game < 90_reset_all_data.sql
-- =============================================================

USE `xxs_game`;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `answer_record`;
TRUNCATE TABLE `game_session`;
TRUNCATE TABLE `progress`;
TRUNCATE TABLE `point_log`;
TRUNCATE TABLE `redeem_order`;
TRUNCATE TABLE `prize`;
TRUNCATE TABLE `question`;
TRUNCATE TABLE `level`;
TRUNCATE TABLE `lesson`;
TRUNCATE TABLE `unit`;
TRUNCATE TABLE `child`;
TRUNCATE TABLE `admin_user`;

-- sys_config 保留（家长在后台改过的规则值不应被重置）

SET FOREIGN_KEY_CHECKS = 1;