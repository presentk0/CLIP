#!/bin/bash
set -e      # 스크립트 실행 중 에러 발생 시 즉시 종료

PROJECT_ROOT="/home/ubuntu/app"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yaml"
NGINX_CONFIG="$PROJECT_ROOT/nginx/nginx.conf"
NGINX_DEFAULT_CONFIG="$PROJECT_ROOT/nginx/default.conf"
CHECKSUM_DIR="/home/ubuntu/.checksums"

cd $PROJECT_ROOT
cp /home/ubuntu/config/.env ./.env

# Nginx 상태 확인 및 재시작
NEED_NGINX_RESTART=false
NEED_NGINX_RELOAD=false

echo "> Nginx 상태 확인 및 설정 변경 사항 감지..."
IS_NGINX_RUNNING=$(docker ps -q --filter "name=nginx-proxy" --filter "status=running")
CURRENT_NGINX_COMPOSE_HASH=$(docker compose config --format json | jq '.services.nginx' | md5sum | awk '{print $1}')
OLD_NGINX_COMPOSE_HASH=$(cat "$CHECKSUM_DIR/nginx_compose.hash" 2>/dev/null || echo "")

if [ -z "$IS_NGINX_RUNNING" ]; then
    echo "> Nginx 컨테이너가 실행 중이 아닙니다. Nginx를 새로 시작합니다."
    NEED_NGINX_RESTART=true
elif [ $CURRENT_NGINX_COMPOSE_HASH != "$OLD_NGINX_COMPOSE_HASH" ]; then
    echo "> Nginx 서비스 설정 변경 감지. Nginx를 재시작합니다."
    NEED_NGINX_RESTART=true
elif
    CURRENT_CONF_HASH=$(find . -name "*.conf" -type f -exec md5sum {} + | sort | md5sum | awk '{print $1}')
    OLD_CONF_HASH=$(cat "$CHECKSUM_DIR/nginx_conf.hash" 2>/dev/null || echo "")
    
    if [ "$CURRENT_CONF_HASH" != "$OLD_CONF_HASH" ]; then
        echo "> Nginx 설정 파일(*.conf) 변경이 발견되었습니다. 무중단 Reload를 수행합니다."
        NEED_NGINX_RELOAD=true
    fi
fi

if [ "$NEED_NGINX_RESTART" = true ]; then
    docker compose down nginx-proxy || true
    docker compose up -d nginx-proxy
elif [ "$NEED_NGINX_RELOAD" = true ]; then
    docker exec nginx-proxy nginx -s reload
fi

# Metabase 상태 확인 및 재시작
NEED_METABASE_RESTART=false

echo "> Metabase 상태 확인 중..."
IS_METABASE_RUNNING=$(docker ps -q --filter "name=metabase" --filter "status=running")
CURRENT_METABASE_COMPOSE_HASH=$(docker compose config --format json | jq '.services.metabase' | md5sum | awk '{print $1}')
OLD_METABASE_COMPOSE_HASH=$(cat "$CHECKSUM_DIR/metabase_compose.hash" 2>/dev/null || echo "")

if [ -z "$IS_METABASE_RUNNING" ]; then
    echo "> Metabase 컨테이너가 실행 중이 아닙니다. Metabase를 새로 시작합니다."
    NEED_METABASE_RESTART=true
elif [ $CURRENT_METABASE_COMPOSE_HASH != "$OLD_METABASE_COMPOSE_HASH" ]; then
    echo "> Metabase 서비스 설정 변경 감지. Metabase를 새로 시작합니다."
    NEED_METABASE_RESTART=true
fi

if [ "$NEED_METABASE_RESTART" = true ]; then
    docker compose down metabase || true
    docker compose up -d metabase
fi

echo "$CURRENT_NGINX_COMPOSE_HASH" > "$CHECKSUM_DIR/nginx_compose.hash"
echo "$CURRENT_CONF_HASH" > "$CHECKSUM_DIR/nginx_conf.hash"

# Spring Boot 애플리케이션 교체
echo "> 기존 컨테이너 중지..."
docker compose down app || true

echo "> 새 컨테이너 빌드 및 실행..."
docker compose up -d --build app

echo "> 사용하지 않는 이미지 및 컨테이너 정리..."
docker image prune -f
docker container prune -f

echo "> 배포가 성공적으로 완료되었습니다!"

