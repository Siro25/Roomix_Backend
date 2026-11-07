#!/bin/sh
exec bash <<'ROOMIX_BOOTSTRAP'
set -Eeuo pipefail

exec > >(tee /var/log/roomix-bootstrap.log) 2>&1
export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y ca-certificates curl git openssl
curl -fsSL https://get.docker.com | sh
systemctl enable --now docker

install -d -m 0755 /opt/roomix
git clone --depth 1 https://github.com/Siro25/Roomix_Backend.git /opt/roomix/Roomix_Backend
git clone --depth 1 https://github.com/thienba33/Roomix.git /opt/roomix/Roomix

cat >/opt/roomix/Roomix/Dockerfile <<'EOF'
# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
FROM node:22-alpine AS builder
WORKDIR /app
ARG API_URL=http://api-gateway:8080
ENV API_URL=${API_URL}
COPY --from=dependencies /app/node_modules ./node_modules
COPY . .
RUN npm run build
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production HOSTNAME=0.0.0.0 PORT=3000
RUN addgroup -S roomix && adduser -S roomix -G roomix
COPY --from=builder --chown=roomix:roomix /app/public ./public
COPY --from=builder --chown=roomix:roomix /app/.next/standalone ./
COPY --from=builder --chown=roomix:roomix /app/.next/static ./.next/static
USER roomix
EXPOSE 3000
CMD ["node", "server.js"]
EOF

cat >/opt/roomix/Roomix/.dockerignore <<'EOF'
node_modules
.next
.git
.env*
npm-debug.log*
docs
tests
.tmp
EOF

python3 - <<'PY'
from pathlib import Path

config = Path('/opt/roomix/Roomix/next.config.mjs')
text = config.read_text()
text = text.replace('const nextConfig = {', "const nextConfig = {\n  output: 'standalone',", 1)
config.write_text(text)

notifications = Path('/opt/roomix/Roomix/src/services/backend/notifications.ts')
text = notifications.read_text()
text = text.replace(
    '`${protocol}//${window.location.hostname}:18080/api/ws`',
    '`${protocol}//${window.location.host}/api/ws`',
)
notifications.write_text(text)

dashboard = Path('/opt/roomix/Roomix/src/app/tenant/page.tsx')
text = dashboard.read_text()
text = text.replace(
    "  const pendingInvoices = invoices.filter((i) => i.status === 'PENDING' || i.overdue);\n"
    "  const totalPendingAmount = pendingInvoices.reduce((sum, i) => sum + (i.outstandingAmount || 0), 0);",
    "  const today = new Date().toISOString().slice(0, 10);\n"
    "  const pendingInvoices = invoices.filter((i) => i.status === 'PENDING' || (i.status !== 'PAID' && i.dueDate < today));\n"
    "  const totalPendingAmount = pendingInvoices.reduce((sum, i) => sum + Math.max(i.totalAmount - i.paidAmount, 0), 0);",
)
dashboard.write_text(text)
PY

cat >/opt/roomix/Roomix_Backend/Caddyfile <<'EOF'
:80 {
  encode zstd gzip
  @websocket path /api/ws*
  reverse_proxy @websocket api-gateway:8080
  reverse_proxy frontend:3000
}
EOF

cat >/opt/roomix/Roomix_Backend/compose.prod.yml <<'EOF'
services:
  kafka:
    environment:
      KAFKA_HEAP_OPTS: -Xms256m -Xmx768m
  core-service:
    environment:
      JAVA_TOOL_OPTIONS: -Xms128m -Xmx768m -XX:+UseG1GC
      REFRESH_TOKEN_COOKIE_SECURE: "false"
    healthcheck:
      start_period: 5m
  rental-service:
    environment:
      JAVA_TOOL_OPTIONS: -Xms128m -Xmx768m -XX:+UseG1GC
      SPRING_PROFILES_ACTIVE: prod
    healthcheck:
      start_period: 5m
  api-gateway:
    environment:
      JAVA_TOOL_OPTIONS: -Xms96m -Xmx512m -XX:+UseG1GC
  frontend:
    build:
      context: ../Roomix
      dockerfile: Dockerfile
      args:
        API_URL: http://api-gateway:8080
    environment:
      API_URL: http://api-gateway:8080
    depends_on:
      api-gateway:
        condition: service_started
    restart: unless-stopped
  caddy:
    image: caddy:2.10-alpine
    ports:
      - "80:80"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy-data:/data
      - caddy-config:/config
    depends_on:
      frontend:
        condition: service_started
    restart: unless-stopped
volumes:
  caddy-data:
  caddy-config:
EOF

cd /opt/roomix/Roomix_Backend
cat >.env <<EOF
ROOMIX_DB_PASSWORD=$(openssl rand -hex 24)
ROOMIX_JWT_SECRET=$(openssl rand -hex 48)
FACE_SERVICE_TOKEN=$(openssl rand -hex 48)
GATEWAY_ALLOWED_ORIGINS=http://localhost
JWT_EXPIRATION_SECONDS=900
REFRESH_TOKEN_EXPIRATION_SECONDS=2592000
REFRESH_TOKEN_COOKIE_SECURE=false
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax
CORE_TELEGRAM_WORKER_ENABLED=false
GEMINI_API_KEY=
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=
VNPAY_TMN_CODE=
VNPAY_HASH_SECRET=
VNPAY_RETURN_URL=
EOF
chmod 600 .env

docker compose -f compose.yml -f compose.prod.yml config --quiet
docker compose -f compose.yml -f compose.prod.yml --parallel 1 build
docker compose -f compose.yml -f compose.prod.yml up -d --no-build
ROOMIX_BOOTSTRAP
