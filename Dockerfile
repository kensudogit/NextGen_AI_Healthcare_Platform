# NextGen Healthcare Platform — Railway 本番用
# フロントエンド + バックエンド + Nginx（deploy/ 外部 COPY 不要）

# --- Backend ---
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -q dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

# --- Frontend ---
FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json ./
RUN npm install
COPY frontend/ .
ENV NEXT_TELEMETRY_DISABLED=1
ENV INTERNAL_API_URL=http://127.0.0.1:8010
RUN npm run build

# --- Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache nginx gettext nodejs \
    && mkdir -p /app/storage/dicom /tmp /var/log/nginx /var/lib/nginx

COPY --from=backend-build /app/target/platform-api-1.0.0.jar app.jar
COPY --from=frontend-build /app/.next/standalone ./frontend
COPY --from=frontend-build /app/.next/static ./frontend/.next/static
COPY --from=frontend-build /app/public ./frontend/public
COPY PACS_EXPORT_SAMPLE /app/pacs-export

# Nginx 設定（Dockerfile 内生成 — Railway ビルドで deploy/ 欠落を防止）
RUN cat > /etc/nginx/nginx.conf.template <<'EOF'
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /tmp/nginx.pid;
events { worker_connections 1024; }
http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    sendfile on;
    keepalive_timeout 65;
    client_max_body_size 100m;
    upstream backend { server 127.0.0.1:8010; }
    upstream frontend { server 127.0.0.1:3010; }
    server {
        listen ${PORT} default_server;
        listen [::]:${PORT} default_server;
        server_name _;
        location /api/ {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        location /fhir/R4/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        location = /fhir/ {
            return 301 /fhir;
        }
        location /swagger-ui {
            proxy_pass http://backend;
            proxy_set_header Host $host;
        }
        location /swagger-ui.html {
            proxy_pass http://backend;
            proxy_set_header Host $host;
        }
        location /v3/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
        }
        location /health {
            proxy_pass http://backend;
            proxy_set_header Host $host;
        }
        location / {
            proxy_pass http://frontend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
EOF

RUN cat > /start.sh <<'EOF'
#!/bin/sh
set -e
RAILWAY_PORT="${PORT:-8080}"
BACKEND_PORT="${BACKEND_PORT:-8010}"
FRONTEND_PORT="${FRONTEND_PORT:-3010}"
mkdir -p /app/storage/dicom /tmp /var/log/nginx /var/lib/nginx
if [ -n "$RAILWAY_PUBLIC_DOMAIN" ]; then
  export FHIR_BASE_URL="https://${RAILWAY_PUBLIC_DOMAIN}/fhir/R4"
  export CORS_ORIGINS="https://${RAILWAY_PUBLIC_DOMAIN}"
  echo "Railway public URL: https://${RAILWAY_PUBLIC_DOMAIN}"
fi
echo "Starting backend on ${BACKEND_PORT}..."
java -jar /app/app.jar --server.port="${BACKEND_PORT}" &
BACKEND_PID=$!
echo "Starting frontend on ${FRONTEND_PORT}..."
cd /app/frontend
HOSTNAME=127.0.0.1 PORT="${FRONTEND_PORT}" INTERNAL_API_URL="http://127.0.0.1:${BACKEND_PORT}" node server.js &
FRONTEND_PID=$!
sleep 5
echo "Starting nginx on ${RAILWAY_PORT}..."
export PORT="${RAILWAY_PORT}"
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /tmp/nginx.conf
nginx -c /tmp/nginx.conf -g 'daemon off;' &
NGINX_PID=$!
trap 'kill $BACKEND_PID $FRONTEND_PID $NGINX_PID 2>/dev/null' TERM INT
wait $NGINX_PID
EOF
RUN chmod +x /start.sh

ENV BACKEND_PORT=8010
ENV FRONTEND_PORT=3010
ENV INTERNAL_API_URL=http://127.0.0.1:8010
ENV PACS_EXPORT_PATH=/app/pacs-export
ENV PORT=8080

EXPOSE 8080
CMD ["/start.sh"]
