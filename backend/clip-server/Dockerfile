# 1단계: 의존성 라이브러리 캐싱
FROM gradle:8-jdk17-alpine AS deps
WORKDIR /app
COPY build.gradle settings.gradle /app/
RUN gradle build -x test --no-daemon > /dev/null 2>&1 || true

# 2단계: 애플리케이션 빌드
FROM gradle:8-jdk17-alpine AS builder
WORKDIR /app
COPY --from=deps /home/gradle/.gradle /home/gradle/.gradle
COPY . .
RUN gradle bootJar --no-daemon


# 3단계: 최종 실행 이미지 (Ubuntu 20.04 기반)
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