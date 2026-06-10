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


#  3단계: 최종 실행 이미지
FROM eclipse-temurin:17-jre

# 데비안/우분투 계열 패키지 매니저(apt)로 curl과 ffmpeg 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 빌더 단계에서 컴파일과 원격 라이브러리 다운로드가 완료된 jar 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 로드 디렉토리 생성 및 권한 설정
RUN mkdir -p /home/ubuntu/app/log

ENTRYPOINT ["java", "-jar", "app.jar"]
