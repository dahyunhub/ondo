# syntax=docker/dockerfile:1

### build stage — Gradle 로 bootJar 생성(테스트 제외; 테스트는 CI/로컬에서)
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# 의존성 레이어 캐시: 빌드 설정 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies >/dev/null 2>&1 || true
# 소스 복사 후 jar 빌드
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

### runtime stage — JRE 만
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
# 헬스체크용 curl
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# 비루트 사용자로 실행(보안)
RUN useradd --system --uid 1001 appuser
COPY --from=build --chown=appuser:root /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
