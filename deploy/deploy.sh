#!/usr/bin/env bash
# Builds the server here and ships it to the VM: ./deploy/deploy.sh [ssh-host]   (default oracle-vm)
# The VM gets only the server's distribution (server + core/protocol + libraries), never app code.
# Keeps the previous release in /opt/liftly/previous for a quick rollback.
set -euo pipefail
host="${1:-oracle-vm}"
cd "$(dirname "$0")/.."
./gradlew -q :server:installDist
release="liftly-$(date +%Y%m%d-%H%M%S)"
# tar over ssh: macOS ships openrsync, which does not interoperate with every server rsync.
COPYFILE_DISABLE=1 tar --no-xattrs -C server/build/install/server -czf - . | ssh "$host" "mkdir -p /tmp/$release && tar -C /tmp/$release -xzf -"
ssh "$host" "set -e
  sudo rm -rf /opt/liftly/previous
  if [ -d /opt/liftly/current ]; then sudo mv /opt/liftly/current /opt/liftly/previous; fi
  sudo mv /tmp/$release /opt/liftly/current
  sudo chown -R root:root /opt/liftly/current
  sudo systemctl restart liftly
  for i in \$(seq 1 30); do curl -fsS http://127.0.0.1:8090/health >/dev/null 2>&1 && break; sleep 1; done
  curl -fsS http://127.0.0.1:8090/health && echo ' <- liftly is up'"
