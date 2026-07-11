# 1단계: 빌드 스테이지
FROM gradle:8-jdk17-alpine AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootjar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx1024m"

# 2단계: 최종 실행 이미지 (Ubuntu 20.04 기반)
FROM eclipse-temurin:17-jre-focal

# Azure Speech SDK 필수 네이티브 패키지
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ffmpeg \
    ca-certificates \
    libssl1.1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN mkdir -p /home/ubuntu/app/log

ENTRYPOINT ["java", "-jar", "app.jar"]