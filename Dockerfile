FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
ENV ORG_GRADLE_PROJECT_githubUsername=${GITHUB_ACTOR}
ENV ORG_GRADLE_PROJECT_githubToken=${GITHUB_TOKEN}

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

COPY core ./core
COPY api ./api

RUN chmod +x ./gradlew \ && ./gradlew clean :api:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/api/build/libs/*.jar app.jar

EXPOSE 8080
ENV TZ=Asia/Seoul
ENV SPRING_PROFILES_ACTIVE=local

ENTRYPOINT ["java","-jar","/app/app.jar"]
