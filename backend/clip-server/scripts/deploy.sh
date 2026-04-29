#!/bin/bash
PROJECT_ROOT="/home/ubuntu/app"
cd $PROJECT_ROOT

# 1. 기존에 실행 중인 컨테이너가 있다면 중지
# 처음 배포 시에는 컨테이너가 없으므로 에러를 무시하도록 || true 추가
echo "> 기존 컨테이너 중지..."
docker compose down || true

# 2. 새로운 이미지 빌드 및 컨테이너 실행
# --build: Dockerfile 변경 사항을 반영하여 이미지 새로 생성
# -d: 백그라운드 실행
echo "> 새 컨테이너 빌드 및 실행..."
docker compose up -d --build

# 3. 사용하지 않는(Dangling) 이미지 정리 (용량 확보)
echo "> 사용하지 않는 이미지 제거..."
docker image prune -f
