FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY domain ./domain
COPY app ./app

RUN chmod +x ./gradlew && ./gradlew clean :app:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul
ENV SPRING_PROFILES_ACTIVE=dev

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]