#!/usr/bin/env bash
# =============================================================
# 每日 mysqldump 备份 xxs_game 数据库
# 用法（cron）：0 3 * * * /opt/xxs/xxs-backup.sh
# 保留最近 30 天备份
# =============================================================
set -euo pipefail

BACKUP_DIR="/opt/xxs/backups"
DB_NAME="xxs_game"
DB_USER="root"
RETENTION_DAYS=30

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

mysqldump -u"$DB_USER" "$DB_NAME" 2>/dev/null | gzip > "$FILENAME"

# 清理超过保留期的旧备份
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +${RETENTION_DAYS} -delete

echo "$(date '+%Y-%m-%d %H:%M:%S') 备份完成: $FILENAME ($(du -h "$FILENAME" | cut -f1))"
