# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY common/build.gradle common/build.gradle
COPY app/build.gradle app/build.gradle

RUN chmod +x ./gradlew
RUN --mount=type=secret,id=gh_token,required=false \
    --mount=type=secret,id=github_actor,required=false \
    if [ -f /run/secrets/gh_token ]; then export GH_TOKEN="$(cat /run/secrets/gh_token)"; fi; \
    if [ -f /run/secrets/github_actor ]; then export GITHUB_ACTOR="$(cat /run/secrets/github_actor)"; fi; \
    ./gradlew :app:dependencies --no-daemon >/dev/null

COPY common ./common
COPY app ./app

RUN --mount=type=secret,id=gh_token,required=false \
    --mount=type=secret,id=github_actor,required=false \
    if [ -f /run/secrets/gh_token ]; then export GH_TOKEN="$(cat /run/secrets/gh_token)"; fi; \
    if [ -f /run/secrets/github_actor ]; then export GITHUB_ACTOR="$(cat /run/secrets/github_actor)"; fi; \
    ./gradlew clean :app:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul
ENV SPRING_PROFILES_ACTIVE=dev

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
