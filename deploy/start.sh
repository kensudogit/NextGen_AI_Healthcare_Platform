#!/bin/sh
set -e

BACKEND_PORT="${BACKEND_PORT:-8010}"
FRONTEND_PORT="${FRONTEND_PORT:-3010}"
export PORT="${PORT:-8080}"

mkdir -p /app/storage/dicom /tmp /var/log/nginx /var/lib/nginx

echo "Starting backend on ${BACKEND_PORT}..."
java -jar /app/app.jar --server.port="${BACKEND_PORT}" &
BACKEND_PID=$!

echo "Starting frontend on ${FRONTEND_PORT}..."
cd /app/frontend
export HOSTNAME=127.0.0.1
export PORT="${FRONTEND_PORT}"
export INTERNAL_API_URL="http://127.0.0.1:${BACKEND_PORT}"
node server.js &
FRONTEND_PID=$!

sleep 3

echo "Starting nginx on ${PORT}..."
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /tmp/nginx.conf
nginx -c /tmp/nginx.conf -g 'daemon off;' &
NGINX_PID=$!

trap 'kill $BACKEND_PID $FRONTEND_PID $NGINX_PID 2>/dev/null' TERM INT

wait $NGINX_PID
