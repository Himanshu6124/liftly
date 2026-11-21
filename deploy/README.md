# Deploying the Liftly server

Production runs on a small Oracle Cloud VM (`oracle-vm` in `~/.ssh/config`, 1 GB RAM, x86, Ubuntu
24.04) that also hosts another app. To fit, there is **no Docker**: PostgreSQL from Ubuntu, the
server as a systemd service, and the VM's existing **nginx + certbot** in front.

```
App ──HTTPS──▶ nginx (liftly-api.duckdns.org, Let's Encrypt) ──▶ 127.0.0.1:8090 liftly.service ──▶ PostgreSQL 16 (localhost)
```

| Piece | Where |
|---|---|
| Server build | `/opt/liftly/current` (previous release in `/opt/liftly/previous`) |
| Settings + secrets | `/etc/liftly/liftly.env` (root:liftly 0640), shape in `liftly.env.example` |
| Service | `/etc/systemd/system/liftly.service` (heap 160 MB, SerialGC) |
| Database | PostgreSQL 16, db/user `liftly`, tuned by `postgres-small.conf` |
| Proxy | `/etc/nginx/sites-available/liftly` + `/etc/nginx/liftly-proxy.conf` |
| Backups | `/opt/liftly/backup.sh`, cron 03:30 daily → `/var/backups/liftly`, 14 days |

## Ship a new version

```bash
./deploy/deploy.sh
```

Builds `:server:installDist` here (any Gradle build in this repo needs the Android SDK), copies only
the server distribution over SSH, keeps the old release, restarts and waits for `/health`.
Database migrations (Flyway) run on start.

Rollback:

```bash
ssh oracle-vm 'sudo mv /opt/liftly/current /opt/liftly/broken && sudo mv /opt/liftly/previous /opt/liftly/current && sudo systemctl restart liftly'
```

## Change settings (e.g. Google client ids)

```bash
ssh oracle-vm 'sudoedit /etc/liftly/liftly.env && sudo systemctl restart liftly'
```

`GOOGLE_AUDIENCES` takes the Web (Android) and iOS OAuth client ids, comma-separated.
Rotating `JWT_SECRET` signs every phone out of its current access token (they refresh silently).

## Look at it

```bash
ssh oracle-vm 'systemctl status liftly --no-pager; sudo journalctl -u liftly -n 50 --no-pager'
curl https://liftly-api.duckdns.org/health
```

## First-time setup (already done on oracle-vm; for a new machine)

1. `sudo apt-get install -y postgresql nginx certbot python3-certbot-nginx openjdk-21-jre-headless`
2. Copy `postgres-small.conf` to `/etc/postgresql/16/main/conf.d/` and restart PostgreSQL.
3. `sudo useradd --system --home-dir /opt/liftly --no-create-home --shell /usr/sbin/nologin liftly`,
   create role and database `liftly` with a random password, and write `/etc/liftly/liftly.env`
   from `liftly.env.example` with `openssl rand` secrets. Generate secrets **on the server**.
4. Install `liftly.service`, `backup.sh` (+ `/etc/cron.d/liftly-backup`), `systemctl enable liftly`,
   then `./deploy/deploy.sh`.
5. Point a DNS name at the VM, install `nginx-liftly.conf` (replace `LIFTLY_DOMAIN`) and
   `nginx-liftly-proxy.conf`, `sudo nginx -t && sudo systemctl reload nginx`, then
   `sudo certbot --nginx -d <domain> --redirect`.
6. Ports 80/443 must be open in both the OCI security list and the VM's iptables. Port 8090 stays
   closed: the server listens on 127.0.0.1 and trusts nginx's `X-Forwarded-For` for rate limits.

## Notes for this 1 GB VM

- Memory: Liftly ~165 MB RSS, PostgreSQL ~50 MB; the other app ~165 MB; 2 GB swap.
- Backups stay on the VM's disk. Copying them off the machine (OCI Object Storage) is still to do.
- The Docker files in `server/` remain for machines with more room.
