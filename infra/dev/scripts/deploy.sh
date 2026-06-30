#!/bin/bash
PROJECT_ROOT="/home/ubuntu/app"
cd $PROJECT_ROOT
cp /home/ubuntu/config/.env ./.env

NGINX_CONTAINER_NAME="nginx-proxy"
NGINX_CONF_PATH="./nginx/service-env.inc"

# 1. 구동 중인 스프링 컨테이너 포트 확인 및 환경 변수 설정
echo "> 현재 구동 중인 스프링 애플리케이션 포트 확인..."
CURRENT_STATUS=$(curl -s -o /dev/null -2 "%{http_code}" http://172.18.0.1:8081/health)

if [ "$CURRENT_STATUS" == "OK" ]; then
    TARGET_PORT=8082
    TARGET_SERVICE="app-b"
    EXISTING_CONTAINER="spring-app-a"
    EXISTING_SERVICE="app-a"

else
    TARGET_PORT=8081
    TARGET_SERVICE="app-a"
    EXISTING_CONTAINER="spring-app-b"
    EXISTING_SERVICE="app-b"

fi

echo "> [$TARGET_SERVICE] ($TARGET_PORT 포트)로 새 버전을 빌드 및 배포합니다."

# 2. 새로운 이미지 빌드 및 컨테이너 실행
echo "> 새 컨테이너 빌드 및 실행..."
docker compose up -d --build $TARGET_SERVICE

# 3. 새 컨테이너 헬스체크
echo "> 새 컨테이너 헬스 체크 시작..."
for retry_count in {1..10}
do
    echo "> 헬스 체크 시도 ($retry_count/10)..."
    STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://172.18.0.1:$TARGET_PORT/health)

    if [ "$STATUS_CODE" -eq 200 ]; then
        echo "> 새 컨테이너 구동 성공 (HTTP 200)"
        break
    fi

    if [ $retry_count -eq 10 ]; then
        echo "> [ERROR] 새 컨테이너 구동 실패 혹은 헬스체크 타임아웃. 배포를 중단합니다."
        docker compose stop $TARGET_SERVICE
        exit 1
    fi

    sleep 5
done

# 4. Nginx 라우팅 스위칭
echo "> Nginx 라우팅 설정 변경..."
echo "set \$service_url http://172.18.0.1:$TARGET_PORT;" > $NGINX_CONF_PATH
docker exec -i $NGINX_CONTAINER_NAME sh -c "echo 'set \$service_url http://172.18.0.1:$TARGET_PORT;' > /etc/nginx/conf.d/service-env.inc"

# Nginx 설정 문법 검사 실행
docker exec -i $NGINX_CONTAINER_NAME nginx -t
if [ $? -eq 0 ]; then
    echo "> Nginx 문법 검사 통과. 변경된 설정 적용..."
    docker exec $NGINX_CONTAINER_NAME nginx -s reload
else
    echo "> [ERROR] Nginx 설정 파일에 문제가 있습니다. 배포를 롤백합니다."
    docker compose stop $TARGET_SERVICE
    exit 1
fi

# 5. 기존 컨테이너 종료 및 자원 정리
echo "> 구버전 컨테이너($EXISTING_CONTAINER) 종료..."
docker compose stop $EXISTING_SERVICE || true    # 첫 배포 시 spring-app-b가 존재하지 않음으로 인한  에러 방지
docker compose rm -f $EXISTING_SERVICE || true

# 6. 사용하지 않는 이미지 정리
echo "> 사용하지 않는 이미지 및 컨테이너 정리..."
docker image prune -f
docker container prune -f

echo "> 배포가 성공적으로 완료되었습니다!"
