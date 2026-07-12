#!/bin/bash
set -e        # 스크립트 실행 중 에러 발생 시 즉시 종료

PROJECT_ROOT="/home/ubuntu/app"
cd $PROJECT_ROOT
cp /home/ubuntu/config/.env ./.env

# Spring Boot 애플리케이션 교체
echo "> 기존 컨테이너 중지..."
docker compose down app || true

echo "> 새 컨테이너 빌드 및 실행..."
docker compose up -d --build app

echo "> 사용하지 않는 이미지 및 컨테이너 정리..."
docker image prune -f
docker container prune -f

echo "> 배포가 성공적으로 완료되었습니다!"
