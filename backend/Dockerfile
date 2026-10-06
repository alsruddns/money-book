FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 moneybook \
    && useradd --system --uid 10001 --gid moneybook --home-dir /app --shell /usr/sbin/nologin moneybook

WORKDIR /app
COPY --from=build --chown=moneybook:moneybook /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS=""

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8080/api/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
