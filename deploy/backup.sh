#!/usr/bin/env bash
# /opt/liftly/backup.sh — nightly pg_dump (cron as postgres), keeps 14 days in /var/backups/liftly.
# Restore: gunzip -c FILE | sudo -u postgres psql -d liftly   (into an empty database)
set -euo pipefail
dir=/var/backups/liftly
mkdir -p "$dir"
pg_dump --no-owner liftly | gzip > "$dir/liftly-$(date +%F).sql.gz"
find "$dir" -name 'liftly-*.sql.gz' -mtime +14 -delete
