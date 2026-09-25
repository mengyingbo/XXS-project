#!/usr/bin/env bash
# =============================================================
# 允许局域网访问 MySQL（开发期本机后端需连接服务器数据库）
# 用法（需 root）：sudo bash enable_mysql_lan.sh
# 变更点：/etc/mysql/mysql.conf.d/mysqld.cnf 的 bind-address 改为 0.0.0.0
# =============================================================
set -euo pipefail

CONF=/etc/mysql/mysql.conf.d/mysqld.cnf
BACKUP="${CONF}.bak.$(date +%Y%m%d%H%M%S)"

cp "$CONF" "$BACKUP"
echo "已备份原配置到 $BACKUP"

sed -i 's/^bind-address.*/bind-address = 0.0.0.0/' "$CONF"

systemctl restart mysql
sleep 4

echo "=== mysql 服务状态 ==="
systemctl is-active mysql

echo "=== 当前 bind-address ==="
grep -n '^bind-address' "$CONF"

echo "=== 监听端口 ==="
ss -tln | grep 3306 || true

echo "=== 完成（如需回滚：cp $BACKUP $CONF && systemctl restart mysql）==="