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
