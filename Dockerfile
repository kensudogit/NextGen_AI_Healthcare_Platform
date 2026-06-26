# NextGen Healthcare Platform — Railway 本番用
# フロントエンド (Next.js) + バックエンド (Spring Boot) + Nginx リバースプロキシ

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
# 同一オリジン — 相対パスで API 呼び出し
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

COPY deploy/nginx.conf.template /etc/nginx/nginx.conf.template
COPY deploy/start.sh /start.sh
RUN chmod +x /start.sh

ENV BACKEND_PORT=8010
ENV FRONTEND_PORT=3010
ENV INTERNAL_API_URL=http://127.0.0.1:8010
ENV PORT=8080

EXPOSE 8080
CMD ["/start.sh"]
