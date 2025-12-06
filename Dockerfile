# 1단계: 빌드용 이미지 (Gradle로 bootJar 빌드)
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

# Gradle 관련 파일 먼저 복사 (캐시 활용)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# 소스 코드 복사
COPY src ./src

# Gradle 실행 권한 부여 후 빌드
RUN chmod +x ./gradlew \
    && ./gradlew clean bootJar -x test

# 2단계: 실행용 이미지 (JRE만 포함, 가볍게)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# builder 단계에서 만든 JAR 복사
COPY --from=builder /workspace/build/libs/*.jar app.jar

# 컨테이너 내부 포트
EXPOSE 8080

# 시간대 맞추고 싶으면 (옵션)
ENV TZ=Asia/Seoul

# Spring 프로필 기본값(local) 지정 (원하면 docker-compose에서 덮어써도 됨)
ENV SPRING_PROFILES_ACTIVE=local

ENTRYPOINT ["java", "-jar", "app.jar"]
