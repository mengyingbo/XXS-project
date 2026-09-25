#!/usr/bin/env bash
# =============================================================
# 在 Ubuntu 服务器 192.168.1.53 上初始化 xxs_game 数据库
# 用法（需 root）：sudo bash setup_mysql.sh <SQL脚本目录>
# 幂等：可重复执行
# =============================================================
set -euo pipefail

SQL_DIR="${1:-/tmp}"
APP_USER="xxs"
APP_PWD="Xxs2026pwd"
ALLOW_HOST="192.168.1.%"

echo "=== 1. 建库建表 ==="
mysql < "${SQL_DIR}/01_schema.sql"

echo "=== 2. 写入系统配置 ==="
mysql < "${SQL_DIR}/02_seed_config.sql"

echo "=== 3. 创建应用账号 ${APP_USER}@${ALLOW_HOST} ==="
mysql -e "CREATE USER IF NOT EXISTS '${APP_USER}'@'${ALLOW_HOST}' IDENTIFIED BY '${APP_PWD}';"
mysql -e "ALTER USER '${APP_USER}'@'${ALLOW_HOST}' IDENTIFIED BY '${APP_PWD}';"
mysql -e "GRANT ALL PRIVILEGES ON xxs_game.* TO '${APP_USER}'@'${ALLOW_HOST}';"
mysql -e "FLUSH PRIVILEGES;"

echo "=== 4. 校验结果 ==="
mysql -e "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='xxs_game';"
mysql -e "SELECT config_key, config_value FROM xxs_game.sys_config;"
mysql -e "SELECT user, host FROM mysql.user WHERE user='${APP_USER}';"

echo "=== 完成 ==="