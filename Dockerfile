# syntax=docker/dockerfile:1.7

ARG SERVICE

FROM eclipse-temurin:21-jdk-alpine AS build
ARG SERVICE
WORKDIR /workspace
ENV MAVEN_OPTS="-Xmx768m -XX:MaxMetaspaceSize=256m"

COPY ${SERVICE}/mvnw ./mvnw
COPY ${SERVICE}/.mvn ./.mvn
COPY ${SERVICE}/pom.xml ./pom.xml

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw

COPY ${SERVICE}/src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S roomix \
    && adduser -S roomix -G roomix

WORKDIR /app
COPY --from=build --chown=roomix:roomix /workspace/target/*.jar ./application.jar

USER roomix
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
