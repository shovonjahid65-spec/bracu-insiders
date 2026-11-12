#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v docker >/dev/null || { echo "Install and start Docker with the Compose plugin first."; exit 1; }
docker compose version >/dev/null
if [[ ! -f .env ]]; then
  command -v openssl >/dev/null || { echo "OpenSSL is required for first-run password generation."; exit 1; }
  umask 077
  {
    echo "DB_ADMIN_PASSWORD=$(openssl rand -hex 32)"
    echo "DB_PASSWORD=$(openssl rand -hex 32)"
    echo "ADMIN_USERNAME=editor"
    echo "ADMIN_PASSWORD=$(openssl rand -hex 24)"
    echo "HTTP_PORT=8080"
    echo "COOKIE_SECURE=false"
    echo "SEED_EDITION=true"
  } > .env
fi
docker compose up --build -d --wait
port="$(sed -n 's/^HTTP_PORT=//p' .env | head -1)"
echo "Website: http://localhost:${port:-8080}/"
echo "Dashboard: http://localhost:${port:-8080}/admin"
echo "Your first editor username and password are in .env."
